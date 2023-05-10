package pt.haslab.alloy4fun;

import org.higena.graph.hint.Hint;
import org.higena.graph.hint.HintGenType;
import org.json.JSONException;
import org.json.JSONObject;
import org.higena.graph.Graph;
import org.parser.A4FExprParser;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/higena")
public class AlloyHiGenA {

  @POST
  @Produces("text/json")
  public Response doGet(String body) {
    // Parse request
    HiGenARequest request = HiGenARequest.parseJSON(body);
    if (request == null) {
      // Failed to parse request
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    // Get expression from model
    A4FExprParser parser = new A4FExprParser(request.model);
    String expression = parser.parse(request.predicate);

    // Generate hint
    Graph graph = new Graph(request.challenge, request.predicate);
    Hint hint = graph.getHint(expression, request.model, HintGenType.TED);

    // Build response
    JSONObject response = new JSONObject();
    try {
      response.put("hint", hint.toString());
    } catch (JSONException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    return Response.ok(response).build();
  }
}

/**
 * Class to represent an API request for HiGenA.
 * It contains the challenge, the model and the command index.
 */
class HiGenARequest {
  final String challenge;
  final String predicate;
  final String model;

  HiGenARequest(String challenge, String predicate, String model) {
    this.challenge = challenge;
    this.predicate = predicate;
    this.model = model;
  }

  /**
   * Parses a JSON string into a HiGenARequest object.
   *
   * @param body the JSON string to parse
   * @return the HiGenARequest object
   */
  static HiGenARequest parseJSON(String body) {
    String challenge, predicate, model;

    try {
      JSONObject requestData = new JSONObject(body);
      challenge = requestData.getString("challenge");
      predicate = requestData.getString("predicate");
      model = requestData.getString("model");
    } catch (JSONException e) {
      return null;
    }

    return new HiGenARequest(challenge, predicate, model);
  }

}
