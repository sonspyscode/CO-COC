package com.r3.developers.chainofcustody.contracts

import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class DigitalEvidenceContract: Contract {

    // Use an internal scoped constant to hold the error messages
    // This allows the tests to use them, meaning if they are updated you won't need to fix tests just because the wording was updated
    internal companion object {

        const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
        const val UNKNOWN_COMMAND = "Command not allowed."
        const val PARTICIPANTS_MUST_BE_TWO = "Participants must be two"
        const val TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS = "Transaction must be signed by all participants"

        const val CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES = "When command is Create there should be no input states."
        const val CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is Create there should be one and only one output state."
        const val CREATE_DE_COMMAND_JUST_FOR_INVESTIGATOR = "Only Investigator can create digital evidence documentation"

        const val UPDATE_DE_COMMAND_JUST_FOR_ORG1 = "Only member from Organisation 1 can update evidence report"
        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update there should be one and only one input state."
        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Update there should be one and only one output state."
        const val UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Update id must not change."
        const val UPDATE_COMMAND_CID_SHOULD_NOT_CHANGE = "When command is Update CID must not change."
        const val UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Update participants must not change."
        const val UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY = "When command is Update, only holder of digital evidence can do it."
        const val UPDATE_COMMAND_LAB_SHOULD_NOT_CHANGE = "When command is Update Lab Report must not change."

        const val TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Transfer there should be one and only one input state."
        const val TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Transfer there should be one and only one output state."
        const val TRANSFER_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Transfer id must not change."
        const val TRANSFER_COMMAND_CID_SHOULD_NOT_CHANGE = "When command is Transfer CID must not change."
        const val TRANSFER_COMMAND_REGNUM_SHOULD_NOT_CHANGE = "When command is Transfer Register Number must not change."
        const val TRANSFER_COMMAND_TYPE_SHOULD_NOT_CHANGE = "When command is Transfer Type Digital Evidence must not change."
        const val TRANSFER_COMMAND_MODEL_SHOULD_NOT_CHANGE = "When command is Transfer Model Digital Evidence must not change."
        const val TRANSFER_COMMAND_MANUFACTURER_SHOULD_NOT_CHANGE = "When command is Transfer Manufacturer Digital Evidence must not change."
        const val TRANSFER_COMMAND_SERIAL_SHOULD_NOT_CHANGE = "When command is Transfer Serial Number must not change."
        const val TRANSFER_COMMAND_REASON_SHOULD_NOT_CHANGE = "When command is Transfer Seizure Reason must not change."
        const val TRANSFER_COMMAND_CASEID_SHOULD_NOT_CHANGE = "When command is Transfer Case Identifier must not change."
        const val TRANSFER_COMMAND_LAB_SHOULD_NOT_CHANGE = "When command is Transfer Lab Report must not change."
        const val TRANSFER_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Transfer participants must not change."
//        const val TRANSFER_COMMAND_JUST_FOR_ORG1 = "When command is Transfer, only org1 can do it."
        const val TRANSFER_COMMAND_SHOULD_BE_CHANGE_HOLDER = "When command is Transfer, holder of digital evidence should be different between input and output."
        const val TRANSFER_COMMAND_JUST_FOR_HOLDER_ONLY = "When command is Transfer, holder of digital evidence should be from org1, org2, and org3."


        const val ADDLABREPORT_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Transfer there should be one and only one input state."
        const val ADDLABREPORT_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Transfer there should be one and only one output state."
        const val ADDLABREPORT_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Transfer id must not change."
        const val ADDLABREPORT_COMMAND_CID_SHOULD_NOT_CHANGE = "When command is Transfer CID must not change."
        const val ADDLABREPORT_COMMAND_REGNUM_SHOULD_NOT_CHANGE = "When command is Transfer Register Number must not change."
        const val ADDLABREPORT_COMMAND_TYPE_SHOULD_NOT_CHANGE = "When command is Transfer Type Digital Evidence must not change."
        const val ADDLABREPORT_COMMAND_MODEL_SHOULD_NOT_CHANGE = "When command is Transfer Model Digital Evidence must not change."
        const val ADDLABREPORT_COMMAND_MANUFACTURER_SHOULD_NOT_CHANGE = "When command is Transfer Manufacturer Digital Evidence must not change."
        const val ADDLABREPORT_COMMAND_SERIAL_SHOULD_NOT_CHANGE = "When command is Transfer Serial Number must not change."
        const val ADDLABREPORT_COMMAND_REASON_SHOULD_NOT_CHANGE = "When command is Transfer Seizure Reason must not change."
        const val ADDLABREPORT_COMMAND_CASEID_SHOULD_NOT_CHANGE = "When command is Transfer Case Identifier must not change."
        const val ADDLABREPORT_COMMAND_HOLDER_SHOULD_NOT_CHANGE = "When command is Transfer Holder must not change."
        const val ADDLABREPORT_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Transfer participants must not change."
        const val ADDLABREPORT_COMMAND_JUST_FOR_ANALYST_ONLY = "When command is Transfer, only holder of digital evidence can do it."
    }

    // Command Class used to indicate that the transaction should start a new chat.
    class Create: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Update: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Transfer: Command
    class AddLabReport: Command

    // verify() function is used to apply contract rules to the transaction.
    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        // Applies a universal constraint (applies to all transactions irrespective of command)
        PARTICIPANTS_MUST_BE_TWO using {
            val output = transaction.outputContractStates.first() as DigitalEvidenceState
            output.participants.size>= 2
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as DigitalEvidenceState
            transaction.signatories.containsAll(output.participants)
        }

        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is Create -> {

                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
                CREATE_DE_COMMAND_JUST_FOR_INVESTIGATOR using {
                    val output = transaction.outputContractStates.single() as DigitalEvidenceState
                    output.holderEvidence.commonName == "Investigator" &&
                            output.holderEvidence.organization == "Org1"
                }
            }
            // Rules applied only to transactions with the Update Command.
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
                UPDATE_DE_COMMAND_JUST_FOR_ORG1 using {
                    val output = transaction.outputContractStates.single() as DigitalEvidenceState
                    output.holderEvidence.organization == "Org1"
                }
                UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY using {
                    val input = transaction.inputContractStates.single() as DigitalEvidenceState
                    input.holderEvidence.organization == "Org1"
                }

                val input = transaction.inputContractStates.single() as DigitalEvidenceState
                val output = transaction.outputContractStates.single() as DigitalEvidenceState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                UPDATE_COMMAND_CID_SHOULD_NOT_CHANGE using (input.cid == output.cid)
                UPDATE_COMMAND_LAB_SHOULD_NOT_CHANGE using (input.labReport == output.labReport)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size >= 2)
                }
            is Transfer -> {
                TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
//                TRANSFER_COMMAND_JUST_FOR_ORG1 using {
//                    val output = transaction.outputContractStates.single() as DigitalEvidenceState
//                    output.holderEvidence.organization == "Org1"
//                }

                val input = transaction.inputContractStates.single() as DigitalEvidenceState
                val output = transaction.outputContractStates.single() as DigitalEvidenceState
                TRANSFER_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                TRANSFER_COMMAND_CID_SHOULD_NOT_CHANGE using (input.cid == output.cid)
                TRANSFER_COMMAND_REGNUM_SHOULD_NOT_CHANGE using (input.registerNumber == output.registerNumber)
                TRANSFER_COMMAND_TYPE_SHOULD_NOT_CHANGE using (input.typeDE == output.typeDE)
                TRANSFER_COMMAND_MODEL_SHOULD_NOT_CHANGE using (input.modelDE == output.modelDE)
                TRANSFER_COMMAND_MANUFACTURER_SHOULD_NOT_CHANGE using (input.manufacturerDE == output.manufacturerDE)
                TRANSFER_COMMAND_SERIAL_SHOULD_NOT_CHANGE using (input.serialNumber == output.serialNumber)
                TRANSFER_COMMAND_REASON_SHOULD_NOT_CHANGE using (input.seizureReason == output.seizureReason)
                TRANSFER_COMMAND_CASEID_SHOULD_NOT_CHANGE using (input.caseID == output.caseID)
                TRANSFER_COMMAND_LAB_SHOULD_NOT_CHANGE using (input.labReport == output.labReport)
                val allowedOrgs = listOf("Org1", "Org2", "Org3")
                TRANSFER_COMMAND_JUST_FOR_HOLDER_ONLY using (allowedOrgs.contains(input.holderEvidence.organization))
                TRANSFER_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (input.participants == output.participants)
                TRANSFER_COMMAND_SHOULD_BE_CHANGE_HOLDER using (input.holderEvidence != output.holderEvidence)
            }
            is AddLabReport -> {
                ADDLABREPORT_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                ADDLABREPORT_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as DigitalEvidenceState
                val output = transaction.outputContractStates.single() as DigitalEvidenceState
                ADDLABREPORT_COMMAND_JUST_FOR_ANALYST_ONLY using (input.holderEvidence.organization ==  "Org2")
                ADDLABREPORT_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                ADDLABREPORT_COMMAND_CID_SHOULD_NOT_CHANGE using (input.cid == output.cid)
                ADDLABREPORT_COMMAND_REGNUM_SHOULD_NOT_CHANGE using (input.registerNumber == output.registerNumber)
                ADDLABREPORT_COMMAND_TYPE_SHOULD_NOT_CHANGE using (input.typeDE == output.typeDE)
                ADDLABREPORT_COMMAND_MODEL_SHOULD_NOT_CHANGE using (input.modelDE == output.modelDE)
                ADDLABREPORT_COMMAND_MANUFACTURER_SHOULD_NOT_CHANGE using (input.manufacturerDE == output.manufacturerDE)
                ADDLABREPORT_COMMAND_SERIAL_SHOULD_NOT_CHANGE using (input.serialNumber == output.serialNumber)
                ADDLABREPORT_COMMAND_REASON_SHOULD_NOT_CHANGE using (input.seizureReason == output.seizureReason)
                ADDLABREPORT_COMMAND_CASEID_SHOULD_NOT_CHANGE using (input.caseID == output.caseID)
                ADDLABREPORT_COMMAND_HOLDER_SHOULD_NOT_CHANGE using (input.labReport == output.labReport)
                ADDLABREPORT_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                    input.participants.toSet().intersect(output.participants.toSet()).size >= 2)
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