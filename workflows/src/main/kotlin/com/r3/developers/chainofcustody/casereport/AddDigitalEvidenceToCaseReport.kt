/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "transferDE-1",
    "flowClassName": "com.r3.developers.chainofcustody.casereport.AddDigitalEvidenceToCaseReportFlow",
    "requestBody": {
        "id":"identifier untuk suatu digital evidence",
        "digitalEvidencePack":"MemberX500Name"
        }
}
 */

package com.r3.developers.chainofcustody.casereport

import com.r3.developers.chainofcustody.contracts.DigitalEvidenceContract
import com.r3.developers.chainofcustody.states.CaseReportState
import com.r3.developers.chainofcustody.states.CustodyInteraction
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.StateRef
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

@CordaSerializable
data class AddDigitalEvidenceToCaseReportFlowArgs(
    val id: UUID,
    val digitalEvidencePack: List<StateRef>
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
                it.state.contractState.idCase == flowArgs.id
            } ?: throw CordaRuntimeException("Multiple or zero Digital Evidence states with id ${flowArgs.id} found.")

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState

            val members = state.participants.map {
                memberLookup.lookup(it) ?: throw CordaRuntimeException("Member not found from public key $it.")}
            val otherMember = (members - myInfo).singleOrNull()
                ?: throw CordaRuntimeException("Should be only one participant other than the initiator.")

            val custodyTracker = CustodyInteraction (
                typeReport = "Case-Report",
                officerName = myInfo.name,
                interaction = "Add Digital Evidence ${flowArgs.digitalEvidencePack} to case report with ID ${flowArgs.id}",
                timestamp = Instant.now()
            )

            val updateTracker = listOf(custodyTracker)

            // Tambahkan lab report (reference)
            val newCaseReportState = state.addDigitalEvidenceToCaseReport(
                flowArgs.digitalEvidencePack, custodyHistory = updateTracker)

// Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newCaseReportState)
                .addInputState(stateAndRef.ref)
                .addCommand(DigitalEvidenceContract.AddLabReport())
                .addSignatories(newCaseReportState.participants)

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeCaseReportSubFlow(signedTransaction, otherMember.name))


        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}
