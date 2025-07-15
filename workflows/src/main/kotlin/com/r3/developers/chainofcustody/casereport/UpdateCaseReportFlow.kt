/*
RequestBody for triggering the flow via REST:
{
  "clientRequestId": "updateCR-01",
    "flowClassName": "com.r3.developers.chainofcustody.casereport.UpdateCaseReportFlow",
    "requestBody": {
        "idCase": "f7dbb529-6655-419f-9871-c75f46b0c593",
        "caseNumber": "CNum-001x",
        "caseName": "CN-001",
        "suspectName": "Alucard dan Lapulapu",
        "victimName": "Hylos",
        "locationCase": "BTN Land of Dawn",
        "dateNtime": "01/01/2020 12:12:00",
        "toolName": "FTK Imager",
        "toolDesc": "Alat akusisi",
        "statusCase": "ACTIVE"
        }
}
 */

package com.r3.developers.chainofcustody.casereport

import com.r3.developers.chainofcustody.contracts.CaseReportContract
import com.r3.developers.chainofcustody.states.CaseReportState
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
data class UpdateCaseReportFlowArgs(val idCase: UUID, val caseNumber: String,
                                         val caseName: String, val suspectName: String,
                                            val victimName: String, val locationCase: String,
                                                val dateNtime: String, val toolName: String,
                                                    val toolDesc: String, val statusCase: String)


// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class UpdateCaseReportFlow: ClientStartableFlow {

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

        log.info("UpdateCaseReportFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateCaseReportFlowArgs::class.java)

            // Look up the latest unconsumed ChatState with the given id.
            // Note, this code brings all unconsumed states back, then filters them.
            // This is an inefficient way to perform this operation when there are a large number of chats.
            // Note, you will get this error if you input an id which has no corresponding ChatState (common error).
            val stateAndRef = ledgerService.findUnconsumedStatesByExactType(CaseReportState::class.java, 100, Instant.now()).results.singleOrNull {
                it.state.contractState.idCase == flowArgs.idCase
            } ?: throw CordaRuntimeException("Multiple or zero Digital Evidence states with id ${flowArgs.idCase} found.")

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
            if (myInfo.name.commonName != allowedCommonName || myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only $allowedCommonName from ${allowedOrgs.joinToString()} are allowed to update this report.")
            }

            val otherMembers = allMembers.filter { it.name != myInfo.name }
            val parties = otherMembers.map { it.name }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Case-Report",
                officerName = myInfo.name,
                interaction = "UPDATE case report by ${myInfo.name}",
                timestamp = Instant.now()
            )

            // Create a new ChatState using the updateMessage helper function.
            val newCaseReportState = state.updateCaseReport(
                caseName = flowArgs.caseName, suspectName = flowArgs.suspectName,
                victimName = flowArgs.victimName, locationCase = flowArgs.locationCase,
                dateNtime = flowArgs.dateNtime, toolName = flowArgs.toolName,
                toolDesc = flowArgs.toolDesc, statusCase = flowArgs.statusCase,
                custodyHistory = state.custodyHistory + custodyInteraction)

            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newCaseReportState)
                .addInputState(stateAndRef.ref)
                .addCommand(CaseReportContract.Update())
                .addSignatories(newCaseReportState.participants)

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