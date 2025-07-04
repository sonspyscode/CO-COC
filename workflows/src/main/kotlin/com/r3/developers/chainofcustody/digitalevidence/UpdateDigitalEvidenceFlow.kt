/*
RequestBody for triggering the flow via REST:
{
  "clientRequestId": "updateDE-01",
    "flowClassName": "com.r3.developers.chainofcustody.digitalevidence.UpdateDigitalEvidenceFlow",
    "requestBody": {
        "id":"70f6d6fb-350e-488e-a346-86a0c0b8fcba",
        "registerNumber":"DE-01821398",
        "typeDE":"flashdisk",
        "modelDE":"turbo",
        "manufacturerDE":"nikon",
        "serialNumber":"nikon-01821379823",
        "seizureReason":"file yang berisi informasi berita hoax",
        "caseID":"CC-001"
        }
}
 */

package com.r3.developers.chainofcustody.digitalevidence

import com.r3.developers.chainofcustody.contracts.DigitalEvidenceContract
import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import com.r3.developers.chainofcustody.states.CustodyInteraction
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

// A class to hold the deserialized arguments required to start the flow.
data class UpdateDigitalEvidenceFlowArgs(val id: UUID, val registerNumber: String,
                                         val typeDE: String, val modelDE: String,
                                         val manufacturerDE: String, val serialNumber: String, val seizureReason: String,
                                         val caseID: String)


// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class UpdateDigitalEvidenceFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("UpdateDigitalEvidenceFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateDigitalEvidenceFlowArgs::class.java)

            // Look up the latest unconsumed ChatState with the given id.
            // Note, this code brings all unconsumed states back, then filters them.
            // This is an inefficient way to perform this operation when there are a large number of chats.
            // Note, you will get this error if you input an id which has no corresponding ChatState (common error).
            val stateAndRef = ledgerService.findUnconsumedStatesByExactType(DigitalEvidenceState::class.java, 100, Instant.now()).results.singleOrNull {
                it.state.contractState.id == flowArgs.id
            } ?: throw CordaRuntimeException("Multiple or zero Digital Evidence states with id ${flowArgs.id} found.")

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState
            val participantsKey = state.participants
            val allMembers = participantsKey.map { key ->
                memberLookup.lookup(key) ?: throw CordaRuntimeException("Member not found from public key: $key")
            }

            // Daftar organisasi atau role yang diizinkan membuat Digital Evidence
            val allowedCommonName = "Custodian"
            val allowedOrgs = listOf("Org1", "Org3")

            // Validasi hanya role dan organisasi tertentu yang diizinkan
            if (myInfo.name.commonName != allowedCommonName && myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only members from ${allowedOrgs.joinToString()} are allowed to add Evidence Pack in Case Report.")
            }

            val otherMembers = allMembers.filter { it.name != myInfo.name }
            val parties = otherMembers.map { it.name }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Evidence-Report",
                officerName = myInfo.name,
                interaction = "UPDATE",
                timestamp = Instant.now()
            )

            // Create a new ChatState using the updateMessage helper function.
            val newDigitalEvidenceState = state.updateDigitalEvidence(registerNumber = flowArgs.registerNumber, typeDE = flowArgs.typeDE,
                modelDE = flowArgs.modelDE, manufacturerDE = flowArgs.manufacturerDE, serialNumber = flowArgs.serialNumber,
                seizureReason = flowArgs.seizureReason, caseID = flowArgs.caseID, custodyHistory = state.custodyHistory + custodyInteraction)

            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newDigitalEvidenceState)
                .addInputState(stateAndRef.ref)
                .addCommand(DigitalEvidenceContract.Update())
                .addSignatories(newDigitalEvidenceState.participants)

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