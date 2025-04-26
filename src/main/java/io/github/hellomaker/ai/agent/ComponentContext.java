package io.github.hellomaker.ai.agent;

import java.util.Map;

public interface ComponentContext<Input> {

    AgentContext getAgentContext();

    Map<String, Object> componentExecuteMap();

    void putExecuteMap(String key, Object value);

    Map<String, Object> contextVariablesMap();

    void putContextVariablesMap(String key, Object value);

    Input getInput();

}
