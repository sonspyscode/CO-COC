/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "listDE-01",
    "flowClassName": "com.r3.developers.chainofcustody.support.ListDigitalEvidenceByUserFlow",
    "requestBody": {}
}
*/

package com.r3.developers.chainofcustody.support

//import com.r3.developers.chainofcustody.states.CustodyInteraction
import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CtordaInject
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
data class ListDigitalEvidenceByUserFlowArgs(val id: UUID, val cid: String,
                                             val registerNumber: String, val typeDE: String,
                                             val modelDE: String, val manufacturerDE: String,
                                             val serialNumber: String, val seizureReason: String,
                                             val caseID: String, val holderEvidence: MemberX500Name,
                                             val labReport: List<UUID>,
                                             val participants: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListDigitalEvidenceByUserFlow : ClientStartableFlow {

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

        log.info("ListDigitalEvidenceFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        // Menanyakan vault VNode untuk state yang tidak dikonsumsi dan mengubah hasilnya menjadi DTO yang dapat diserialisasi.
        val states = ledgerService.findUnconsumedStatesByExactType(DigitalEvidenceState::class.java, 100, Instant.now()).results
        val results = states.map {
            ListDigitalEvidenceByUserFlowArgs(
                it.state.contractState.id,
                it.state.contractState.cid,
                it.state.contractState.registerNumber,
                it.state.contractState.typeDE,
                it.state.contractState.modelDE,
                it.state.contractState.manufacturerDE,
                it.state.contractState.serialNumber,
                it.state.contractState.seizureReason,
                it.state.contractState.caseID,
                it.state.contractState.holderEvidence,
                it.state.contractState.labReport,
                it.state.contractState.participants.toString(),
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        // Menggunakan fungsi format() milik JsonMarshallingService untuk menserialisasi DTO ke Json.
        return jsonMarshallingService.format(results)
    }
}

