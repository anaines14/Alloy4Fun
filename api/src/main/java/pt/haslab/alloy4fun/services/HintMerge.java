package pt.haslab.alloy4fun.services;

import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.higena.graph.Graph;
import org.higena.hint.Hint;
import org.higena.hint.HintGenType;
import org.higena.parser.A4FParser;
import org.higena.parser.ExprExtractor;
import pt.haslab.alloyaddons.Util;
import pt.haslab.specassistant.HintGenerator;
import pt.haslab.specassistant.data.models.HintExercise;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.repositories.HintExerciseRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class HintMerge {

    @Inject
    HintGenerator specAssistantGen;
    @Inject
    HintExerciseRepository hintExerciseRepo;

    public String specAssistantGraphToHigena(String originId, String command_label, String model) {
        CompModule world = Util.parseModel(model);
        HintExercise exercise = hintExerciseRepo.findByModelIdAndCmdN(originId, command_label).orElseThrow();
        Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
        Optional<HintNode> target = specAssistantGen.nextState(exercise.graph_id, formula);

        if (target.isEmpty()) return null; //

        String old_expr = formula.values().stream().findFirst().orElseThrow();
        String new_expr = target.orElseThrow().formula.values().stream().findFirst().orElseThrow();

        // Generate hint message
        String oldAST = A4FParser.parse(old_expr, model).toTreeString(),
                newAST = A4FParser.parse(new_expr, model).toTreeString();

        return new Hint(oldAST, newAST).toString();
    }

    public HintMsg higenaGraphToSpecAssistant(String challenge, String predicate, String model) {
        CompModule world = Util.parseModel(model);

        // Get expression from model
        ExprExtractor extractor = new ExprExtractor(model);
        String old_expr = extractor.parse(predicate);

        // Generate hint
        Graph graph = new Graph(challenge, predicate);
        org.higena.hint.HintGenerator.cantCreatePath = true;
        org.higena.hint.HintGenerator generator = graph.getHintPath(old_expr, model, HintGenType.TED);
        String new_expr = generator.getNextExpr();

        try {
            Map<String, Expr> oldFormulaExpr = HintNode.getFormulaExprFrom(world.getAllFunc().makeConstList(), Set.of(predicate));
            Map<String, Expr> newFormulaExpr = Map.of(predicate, world.parseOneExpressionFromString(new_expr));

            return HintGenerator.firstHint(oldFormulaExpr, newFormulaExpr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
