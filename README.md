<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

# Telegram Bot Agent

![Build](https://github.com/embabel/kotlin-agent-template/actions/workflows/maven.yml/badge.svg)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) ![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) ![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)

<br clear="left"/>


A Telegram bot built with the [Embabel framework](https://github.com/embabel/embabel-agent) for AI-powered messaging.

Built with Spring Boot 3.5.9 and Embabel 0.3.1.

# Setup

## Prerequisites

1. A Telegram bot token (see [TELEGRAM_INTEGRATION.md](./TELEGRAM_INTEGRATION.md) for setup instructions)
2. Your Telegram chat ID

## Configuration

Set your Telegram bot token via environment variable:

```bash
export TELEGRAM_BOT_TOKEN="your_bot_token_here"
```

Or edit `src/main/resources/application.properties`:

```properties
telegram.bot.token=your_bot_token_here
```

# Running

Start the Embabel Spring Shell:

```bash
./scripts/shell.sh
```

## Usage

### Natural Language Commands (Primary Method)

Use the `x` command with natural language to send messages:

```shell
# Send a message to a user
x "Message user 8360446449 'Hello from Embabel'"

# Send availability request
x "Message user 8360446449 'Are you available for a meeting next week?'"

# Notify about completion
x "Send a telegram to 8360446449 saying the deployment is complete"
```

The agent will:
1. Parse your natural language request
2. Extract the chat ID and message content
3. Send the message via Telegram
4. Confirm delivery

### Shell Command (Testing)

For direct testing without natural language parsing:

```shell
telegram --chat-id 8360446449 --message "Hello from Embabel!"
```

## How It Works

The `TelegramNotificationAgent` uses Embabel's AI capabilities to:
- Understand natural language messaging requests
- Extract chat IDs and message content
- Call the `sendTelegramMessage` tool automatically
- Provide confirmation responses

This messaging capability is designed to be part of larger agent workflows, such as organizing work trips by collecting availability from multiple users.
