/*
RequestBody for triggering the flow via REST:
{
  "clientRequestId": "createDE-01",
    "flowClassName": "com.r3.developers.chainofcustody.analysisreport.UpdateAnalysisReport",
    "requestBody": {
        "idReport":"identifier suatu laporan"
        "fileName":"DE-01821398",
        "fileSize":"flashdisk",
        "hashSHA1":"lite",
        "hashMD5":"Sundis",
        "sourceFile":"Sundis-01821379823",
        "fileLocation":"file yang berisi informasi yang diduga berita hoax",
        "potentialInfo":"CC-001""
   }
}
 */

package com.r3.developers.chainofcustody.analysisreport

import com.r3.developers.chainofcustody.contracts.AnalysisReportContract
import com.r3.developers.chainofcustody.states.AnalysisReportState
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
data class UpdateAnalysisReportFlowArgs(val idReport: UUID,
                                    val fileName: String, val fileSize: Long,
                                    val hashSHA1: String, val hashMD5: String,
                                    val sourceFile: String, val fileLocation: String,
                                    val potentialInfo: String)


// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class UpdateAnalysisReportFlow: ClientStartableFlow {

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

        log.info("UpdateAnalysisReportFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateAnalysisReportFlowArgs::class.java)

            // Look up the latest unconsumed ChatState with the given id.
            // Note, this code brings all unconsumed states back, then filters them.
            // This is an inefficient way to perform this operation when there are a large number of chats.
            // Note, you will get this error if you input an id which has no corresponding ChatState (common error).
            val stateAndRef = ledgerService.findUnconsumedStatesByExactType(AnalysisReportState::class.java, 100, Instant.now()).results.singleOrNull {
                it.state.contractState.idReport == flowArgs.idReport
            } ?: throw CordaRuntimeException("Multiple or zero Digital Evidence states with id ${flowArgs.idReport} found.")

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState

            val members = state.participants.map {
                memberLookup.lookup(it) ?: throw CordaRuntimeException("Member not found from public key $it.")}
            val otherMember = (members - myInfo).singleOrNull()
                ?: throw CordaRuntimeException("Should be only one participant other than the initiator.")

            val custodyTracker = CustodyInteraction (
                typeReport = "Analysis-Report",
                officerName = myInfo.name,
                interaction = "UPDATE analysis report by ${myInfo.name}",
                timestamp = Instant.now()
            )

            val updateTracker = listOf(custodyTracker)

            // Create a new ChatState using the updateMessage helper function.
            val newAnalysisReportState = state.updateAnalysisReport(fileName= flowArgs.fileName, fileSize= flowArgs.fileSize,
                hashSHA1 = flowArgs.hashSHA1, hashMD5 = flowArgs.hashMD5, sourceFile = flowArgs.sourceFile,
                fileLocation = flowArgs.fileLocation, potentialInfo = flowArgs.potentialInfo, custodyHistory = updateTracker)

            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newAnalysisReportState)
                .addInputState(stateAndRef.ref)
                .addCommand(AnalysisReportContract.Update())
                .addSignatories(newAnalysisReportState.participants)

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeAnalysisReportSubFlow(signedTransaction, otherMember.name))


        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}