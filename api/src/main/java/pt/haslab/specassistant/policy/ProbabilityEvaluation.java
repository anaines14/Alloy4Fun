package pt.haslab.specassistant.policy;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.function.BiFunction;

public enum ProbabilityEvaluation implements BiFunction<HintNode, HintEdge, Double> {
    NONE,
    EDGE,
    ;


    public Double apply(HintNode state, HintEdge action) {
        if (this == EDGE)
            return (double) action.count / (double) state.leaves;
        return 1.0;
    }
}
