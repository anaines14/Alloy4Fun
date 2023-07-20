package pt.haslab.alloy4fun.resources;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.data.transfer.InstanceResponse;
import pt.haslab.alloy4fun.data.transfer.InstanceTrace;
import pt.haslab.alloy4fun.data.transfer.InstancesRequest;
import pt.haslab.alloy4fun.services.SessionService;
import pt.haslab.alloyaddons.Util;
import pt.haslab.alloyaddons.exceptions.UncheckedIOException;
import pt.haslab.specassistant.HintGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Path("/getInstances")
@RequestScoped
public class AlloyGetInstances {

    private static final Logger LOGGER = Logger.getLogger(AlloyGetInstances.class);

    @Inject
    SessionService sessionManager;

    @Inject
    HintGenerator hintGenerator;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(InstancesRequest request) {

        LOGGER.info("Received request for session: " + request.sessionId + "with parent (" + request.parentId + ")");

        if (sessionManager.deleteById(request.parentId))
            LOGGER.debug("Deleted parent session (" + request.parentId + ").");

        try {

            List<ErrorWarning> warnings = new ArrayList<>();
            A4Reporter rep = new A4Reporter() {
                public void warning(ErrorWarning msg) {
                    warnings.add(msg);
                }
            };

            Session session = ensureSession(request, rep);

            return Response.ok(batchAdd(request.numberOfInstances, session, warnings)).build();
        } catch (Err e) {
            LOGGER.info("Responding with an alloy error.");
            return Response.ok(InstanceMsg.from(e)).build();
        } catch (IOException e) {
            LOGGER.info("Responding with an error message.");
            return Response.ok(InstanceMsg.error(e.getMessage())).build();
        }
    }

    public Session ensureSession(InstancesRequest request, A4Reporter rep) throws Err, IOException {
        Session result = sessionManager.findById(request.sessionId);

        if (result == null) {
            CompModule world = Util.parseModel(request.model, rep);
            Command command = world.getAllCommands().get(request.commandIndex);

            A4Options options = Util.defaultOptions(world);
            A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);

            result = Session.create(request.sessionId, ans, command, world.getAllFunc().makeConstList());

            if (request.parentId != null && command.check && ans.satisfiable())
                result.hintRequest = CompletableFuture.supplyAsync(() -> hintGenerator.getHint(request.parentId, command.label, world));
            else result.hintRequest = CompletableFuture.completedFuture(Optional.empty());
            sessionManager.update(result);
        }

        return result;
    }

    public List<InstanceResponse> batchAdd(Integer numberOfInstances, Session session, List<ErrorWarning> warnings) throws IOException {
        List<InstanceResponse> result = new ArrayList<>();

        for (int i = 0; i < numberOfInstances && session.getSolution().satisfiable(); i++) {
            result.add(assembleInstanceResponse(session, warnings));
            session.next();
        }
        if (!session.getSolution().satisfiable()) {
            result.add(assembleInstanceResponse(session, warnings));
        }

        return result;
    }


    public InstanceResponse assembleInstanceResponse(Session session, List<ErrorWarning> warnings) {
        A4Solution answer = session.getSolution();
        int cnt = session.getCount();

        InstanceResponse result = new InstanceResponse();

        if (warnings.size() > 0)
            result.warning = InstanceMsg.from(warnings.get(0));

        result.sessionId = session.id;
        result.unsat = !answer.satisfiable();
        result.check = session.cmd.check;
        result.cmd_n = session.cmd.label;
        result.cnt = cnt;
        result.is_static = answer.getMaxTrace() < 0;

        if (answer.satisfiable()) {
            result.loop = answer.getLoopState();
            try {
                result.instance = Util.parseInstances(answer, answer.getTraceLength())
                        .stream()
                        .map(InstanceTrace::from)
                        .toList();
            } catch (Err e) {
                LOGGER.error("Alloy errored during solution parsing.", e);
                return InstanceResponse.err(e);
            } catch (UncheckedIOException e) {
                LOGGER.error("IO error during solution parsing.", e);
            }
        }

        return result;
    }

}
