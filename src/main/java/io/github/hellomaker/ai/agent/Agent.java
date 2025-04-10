package com.snibe.ixlabai.service.agent;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

import java.util.List;

/**
 * @author hellomaker
 */
public class Agent<IN, OUT> implements AgentComponent<IN, OUT>{
    @Override
    public OUT doChain(IN input, ChatMemory chatMemory, UserMessage userMessage) {
        AbstractAgentComponent<IN, ?> rootComponent1 = getRootComponent();
        Object o = rootComponent1.doChainAndNext(input, chatMemory, userMessage);
        try {
            return (OUT) o;
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    AbstractAgentComponent<IN, ?> getRootComponent() {
        if (rootComponent == null) {
            throw new RuntimeException("not set root component!");
        }
        return rootComponent;
    }

    AbstractAgentComponent<IN, ?> rootComponent;

    public void setRootComponent(AbstractAgentComponent<IN, ?> rootComponent) {
        this.rootComponent = rootComponent;
    }

}