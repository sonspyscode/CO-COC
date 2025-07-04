/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "listCR-1",
    "flowClassName": "com.r3.developers.chainofcustody.support.ListCaseReportByUserFlow",
    "requestBody": {}
}
*/

package com.r3.developers.chainofcustody.support

import com.r3.developers.chainofcustody.states.CaseReportState
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
data class ListCaseReportByUserFlowArgs(val idCase: UUID, val caseNumber: String,
                                        val caseName: String, val suspectName: String,
                                        val victimName: String, val locationName: String,
                                        val dateNtime: String, val toolName: String,
                                        val toolsDesc: String, val firstResponder: String,
                                        val organisationName: String, val statusCase: String,
                                        val validationStatus: String, val holderCaseReport: MemberX500Name,
                                        val digitalEvidencePack: List<UUID>,
                                        val participants: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListCaseReportByUserFlow : ClientStartableFlow {

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

        log.info("ListCaseReportByUserFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        // Menanyakan vault VNode untuk state yang tidak dikonsumsi dan mengubah hasilnya menjadi DTO yang dapat diserialisasi.
        val states = ledgerService.findUnconsumedStatesByExactType(CaseReportState::class.java, 100, Instant.now()).results
        val results = states.map {
            ListCaseReportByUserFlowArgs(
                it.state.contractState.idCase,
                it.state.contractState.caseNumber,
                it.state.contractState.caseName,
                it.state.contractState.suspectName,
                it.state.contractState.victimName,
                it.state.contractState.locationCase,
                it.state.contractState.dateNtime,
                it.state.contractState.toolName,
                it.state.contractState.toolsDesc,
                it.state.contractState.firstResponder,
                it.state.contractState.organisationName,
                it.state.contractState.statusCase,
                it.state.contractState.validationStatus,
                it.state.contractState.holderCaseReport,
                it.state.contractState.digitalEvidencePack,
                it.state.contractState.participants.toString(),
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        // Menggunakan fungsi format() milik JsonMarshallingService untuk menserialisasi DTO ke Json.
        return jsonMarshallingService.format(results)
    }
}

