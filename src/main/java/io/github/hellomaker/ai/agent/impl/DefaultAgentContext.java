package io.github.hellomaker.ai.agent.impl;

import io.github.hellomaker.ai.agent.AgentContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

public class DefaultAgentContext implements AgentContext {

    ChatMemory chatMemory;
    UserMessage userMessage;


    @Override
    public ChatMemory chatMemory() {
        return chatMemory;
    }

    @Override
    public UserMessage userMessage() {
        return userMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ChatMemory chatMemory;
        UserMessage userMessage;
        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }
        public Builder userMessage(UserMessage userMessage) {
            this.userMessage = userMessage;
            return this;
        }
        public DefaultAgentContext build() {
            DefaultAgentContext context = new DefaultAgentContext();
            context.chatMemory = chatMemory;
            context.userMessage = userMessage;
            return context;
        }
    }

}
