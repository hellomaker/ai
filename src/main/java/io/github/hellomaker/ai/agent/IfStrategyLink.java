package io.github.hellomaker.ai.agent;

/**
 * @author hellomaker
 */
public interface IfStrategyLink<OUT> {

    boolean ifMatch(OUT output);

    AbstractAgentComponent<OUT, ?> nextComponent();

}