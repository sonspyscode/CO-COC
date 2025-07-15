/*
Example of RequestBody for triggering the flow via REST:

{
  "clientRequestId": "createAR-01",
    "flowClassName": "com.r3.developers.chainofcustody.analysisreport.CreateAnalysisReportFlow",
    "requestBody": {
        "idEvidence":"content identifier 1",
        "cidDE":"CID-01821398",
        "fileName":"propaganda.jpeg",
        "fileSize":"10000",
        "hashSHA1":"hash value SHA1",
        "hashMD5":"hash value MD5",
        "sourceFile":"D://Bahan/PropagandaInternet/",
        "fileLocation":"C://HOMEUSER/AnalysisResult/",
        "potentialInfo":"Metadata file menunjukkan file tersebut hasil suntingan dan tidak asli",
        "otherMember":"CN=Custodian, OU=CrimeInvestigationTeam, O=Org1, L=Makassar, C=ID"
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
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Duration
import java.time.Instant

// A class to hold the deserialized arguments required to start the flow.
// Kelas untuk menampung argumen deserialisasi yang diperlukan untuk memulai aliran.
data class CreateAnalysisReportFlowArgs(val idEvidence: String, val cidDE: String, val fileName: String,
                                            val fileSize: Long, val hashSHA1: String,
                                                val hashMD5: String, val sourceFile: String,
                                                    val fileLocation: String, val potentialInfo: String,
                                                        val otherMember: List<String>)


// See Chat CorDapp Design section of the getting started docs for a description of this flow.
//Lihat bagian Desain Chat CorDapp pada dokumen memulai untuk deskripsi alur ini.
class CreateAnalysisReportFlow: ClientStartableFlow {

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

        log.info("CreateAnalysisReportFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            //Dapatkan argumen masukan yang telah dideserialisasi ke aliran dari requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateAnalysisReportFlowArgs::class.java)

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            // Good practice in Kotlin CorDapps is to only throw RuntimeException.
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            // Dapatkan MemberInfos untuk Vnode yang menjalankan alur dan Anggota lainnya.
            // Praktik yang baik di Kotlin CorDapps adalah hanya melempar RuntimeException.
            // Catatan, di Java CorDapps hanya RuntimeExceptions yang tidak dicentang yang bisa dilempar, bukan
            // pengecualian yang dicentang karena ini mengubah tanda tangan metode dan merusak override.
            val myInfo = memberLookup.myInfo()

            // Daftar organisasi atau role yang diizinkan membuat Digital Evidence
//            val allowedCommonName = "Investigator"
            val allowedOrgs = listOf("Org2", "Org4")

            // Validasi hanya role dan organisasi tertentu yang diizinkan
            if (myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only members from ${allowedOrgs.joinToString()} are allowed to create Analysis Report.")
            }

            val otherMembers = flowArgs.otherMember.map { memberString ->
                memberLookup.lookup(MemberX500Name.parse(memberString)) ?:
                throw CordaRuntimeException("Can't find otherMember $memberString  in flow arguments.")
            }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Analysis-report",
                officerName = myInfo.name,
                interaction = "CREATE analysis report for ${flowArgs.idEvidence} by ${myInfo.name}",
                timestamp = Instant.now()
            )

            val allParticipants: List<PublicKey> = listOf(myInfo.ledgerKeys.first()) + otherMembers.map { it.ledgerKeys.first() }

            val partyMembers = otherMembers
            val parties = partyMembers.map { it.name }


            // Create the ChatState from the input arguments and member information.
            //Buat ChatState dari argumen masukan dan informasi anggota.
            val newAnalysisReportState = AnalysisReportState(
                idEvidence = flowArgs.idEvidence,
                cidDE = flowArgs.cidDE,
                fileName = flowArgs.fileName,
                fileSize = flowArgs.fileSize,
                hashSHA1 = flowArgs.hashSHA1,
                hashMD5 = flowArgs.hashMD5,
                sourceFile = flowArgs.sourceFile,
                fileLocation = flowArgs.fileLocation,
                potentialInfo = flowArgs.potentialInfo,
                holderAnalysisReport = myInfo.name,
                custodyHistory = listOf(custodyInteraction),
                participants = allParticipants
            )

            // Obtain the notary.
            // Dapatkan notaris.
            val notary = notaryLookup.notaryServices.single()

            // Use UTXOTransactionBuilder to build up the draft transaction.
            // Gunakan UTXOTransactionBuilder untuk membuat draft transaksi.
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newAnalysisReportState)
                .addCommand(AnalysisReportContract.Create())
                .addSignatories(newAnalysisReportState.participants)

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            // Mengubah pembuat transaksi menjadi UTXOSignedTransaction. Memverifikasi konten
            // UtxoTransactionBuilder dan menandatangani transaksi dengan penandatangan yang diperlukan yang dimiliki oleh
            // node saat ini.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeDigitalEvidenceSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            // Panggil FinalizeDigitalEvidenceSubFlow yang akan menyelesaikan transaksi.
            // Jika berhasil, alur akan mengembalikan sebuah String dari id transaksi yang dibuat,
            // jika tidak berhasil, alur akan mengembalikan pesan kesalahan.
            return flowEngine.subFlow(FinalizeAnalysisReportSubFlow(signedTransaction, parties))


        }
        // Catch any exceptions, log them and rethrow the exception.
        // Tangkap pengecualian apa pun, catat, dan buang pengecualian tersebut.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}