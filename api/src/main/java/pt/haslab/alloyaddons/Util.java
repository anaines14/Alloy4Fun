package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import pt.haslab.alloyaddons.exceptions.UncheckedIOException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Util {

    static CompModule parseModel(String model) throws UncheckedIOException, Err {
        return parseModel(model, A4Reporter.NOP);
    }

    static CompModule parseModel(String model, A4Reporter rep) throws UncheckedIOException, Err {
        try {
            String prefix_name = "thr-%d.alloy_heredoc".formatted(Thread.currentThread().getId());
            File file = File.createTempFile(prefix_name, ".als");
            file.deleteOnExit();

            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                out.write(model.getBytes());
                out.flush();
            }
            return CompUtil.parseEverything_fromFile(rep, null, file.getAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static AlloyInstance parseInstance(A4Solution solution) throws UncheckedIOException, Err {
        return parseInstance(solution, 0);
    }

    static AlloyInstance parseInstance(A4Solution solution, Integer state) throws UncheckedIOException, Err {
        try {
            String prefix_name = "thr-%d.a4f".formatted(Thread.currentThread().getId());
            File file = File.createTempFile(prefix_name, ".als");
            file.deleteOnExit();
            solution.writeXML(file.getAbsolutePath());

            return StaticInstanceReader.parseInstance(file.getAbsoluteFile(), state);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<AlloyInstance> parseInstances(A4Solution solution, Integer count) throws UncheckedIOException, Err {
        return parseInstances(solution, 0, count);
    }

    static List<AlloyInstance> parseInstances(A4Solution solution, Integer from, Integer to) throws UncheckedIOException, Err {
        try {
            String prefix_name = "thr-%d.a4f".formatted(Thread.currentThread().getId());
            File file = File.createTempFile(prefix_name, ".als");
            file.deleteOnExit();
            solution.writeXML(file.getAbsolutePath());

            return IntStream.range(from, to).boxed().map(i -> StaticInstanceReader.parseInstance(file.getAbsoluteFile(), i)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static A4Options defaultOptions(CompModule world) {
        A4Options opt = new A4Options();
        opt.originalFilename = Path.of(world.path()).getFileName().toString();
        opt.solver = A4Options.SatSolver.SAT4J;

        return opt;
    }


    static Expr parseOneExprFromString(CompModule world, String value) {
        try {
            return world.parseOneExpressionFromString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Stream<Func> streamFuncsWithNames(Collection<Func> allFunctions, Set<String> targetNames) {
        return allFunctions.stream().filter(x -> targetNames.contains(x.label.replace("this/", "")));
    }

    static Stream<String> streamFuncsNamesWithNames(Collection<Func> allFunctions, Set<String> targetNames) {
        return allFunctions.stream().filter(x -> targetNames.contains(x.label.replace("this/", ""))).map(x -> x.label.replace("this/", ""));
    }

    static boolean containsFuncs(Collection<Func> allFunctions, Set<String> targetNames) {
        return allFunctions.stream().map(x -> x.label.replace("this/", "")).collect(Collectors.toSet()).containsAll(targetNames);
    }

    static boolean containsCommand(Collection<Command> allFunctions, String targetName) {
        return allFunctions.stream().map(x -> x.label.replace("this/", "")).collect(Collectors.toSet()).contains(targetName);
    }

    static String stripThisFromLabel(String str) {
        if (str != null)
            str = str.replace("this/", "");
        return str;
    }

    static Map<String, Set<String>> getFunctionWithPositions(CompModule module, List<Pos> positions) {
        Map<String, Set<String>> result = new HashMap<>();

        module.getAllCommands().forEach(cmd -> {
            if (posIn(cmd.pos, positions)) {
                Set<String> targets = FunctionSearch
                        .search(f -> f.pos.sameFile(cmd.pos) && notPosIn(f.pos, positions), cmd.formula)
                        .stream()
                        .map(f -> f.label)
                        .map(Util::stripThisFromLabel)
                        .collect(Collectors.toSet());
                if (!targets.isEmpty())
                    result.put(stripThisFromLabel(cmd.label), targets);
            }
        });
        return result;
    }

    static boolean posIn(Pos pos, Collection<Pos> collection) {
        return collection.stream().anyMatch(p -> p.contains(pos));
    }

    static boolean notPosIn(Pos pos, Collection<Pos> collection) {
        return collection.stream().noneMatch(p -> p.contains(pos));
    }

    static List<Pos> offsetsToPos(String code, List<Integer> offsets) {
        return offsetsToPos("alloy_heredoc.als", code, offsets);
    }

    static List<Pos> offsetsToPos(String filename, String code, List<Integer> offsets) {
        List<Integer> integers = offsets.stream().sorted().distinct().toList();
        Pattern p = Pattern.compile("\\n");
        Matcher m = p.matcher(code);

        List<Pos> result = new ArrayList<>(integers.size());

        int line = 1;
        int curr = 0;
        while (m.find() && curr < integers.size()) {
            int t0 = m.end();
            for (; curr < integers.size() && integers.get(curr) < t0; curr++)
                result.add(new Pos(filename, integers.get(curr) - t0, line));
            line++;
        }

        return result;
    }


    static String posAsStringTuple(Pos p) {
        return "(" + p.x + "," + p.y + (p.x2 != p.x || p.y2 != p.y ? "," + p.x2 + "," + p.y2 : "") + ")";
    }

    static String lineCSV(String sep, List<String> strings) {
        if (strings == null || strings.isEmpty())
            return "";
        StringBuilder res = new StringBuilder();
        String last = strings.get(strings.size() - 1);
        for (int i = 0; i < strings.size() - 1; i++) res.append(strings.get(i)).append(sep);

        return res.append(last).toString();
    }
}
