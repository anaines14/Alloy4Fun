package pt.haslab.alloy4fun;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.mutation.mutator.Mutator;
import pt.haslab.util.ExprToString;
import pt.haslab.util.Repairer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Path("/getHint")
public class AlloyHint {

    public static final int HINT_TIMEOUT = 1500;
    public static final int MAX_DEPTH = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlloyGetInstances.class);

    @POST
    @Produces("text/json")
    public Response doGet(String body) throws Err {
        // Extract needed variables
        JSONObject jo = new JSONObject(body);
        String model = jo.getString("model");
        int commandIndex = jo.getInt("commandIndex");
        JSONObject allRepairTargets = jo.getJSONObject("repairTargets");

        LOGGER.info("Request for hint");

        // Parse the given model
        CompModule world;
        try {
            File tmpAls = File.createTempFile("alloy_heredoc", ".als");
            tmpAls.deleteOnExit();
            BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tmpAls.toPath()));
            bos.write(model.getBytes());
            bos.flush();
            bos.close();
            world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, tmpAls.getAbsolutePath());
            tmpAls.deleteOnExit();
        } catch (Err e) {
            LOGGER.info("Alloy errored during model parsing: "+e.getMessage());
            JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
            instanceJSON.add("alloy_error", true);
            instanceJSON.add("msg", e.msg);
            instanceJSON.add("line", e.pos.y);
            instanceJSON.add("column", e.pos.x);
            instanceJSON.add("line2", e.pos.y2);
            instanceJSON.add("column2", e.pos.x2);
            LOGGER.info("Responding with error message.");
            return Response.ok(instanceJSON.build().toString()).build();
        } catch (IOException e) {
            LOGGER.error("IO error during model parsing.",e);
            JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
            instanceJSON.add("alloy_error", true);
            instanceJSON.add("msg", e.getMessage());
            LOGGER.info("Responding with error message.");
            return Response.ok(instanceJSON.build().toString()).build();
        }

        // Find the command to run and the suspissious functions
        Command command = world.getAllCommands().get(commandIndex);
        List<Func> repairTargets = new ArrayList<>();
        for (Object o : allRepairTargets.getJSONArray(command.label.replace("this/", ""))) {
            for (Func func : world.getAllFunc()) {
                if (func.label.replace("this/", "").equals(o)) {
                    repairTargets.add(func);
                }
            }
        }

        // Attempt the repair
        Repairer repairer = Repairer.make(world, command, repairTargets, MAX_DEPTH);
        repairer.repair(HINT_TIMEOUT);

        JsonObjectBuilder res = Json.createObjectBuilder();
        res.add("elapsed", repairer.getElapsedMillis());
        res.add("repaired", repairer.solution.isPresent());

        repairer.solution.ifPresent(candidate -> {
            JsonArrayBuilder mutators = Json.createArrayBuilder();
            for (Mutator mutator : candidate.mutators) {
                JsonObjectBuilder mut = Json.createObjectBuilder();
                Pos pos = mutator.original.expr.pos;
                mut.add("line", pos.y);
                mut.add("column", pos.x);
                mut.add("line2", pos.y2);
                mut.add("column2", pos.x2);
                mut.add("name", mutator.name);
                mutator.hint().ifPresent(hint -> {
                    mut.add("hint", hint);
                });
                mutators.add(mut);
            }
            res.add("mutators", mutators);
            if (repairTargets.size() == 1) {
                res.add("repair", ExprToString.exprToString(repairTargets.get(0).getBody()));
            }
        });

        return Response.ok(res.build().toString()).build();
    }

}
