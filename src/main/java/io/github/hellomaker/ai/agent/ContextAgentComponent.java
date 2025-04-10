package com.snibe.ixlabai.service.agent;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

public class ContextAgentComponent<IN, OUT> extends AbstractAgentComponent<InputWithContext<IN>, OUT>{


    @Override
    public OUT doChainInternal(InputWithContext<IN> input, ChatMemory chatMemory, UserMessage userMessage) {
        return null;
    }

    @Override
    public OUT exception(Exception e, InputWithContext<IN> input, ChatMemory chatMemory, UserMessage userMessage) {
        return null;
    }
}
