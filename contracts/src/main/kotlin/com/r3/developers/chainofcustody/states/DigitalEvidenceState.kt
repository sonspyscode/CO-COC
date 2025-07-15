package com.r3.developers.chainofcustody.states

import com.r3.developers.chainofcustody.contracts.DigitalEvidenceContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*


// DigitalEvidenceState merepresentasikan data yang disimpan di Ledger.
// Sebuah laporan bukti digital terdiri dari serangkaian data linear antara dua partisipan atau lebih dan diwakili oleh UUID.
// Setiap organisasi dapat memiliki beberapa data untuk suatu laporan
// Setiap DigitalEvidenceState menyimpan satu data bukti digital antara dua partisipan atau lebih dalam suatu laporan. Backchain pada DigitalEvidenceState merepresentasikan riwayat perubahan pada laporan bukti digital.

@BelongsToContract(DigitalEvidenceContract::class)
data class DigitalEvidenceState(
    // Id unik untuk digital evidence setelah diinput ke jaringan
    val id : UUID = UUID.randomUUID(),
    // Content identifier dari IPFS
    val cid : String,
    // nomor register penyimpanan sumber bukti digital (bukti fisik)
    val registerNumber: String,
    // Tipe atau jenis barang bukti
    val typeDE: String,
    // Model atau nama seri dari barang bukti
    val modelDE: String,
    // nama manufaktur dari barang bukti
    val manufacturerDE: String,
    // nomor serial dari barang bukti
    val serialNumber: String,
    // alasan penyitaan
    val seizureReason: String,
    // Riwayat kepemilikan bukti digital
    val caseID: String,
    // orang yang memiliki bukti digital (MemberX500Name)
    val holderEvidence: MemberX500Name,
    // Reference state laporan analisis
    val labReport: List<UUID>,
    // History interaksi pada CoC
    val custodyHistory: List<CustodyInteraction>,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function untuk memperbarui isi state.
    fun updateDigitalEvidence(registerNumber: String,
                              typeDE: String, modelDE: String, manufacturerDE: String,
                              serialNumber: String, seizureReason: String, caseID: String,
                              custodyHistory: List<CustodyInteraction>) =
        copy(registerNumber = registerNumber,
            typeDE = typeDE, modelDE = modelDE, manufacturerDE = manufacturerDE,
            serialNumber = serialNumber, seizureReason = seizureReason, caseID = caseID,
            custodyHistory = custodyHistory)

    // Helper function untuk memindahkan nama holder
    fun transferDigitalEvidence(holderEvidence: MemberX500Name, custodyHistory: List<CustodyInteraction>) =
        copy(holderEvidence = holderEvidence, custodyHistory = custodyHistory)

    // Helper function untuk menambahkan reference state dari laporan hasil analisis suatu bukti digital
    fun addLabReportToEvidence(labReport: List<UUID>, custodyHistory: List<CustodyInteraction>) =
        copy(labReport = labReport, custodyHistory = custodyHistory)
}

