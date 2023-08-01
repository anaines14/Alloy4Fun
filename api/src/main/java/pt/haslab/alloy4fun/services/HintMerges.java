package pt.haslab.alloy4fun.services;

import edu.mit.csail.sdg.parser.CompModule;
import jakarta.inject.Inject;
import pt.haslab.alloyaddons.Util;
import pt.haslab.specassistant.HintGenerator;
import pt.haslab.specassistant.data.models.HintExercise;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.repositories.HintExerciseRepository;

import java.util.Map;
import java.util.Optional;

public class HintMerges {

    @Inject
    HintGenerator specAssistantGen;
    @Inject
    HintExerciseRepository hintExerciseRepo;

    public HintGenerator specAssistantGraphToHigena(String originId, String command_label, String model) {
        CompModule world = Util.parseModel(model);
        HintExercise exercise = hintExerciseRepo.findByModelIdAndCmdN(originId, command_label).orElseThrow();
        Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
        Optional<HintNode> target = specAssistantGen.nextState(exercise.graph_id, formula);

        if (target.isEmpty())
            return null; //

        String old_expr = formula.values().stream().findFirst().orElseThrow();
        String new_expr = target.orElseThrow().formula.values().stream().findFirst().orElseThrow();

        //Tree old = A4FParser.parse(old_expr, world);

        //...

        return null;
    }

    public HintMsg higenaGraphToSpecAssistant(String challenge, String predicate, String model) {

        // Preciso das ASTs do no atual e do proximo no para passar a isto, posso dar parse das formulas ou das asts diretamente

        return HintGenerator.firstHint(null, null);
    }

}
