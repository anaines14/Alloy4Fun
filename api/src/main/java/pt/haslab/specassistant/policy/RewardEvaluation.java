package pt.haslab.specassistant.policy;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.function.BiFunction;

public enum RewardEvaluation implements BiFunction<HintNode, HintEdge, Double> {
    NONE,
    ONE,
    TED,
    HOPS,
    VISITS,
    LEAVES,
    HOPSnTED;

    public Double apply(HintNode state, HintEdge action) {
        return (double) switch (this) {
            case NONE -> 0.0;
            case ONE -> 1.0;
            case TED -> action.editDistance;
            case HOPS -> state.hopDistance;
            case VISITS -> state.visits;
            case LEAVES -> state.leaves;
            default -> state.hopDistance * action.editDistance;
        };
    }

}
