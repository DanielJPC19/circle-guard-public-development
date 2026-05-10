package com.circleguard.form.service

import com.circleguard.form.model.HealthSurvey
import com.circleguard.form.model.Question
import com.circleguard.form.model.Questionnaire
import com.circleguard.form.model.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SymptomMapperTest {

    private val mapper = SymptomMapper()

    @Test
    fun `hasSymptoms returns true when fever question answered YES`() {
        val questionId = UUID.randomUUID()

        val question = Question.builder()
            .id(questionId)
            .text("Do you have fever?")
            .type(QuestionType.YES_NO)
            .orderIndex(1)
            .build()

        val questionnaire = Questionnaire.builder()
            .id(UUID.randomUUID())
            .title("Daily Health Check")
            .version(1)
            .questions(listOf(question))
            .build()

        val survey = HealthSurvey.builder()
            .anonymousId(UUID.randomUUID())
            .responses(mapOf(questionId.toString() to "YES"))
            .build()

        assertTrue(mapper.hasSymptoms(survey, questionnaire))
    }

    @Test
    fun `hasSymptoms returns false when survey has null responses`() {
        val questionnaire = Questionnaire.builder()
            .id(UUID.randomUUID())
            .title("Daily Health Check")
            .version(1)
            .questions(emptyList())
            .build()

        val survey = HealthSurvey.builder()
            .anonymousId(UUID.randomUUID())
            .responses(null)
            .build()

        assertFalse(mapper.hasSymptoms(survey, questionnaire))
    }
}
