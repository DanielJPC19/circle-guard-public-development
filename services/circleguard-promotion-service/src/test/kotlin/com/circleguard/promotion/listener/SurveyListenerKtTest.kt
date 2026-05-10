package com.circleguard.promotion.listener

import com.circleguard.promotion.service.HealthStatusService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExtendWith(MockitoExtension::class)
class SurveyListenerKtTest {

    @Mock
    lateinit var healthStatusService: HealthStatusService

    @InjectMocks
    lateinit var surveyListener: SurveyListener

    @Test
    fun `onSurveySubmitted promotes user to SUSPECT when hasSymptoms is true`() {
        val event = mapOf<String, Any>("anonymousId" to "user-abc-123", "hasSymptoms" to true)

        surveyListener.onSurveySubmitted(event)

        verify(healthStatusService).updateStatus("user-abc-123", "SUSPECT")
    }

    @Test
    fun `onSurveySubmitted does not call updateStatus when hasSymptoms is false`() {
        val event = mapOf<String, Any>("anonymousId" to "user-abc-123", "hasSymptoms" to false)

        surveyListener.onSurveySubmitted(event)

        verifyNoInteractions(healthStatusService)
    }
}
