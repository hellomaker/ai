package com.snibe.ixlabai.service.agent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.UserMessage;

public interface InputWithContext<IN> {

    IN getInput();

    ChatMemory getChatMemory();

    Object getContext();

    UserMessage getSrcUserMessage();

}
