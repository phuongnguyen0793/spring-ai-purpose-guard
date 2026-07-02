package com.example.springai.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    // ChatClient is the high-level Spring AI abstraction over the underlying
    // ChatModel (Ollama here). Used by the offline relation trainer.
    @Bean
    fun chatClient(chatModel: ChatModel): ChatClient = ChatClient.builder(chatModel).build()
}
