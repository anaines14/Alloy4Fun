package pt.haslab.alloy4fun.resources;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.higena.graph.Graph;
import org.higena.hint.HintGenType;
import org.higena.parser.ExprExtractor;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.request.ExerciseForm;
import pt.haslab.alloy4fun.data.request.HintRequest;
import pt.haslab.alloy4fun.data.request.YearRange;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.services.HintMerge;
import pt.haslab.alloy4fun.repositories.SessionRepository;
import pt.haslab.alloyaddons.Util;
import pt.haslab.specassistant.services.GraphInjestor;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.HintGenerator;
import pt.haslab.specassistant.services.PolicyManager;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.services.policy.ProbabilityEvaluation;
import pt.haslab.specassistant.services.policy.RewardEvaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Path("/hint")
public class AlloyHint {

    private static final Logger LOG = Logger.getLogger(AlloyHint.class);

    @Inject
    GraphManager graphManager;

    @Inject
    HintGenerator hintGenerator;
    @Inject
    GraphInjestor graphInjestor;
    @Inject
    PolicyManager policyManager;

    @Inject
    SessionRepository sessionManager;

    @Inject
    HintMerge hintMerge;

    private static ObjectId getAGraphID(Map<String, ObjectId> graphspace, String prefix, String label) {
        if (!graphspace.containsKey(label))
            graphspace.put(label, HintGraph.newGraph(prefix + "-" + label).id);
        return graphspace.get(label);
    }

    @POST
    @Path("/group")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeGraphAndExercises(List<ExerciseForm> forms, @QueryParam("graph_id") String graph_id_str, @DefaultValue("Unkown") @QueryParam("name") String graph_name) {
        ObjectId graph_id = (graph_id_str == null || graph_id_str.isEmpty() ? HintGraph.newGraph(graph_name) : HintGraph.findById(graph_id_str).orElseThrow()).id;

        forms.forEach(f -> graphManager.generateExercise(graph_id, f.modelId, f.secretCommandCount, f.cmd_n, f.targetFunctions));
        return Response.ok("Sucess").build();
    }

    @POST
    @Path("/group-secrets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeGraphAndExercisesFromCommands(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix) {
        Map<String, ObjectId> graphspace = new HashMap<>();

        model_ids.forEach(id -> graphManager.generateExercisesWithGraphIdFromSecrets(l -> getAGraphID(graphspace, prefix, l), id));

        return Response.ok("Sucess").build();
    }

    @GET
    @Path("/stress-test-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stressHints(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        hintGenerator.testAllHintsOfModel(model_id, yearRange::testDate);
        return Response.ok().build();
    }

    @GET
    @Path("/scan-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModel(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        graphInjestor.parseModelTree(model_id, yearRange::testDate);
        return Response.ok().build();
    }

    @GET
    @Path("/scan-models")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModels(List<String> model_ids, @BeanParam YearRange yearRange) {
        model_ids.forEach(id -> graphInjestor.parseModelTree(id, yearRange::testDate));
        return Response.ok().build();
    }

    @GET
    @Path("/compute-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicy(@QueryParam("graph_id") String hex_string) {
        ObjectId graphId = new ObjectId(hex_string);
        policyManager.computePolicyForGraph(graphId);
        graphManager.debloatGraph(graphId);
        return Response.ok().build();
    }

    @GET
    @Path("/compute-policy-for-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicyOnModel(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id);
            graphManager.debloatGraph(id);
        });
        return Response.ok().build();
    }

    @POST 
    @Path("/compute-ted-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computeTEDPolicy(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id, 1.0, RewardEvaluation.TED, ProbabilityEvaluation.NONE);
        });
        return Response.ok("TED policy computed.").build();
    }

    @POST
    @Path("/compute-popular-node-policy")    
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePopularNodePolicy(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id, 1.0, RewardEvaluation.VISITS, ProbabilityEvaluation.NONE);
        });
        return Response.ok("Popular policy computed.").build();
    }

    
    @POST
    @Path("/compute-popular-edge-policy")    
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePopularEdgePolicy(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id, 1.0, RewardEvaluation.NONE, ProbabilityEvaluation.EDGE);
        });
        return Response.ok("Popular policy computed.").build();
    }

    @GET
    @Path("/full-test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicyOnModel(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix, @QueryParam("yearLower") Integer yearLower, @QueryParam("yearMiddle") Integer yearMiddle, @QueryParam("yearTop") Integer yearTop) {
        CompletableFuture.runAsync(() -> { // Allows the task to survive even if the http request is canceled
            long start = System.nanoTime();
            LOG.info("Starting test for " + prefix + " with model ids " + model_ids);
            graphManager.deleteExerciseByModelIDs(model_ids, true);
            makeGraphAndExercisesFromCommands(model_ids, prefix).close();
            LOG.debug("Scanning models");
            try {
                CompletableFuture.allOf(model_ids.stream().map(id -> graphInjestor.parseModelTree(id, new YearRange(yearLower, yearMiddle)::testDate)).toArray(CompletableFuture[]::new)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            LOG.debug("Computing policies");
            graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> {
                policyManager.computePolicyForGraph(id);
                graphManager.debloatGraph(id);
            });
            LOG.info("Stressing graph for hints");
            model_ids.forEach(id -> stressHints(id, new YearRange(yearMiddle, yearTop)).close());
            LOG.info("Completed test after " + 1e-9 * (System.nanoTime() - start) + " seconds");
        }).whenComplete((nil, error) -> {
            if (error != null)
                error.printStackTrace();
        });

        return Response.ok("Test started").build();
    }

    /**
     * This method is used to setup the graphs for the first time.
     * It will generate the graphs from the exercises for the given models.
     * It will also compute the policies for the graphs and debloat them.
     */
    @GET
    @Path("/setup-graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response genGraphs(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix) {
        LOG.info("Starting setup for model ids " + model_ids);
        // Clean
        graphManager.deleteExerciseByModelIDs(model_ids, true);
        // Create graph
        makeGraphAndExercisesFromCommands(model_ids, prefix).close();
        // Fill graph
        CompletableFuture.allOf(model_ids.stream().map(id -> graphInjestor.parseModelTree(id)).toArray(CompletableFuture[]::new))
                // Then Compute policy
                .thenAcceptAsync(nil -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> {
                    policyManager.computePolicyForGraph(id); //TODO
                    // graphManager.debloatGraph(id);
                })).whenCompleteAsync((nil, error) -> {
                    if (error != null)
                        LOG.error(error);
                    LOG.info("Setup Completed");
                });
        return Response.ok("Setup in progress.").build();
    }


    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHint(HintRequest request) {
        LOG.info("Hint requested for session " + request.challenge);

        Session session = sessionManager.findById(request.challenge);

        if (session == null)
            return Response.ok(InstanceMsg.error("Invalid Session")).build();

        try {
            Optional<HintMsg> response = session.hintRequest.get();

            if (response.isEmpty())
                LOG.debug("NO HINT AVAILABLE FOR " + request.challenge);

            return Response.ok(response.map(InstanceMsg::from).orElseGet(() -> InstanceMsg.error("Unable to generate hint"))).build();
        } catch (CancellationException | InterruptedException e) {
            LOG.debug("HINT GEN Cancellation/Interruption");
            return Response.ok(InstanceMsg.error("Hint is unavailable")).build();
        } catch (ExecutionException e) {
            LOG.error(e);
            return Response.ok(InstanceMsg.error("Error when generating hint")).build();
        }
    }

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHint(HintRequest request) {
        Optional<HintMsg> result = hintGenerator.getHint(request.challenge, request.predicate, Util.parseModel(request.model));
        return result.map(r -> Response.ok(InstanceMsg.from(r))).orElseGet(() -> Response.status(Response.Status.NO_CONTENT)).build();
    }

    @GET
    @Path("/debug-drop-everything")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug() {
        graphManager.dropEverything();
        return Response.ok().build();
    }

    @GET
    @Path("/debug-rm-hint-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug1() {
        HintGraph.removeAllHintStats();
        return Response.ok().build();
    }

    @POST 
    @Path("/higena-hint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHiGenAHint(HintRequest request) {
        LOG.info("HiGenA Hint requested");
        // Get expression from model
        ExprExtractor extractor = new ExprExtractor(request.model);
        String expression = extractor.parse(request.predicate);
        HintGenType hintGenType = HintGenType.valueOf(request.hintGenType.toUpperCase());
        // Turn off create new paths
        org.higena.hint.HintGenerator.turnOffPathCreation();

         // Generate hint
        Graph graph = new Graph(request.challenge, request.predicate);
        org.higena.hint.HintGenerator hintGen = graph.generateHint(expression, request.model, hintGenType);

        // Build response
        if (hintGen.getHint() == null) // Failed
            return Response.ok(InstanceMsg.error("No hint available")).build();
        // Success
        JSONObject response = hintGen.getJSON();
        return Response.ok(response.toString()).build();
    }

    @POST
    @Path("/higena-setup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHiGenAHint(List<String> model_ids) {
        LOG.info("HiGenA Setup requested");
        CompletableFuture.runAsync(() -> {
            for (String model_id : model_ids) {
                graphManager.getSecretsForModel(model_id).forEach(secret -> {
                    new Graph(model_id, secret).setup();
                    LOG.info("HiGenA setup finished for " + model_id + " " + secret);
                });
            }
        });
        return Response.ok("Setup in progress").build();
    }

    @POST
    @Path("/spec-hint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSpecHint(HintRequest request) {
        LOG.info("Spec Hint requested");
        Map<String,String> hint = hintMerge.specAssistantGraphToHigena(request.challenge, request.predicate, request.model);
        if (hint == null)
            return Response.status(Response.Status.NO_CONTENT).build();

        return Response.ok(hint).build();
    }

}
