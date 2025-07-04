/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "transferDE-1",
    "flowClassName": "com.r3.developers.chainofcustody.digitalevidence.AddLabReportToEvidenceFlow",
    "requestBody": {
        "id":"identifier untuk suatu digital evidence",
        "labReportRefs":"MemberX500Name"
        }
}
 */

package com.r3.developers.chainofcustody.digitalevidence

import com.r3.developers.chainofcustody.contracts.DigitalEvidenceContract
import com.r3.developers.chainofcustody.states.AnalysisReportState
import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import com.r3.developers.chainofcustody.states.CustodyInteraction
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

@CordaSerializable
data class AddLabReportToEvidenceFlowArgs(
    val id: UUID,
    val labReport: List<UUID>
)

class AddLabReportToEvidenceFlow : ClientStartableFlow {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("AddLabReportToEvidenceFlow.call() called")

        try {
            // Deserialisasi input
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, AddLabReportToEvidenceFlowArgs::class.java)

            val stateAndRef = ledgerService.findUnconsumedStatesByExactType(DigitalEvidenceState::class.java, 100, Instant.now()).results.singleOrNull {
                it.state.contractState.id == flowArgs.id
            } ?: throw CordaRuntimeException("Multiple or zero Digital Evidence states with id ${flowArgs.id} found.")

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState

            val analysisReportRef = flowArgs.labReport.map { uuid ->
                ledgerService.findUnconsumedStatesByExactType(AnalysisReportState::class.java, 100, Instant.now())
                    .results.singleOrNull { it.state.contractState.idReport == uuid }
                    ?: throw CordaRuntimeException("Analysis Report wit ID $uuid not found")
            }

            // Daftar organisasi atau role yang diizinkan membuat Digital Evidence
            val allowedCommonName = "Custodian"
            val allowedOrgs = listOf("Org1", "Org3")

            // Validasi hanya role dan organisasi tertentu yang diizinkan
            if (myInfo.name.commonName != allowedCommonName && myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only members from ${allowedOrgs.joinToString()} are allowed to add Evidence Pack in Case Report.")
            }

            // Pendefinisian untuk semua partisipan selain inisiator flow
            val participantsKey = state.participants
            val allMembers = participantsKey.map { key ->
                memberLookup.lookup(key) ?: throw CordaRuntimeException("Member not found from public key: $key")
            }

            val otherMembers = allMembers.filter { it.name != myInfo.name }
            val parties = otherMembers.map { it.name }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Evidence-Report",
                officerName = myInfo.name,
                interaction = "Add Analysis Report ${flowArgs.labReport} to ${flowArgs.id}",
                timestamp = Instant.now()
            )

            // Tambahkan lab report (reference)
            val newDigitalEvidenceState = state.addLabReportToEvidence(
                labReport = state.labReport + flowArgs.labReport,
                custodyHistory = state.custodyHistory + custodyInteraction)

// Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newDigitalEvidenceState)
                .addInputState(stateAndRef.ref)
                .addCommand(DigitalEvidenceContract.AddLabReport())
                .addSignatories(newDigitalEvidenceState.participants)
            analysisReportRef.forEach { txBuilder.addReferenceState(it.ref)}

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeDigitalEvidenceSubFlow(signedTransaction, parties))


        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}
