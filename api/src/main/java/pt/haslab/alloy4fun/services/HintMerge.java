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
import pt.haslab.alloy4fun.util.ParseUtil;
import pt.haslab.specassistant.data.aggregation.Transition;
import pt.haslab.specassistant.data.models.Challenge;
import pt.haslab.specassistant.data.models.Node;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.services.HintGenerator;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class HintMerge {

    @Inject
    ModelRepository modelRepo;

    @Inject
    HintGenerator specAssistantGen;

    public Optional<HintMsg> specAssistantGraphToHigena(String originId, String command_label, String model) {
        String originid = modelRepo.getOriginalById(originId);
        Optional<Transition> target = specAssistantGen.bareTransition(originid, command_label, model);

        if (target.isEmpty()) return Optional.empty(); //
        Optional<HintMsg> msg = specAssistantGen.getHint(originid, command_label, ParseUtil.parseModel(model));

        String old_expr = target.orElseThrow().getFrom().getFormula().values().stream().findFirst().orElseThrow();
        String new_expr = target.orElseThrow().getTo().getFormula().values().stream().findFirst().orElseThrow();

        // Generate hint message
        String oldAST = A4FParser.parse(old_expr, model).toTreeString(),
                newAST = A4FParser.parse(new_expr, model).toTreeString();

        return Optional.of(HintMsg.from(msg.map(x -> x.pos).orElse(null), new Hint(oldAST, newAST).toString()));
    }

    public Map<String, String> specAssistantGraphToHigenaFormula(CompModule base_world, Challenge challenge, String formula) {
        Optional<Transition> target = specAssistantGen.externalFormulaTransition(base_world, challenge, formula);

        if (target.isEmpty()) return null; //

        String old_expr = target.orElseThrow().getFrom().getFormula().values().stream().findFirst().orElseThrow();
        String new_expr = target.orElseThrow().getTo().getFormula().values().stream().findFirst().orElseThrow();

        // Generate hint message
        String oldAST = A4FParser.parse(old_expr, base_world).toTreeString(),
                newAST = A4FParser.parse(new_expr, base_world).toTreeString();

        return Map.of("hint", new Hint(oldAST, newAST).toString(), "nextExpr", new_expr, "oldExpr", old_expr);
    }

    public HintMsg higenaGraphToSpecAssistant(String challenge, String predicate, String model) {
        CompModule world = ParseUtil.parseModel(model);

        // Get expression from a model
        ExprExtractor extractor = new ExprExtractor(model);
        String old_expr = extractor.parse(predicate);

        // Generate hint
        Graph graph = new Graph(challenge, predicate);
        org.higena.hint.HintGenerator.turnOffPathCreation();
        org.higena.hint.HintGenerator generator = graph.getHintPath(old_expr, model, HintGenType.TED);
        String new_expr = generator.getNextExpr();

        try {
            Map<String, Expr> oldFormulaExpr = Node.getFormulaExprFrom(world.getAllFunc().makeConstList(), Set.of(predicate));
            Map<String, Expr> newFormulaExpr = Map.of(predicate, world.parseOneExpressionFromString(new_expr));

            return HintGenerator.firstHint(oldFormulaExpr, newFormulaExpr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
