package com.r3.developers.chainofcustody.states

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import java.time.Instant
import java.util.*


// The ChatState represents data stored on ledger. A chat consists of a linear series of messages between two
// participants and is represented by a UUID. Any given pair of participants can have multiple chats
// Each ChatState stores one message between the two participants in the chat. The backchain of ChatStates
// represents the history of the chat.

@CordaSerializable
data class CustodyInteraction(
    val typeReport: String,
    val officerName: MemberX500Name,
    val interaction: String,
    val timestamp: Instant,
    val newHolder: MemberX500Name? = null
)
