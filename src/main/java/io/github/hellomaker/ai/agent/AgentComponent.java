package com.snibe.ixlabai.service.agent;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.tool.ToolProvider;


/**
 * ai agent 组件
 * @author xianzhikun
 */
public interface AgentComponent<IN, OUT> {

//    ChatMemoryProvider chatMemoryProvider();

//    SystemMessage systemMessage();
//
//    MutiModelProvider mutiModelProvider();
//
//    ToolProvider toolProvider();

    OUT doChain(IN input, ChatMemory chatMemory, UserMessage userMessage);

//    List<StrategyLink<OUT>> strategyLinks();

}