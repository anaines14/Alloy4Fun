package pt.haslab.specassistant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.policy.PolicyContext;
import pt.haslab.specassistant.policy.ProbabilityEvaluation;
import pt.haslab.specassistant.policy.RewardEvaluation;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.util.Ordered;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyManager {

    private static final Logger LOG = Logger.getLogger(PolicyManager.class);
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;

    @ConfigProperty(name = "policy.discount", defaultValue = "0.99")
    Double mutationsEnabled;

    @ConfigProperty(name = "policy.reward", defaultValue = "HOPSnTED")
    RewardEvaluation rewardEvaluation;

    @ConfigProperty(name = "policy.probability", defaultValue = "EDGE")
    ProbabilityEvaluation probabilityEvaluation;


    public void computePolicyForGraph(ObjectId graph_id) {
        HintGraph.removeAllPolicyStats(graph_id);
        long t = System.nanoTime();
        Collection<PolicyContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(PolicyContext::init).toList();

        while (!batch.isEmpty()) {
            PolicyContext targetScore = Collections.min(batch);

            Map<Boolean, List<PolicyContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(targetScore::isGreaterOrEqualTo));

            try {
                List<CompletableFuture<List<PolicyContext>>> actionPool = targetIds.get(true)
                        .stream()
                        .peek(PolicyContext::save)
                        .map(x -> CompletableFuture.supplyAsync(() ->
                                edgeRepo.streamByDestinationNodeIdAndAllScoreGT(x.nodeId(), x.score)
                                        .map(y -> x.nextContext(y, nodeRepo.findById(y.origin))).filter(Objects::nonNull).toList())).toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<PolicyContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<PolicyContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(Collectors.toMap(PolicyContext::nodeId, x -> x, Ordered::min)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
        HintGraph.registerPolicyCalculationTime(graph_id, System.nanoTime() - t);
    }


}
