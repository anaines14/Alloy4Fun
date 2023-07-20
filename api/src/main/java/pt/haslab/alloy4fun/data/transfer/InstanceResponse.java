package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import edu.mit.csail.sdg.alloy4.Err;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class InstanceResponse {

    @JsonInclude(NON_EMPTY)
    public String err;
    @JsonInclude(NON_NULL)
    @JsonUnwrapped
    public InstanceMsg warning;

    public String sessionId;
    public Boolean unsat;
    public Boolean check;
    public String cmd_n;
    public Integer cnt;
    @JsonProperty("static") // can't name the actual variable static because it is a java keyword
    public Boolean is_static;

    @JsonInclude(NON_NULL)
    public Integer loop;

    @JsonInclude(NON_EMPTY)
    public List<InstanceTrace> instance;

    public InstanceResponse() {
    }

    public static InstanceResponse err(Err e) {
        InstanceResponse response = new InstanceResponse();
        response.err = "Evaluator error occurred: " + e;
        return response;
    }
}
