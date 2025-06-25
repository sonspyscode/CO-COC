package com.r3.developers.cordapptemplate.utxoexample.workflows

import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*


// Data class to hold the Flow results.
// The ChatState(s) cannot be returned directly as the JsonMarshallingService can only serialize simple classes
// that the underlying Jackson serializer recognises, hence creating a DTO style object which consists only of Strings
// and a UUID. It is possible to create custom serializers for the JsonMarshallingService, but this beyond the scope
// of this simple example.
// Kelas data untuk menampung hasil Flow.
// ChatState tidak dapat dikembalikan secara langsung karena JsonMarshallingService hanya dapat menserialisasi kelas sederhana
// yang dikenali oleh serializer Jackson yang mendasarinya, sehingga membuat objek gaya DTO yang hanya terdiri dari String
// dan UUID. Dimungkinkan untuk membuat serializer khusus untuk JsonMarshallingService, tetapi ini di luar cakupan
// contoh sederhana ini.
data class ChatStateResults(val id: UUID, val chatName: String,val messageFromName: String, val message: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListChatsFlow : ClientStartableFlow {

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

        log.info("ListChatsFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        // Menanyakan vault VNode untuk state yang tidak dikonsumsi dan mengubah hasilnya menjadi DTO yang dapat diserialisasi.
        val states = ledgerService.findUnconsumedStatesByExactType(ChatState::class.java, 100, Instant.now()).results
        val results = states.map {
            ChatStateResults(
                it.state.contractState.id,
                it.state.contractState.chatName,
                it.state.contractState.messageFrom.toString(),
                it.state.contractState.message) }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        // Menggunakan fungsi format() milik JsonMarshallingService untuk menserialisasi DTO ke Json.
        return jsonMarshallingService.format(results)
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.ListChatsFlow",
    "requestBody": {}
}
*/
