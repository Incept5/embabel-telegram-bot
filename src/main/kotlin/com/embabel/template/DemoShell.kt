package com.embabel.template

import com.embabel.template.tools.TelegramTools
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class DemoShell(
    private val telegramTools: TelegramTools,
) {

    @ShellMethod("Send a Telegram message")
    fun telegram(
        @ShellOption(help = "Chat ID to send the message to") chatId: Long,
        @ShellOption(help = "Message to send") message: String
    ): String {
        val result = telegramTools.sendTelegramMessage(chatId, message)
        return result
    }
}
