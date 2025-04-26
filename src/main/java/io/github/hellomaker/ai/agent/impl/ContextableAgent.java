package io.github.hellomaker.ai.agent.impl;


import io.github.hellomaker.ai.agent.AgentComponent;
import io.github.hellomaker.ai.agent.AgentContext;
import io.github.hellomaker.ai.agent.ComponentContext;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author hellomaker
 */
public class ContextableAgent<IN, OUT> implements AgentComponent<IN, OUT> {


    @Override
    public OUT doChain(IN input) {
        ContextableComponent<IN, ?> rootComponent1 = getRootComponent();
        DefaultComponentContext<IN> context = new DefaultComponentContext<>();
        context.setInput(input);
        context.setAgentContext(getAgentContext(input));
        beforeChain(context);
        Object o = rootComponent1.doChainAndNext(context);
        afterChain(context, o);
        try {
            if (o instanceof ComponentContext context1) {
                return (OUT) context1.getInput();
            }
            return (OUT) o;
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public AgentContext getAgentContext(IN input) {
        return DefaultAgentContext.builder()
            .chatMemory(new InMemoryChatMemory())
            .userMessage(new UserMessage(""))
            .build();
    }

    public void beforeChain(ComponentContext<IN> context) {

    }

    public void afterChain(ComponentContext<IN> context, Object o) {

    }

    @Override
    public String componentKey() {
        return "agent : ";
    }

    ContextableComponent<IN, ?> getRootComponent() {
        if (rootComponent == null) {
            throw new RuntimeException("not set root component!");
        }
        return rootComponent;
    }

    ContextableComponent<IN, ?> rootComponent;

    public void setRootComponent(ContextableComponent<IN, ?> rootComponent) {
        this.rootComponent = rootComponent;
    }

}
