package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintRequest {

    @JsonAlias({"challenge", "parentId", "modelId","sessionId"})
    public String challenge;
    @JsonAlias({"predicate", "command_label"})
    public String predicate;
    public String model;
}
