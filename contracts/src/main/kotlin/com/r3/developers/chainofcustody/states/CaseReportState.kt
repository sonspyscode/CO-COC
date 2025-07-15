package com.r3.developers.chainofcustody.states

import com.r3.developers.chainofcustody.contracts.CaseReportContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

// CaseReportState merepresentasikan data yang disimpan di Ledger.
// Sebuah laporan kasus terdiri dari serangkaian data linear antara dua partisipan atau lebih dan diwakili oleh UUID.
// Setiap organisasi dapat memiliki beberapa data untuk suatu laporannya sendiri
// Setiap CaseReportState menyimpan satu data bukti digital antara dua partisipan atau lebih dalam suatu laporan. Backchain pada CaseReportState merepresentasikan riwayat perubahan pada suatu laporan kasus.

@BelongsToContract(CaseReportContract::class)
data class CaseReportState(
    // Id unik untuk laporan kasus
    val idCase : UUID = UUID.randomUUID(),
    // Nomor kasus
    val caseNumber : String,
    // Nama kasus
    val caseName: String,
    // Nama tersangka
    val suspectName: String,
    // Nama korban
    val victimName: String,
    // Lokasi
    val locationCase: String,
    // Tanggal dan waktu
    val dateNtime: String,
    // Alat yang digunakan
    val toolName: String,
    // deskripsi alat yang digunakan
    val toolsDesc: String,
    // Nama first responder
    val firstResponder: String,
    // Nama organisasi
    val organisationName: String,
    // Status Kasus
    val statusCase: String,
    // Status Berkas
    val validationStatus: String,
    // Nama pemilik laporan
    val holderCaseReport: MemberX500Name,
    // Daftar reference state untuk bukti digital
    val digitalEvidencePack: List<UUID>,
    // History interaksi pada CoC
    val custodyHistory: List<CustodyInteraction>,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function untuk memperbarui laporan kasus.
    fun updateCaseReport(caseName: String, suspectName: String, victimName: String,
                              locationCase: String, dateNtime: String, toolName: String, toolDesc: String,
                                    statusCase: String, custodyHistory: List<CustodyInteraction>) =
        copy(caseName = caseName, suspectName = suspectName, victimName = victimName,
                locationCase = locationCase, dateNtime = dateNtime, toolName = toolName, toolsDesc = toolDesc,
                     statusCase = statusCase, custodyHistory = custodyHistory)

    // Hlper function untuk memindahkan holder suatu laporan kasus
    fun transferCaseReport(holderCaseReport : MemberX500Name, custodyHistory: List<CustodyInteraction>) =
        copy(holderCaseReport = holderCaseReport, custodyHistory = custodyHistory)

    // Helper function untuk mengubah status validasi dokumen bukti dari suatu laporan kasus
    fun validationCaseReport(validationStatus: String, custodyHistory: List<CustodyInteraction>) =
        copy(validationStatus = validationStatus, custodyHistory = custodyHistory)

    // Helper function untuk menambahkan reference state digital evidence pada suatu laporan kasus tertentu
    fun addDigitalEvidenceToCaseReport(digitalEvidencePack: List<UUID>, custodyHistory: List<CustodyInteraction>) =
        copy(digitalEvidencePack = digitalEvidencePack, custodyHistory = custodyHistory)

}

