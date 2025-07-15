package com.r3.developers.chainofcustody.contracts

import com.r3.developers.chainofcustody.states.AnalysisReportState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class AnalysisReportContract: Contract {

    // Use an internal scoped constant to hold the error messages
    // This allows the tests to use them, meaning if they are updated you won't need to fix tests just because the wording was updated
    internal companion object {

        const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
        const val UNKNOWN_COMMAND = "Command not allowed."
        const val PARTICIPANTS_MUST_BE_TWO = "Participants must be two"
        const val TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS = "Transaction must be signed by all participants"

        const val CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES = "When command is create there should be no input states."
        const val CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is create there should be one and only one output state."
        const val CREATE_AR_COMMAND_JUST_FOR_LAB_MEMBER_ONLY = "Only Lab Member can create analysis report"

        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update there should be one and only one input state."
        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Update there should be one and only one output state."
        const val UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Update, id must not change."
        const val UPDATE_COMMAND_IDEVIDENCE_SHOULD_NOT_CHANGE = "When command is Update, Identifier Evidence must not change."
        const val UPDATE_COMMAND_CID_SHOULD_NOT_CHANGE = "When command is Update, CID must not change."
        const val UPDATE_COMMAND_SHA1_SHOULD_NOT_CHANGE = "When command is Update, hash value SHA1 must not change."
        const val UPDATE_COMMAND_MD5_SHOULD_NOT_CHANGE = "When command is Update, hash value MD5 must not change."
        const val UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Update, participants must not change."
        const val UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY = "Only holder can update this evidence report."
    }

    // Command Class used to indicate that the transaction should start a new chat.
    class Create: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Update: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.

    // verify() function is used to apply contract rules to the transaction.
    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        // Applies a universal constraint (applies to all transactions irrespective of command)
        PARTICIPANTS_MUST_BE_TWO using {
            val output = transaction.outputContractStates.first() as AnalysisReportState
            output.participants.size>= 2
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as AnalysisReportState
            transaction.signatories.containsAll(output.participants)
        }

        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is Create -> {
                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
//                val input = transaction.inputContractStates.single() as AnalysisReportState
                CREATE_AR_COMMAND_JUST_FOR_LAB_MEMBER_ONLY using {
                    val output = transaction.outputContractStates.single() as AnalysisReportState
                        output.holderAnalysisReport.organization == "Org2" ||
                                output.holderAnalysisReport.organization == "Org4"
                }
            }
            // Rules applied only to transactions with the Update Command.
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as AnalysisReportState
                val output = transaction.outputContractStates.single() as AnalysisReportState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.idReport == output.idReport)
                UPDATE_COMMAND_IDEVIDENCE_SHOULD_NOT_CHANGE using (input.idEvidence == output.idEvidence)
                UPDATE_COMMAND_CID_SHOULD_NOT_CHANGE using (input.cidDE == output.cidDE)
                UPDATE_COMMAND_SHA1_SHOULD_NOT_CHANGE using (input.hashSHA1 == output.hashSHA1)
                UPDATE_COMMAND_MD5_SHOULD_NOT_CHANGE using (input.hashMD5 == output.hashMD5)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size >= 2)
                UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY using (input.holderAnalysisReport == output.holderAnalysisReport)
            }
            else -> {
                throw CordaRuntimeException(UNKNOWN_COMMAND)
            }
        }
    }
    // Helper function to allow writing constraints in the Corda 4 '"text" using (boolean)' style
    private infix fun String.using(expr: Boolean) {
        if (!expr) throw CordaRuntimeException("Failed requirement: $this")
    }

    // Helper function to allow writing constraints in '"text" using {lambda}' style where the last expression
    // in the lambda is a boolean.
    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }
}