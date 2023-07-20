package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.csail.sdg.alloy4viz.*;

import java.util.List;

public class InstanceTrace {

    @JsonProperty("types")
    List<UnaryTrace> types;
    @JsonProperty("sets")

    List<UnaryTrace> sets;
    @JsonProperty("rels")

    List<TupleTrace> rels;


    public static InstanceTrace from(AlloyInstance instance) {
        InstanceTrace result = new InstanceTrace();

        result.types = instance.model.getTypes().stream().map(sig -> UnaryTrace.from(instance, sig)).toList();
        result.sets = instance.model.getSets().stream().map(set -> UnaryTrace.from(instance, set)).toList();
        result.rels = instance.model.getRelations().stream().map(rel -> TupleTrace.from(instance, rel)).toList();
        return result;
    }


    public static class UnaryTrace {
        public String name;
        public String parent;
        public List<String> atoms;

        public static UnaryTrace from(AlloyInstance instance, AlloyType signature) {
            UnaryTrace result = new UnaryTrace();
            result.name = signature.getName();
            AlloyType pr = instance.model.getSuperType(signature);
            result.parent = pr == null ? "null" : pr.toString();
            result.atoms = instance.type2atoms(signature)
                    .stream()
                    .filter(at -> at.getType().equals(signature))
                    .map(AlloyAtom::toString)
                    .toList();

            return result;
        }

        public static UnaryTrace from(AlloyInstance instance, AlloySet set) {
            UnaryTrace result = new UnaryTrace();
            result.name = set.getName();
            result.parent = set.getType().toString();
            result.atoms = instance.set2atoms(set).stream().map(AlloyAtom::toString).toList();

            return result;
        }
    }

    public static class TupleTrace {
        public String name;
        public List<List<String>> atoms;

        public static TupleTrace from(AlloyInstance instance, AlloyRelation relation) {
            TupleTrace result = new TupleTrace();
            result.name = relation.getName();
            result.atoms = instance.relation2tuples(relation)
                    .stream()
                    .map(tuple -> tuple.getAtoms()
                            .stream()
                            .map(AlloyAtom::toString)
                            .toList())
                    .toList();

            return result;
        }
    }
}
