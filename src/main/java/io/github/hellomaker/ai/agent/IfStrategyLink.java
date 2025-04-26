package io.github.hellomaker.ai.agent;

/**
 * @author hellomaker
 */
public interface IfStrategyLink<OUT> {

    default boolean ifMatch(OUT output) {
        return true;
    }

    AbstractAgentComponent<OUT, ?> nextComponent();

}