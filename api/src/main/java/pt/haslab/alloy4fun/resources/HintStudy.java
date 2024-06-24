package pt.haslab.alloy4fun.resources;

import edu.mit.csail.sdg.parser.CompModule;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.services.HintMerge;
import pt.haslab.specassistant.data.models.Challenge;
import pt.haslab.specassistant.data.models.Model;
import pt.haslab.specassistant.data.policy.PolicyOption;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.PolicyManager;
import pt.haslab.specassistant.services.SpecAssistantTestService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Path("/study")
public class HintStudy {

    @Inject
    Logger log;

    @Inject
    GraphManager graphManager;
    @Inject
    PolicyManager policyManager;

    @Inject
    SpecAssistantTestService testService;

    @Inject
    HintMerge merge;


    @POST
    @Path("/test-all-policies-on-formulas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testFormulas(@QueryParam("model_id") String model_id, Map<String, List<String>> predicateToFormulaStrings) {

        List<PolicyTestResponse> responses = new ArrayList<>();

        CompModule base_world = Model.getWorld(model_id);

        Map<Challenge, List<String>> challenges = graphManager.getModelChallenges(model_id)
                .stream()
                .filter(x -> predicateToFormulaStrings.containsKey(x.getCmd_n()))
                .collect(Collectors.toMap(x -> x, x -> predicateToFormulaStrings.get(x.getCmd_n())));

        Set<ObjectId> graph_ids = challenges.keySet().stream().map(Challenge::getGraph_id).collect(Collectors.toSet());

        PolicyOption.samples.forEach((pName, pOption) -> {
            graph_ids.forEach(g -> policyManager.computePolicyForGraph(g, pOption));
            challenges.forEach((challenge, formulas) ->
                    formulas.forEach(formula -> {
                                Map<String, String> hint = merge.specAssistantGraphToHigenaFormula(base_world, challenge, formula);
                                responses.add(PolicyTestResponse.builder()
                                        .formula(formula)
                                        .predicate(challenge.getCmd_n())
                                        .policy(pName)
                                        .hint(hint.get("hint"))
                                        .normalizedFormula(hint.get("oldExpr"))
                                        .nextFormula(hint.get("nextExpr"))
                                        .build()
                                );
                            }
                    ));
        });

        return Response.ok(responses).build();
    }

    @POST
    @Path("/test-spec-assist")
    public Response testSpecAssist(Map<String, List<String>> names_to_model_ids) {
        log.info("Performing train/test split and rebuilding hint Database");
        testService.specTestDefaultPolicies(names_to_model_ids);
        log.info("Evaluating test data-set");
        testService.testPartitionFromFile("testing.txt");
        return Response.ok().build();
    }


    @POST
    @Path("/test-TAR")
    public Response testTAR() {
        log.info("Performing train/test split and rebuilding hint Database");
        Set<String> model_ids = testService.readSetFromFile("testing.txt");
        Predicate<Model> test = x -> model_ids.contains(x.getId());
        if (model_ids.isEmpty())
            test = x -> true;
        try {
            testService.testAllChallengesWithTAR(test).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/fix-naming")
    public Response fixNaming(Map<String, List<String>> names_to_model_ids) {
        log.info("Performing train/test split and rebuilding hint Database");
        testService.fixTestGraphIds();
        return Response.ok().build();
    }


    @Data
    @Builder
    public static class PolicyTestResponse {
        private String policy, formula, predicate, hint, nextFormula, normalizedFormula;
    }


}
