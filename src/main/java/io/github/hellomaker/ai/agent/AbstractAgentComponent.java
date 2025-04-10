package io.github.hellomaker.ai.agent;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAgentComponent<IN, OUT> implements AgentComponent<IN, OUT> {

    @Override
    public OUT doChain(IN input) {
        try {
            return doChainInternal(input);
        } catch (Exception e) {
            return exception(e, input);
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

    public abstract OUT doChainInternal(IN input) ;

    public abstract OUT exception(Exception e, IN input);

    private List<IfStrategyLink<OUT>> strategies;

    public boolean containsStrategy() {
        return strategies != null && !strategies.isEmpty();
    }
    //    @Override
    public List<IfStrategyLink<OUT>> strategyLinks() {
        return strategies;
    }
    public void addStrategy(IfStrategyLink<OUT> strategy) {
        if (strategies == null) {
            strategies = new ArrayList<>();
        }
        strategies.add(strategy);
    }
}