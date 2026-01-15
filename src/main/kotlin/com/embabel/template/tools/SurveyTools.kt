/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.template.tools

import com.embabel.template.domain.SurveyResults
import com.embabel.template.entity.SurveyStatus
import com.embabel.template.service.SurveyService
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class SurveyTools(
    private val surveyService: SurveyService,
    private val telegramTools: TelegramTools
) {
    private val logger = LoggerFactory.getLogger(SurveyTools::class.java)

    @Tool(description = "Create and send a survey to a Telegram chat, waiting for a specific number of responses")
    fun createSurvey(chatId: Long, question: String, expectedCount: Int): String {
        logger.info("Creating survey for chat $chatId with question: $question, expectedCount: $expectedCount")

        val survey = surveyService.createSurvey(chatId, question, expectedCount)

        val message = """
            📊 New Survey!

            Question: $question

            Please reply with your answer. Survey completes after $expectedCount responses.
        """.trimIndent()

        val result = telegramTools.sendTelegramMessage(chatId, message)

        return "Survey created successfully (ID: ${survey.id}). Waiting for $expectedCount responses. $result"
    }

    @Tool(description = "Get the status of the current active survey in a Telegram group")
    fun getSurveyStatus(chatId: Long): String {
        val survey = surveyService.getActiveSurvey(chatId)
            ?: return "No active survey in this group"

        val responseCount = surveyService.getActiveSurvey(chatId)?.let {
            "Active"
        } ?: "Unknown"

        return "Active survey: ${survey.question}\nStatus: Waiting for ${survey.expectedCount} responses"
    }

    @Tool(description = "Wait for a specific survey to be completed and return the results. Polls until survey is complete or timeout is reached. Sends reminders every 10 minutes.")
    fun waitForSurveyCompletion(surveyId: Long, timeoutMinutes: Int = 15, reminderIntervalMinutes: Int = 5): SurveyResults {
        logger.info("Waiting for survey $surveyId completion with timeout of $timeoutMinutes minutes")

        val startTime = Instant.now()
        val timeout = Duration.ofMinutes(timeoutMinutes.toLong())
        var lastReminderTime = startTime
        val reminderInterval = Duration.ofMinutes(reminderIntervalMinutes.toLong())

        while (Duration.between(startTime, Instant.now()) < timeout) {
            val survey = surveyService.getSurveyById(surveyId)

            if (survey.status == SurveyStatus.COMPLETED) {
                logger.info("Survey $surveyId completed successfully")
                return surveyService.getSurveyResults(surveyId)
            }

            val timeSinceLastReminder = Duration.between(lastReminderTime, Instant.now())
            if (timeSinceLastReminder >= reminderInterval) {
                sendSurveyReminder(survey)
                lastReminderTime = Instant.now()
            }

            logger.debug("Survey $surveyId still active, waiting... (${Duration.between(startTime, Instant.now()).seconds}s elapsed)")
            Thread.sleep(2000)
        }

        throw IllegalStateException("Survey $surveyId timed out after $timeoutMinutes minutes")
    }

    private fun sendSurveyReminder(survey: com.embabel.template.entity.Survey) {
        val responseCount = surveyService.getResponseCount(survey.id!!)
        val remaining = survey.expectedCount - responseCount

        val reminderMessage = """
            ⏰ Survey Reminder

            We're still waiting for $remaining more response${if (remaining == 1) "" else "s"}.

            Question: ${survey.question}

            Please reply with your answer if you haven't yet!
        """.trimIndent()

        logger.info("Sending reminder for survey ${survey.id} - $remaining responses remaining")
        telegramTools.sendTelegramMessage(survey.chatId, reminderMessage)
    }
}
