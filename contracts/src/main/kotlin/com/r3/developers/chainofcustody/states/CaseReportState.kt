package com.r3.developers.chainofcustody.states

import com.r3.developers.chainofcustody.contracts.CaseReportContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.StateRef
import java.security.PublicKey
import java.time.Instant
import java.util.*


// The ChatState represents data stored on ledger. A chat consists of a linear series of messages between two
// participants and is represented by a UUID. Any given pair of participants can have multiple chats
// Each ChatState stores one message between the two participants in the chat. The backchain of ChatStates
// represents the history of the chat.

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
    val dateNtime: Instant,
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
    val digitalEvidencePack: List<StateRef>,
    // History interaksi pada CoC
    val custodyHistory: List<CustodyInteraction>,
    // The participants to the chat, represented by their public key.
    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    // Helper function to create a new DigitalEvidenceState from the previous (input) DigitalEvidenceState.
    fun updateCaseReport(caseName: String, suspectName: String, victimName: String,
                              locationCase: String, dateNtime: Instant, toolName: String, toolDesc: String,
                                    statusCase: String, custodyHistory: List<CustodyInteraction>) =
        copy(caseName = caseName, suspectName = suspectName, victimName = victimName,
                locationCase = locationCase, dateNtime = dateNtime, toolName = toolName, toolsDesc = toolDesc,
                     statusCase = statusCase, custodyHistory = custodyHistory)

    fun transferCaseReport(holderCaseReport : MemberX500Name, custodyHistory: List<CustodyInteraction>, participants: List<PublicKey>) =
        copy(holderCaseReport = holderCaseReport, custodyHistory = custodyHistory, participants = participants)

    fun validationCaseReport(validationStatus: String, custodyHistory: List<CustodyInteraction>) =
        copy(validationStatus = validationStatus, custodyHistory = custodyHistory)

    fun addDigitalEvidenceToCaseReport(digitalEvidencePack: List<StateRef>, custodyHistory: List<CustodyInteraction>) =
        copy(digitalEvidencePack = digitalEvidencePack, custodyHistory = custodyHistory)

}

