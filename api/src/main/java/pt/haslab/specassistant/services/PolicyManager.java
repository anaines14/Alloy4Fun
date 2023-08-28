package pt.haslab.specassistant.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.services.policy.PolicyContext;
import pt.haslab.specassistant.services.policy.ProbabilityEvaluation;
import pt.haslab.specassistant.services.policy.RewardEvaluation;
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

    public void computePolicyForGraph(ObjectId graph_id) {
        computePolicyForGraph(graph_id, 0.99, RewardEvaluation.TED, ProbabilityEvaluation.EDGE);
    }

    public void computePolicyForGraph(ObjectId graph_id, Double policyDiscount, RewardEvaluation rewardEvaluation, ProbabilityEvaluation probabilityEvaluation) {
        HintGraph.removeAllPolicyStats(graph_id);
        nodeRepo.unsetAllScoresFrom(graph_id);
        edgeRepo.unsetAllScoresFrom(graph_id);
        long t = System.nanoTime();
        Collection<PolicyContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(n -> PolicyContext.init(n, policyDiscount, rewardEvaluation, probabilityEvaluation)).toList();

        while (!batch.isEmpty()) {
            PolicyContext targetScore = Collections.min(batch);

            Map<Boolean, List<PolicyContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(targetScore::isGreaterOrEqualTo));


            List<CompletableFuture<List<PolicyContext>>> actionPool = targetIds.get(true)
                    .stream()
                    .peek(PolicyContext::save)
                    .map(x -> CompletableFuture.supplyAsync(() ->
                            edgeRepo.streamByDestinationNodeIdAndAllScoreGT(x.nodeId(), x.score)
                                    .map(y -> x.nextContext(y, nodeRepo.findById(y.origin))).filter(Objects::nonNull).toList())).toList();
            try {
                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Failed to process batch, skipping " + targetIds.size() + " endpoints");
            }

            List<List<PolicyContext>> result = new ArrayList<>();
            result.add(targetIds.get(false));
            for (CompletableFuture<List<PolicyContext>> l : actionPool) {
                try {
                    result.add(l.get());
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Failed to process edges a node, skipping...");
                }
            }
            batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(Collectors.toMap(PolicyContext::nodeId, x -> x, Ordered::min)).values());
        }
        HintGraph.registerPolicyCalculationTime(graph_id, System.nanoTime() - t);
    }
}