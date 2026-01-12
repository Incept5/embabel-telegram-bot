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

import org.springframework.ai.tool.annotation.Tool
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


@Component
class TelegramTools(
    @Value("\${telegram.bot.token}") private val botToken: String
) : DefaultAbsSender(DefaultBotOptions()) {

    private val logger = LoggerFactory.getLogger(TelegramTools::class.java)

    override fun getBotToken(): String = botToken

    @Tool(description = "Sends a text message to a Telegram user or chat by chat ID")
    fun sendTelegramMessage(chatId: Long, message: String): String {
        logger.info("Attempting to send Telegram message to chat ID: $chatId with token: ${botToken.take(10)}...")
        return try {
            val sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .build()

            val result = execute(sendMessage)
            logger.info("Successfully sent message to chat ID: $chatId. Message ID: ${result.messageId}")
            "Message sent successfully to chat ID: $chatId. Message ID: ${result.messageId}"
        } catch (e: TelegramApiException) {
            logger.error("Failed to send Telegram message to chat ID: $chatId. Error: ${e.message}", e)
            "Failed to send message: ${e.message}"
        } catch (e: Exception) {
            logger.error("Unexpected error sending Telegram message to chat ID: $chatId", e)
            "Failed to send message: ${e.message}"
        }
    }
}