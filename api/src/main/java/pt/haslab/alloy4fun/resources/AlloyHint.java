package pt.haslab.alloy4fun.resources;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.higena.graph.Graph;
import org.higena.hint.HintGenType;
import org.higena.parser.ExprExtractor;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.request.HintRequest;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.repositories.SessionRepository;
import pt.haslab.specassistant.data.policy.PolicyOption;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.SpecAssistantTestService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Path("/hint")
public class AlloyHint {

    @Inject
    Logger log;

    @Inject
    GraphManager graphManager;

    @Inject
    SpecAssistantTestService specAssistantTestService;

    @Inject
    SessionRepository sessions;


    @POST
    @Path("/spec-higena")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSpecHint(HintRequest request) {
        log.info("Spec Hint requested");
        try {
            return Response.ok(InstanceMsg.from(sessions.findById(request.challenge).hintRequest.get().orElseThrow())).build();
        } catch (InterruptedException | ExecutionException | NoSuchElementException | NullPointerException e) {
            return Response.ok(InstanceMsg.error("No hint")).build();
        }
    }

    @POST
    @Path("/compute-all-policies-for-rule")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicyForAll(@QueryParam("rule") String rule, @DefaultValue("true") @QueryParam("await") Boolean await) {
        try {
            CompletableFuture<Void> future = specAssistantTestService.computePoliciesForAll(PolicyOption.samples.get(rule));
            if (await)
                future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
            return Response.serverError().build();
        }
        return Response.ok("Policy computed").build();
    }

    /**
     * This method is used to setup the graphs for the first time.
     * It will generate the graphs from the exercises for the given models.
     */
    @GET
    @Path("/specassistant-setup")
    @Produces(MediaType.TEXT_PLAIN)
    public Response genGraphs(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix, @DefaultValue("true") @QueryParam("await") Boolean await) {
        try {
            CompletableFuture<Void> future = specAssistantTestService.genGraphs(prefix, model_ids);
            if (await)
                future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
            return Response.serverError().build();
        }
        return Response.ok("Setup completed for " + prefix + " with model_ids " + model_ids).build();
    }

    @GET
    @Path("/debug-drop-db")
    public Response dropDB() {
        graphManager.dropEverything();
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/higena-hint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHiGenAHint(HintRequest request) {
        try {
            log.info("HiGenA Hint requested");
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
            return Response.ok(hintGen.getJSON().toString()).build();
        } catch (Exception e) {
            log.error(e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/higena-setup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHiGenAHint(List<String> model_ids) {
        log.info("HiGenA Setup requested");
        CompletableFuture.runAsync(() -> {
            for (String model_id : model_ids) {
                graphManager.parseSecretFunctionNames(model_id).forEach(secret -> {
                    new Graph(model_id, secret).setup();
                    log.info("HiGenA setup finished for " + model_id + " " + secret);
                });
            }
        });
        return Response.ok("Setup in progress").build();
    }


}
