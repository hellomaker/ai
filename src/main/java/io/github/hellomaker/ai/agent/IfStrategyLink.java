package io.github.hellomaker.ai.agent;

/**
 * @author hellomaker
 */
public interface StrategyLink<OUT> {

    boolean isMatch(OUT output);

    AbstractAgentComponent<OUT, ?> nextComponent();

}