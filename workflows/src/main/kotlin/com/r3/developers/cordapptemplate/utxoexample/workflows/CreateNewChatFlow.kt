package com.r3.developers.cordapptemplate.utxoexample.workflows

import com.r3.developers.cordapptemplate.utxoexample.contracts.ChatContract
import com.r3.developers.cordapptemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

// A class to hold the deserialized arguments required to start the flow.
// Kelas untuk menampung argumen deserialisasi yang diperlukan untuk memulai aliran.
data class CreateNewChatFlowArgs(val chatName: String, val message: String, val otherMember: String)

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
//Lihat bagian Desain Chat CorDapp pada dokumen memulai untuk deskripsi alur ini.
class CreateNewChatFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    //Suntikkan UtxoLedgerService untuk memungkinkan aliran menggunakan API Ledger.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    // FlowEngine service is required to run SubFlows.
    //Layanan FlowEngine diperlukan untuk menjalankan SubFlows.
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("CreateNewChatFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            //Dapatkan argumen masukan yang telah dideserialisasi ke aliran dari requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateNewChatFlowArgs::class.java)

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            // Good practice in Kotlin CorDapps is to only throw RuntimeException.
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            // Dapatkan MemberInfos untuk Vnode yang menjalankan alur dan Anggota lainnya.
            // Praktik yang baik di Kotlin CorDapps adalah hanya melempar RuntimeException.
            // Catatan, di Java CorDapps hanya RuntimeExceptions yang tidak dicentang yang bisa dilempar, bukan
            // pengecualian yang dicentang karena ini mengubah tanda tangan metode dan merusak override.
            val myInfo = memberLookup.myInfo()
            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.otherMember)) ?:
                throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            // Create the ChatState from the input arguments and member information.
            //Buat ChatState dari argumen masukan dan informasi anggota.
            val chatState = ChatState(
                chatName = flowArgs.chatName,
                messageFrom = myInfo.name,
                message = flowArgs.message,
                participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
            )

            // Obtain the notary.
            // Dapatkan notaris.
            val notary = notaryLookup.notaryServices.single()

            // Use UTXOTransactionBuilder to build up the draft transaction.
            // Gunakan UTXOTransactionBuilder untuk membuat draft transaksi.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(chatState)
                .addCommand(ChatContract.Create())
                .addSignatories(chatState.participants)

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            // Mengubah pembuat transaksi menjadi UTXOSignedTransaction. Memverifikasi konten
            // UtxoTransactionBuilder dan menandatangani transaksi dengan penandatangan yang diperlukan yang dimiliki oleh
            // node saat ini.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeChatSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            // Panggil FinalizeChatSubFlow yang akan menyelesaikan transaksi.
            // Jika berhasil, alur akan mengembalikan sebuah String dari id transaksi yang dibuat,
            // jika tidak berhasil, alur akan mengembalikan pesan kesalahan.
            return flowEngine.subFlow(FinalizeChatSubFlow(signedTransaction, otherMember.name))


        }
        // Catch any exceptions, log them and rethrow the exception.
        // Tangkap pengecualian apa pun, catat, dan buang pengecualian tersebut.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}


/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.CreateNewChatFlow",
    "requestBody": {
        "chatName":"Chat with Bob",
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "message": "Hello Bob"
        }
}
 */

//package com.r3.developers.chainofcustody
//
//import net.corda.v5.base.annotations.Suspendable
//import net.corda.v5.application.chainofcustody.*
//import net.corda.v5.application.identity.Party
//import net.corda.v5.ledger.utxo.*
//import net.corda.v5.base.annotations.CordaSerializable
//import net.corda.v5.base.types.MemberX500Name
//import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder
//import java.time.Instant
//import java.util.UUID
//
//import com.r3.developers.states.DigitalEvidenceState
//import com.r3.developers.states.EvidenceHistory
//import com.r3.developers.contracts.DigitalEvidenceContract
//
//class CreateDigitalEvidenceFlow {
//
//    // Data class untuk metadata evidence yang diinput investigator
//    @CordaSerializable
//    data class EvidenceMetadata(
//        val cid: String,          // CID dari IPFS
//        val description: String?  // Deskripsi singkat evidence (opsional)
//    )
//
//    // Flow utama untuk membuat digital evidence baru
//    @InitiatingFlow
//    @StartableByRPC
//    class CreateDigitalEvidenceFlow(
//        private val metadata: EvidenceMetadata,      // Metadata evidence dari investigator
//        private val participants: List<MemberX500Name> // Daftar X500Name pihak yang menjadi peserta state
//    ) : FlowLogic<SignedTransaction>() {
//
//        @Suspendable
//        override fun call(): SignedTransaction {
//            // 1. Ambil identity initiator (investigator)
//            val owner = ourIdentity
//
//            // 2. Resolusi daftar Party peserta dari X500Name
//            val participantParties = participants.map { serviceHub.identityService.partyFromName(it) ?: throw IllegalArgumentException("Party ${it} tidak ditemukan di network map") }
//
//            // 3. Buat riwayat awal evidence
//            val history = listOf(
//                EvidenceHistory(
//                    action = "CREATE",
//                    actor = owner,
//                    timestamp = Instant.now(),
//                    approvedAt = null,
//                    receivedAt = null
//                )
//            )
//
//            // 4. Buat state evidence baru
//            val evidenceState = DigitalEvidenceState(
//                cid = metadata.cid,
//                owner = owner,
//                registerNumber = null,      // Diisi custodian nanti
//                type = null,                // Diisi custodian nanti
//                model = null,
//                manufacturer = null,
//                serialNumber = null,
//                seizureReason = null,
//                createdAt = Instant.now(),
//                updatedAt = Instant.now(),
//                history = history,
//                participants = listOf(owner) + participantParties
//            )
//
//            // 5. Tentukan notary dari network map
//            val notary = serviceHub.networkMapService.notaryIdentities.first()
//
//            // 6. Build transaction
//            val txBuilder = UtxoTransactionBuilder(notary)
//                .addOutputState(evidenceState, DigitalEvidenceContract.ID)
//                .addCommand(DigitalEvidenceContract.Commands.Create(), evidenceState.participants.map { it.owningKey })
//
//            // 7. Verifikasi transaksi
//            txBuilder.verify(serviceHub)
//
//            // 8. Tanda tangani transaksi oleh initiator
//            val signedTx = serviceHub.signInitialTransaction(txBuilder)
//
//            // 9. Inisiasi sesi flow ke semua peserta lain (kecuali initiator)
//            val otherParticipants = evidenceState.participants.filter { it != owner }
//            val sessions = otherParticipants.map { initiateFlow(it) }
//
//            // 10. Kumpulkan signature dari peserta lain (jika ada)
//            val fullySignedTx = if (sessions.isNotEmpty())
//                subFlow(CollectSignaturesFlow(signedTx, sessions))
//            else
//                signedTx
//
//            // 11. Finalisasi transaksi agar evidence state tersedia di vault semua peserta
//            return subFlow(FinalityFlow(fullySignedTx, sessions))
//        }
//    }
//
//    // Responder flow untuk peserta lain
//    @InitiatedBy(CreateDigitalEvidenceFlow::class)
//    class CreateDigitalEvidenceResponderFlow(
//        private val session: FlowSession
//    ) : FlowLogic<Unit>() {
//        @Suspendable
//        override fun call() {
//            // 1. Tanda tangani transaksi jika valid
//            subFlow(SignTransactionFlow(session) {
//                // Implementasi validasi tambahan bisa ditambahkan di sini
//            })
//            // 2. Terima finalisasi transaksi
//            subFlow(ReceiveFinalityFlow(session))
//        }
//    }
//}