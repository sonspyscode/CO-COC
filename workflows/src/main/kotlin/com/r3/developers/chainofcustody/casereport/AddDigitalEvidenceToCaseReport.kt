/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "AddEvidenceToCaseReport-${__UUID}",
    "flowClassName": "com.r3.developers.chainofcustody.casereport.AddDigitalEvidenceToCaseReportFlow",
    "requestBody": {
        "idCase":"a2829222-7ade-4e78-8111-86e55fa5f3de",
        "digitalEvidencePack": ["e0bea357-471a-4d3f-b88f-273780ad6c43"]
        }
}
 */

package com.r3.developers.chainofcustody.casereport

import com.r3.developers.chainofcustody.contracts.CaseReportContract
import com.r3.developers.chainofcustody.states.CaseReportState
import com.r3.developers.chainofcustody.states.CustodyInteraction
import com.r3.developers.chainofcustody.states.DigitalEvidenceState
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
data class AddDigitalEvidenceToCaseReportFlowArgs(
    val idCase: UUID,
    val digitalEvidencePack: List<UUID>
)

class AddDigitalEvidenceToCaseReportFlow : ClientStartableFlow {
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
        log.info("AddDigitalEvidenceToCaseReportFlow.call() called")

        try {
            // Deserialisasi input
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, AddDigitalEvidenceToCaseReportFlowArgs::class.java)

            val stateAndRef = ledgerService.findUnconsumedStatesByExactType(CaseReportState::class.java, 100, Instant.now()).results.singleOrNull {
                it.state.contractState.idCase == flowArgs.idCase
            } ?: throw CordaRuntimeException("Multiple or zero Case Report states with id ${flowArgs.idCase} found.")

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState

            val evidenceRef = flowArgs.digitalEvidencePack.map { uuid ->
                ledgerService.findUnconsumedStatesByExactType(DigitalEvidenceState::class.java, 100, Instant.now())
                    .results.singleOrNull { it.state.contractState.id == uuid }
                    ?: throw CordaRuntimeException ("Evidence with ID $uuid not found")
            }

            // Daftar organisasi atau role yang diizinkan membuat Digital Evidence
            val allowedCommonName = "Custodian"
            val allowedOrgs = listOf("Org1", "Org3")

            // Validasi hanya role dan organisasi tertentu yang diizinkan
            if (myInfo.name.commonName != allowedCommonName || myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only $allowedCommonName from ${allowedOrgs.joinToString()} are allowed to add Evidence Pack in Case Report.")
            }

            // Pendefinisian untuk semua partisipan selain inisiator flow
            val participantsKey = state.participants
            val allMembers = participantsKey.map { key ->
                memberLookup.lookup(key) ?: throw CordaRuntimeException("Member not found from public key: $key")
            }

            val otherMembers = allMembers.filter { it.name != myInfo.name }
            val parties = otherMembers.map { it.name }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Case-Report",
                officerName = myInfo.name,
                interaction = "Add Digital Evidence ${flowArgs.digitalEvidencePack} to case report with ID ${flowArgs.idCase}",
                timestamp = Instant.now()
            )

            // Tambahkan lab report (reference)
            val newCaseReportState = state.addDigitalEvidenceToCaseReport(
                digitalEvidencePack = state.digitalEvidencePack + flowArgs.digitalEvidencePack,
                custodyHistory = state.custodyHistory + custodyInteraction)

// Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newCaseReportState)
                .addInputState(stateAndRef.ref)
                .addCommand(CaseReportContract.AddEvidencePack())
                .addSignatories(newCaseReportState.participants)
            evidenceRef.forEach { txBuilder.addReferenceState(it.ref) }

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeCaseReportSubFlow(signedTransaction, parties))


        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}
