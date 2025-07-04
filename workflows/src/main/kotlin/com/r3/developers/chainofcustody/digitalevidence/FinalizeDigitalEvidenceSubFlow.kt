package com.r3.developers.chainofcustody.digitalevidence

import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

// See Chat CorDapp Design section of the getting started docs for a description of this flow.

// @InitiatingFlow declares the protocol which will be used to link the initiator to the responder.
// @InitiatingFlow mendeklarasikan protokol yang akan digunakan untuk menghubungkan inisiator ke responder.
@InitiatingFlow(protocol = "finalize-digitalevidence-protocol")
class FinalizeDigitalEvidenceSubFlow(private val signedTransaction: UtxoSignedTransaction, private val parties: List<MemberX500Name>): SubFlow<String> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    // Menyuntikkan UtxoLedgerService untuk memungkinkan aliran menggunakan API Ledger.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): String {

        log.info("FinalizeDigitalEvidenceFlow.call() called")

        // Initiates a session with the other Member.
        // Memulai sesi dengan Anggota lain.
        val session = parties.map { flowMessaging.initiateFlow(it) }

        return try {
            // Calls the Corda provided finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            // On success returns the id of the transaction created. (This is different to the ChatState id)
            // Memanggil fungsi finalise() yang disediakan Corda yang mengumpulkan tanda tangan dari rekanan,
            // mencatat transaksi dan menyimpan transaksi ke brankas masing-masing pihak.
            // Jika berhasil, kembalikan id dari transaksi yang dibuat. (Ini berbeda dengan id ChatState)
            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                session
            )
            // Returns the transaction id converted to a string.
            // Mengembalikan id transaksi yang dikonversi menjadi sebuah string.
            finalizedSignedTransaction.transaction.id.toString().also {
                log.info("Success! Response: $it")
            }
        }
        // Soft fails the flow and returns the error message without throwing a flow exception.
        // Soft gagal dalam aliran dan mengembalikan pesan kesalahan tanpa melemparkan pengecualian aliran.
        catch (e: Exception) {
            log.warn("Finality failed", e)
            "Finality failed, ${e.message}"
        }
    }
}

// See Chat CorDapp Design section of the getting started docs for a description of this flow.

//@InitiatingBy declares the protocol which will be used to link the initiator to the responder.
//@InitiatingBy mendeklarasikan protokol yang akan digunakan untuk menghubungkan inisiator ke responder.
@InitiatedBy(protocol = "finalize-digitalevidence-protocol")
class FinalizeDigitalEvidenceResponderFlow: ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    // Menyuntikkan UtxoLedgerService untuk memungkinkan aliran menggunakan API Ledger.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("FinalizeDigitalEvidenceResponderFlow.call() called")

        try {
            // Calls receiveFinality() function which provides the responder to the finalise() function
            // in the Initiating Flow. Accepts a lambda validator containing the business logic to decide whether
            // responder should sign the Transaction.
            // Memanggil fungsi receiveFinality() yang menyediakan responder ke fungsi finalise()
            // di Alur Pemulaian. Menerima validator lambda yang berisi logika bisnis untuk memutuskan apakah
            // responder harus menandatangani Transaksi.
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { ledgerTransaction ->

                // Note, this exception will only be shown in the logs if Corda Logging is set to debug.
                // Catatan, pengecualian ini hanya akan ditampilkan di log jika Corda Logging diatur ke debug.
                val state = ledgerTransaction.getOutputStates(DigitalEvidenceState::class.java).singleOrNull() ?:
                    throw CordaRuntimeException("Failed verification - transaction did not have exactly one output ChatState.")

                    // Uses checkForBannedWords() and checkMessageFromMatchesCounterparty() functions
                    // to check whether to sign the transaction.
                    // Menggunakan fungsi checkForBannedWords() dan checkMessageFromMatchesCounterparty()
                    // untuk memeriksa apakah akan menandatangani transaksi.
    //          checkForBannedWords(state.message)
                checkMessageFromMatchesCounterparty(state, session.counterparty)

                log.info("Verified the transaction- ${ledgerTransaction.id}")
            }
            log.info("Finished responder flow - ${finalizedSignedTransaction.transaction.id}")
        }
        // Soft fails the flow and log the exception.
        // Soft gagal dalam aliran dan mencatat pengecualian.
        catch (e: Exception) {
            log.warn("Exceptionally finished responder flow", e)
        }
    }
}