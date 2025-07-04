package com.r3.developers.chainofcustody.states

import com.r3.developers.chainofcustody.contracts.AnalysisReportContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.time.Instant
import java.util.*


// AnalysisReportState merepresentasikan data yang disimpan di Ledger.
// Sebuah laporan kasus terdiri dari serangkaian data linear antara dua partisipan atau lebih dan diwakili oleh UUID.
// Setiap organisasi dapat memiliki beberapa data untuk suatu laporannya sendiri
// Setiap AnalysisReportState menyimpan satu data bukti digital antara dua partisipan atau lebih dalam suatu laporan. Backchain pada CaseReportState merepresentasikan riwayat perubahan pada suatu laporan analysis bukti digital tertentu.

@BelongsToContract(AnalysisReportContract::class)
data class AnalysisReportState(
    // Id unik untuk laporan analisis
    val idReport : UUID = UUID.randomUUID(),
    // Nomor laporan analisis
    val idEvidence: String,
    val cidDE: String,
    // Nama file
    val fileName: String,
    // Ukuran file
    val fileSize: Long,
    // Nila hash SHA1
    val hashSHA1: String,
    // Nilai hash MD5
    val hashMD5: String,
    // Alamat path penyimpanan file analisis
    val sourceFile: String,
    // Struktur penyimpanan file image pada sistem
    val fileLocation: String,
    // Deskripsi dan potensi informasi
    val potentialInfo: String,
    // Nama analyst dan pemegang laporan analisis
    val holderAnalysisReport: MemberX500Name,
    // History interaksi pada CoC
    val custodyHistory: List<CustodyInteraction>,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function to create a new DigitalEvidenceState from the previous (input) DigitalEvidenceState.
    fun updateAnalysisReport(fileName: String, fileSize: Long, hashSHA1: String,
                         hashMD5: String, sourceFile: String, fileLocation: String,
                         potentialInfo: String, custodyHistory: List<CustodyInteraction>) =
        copy(fileName = fileName, fileSize = fileSize, hashSHA1= hashSHA1,
            hashMD5 = hashMD5, sourceFile = sourceFile, fileLocation = fileLocation,
            potentialInfo = potentialInfo, custodyHistory = custodyHistory)

//    fun transferAnalysisReport(holderAnalysisReport: MemberX500Name) =
//        copy(holderAnalysisReport= holderAnalysisReport)

//    fun AddAnalysisReport2DigitalEvidence(labReport: List<AnalysisReportState>) =
//        copy(labReport = labReport)
}

