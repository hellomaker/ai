package io.github.hellomaker.ai.agent.impl;



import io.github.hellomaker.ai.agent.AgentContext;
import io.github.hellomaker.ai.agent.ComponentContext;

import java.util.HashMap;
import java.util.Map;

public final class DefaultComponentContext<T> implements ComponentContext<T> {

    private AgentContext agentContext;
    private Map<String, Object> componentExecuteMap;
    private Map<String, Object> contextVariablesMap;
    private T input;

    @Override
    public AgentContext getAgentContext() {
        return agentContext;
    }

    @Override
    public Map<String, Object> componentExecuteMap() {
        if (componentExecuteMap == null) {
            componentExecuteMap = new HashMap<>();
        }
        return componentExecuteMap;
    }

    @Override
    public void putExecuteMap(String key, Object value) {
        if (componentExecuteMap == null) {
            componentExecuteMap = new HashMap<>();
        }
        componentExecuteMap.put(key, value);
    }

    @Override
    public Map<String, Object> contextVariablesMap() {
        if (contextVariablesMap == null) {
            contextVariablesMap = new HashMap<>();
        }
        return contextVariablesMap;
    }

    @Override
    public void putContextVariablesMap(String key, Object value) {
        if (contextVariablesMap == null) {
            contextVariablesMap = new HashMap<>();
        }
        contextVariablesMap.put(key, value);
    }

    @Override
    public T getInput() {
        return input;
    }

    public void setAgentContext(AgentContext agentContext) {
        this.agentContext = agentContext;
    }
    public void setInput(T input) {
        this.input = input;
    }
    public void putAllExecuteMap(Map<String, Object> componentExecuteMap) {
        if (this.componentExecuteMap == null) {
            this.componentExecuteMap = new HashMap<>();
        }
        this.componentExecuteMap.putAll(componentExecuteMap);
    }
    public void putAllContextVariablesMap(Map<String, Object> contextVariablesMap) {
        if (this.contextVariablesMap == null) {
            this.contextVariablesMap = new HashMap<>();
        }
        this.contextVariablesMap.putAll(contextVariablesMap);
    }
}
