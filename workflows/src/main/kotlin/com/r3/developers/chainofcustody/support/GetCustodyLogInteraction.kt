package com.r3.developers.chainofcustody.support

import com.r3.developers.chainofcustody.states.DigitalEvidenceState
import com.r3.developers.chainofcustody.states.CaseReportState
import com.r3.developers.chainofcustody.states.AnalysisReportState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

data class  GetCustodyLogInteractionFlowArgs (
    val typeReport: String, val id: UUID
)
class GetCustodyLogInteractionFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("GetCustodyLogInteractionFlow.call() called")
        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetCustodyLogInteractionFlowArgs::class.java)

            val result = when (flowArgs.typeReport) {
                "Digital-Evidence" -> {
                    val state = ledgerService
                        .findUnconsumedStatesByExactType(DigitalEvidenceState::class.java, 100, Instant.now())
                        .results.singleOrNull { it.state.contractState.id == flowArgs.id }
                        ?: throw CordaRuntimeException("DigitalEvidence with cid ${flowArgs.id} not found")
                    state.state.contractState.custodyHistory
                }

                "Case-Report" -> {
                    val state = ledgerService
                        .findUnconsumedStatesByExactType(CaseReportState::class.java, 100, Instant.now())
                        .results.singleOrNull { it.state.contractState.idCase == flowArgs.id }
                        ?: throw CordaRuntimeException("CaseReport with caseNumber ${flowArgs.id} not found")
                    state.state.contractState.custodyHistory
                }

                "Analysis-Report" -> {
                    val state = ledgerService
                        .findUnconsumedStatesByExactType(AnalysisReportState::class.java, 100, Instant.now())
                        .results.singleOrNull { it.state.contractState.idReport == flowArgs.id }
                        ?: throw CordaRuntimeException("AnalysisReport with idEvidence ${flowArgs.id} not found")
                    state.state.contractState.custodyHistory
                }

                else -> throw CordaRuntimeException("Invalid type: ${flowArgs.typeReport}")
            }

            return jsonMarshallingService.format(result)

        } catch (e: Exception) {
            log.error("Error in GetCustodyLogFlow: ${e.message}", e)
            throw e
        }
    }
}
