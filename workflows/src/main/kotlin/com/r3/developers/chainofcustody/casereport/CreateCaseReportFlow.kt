/*
Example of RequestBody for triggering the flow via REST:
{
  "clientRequestId": "createCR-01",
    "flowClassName": "com.r3.developers.chainofcustody.casereport.CreateCaseReportFlow",
    "requestBody": {
        "caseNumber": "CNum-001",
        "caseName": "CN-01821398",
        "suspectName": "Alucard",
        "victimName": "Hylos",
        "locationCase": "BTN Land of Dawn",
        "dateNtime": "01/01/2020 12:12:00",
        "toolName": "FTKImager",
        "toolsDesc": "Tools akuisisi data",
        "firstResponder": "Investigator",
        "organisationName": "Organisasi 1",
        "statusCase": "ACTIVE",
        "validationStatus": "ON PROGRESS",
        "otherMember": "CN=Custodian, OU=CrimeInvestigationTeam, O=Org1, L=Makassar, C=ID"
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
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.UUID

// A class to hold the deserialized arguments required to start the flow.
// Kelas untuk menampung argumen deserialisasi yang diperlukan untuk memulai aliran.
data class CreateCaseReportFlowArgs(val caseNumber: String, val caseName: String,
                                    val suspectName: String, val victimName: String,
                                    val locationCase: String, val dateNtime: String,
                                    val toolName: String, val toolsDesc: String, val firstResponder: String,
                                    val organisationName: String, val statusCase: String,
                                    val validationStatus: String,
                                    val otherMember: List<String>)


// See Chat CorDapp Design section of the getting started docs for a description of this flow.
//Lihat bagian Desain Chat CorDapp pada dokumen memulai untuk deskripsi alur ini.
class CreateCaseReportFlow: ClientStartableFlow {

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

        log.info("CreateCaseReportFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            //Dapatkan argumen masukan yang telah dideserialisasi ke aliran dari requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateCaseReportFlowArgs::class.java)

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
            val allowedCommonName = "Investigator"
            val allowedOrgs = listOf("Org1", "Org3")

            // Validasi hanya role dan organisasi tertentu yang diizinkan
            if (myInfo.name.commonName != allowedCommonName && myInfo.name.organization !in allowedOrgs) {
                throw CordaRuntimeException("Only members from ${allowedOrgs.joinToString()} are allowed to create Case Report.")
            }

            val otherMembers = flowArgs.otherMember.map { memberString ->
                 memberLookup.lookup(MemberX500Name.parse(memberString)) ?:
                    throw CordaRuntimeException("Can't find otherMember $memberString  in flow arguments.")
            }

            val allParticipants: List<PublicKey> = listOf(myInfo.ledgerKeys.first()) + otherMembers.map { it.ledgerKeys.first() }

            val partyMembers = otherMembers
            val parties = partyMembers.map { it.name }

            val custodyInteraction = CustodyInteraction (
                typeReport = "Case-Report",
                officerName = myInfo.name,
                interaction = "CREATE Case Report with number ${flowArgs.caseNumber}",
                timestamp = Instant.now()
            )

            // Create the ChatState from the input arguments and member information.
            //Buat ChatState dari argumen masukan dan informasi anggota.
            val newCaseReportState = CaseReportState(
                caseNumber = flowArgs.caseNumber,
                caseName = flowArgs.caseName,
                suspectName = flowArgs.suspectName,
                victimName = flowArgs.victimName,
                locationCase = flowArgs.locationCase,
                dateNtime = flowArgs.dateNtime,
                toolName = flowArgs.toolName,
                toolsDesc = flowArgs.toolsDesc,
                firstResponder = flowArgs.firstResponder,
                organisationName = flowArgs.organisationName,
                statusCase = flowArgs.statusCase,
                validationStatus = flowArgs.validationStatus,
                holderCaseReport = myInfo.name,
                digitalEvidencePack = listOf(),
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
                .addOutputState(newCaseReportState)
                .addCommand(CaseReportContract.Create())
                .addSignatories(newCaseReportState.participants)

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

            return flowEngine.subFlow(FinalizeCaseReportSubFlow(signedTransaction, parties))
        }
        // Catch any exceptions, log them and rethrow the exception.
        // Tangkap pengecualian apa pun, catat, dan buang pengecualian tersebut.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}