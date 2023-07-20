package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExerciseForm {
    public String modelId;
    public Integer secretCommandCount;

    public String cmd_n;

    public Set<String> targetFunctions;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Integer getSecretCommandCount() {
        return secretCommandCount;
    }

    public void setSecretCommandCount(Integer secretCommandCount) {
        this.secretCommandCount = secretCommandCount;
    }

    public String getCmd_n() {
        return cmd_n;
    }

    public void setCmd_n(String cmd_n) {
        this.cmd_n = cmd_n;
    }

    public Set<String> getTargetFunctions() {
        return targetFunctions;
    }

    public void setTargetFunctions(Set<String> targetFunctions) {
        this.targetFunctions = targetFunctions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExerciseForm that = (ExerciseForm) o;

        if (!modelId.equals(that.modelId)) return false;
        return cmd_n.equals(that.cmd_n);
    }

    @Override
    public int hashCode() {
        return 31 * modelId.hashCode() + cmd_n.hashCode();
    }
}
