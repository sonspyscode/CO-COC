/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "listAR-1",
    "flowClassName": "com.r3.developers.chainofcustody.support.ListAnalysisReportByUserFlow",
    "requestBody": {}
}
*/

package com.r3.developers.chainofcustody.support

import com.r3.developers.chainofcustody.states.AnalysisReportState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

// Kelas data untuk menampung hasil Flow.
// ChatState tidak dapat dikembalikan secara langsung karena JsonMarshallingService hanya dapat menserialisasi kelas sederhana
// yang dikenali oleh serializer Jackson yang mendasarinya, sehingga membuat objek gaya DTO yang hanya terdiri dari String
// dan UUID. Dimungkinkan untuk membuat serializer khusus untuk JsonMarshallingService, tetapi ini di luar cakupan
// contoh sederhana ini.
data class ListAnalysisReportByUserFlowArgs(val idReport: UUID, val idEvidence: String,
                                        val cidDE: String, val fileName: String,
                                        val fileSize: String, val hashSHA1: String,
                                        val hashMD5: String, val sourceFile: String,
                                        val fileLocation: String, val potentialInfo: String,
                                        val holderAnalysisReport: MemberX500Name,
                                        val participants: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListAnalysisReportByUserFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    // Menyuntikkan UtxoLedgerService untuk memungkinkan aliran menggunakan API Ledger.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("ListAnalysisReportByUserFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        // Menanyakan vault VNode untuk state yang tidak dikonsumsi dan mengubah hasilnya menjadi DTO yang dapat diserialisasi.
        val states = ledgerService.findUnconsumedStatesByExactType(AnalysisReportState::class.java, 100, Instant.now()).results
        val results = states.map {
            ListAnalysisReportByUserFlowArgs(
                it.state.contractState.idReport,
                it.state.contractState.idEvidence,
                it.state.contractState.cidDE,
                it.state.contractState.fileName,
                it.state.contractState.fileSize.toString(),
                it.state.contractState.hashSHA1,
                it.state.contractState.hashMD5,
                it.state.contractState.sourceFile,
                it.state.contractState.fileLocation,
                it.state.contractState.potentialInfo,
                it.state.contractState.holderAnalysisReport,
                it.state.contractState.participants.toString(),
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        // Menggunakan fungsi format() milik JsonMarshallingService untuk menserialisasi DTO ke Json.
        return jsonMarshallingService.format(results)
    }
}

