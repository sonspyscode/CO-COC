package com.r3.developers.chainofcustody.contracts

import com.r3.developers.chainofcustody.states.CaseReportState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class CaseReportContract: Contract {

    // Use an internal scoped constant to hold the error messages
    // This allows the tests to use them, meaning if they are updated you won't need to fix tests just because the wording was updated
    internal companion object {

        const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
        const val UNKNOWN_COMMAND = "Command not allowed."
        const val PARTICIPANTS_MUST_BE_TWO = "Participants must be two"
        const val TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS = "Transaction must be signed by all participants"

        const val CREATE_CR_COMMAND_JUST_FOR_INVESTIGATOR = "Only Investigator can create case report documentation"
        const val CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES = "When command is Create there should be no input states."
        const val CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is Create there should be one and only one output state."

        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update there should be one and only one input state."
        const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Update there should be one and only one output state."
        const val UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Update, id must not change."
        const val UPDATE_COMMAND_CASENUM_SHOULD_NOT_CHANGE = "When command is Update, CID must not change."
        const val UPDATE_COMMAND_FR_SHOULD_NOT_CHANGE = "When command is Update, First Responder must not change."
        const val UPDATE_COMMAND_FRORG_SHOULD_NOT_CHANGE = "When command is Update, First Responder's Organisation must not change."
        const val UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Update, participants must not change."
        const val UPDATE_COMMAND_HOLDER_SHOULD_NOT_CHANGE = "When command is Update, Holder case must not change."
        const val UPDATE_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE = "When command is Update, Digital Evidence Pack must not change."
        const val UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY = "Only holder can update this evidence report."

        const val TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Transfer, there should be one and only one input state."
        const val TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Transfer, there should be one and only one output state."
        const val TRANSFER_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Transfer, id must not change."
        const val TRANSFER_COMMAND_CASENUM_SHOULD_NOT_CHANGE = "When command is Transfer, Case Number must not change."
        const val TRANSFER_COMMAND_CASENAME_SHOULD_NOT_CHANGE = "When command is Transfer, Case Name must not change."
        const val TRANSFER_COMMAND_SUSNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Suspect Name must not change."
        const val TRANSFER_COMMAND_VICNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Victim Name must not change."
        const val TRANSFER_COMMAND_TIME_SHOULD_NOT_CHANGE = "When command is Transfer, Date and Time must not change."
        const val TRANSFER_COMMAND_TOOLNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Tool Name must not change."
        const val TRANSFER_COMMAND_TOOLDESC_SHOULD_NOT_CHANGE = "When command is Transfer, Tool Description must not change."
        const val TRANSFER_COMMAND_FR_SHOULD_NOT_CHANGE = "When command is Update, First Responder must not change."
        const val TRANSFER_COMMAND_FRORG_SHOULD_NOT_CHANGE = "When command is Update, First Responder's Organisation must not change."
        const val TRANSFER_COMMAND_STATUSCASE_SHOULD_NOT_CHANGE = "When command is Transfer, Status Case must not change."
        const val TRANSFER_COMMAND_VALIDATE_SHOULD_NOT_CHANGE = "When command is Transfer, Validation Status must not change."
        const val TRANSFER_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE = "When command is Update, Digital Evidence Pack must not change."
        const val TRANSFER_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Transfer, participants must not change."
        const val TRANSFER_COMMAND_JUST_FOR_HOLDER_ONLY = "When command is Transfer, only holder of digital evidence can do it."
        const val TRANSFER_COMMAND_SHOULD_BE_CHANGE_HOLDER = "When command is Transfer, holder of digital evidence should be different between input and output."

        const val VALIDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update, there should be one and only one input state."
        const val VALIDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is Validate, there should be one and only one output state."
        const val VALIDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Transfer, id must not change."
        const val VALIDATE_COMMAND_CASENUM_SHOULD_NOT_CHANGE = "When command is Transfer, Case Number must not change."
        const val VALIDATE_COMMAND_CASENAME_SHOULD_NOT_CHANGE = "When command is Transfer, Case Name must not change."
        const val VALIDATE_COMMAND_SUSNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Suspect Name must not change."
        const val VALIDATE_COMMAND_VICNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Victim Name must not change."
        const val VALIDATE_COMMAND_TIME_SHOULD_NOT_CHANGE = "When command is Transfer, Date and Time must not change."
        const val VALIDATE_COMMAND_TOOLNAME_SHOULD_NOT_CHANGE = "When command is Transfer, Tool Name must not change."
        const val VALIDATE_COMMAND_TOOLDESC_SHOULD_NOT_CHANGE = "When command is Transfer, Tool Description must not change."
        const val VALIDATE_COMMAND_FR_SHOULD_NOT_CHANGE = "When command is Update, First Responder must not change."
        const val VALIDATE_COMMAND_FRORG_SHOULD_NOT_CHANGE = "When command is Update, First Responder's Organisation must not change."
        const val VALIDATE_COMMAND_STATUSCASE_SHOULD_NOT_CHANGE = "When command is Transfer, Status Case must not change."
        const val VALIDATE_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE = "When command is Update, Digital Evidence Pack must not change."
        const val VALIDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Transfer, participants must not change."
        const val VALIDATE_COMMAND_JUST_FOR_ORG3 = "Only Organisation 3 can validate the report."
        const val VALIDATE_COMMAND_VALIDATE_SHOULD_BE_CHANGE = "When command is Validate, Validation Status of digital evidence should be different between input and output."

        const val ADDEVIDENCEPACK_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Add Evidence Pack, there should be one and only one input state."
        const val ADDEVIDENCEPACK_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Add Evidence Pack, there should be one and only one output state."
        const val ADDEVIDENCEPACK_COMMAND_JUST_FOR_CUSTODIAN = "Only custodian can add evidence to case report."
    }

    // Command Class used to indicate that the transaction should start a new chat.
    class Create: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Update: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Transfer: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Validate: Command
    class AddEvidencePack: Command

    // verify() function is used to apply contract rules to the transaction.
    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        // Applies a universal constraint (applies to all transactions irrespective of command)
        PARTICIPANTS_MUST_BE_TWO using {
            val output = transaction.outputContractStates.first() as CaseReportState
            output.participants.size>= 2
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as CaseReportState
            transaction.signatories.containsAll(output.participants)
        }

        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is Create -> {
                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
                CREATE_CR_COMMAND_JUST_FOR_INVESTIGATOR using {
                    val output = transaction.outputContractStates.single() as CaseReportState
                    output.holderCaseReport.commonName == "Investigator" &&
                            output.holderCaseReport.organization == "Org1"
                }
            }
            // Rules applied only to transactions with the Update Command.
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
                UPDATE_COMMAND_JUST_FOR_HOLDER_ONLY using {
                    val input = transaction.inputContractStates.single() as CaseReportState
                    input.holderCaseReport.organization == "Org1"
                }

                val input = transaction.inputContractStates.single() as CaseReportState
                val output = transaction.outputContractStates.single() as CaseReportState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.idCase == output.idCase)
                UPDATE_COMMAND_CASENUM_SHOULD_NOT_CHANGE using (input.caseNumber == output.caseNumber)
                UPDATE_COMMAND_FR_SHOULD_NOT_CHANGE using (input.firstResponder == output.firstResponder)
                UPDATE_COMMAND_FRORG_SHOULD_NOT_CHANGE using (input.organisationName == output.organisationName)
                UPDATE_COMMAND_HOLDER_SHOULD_NOT_CHANGE using (input.holderCaseReport == output.holderCaseReport)
                UPDATE_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE using (input.digitalEvidencePack == output.digitalEvidencePack)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (input.participants == output.participants)
            }
            is Transfer -> {
                TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                TRANSFER_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as CaseReportState
                val output = transaction.outputContractStates.single() as CaseReportState
                TRANSFER_COMMAND_ID_SHOULD_NOT_CHANGE using (input.idCase == output.idCase)
                TRANSFER_COMMAND_CASENUM_SHOULD_NOT_CHANGE using (input.caseNumber == output.caseNumber)
                TRANSFER_COMMAND_CASENAME_SHOULD_NOT_CHANGE using (input.caseName == output.caseName)
                TRANSFER_COMMAND_SUSNAME_SHOULD_NOT_CHANGE using (input.suspectName == output.suspectName)
                TRANSFER_COMMAND_VICNAME_SHOULD_NOT_CHANGE using (input.victimName == output.victimName)
                TRANSFER_COMMAND_TIME_SHOULD_NOT_CHANGE using (input.dateNtime == output.dateNtime)
                TRANSFER_COMMAND_TOOLNAME_SHOULD_NOT_CHANGE using (input.toolName == output.toolName)
                TRANSFER_COMMAND_TOOLDESC_SHOULD_NOT_CHANGE using (input.toolsDesc == output.toolsDesc)
                TRANSFER_COMMAND_FR_SHOULD_NOT_CHANGE using (input.firstResponder == output.firstResponder)
                TRANSFER_COMMAND_FRORG_SHOULD_NOT_CHANGE using (input.organisationName == output.organisationName)
                TRANSFER_COMMAND_STATUSCASE_SHOULD_NOT_CHANGE using (input.statusCase == output.statusCase)
                TRANSFER_COMMAND_VALIDATE_SHOULD_NOT_CHANGE using (input.validationStatus == output.validationStatus)
                TRANSFER_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE using (input.digitalEvidencePack == output.digitalEvidencePack)
                TRANSFER_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (input.participants == output.participants)
                val allowedOrgs = listOf("Org1", "Org3")
                TRANSFER_COMMAND_JUST_FOR_HOLDER_ONLY using (allowedOrgs.contains(input.holderCaseReport.organization))
                TRANSFER_COMMAND_SHOULD_BE_CHANGE_HOLDER using (input.holderCaseReport != output.holderCaseReport)

            }
            is Validate -> {
                VALIDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                VALIDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as CaseReportState
                val output = transaction.outputContractStates.single() as CaseReportState
                VALIDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.idCase == output.idCase)
                VALIDATE_COMMAND_CASENUM_SHOULD_NOT_CHANGE using (input.caseNumber == output.caseNumber)
                VALIDATE_COMMAND_CASENAME_SHOULD_NOT_CHANGE using (input.caseName == output.caseName)
                VALIDATE_COMMAND_SUSNAME_SHOULD_NOT_CHANGE using (input.suspectName == output.suspectName)
                VALIDATE_COMMAND_VICNAME_SHOULD_NOT_CHANGE using (input.victimName == output.victimName)
                VALIDATE_COMMAND_TIME_SHOULD_NOT_CHANGE using (input.dateNtime == output.dateNtime)
                VALIDATE_COMMAND_TOOLNAME_SHOULD_NOT_CHANGE using (input.toolName == output.toolName)
                VALIDATE_COMMAND_TOOLDESC_SHOULD_NOT_CHANGE using (input.toolsDesc == output.toolsDesc)
                VALIDATE_COMMAND_FR_SHOULD_NOT_CHANGE using (input.firstResponder == output.firstResponder)
                VALIDATE_COMMAND_FRORG_SHOULD_NOT_CHANGE using (input.organisationName == output.organisationName)
                VALIDATE_COMMAND_STATUSCASE_SHOULD_NOT_CHANGE using (input.statusCase == output.statusCase)
                VALIDATE_COMMAND_EVIDENCEPACK_SHOULD_NOT_CHANGE using (input.digitalEvidencePack == output.digitalEvidencePack)
                VALIDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (input.participants == output.participants)

                VALIDATE_COMMAND_JUST_FOR_ORG3 using (input.holderCaseReport.organization == "Org3")
                VALIDATE_COMMAND_VALIDATE_SHOULD_BE_CHANGE using (input.validationStatus != output.validationStatus)
            }

            is AddEvidencePack -> {
                ADDEVIDENCEPACK_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                ADDEVIDENCEPACK_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as CaseReportState
//                val output = transaction.outputContractStates.single() as CaseReportState
                ADDEVIDENCEPACK_COMMAND_JUST_FOR_CUSTODIAN using (input.holderCaseReport.commonName == "Custodian")
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