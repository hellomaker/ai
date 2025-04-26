package io.github.hellomaker.ai.agent.impl;


import io.github.hellomaker.ai.agent.AbstractContextAgentComponent;
import io.github.hellomaker.ai.agent.AgentContext;
import io.github.hellomaker.ai.agent.ComponentContext;

import java.util.Map;

public abstract class ContextableComponent<IN, OUT> extends AbstractContextAgentComponent<IN, OUT> {

    public ContextableComponent(String componentKey) {
        super(componentKey);
    }

    public ContextableComponent() {
        super("default");
    }

//    @Override
//    protected OUT doChainOnContext(ComponentContext<IN> input) {
//        return null;
//    }

    @Override
    public OUT onException(Exception e, ComponentContext<IN> input) {
//        return null;
        throw new RuntimeException(e);
    }

    @Override
    protected ComponentContext<OUT> componentContext(ComponentContext<IN> input, OUT output) {
        DefaultComponentContext<OUT> nextContext = new DefaultComponentContext<>();
        Map<String, Object> componentExecuteMap = input.componentExecuteMap();
        if (componentExecuteMap != null && !componentExecuteMap.isEmpty()) {
            nextContext.putAllExecuteMap(componentExecuteMap);
        }
        Map<String, Object> stringObjectMap = input.contextVariablesMap();
        if (stringObjectMap != null && !stringObjectMap.isEmpty()) {
            nextContext.putAllContextVariablesMap(stringObjectMap);
        }
        AgentContext agentContext = input.getAgentContext();
        nextContext.setAgentContext(agentContext);
        nextContext.setInput(output);
        return nextContext;
    }


}
