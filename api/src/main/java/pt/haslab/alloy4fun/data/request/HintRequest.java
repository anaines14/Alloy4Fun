package pt.haslab.alloy4fun.data.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintRequest {

    @JsonAlias({"challenge", "parentId", "modelId","sessionId","currentModelId"})
    public String challenge;
    @JsonAlias({"predicate", "command_label"})
    public String predicate;
    @JsonAlias({"model", "code"})
    public String model;
    public String hintGenType = "TED";
}
