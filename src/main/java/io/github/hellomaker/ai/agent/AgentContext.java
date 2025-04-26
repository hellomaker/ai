package io.github.hellomaker.ai.agent;


import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

public interface AgentContext {

    ChatMemory chatMemory();

    UserMessage userMessage();

}
