package io.github.hellomaker.ai.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractAgentComponent<IN, OUT> implements AgentComponent<IN, OUT> {

    @Override
    public String componentKey() {
        return componentKey;
    }

    private final String componentKey;

    public AbstractAgentComponent(String componentKey) {
        this.componentKey = componentKey;
    }

    @Override
    public OUT doChain(IN input) {
        try {
            return doChainInternal(input);
        } catch (Exception e) {
            return handelException(e, input);
        }
    }

    public Object doChainAndNext(IN input) {
        OUT out = doChain(input);
        if (containsStrategy()) {
            for (IfStrategyLink<OUT> strategy : strategies) {
                if (strategy.ifMatch(out)) {
                    return strategy.nextComponent().doChainAndNext(out);
                }
            }
        }
        return out;
    }

    protected abstract OUT doChainInternal(IN input) ;

    public OUT handelException(Exception e, IN input) throws RuntimeException {
        throw new RuntimeException(e);
    }

    private List<IfStrategyLink<OUT>> strategies;

    public boolean containsStrategy() {
        return strategies != null && !strategies.isEmpty();
    }

    //    @Override
    public List<IfStrategyLink<OUT>> strategyLinks() {
        return strategies;
    }
    public void ifStrategy(IfStrategyLink<OUT> strategy) {
        if (strategies == null) {
            strategies = new ArrayList<>();
        }
        strategies.add(strategy);
    }

    public void ifStrategy(Function<OUT, Boolean> strategy, AbstractAgentComponent<OUT, ?> component) {
        if (strategies == null) {
            strategies = new ArrayList<>();
        }
        strategies.add(new IfStrategyLink<OUT>() {
            @Override
            public boolean ifMatch(OUT output) {
                return strategy.apply(output);
            }

            @Override
            public AbstractAgentComponent<OUT, ?> nextComponent() {
                return component;
            }
        });
    }

    public void thenStrategy(AbstractAgentComponent<OUT, ?> component) {
        if (strategies == null) {
            strategies = new ArrayList<>();
        }
        strategies.add(() -> component);

    }
}