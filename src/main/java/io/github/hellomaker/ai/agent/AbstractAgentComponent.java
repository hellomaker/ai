package com.snibe.ixlabai.service.agent;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAgentComponent<IN, OUT> implements AgentComponent<IN, OUT> {

    @Override
    public OUT doChain(IN input, ChatMemory chatMemory, UserMessage userMessage) {
        try {
            return doChainInternal(input, chatMemory, userMessage);
        } catch (Exception e) {
            return exception(e, input, chatMemory, userMessage);
        }
    }

    public Object doChainAndNext(IN input, ChatMemory chatMemory, UserMessage userMessage) {
        OUT out = doChain(input, chatMemory, userMessage);
        if (containsStrategy()) {
            for (StrategyLink<OUT> strategy : strategies) {
                if (strategy.isMatch(out)) {
                    return strategy.nextComponent().doChainAndNext(out, chatMemory, userMessage);
                }
            }
        }
        return out;
    }

    public abstract OUT doChainInternal(IN input, ChatMemory chatMemory, UserMessage userMessage) ;

    public abstract OUT exception(Exception e, IN input, ChatMemory chatMemory, UserMessage userMessage);

    private List<StrategyLink<OUT>> strategies;

    public boolean containsStrategy() {
        return strategies != null && !strategies.isEmpty();
    }
    //    @Override
    public List<StrategyLink<OUT>> strategyLinks() {
        return strategies;
    }
    public void addStrategy(StrategyLink<OUT> strategy) {
        if (strategies == null) {
            strategies = new ArrayList<>();
        }
        strategies.add(strategy);
    }
}