package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstancesRequest {
    public String model;
    public int numberOfInstances;
    public int commandIndex;
    public String sessionId;
    public String parentId;

}
