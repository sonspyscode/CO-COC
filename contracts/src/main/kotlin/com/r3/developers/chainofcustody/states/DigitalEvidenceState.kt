package com.r3.developers.chainofcustody.states

import com.r3.developers.chainofcustody.contracts.AnalysisReportContract
import com.r3.developers.chainofcustody.contracts.DigitalEvidenceContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.StateRef
import java.security.PublicKey
import java.time.Instant
import java.util.*
import javax.swing.plaf.nimbus.State


// The ChatState represents data stored on ledger. A chat consists of a linear series of messages between two
// participants and is represented by a UUID. Any given pair of participants can have multiple chats
// Each ChatState stores one message between the two participants in the chat. The backchain of ChatStates
// represents the history of the chat.

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
    val labReport: List<StateRef>,
    // History interaksi pada CoC
    val custodyHistory: List<CustodyInteraction>,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function to create a new DigitalEvidenceState from the previous (input) DigitalEvidenceState.
    fun updateDigitalEvidence(registerNumber: String,
                              typeDE: String, modelDE: String, manufacturerDE: String,
                              serialNumber: String, seizureReason: String, caseID: String,
                              custodyHistory: List<CustodyInteraction>) =
        copy(registerNumber = registerNumber,
            typeDE = typeDE, modelDE = modelDE, manufacturerDE = manufacturerDE,
            serialNumber = serialNumber, seizureReason = seizureReason, caseID = caseID,
            custodyHistory = custodyHistory)

    fun transferDigitalEvidence(holderEvidence: MemberX500Name, custodyHistory: List<CustodyInteraction>, participants: List<PublicKey>) =
        copy(holderEvidence = holderEvidence, custodyHistory = custodyHistory, participants = participants)

    fun addLabReportToEvidence(refs: List<StateRef>, custodyHistory: List<CustodyInteraction>) =
        copy(labReport = refs, custodyHistory = custodyHistory)
//    fun AddAnalysisReport2DigitalEvidence(labReport: List<AnalysisReportState>) =
//        copy(labReport = labReport)
}

