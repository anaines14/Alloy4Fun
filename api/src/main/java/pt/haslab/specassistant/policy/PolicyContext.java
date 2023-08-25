package pt.haslab.specassistant.policy;

import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.util.Ordered;

public class PolicyContext implements Ordered<PolicyContext> {

    public final Double discount; // gamma
    public final RewardEvaluation R; // Imminent Reward Function
    public final ProbabilityEvaluation P; // Imminent Reward Function

    public HintNode node;
    public Double score;
    public Integer distance;


    public PolicyContext(HintNode node, Double discount, RewardEvaluation r, ProbabilityEvaluation p) {
        this.node = node;
        this.discount = discount;
        R = r;
        P = p;
    }

    public static PolicyContext init(HintNode n, Double discount, RewardEvaluation rw_eval, ProbabilityEvaluation prob_eval) {
        PolicyContext result = new PolicyContext(n, discount, rw_eval, prob_eval);
        result.node = n;
        result.score = 0.0;
        result.distance = 0;
        return result;
    }

    public static PolicyContext init(HintNode n) {
        return init(n, 0.99, RewardEvaluation.HOPSnTED, ProbabilityEvaluation.EDGE);
    }

    public ObjectId nodeId() {
        return node.id;
    }

    public void save() {
        this.node.score = this.score;
        this.node.persistOrUpdate();
    }

    public PolicyContext nextContext(HintEdge action, HintNode previousState) {
        PolicyContext next = new PolicyContext(previousState, this.discount, this.R, this.P);
        previousState.hopDistance = next.distance = distance + 1;
        action.score = next.applyValueFunction(action, this.score);
        action.update();
        return next;
    }

    public Double applyValueFunction(HintEdge action, Double followUpScore) {
        return this.score = R.apply(node, action) + discount * P.apply(node, action) * followUpScore;
    }


    @Override
    public int compareTo(PolicyContext o) {
        return this.score.compareTo(o.score);
    }

    @Override
    public String toString() {
        return "{%s, score=%s, distance=%d}".formatted(node, score, distance);
    }
}
