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
package com.embabel.template.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.domain.io.UserInput
import com.embabel.template.domain.SurveyResults
import com.embabel.template.tools.SurveyTools
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

@Agent(description = "Ask questions and collect responses from Telegram users (one or more people). Use this agent whenever the user wants to ASK someone a question and get their response back. This includes surveys, polls, collecting feedback, or asking any individual or group for their input. This agent creates the survey, waits for all responses, and then processes the results. Keywords: ask, question, survey, poll, fetch, collect, response, answer, what is, tell me.")
@Profile("!test")
class SurveyAgent(
    private val surveyTools: SurveyTools
) {
    private val logger = LoggerFactory.getLogger(SurveyAgent::class.java)

    @Action
    fun collectSurveyResponses(
        userInput: UserInput,
        context: OperationContext
    ): SurveyResults {
        val extractionPrompt = """
            Parse this survey request and extract the parameters in this exact format:
            chatId: <number>
            question: <the question>
            expectedCount: <number>

            User request: ${userInput.content}

            Examples:
            - "Ask user 8360446449 what their favourite colour is"
              → chatId: 8360446449
              → question: What is your favourite colour?
              → expectedCount: 1

            - "Ask 5 users in group -123456 what their favorite food is"
              → chatId: -123456
              → question: What is your favorite food?
              → expectedCount: 5

            If the user mentions asking ONE person or ONE user, set expectedCount to 1.
            If they mention a specific number, use that number.

            Respond with ONLY the three lines in the format shown above, nothing else.
        """.trimIndent()

        val extracted = context.ai()
            .withAutoLlm()
            .generateText(extractionPrompt)

        logger.info("Extracted parameters: $extracted")

        val lines = extracted.trim().lines()
        val chatId = lines.find { it.startsWith("chatId:") }
            ?.substringAfter("chatId:")?.trim()?.toLongOrNull()
            ?: throw IllegalArgumentException("Could not extract chatId from: $extracted")

        val question = lines.find { it.startsWith("question:") }
            ?.substringAfter("question:")?.trim()
            ?: throw IllegalArgumentException("Could not extract question from: $extracted")

        val expectedCount = lines.find { it.startsWith("expectedCount:") }
            ?.substringAfter("expectedCount:")?.trim()?.toIntOrNull()
            ?: throw IllegalArgumentException("Could not extract expectedCount from: $extracted")

        logger.info("Parsed - chatId: $chatId, question: $question, expectedCount: $expectedCount")

        val createResult = surveyTools.createSurvey(chatId, question, expectedCount)
        val surveyId = createResult.substringAfter("ID: ").substringBefore(")").toLong()

        logger.info("Survey created (ID: $surveyId), waiting for completion")

        return surveyTools.waitForSurveyCompletion(surveyId, timeoutMinutes = 10)
    }

    @AchievesGoal(description = "Survey responses have been collected and processed")
    @Action
    fun processSurveyResults(
        surveyResults: SurveyResults,
        context: OperationContext
    ): String {
        return context.ai()
            .withAutoLlm()
            .generateText(
                """
                Survey has been completed! The results are available on the blackboard.

                Question: ${surveyResults.question}

                Responses:
                ${surveyResults.responses.joinToString("\n") { response ->
                    "- ${response.userName ?: "User ${response.userId}"}: ${response.response}"
                }}

                Now format a summary message, for example:
                "Availability of everyone: [list each person's response]" or "Everyone's favourite colours: [list each person's response]", make sure it applies to the question that was initially asked.
                """.trimIndent()
            )
    }
}
