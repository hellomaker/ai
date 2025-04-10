package io.github.hellomaker.ai.agent;

/**
 * @author hellomaker
 */
public class Agent<IN, OUT> implements AgentComponent<IN, OUT>{


    @Override
    public OUT doChain(IN input) {
        AbstractAgentComponent<IN, ?> rootComponent1 = getRootComponent();
        Object o = rootComponent1.doChainAndNext(input);
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