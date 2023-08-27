package pt.haslab.alloy4fun.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExerciseForm {
    public String modelId;
    public Integer secretCommandCount;

    public String cmd_n;

    public Set<String> targetFunctions;

    public ExerciseForm() {
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
