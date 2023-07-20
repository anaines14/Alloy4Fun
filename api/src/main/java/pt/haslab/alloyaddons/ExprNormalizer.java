package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.mit.csail.sdg.ast.ExprBinary.Op.*;
import static edu.mit.csail.sdg.ast.ExprUnary.Op.NOOP;

public class ExprNormalizer {
    public static Expr normalize(Expr expr) {
        return new ExprVisitReturn().visitThis(expr);
    }

    public static Expr normalize(Func func) {
        return new ExprVisitReturn().visitThis(func.getBody());
    }

    private static class QuantifierDecl implements Comparable<QuantifierDecl> {
        public int depth;
        public ExprHasName name;

        public Expr type;
        public ExprQt.Op quantifier;

        public QuantifierDecl(int depth, ExprHasName name, Expr type, ExprQt.Op quantifier) {
            this.depth = depth;
            this.name = name;
            this.type = type;
            this.quantifier = quantifier;
        }

        public String fieldName() {
            return name.label;
        }

        public String migrateName(String new_label) {
            if (name instanceof ExprVar) {
                String result = name.label;
                name = ExprVar.make(name.pos, new_label);
                return result;
            }
            return null;
        }

        public int compareTo(QuantifierDecl o) {
            int t0 = Integer.compare(depth, o.depth);
            if (t0 != 0)
                return t0;
            return quantifier.compareTo(o.quantifier);
        }

        public boolean canMigrateName() {
            return name instanceof ExprVar;
        }

        @Override
        public String toString() {
            return "#" + depth + " " + quantifier.toString() + " " + name + ":" + type;
        }
    }

    private static class ExprVisitReturn extends VisitReturn<Expr> {


        private static final Map<ExprBinary.Op, ExprBinary.Op> BinOpSym = Map.of(GTE, LT, LTE, GT, NOT_LTE, NOT_GT, NOT_GTE, NOT_LT);
        private static final List<ExprBinary.Op> UnorderedOp = List.of(INTERSECT, PLUS, MUL, EQUALS, NOT_EQUALS, OR, IFF);
        Map<String, Expr> var_context = new HashMap<>();
        Integer var_counter = 0;

        @Override
        public Expr visit(ExprBinary exprBinary) throws Err {
            ExprBinary.Op op = exprBinary.op;

            Expr left = this.visitThis(exprBinary.left);
            Expr right = this.visitThis(exprBinary.right);
            Expr swap = right;

            if (UnorderedOp.contains(exprBinary.op) && left.toString().compareTo(right.toString()) > 0) {
                right = left;
                left = swap;
            } else {
                if (BinOpSym.containsKey(exprBinary.op)) {
                    right = left;
                    left = swap;
                    op = BinOpSym.get(exprBinary.op);
                }
            }

            return op.make(exprBinary.pos, exprBinary.closingBracket, left, right);

        }


        @Override
        public Expr visit(ExprList exprList) throws Err {

            List<Expr> exprReturnObjs = exprList.args.stream().map(this::visitThis).sorted(Comparator.comparing(Expr::toString)).toList();

            return ExprList.make(exprList.pos(), exprList.closingBracket, exprList.op, exprReturnObjs.stream().toList());
        }

        @Override
        public Expr visit(ExprCall exprCall) throws Err {

            List<Expr> args = exprCall.args.stream().map(this::visitThis).toList();

            if (!exprCall.fun.pos.sameFile(exprCall.pos)) {
                return ExprCall.make(exprCall.pos(), exprCall.closingBracket, exprCall.fun, args, exprCall.extraWeight);
            }

            Map<String, Expr> backup = new HashMap<>(var_context);

            for (int i = 0; i < exprCall.fun.decls.size(); i++) {
                int finalI = i;
                exprCall.fun.decls.get(i).names.forEach(n -> var_context.put(n.label, args.get(finalI)));
            }

            Expr result = visitThis(exprCall.fun.getBody());

            var_context = backup;

            return result;
        }

        @Override
        public Expr visit(ExprConstant exprConstant) throws Err {
            return exprConstant;
        }

        @Override
        public Expr visit(ExprITE exprITE) throws Err {
            Expr cond = this.visitThis(exprITE.cond);
            Expr left = this.visitThis(exprITE.left);
            Expr right = this.visitThis(exprITE.right);

            return ExprITE.make(exprITE.pos(), cond, left, right);
        }

        @Override
        public Expr visit(ExprLet exprLet) throws Err {
            String var_label = exprLet.var.label;
            Expr var_expr = this.visitThis(exprLet.expr);
            var_context.put(var_label, var_expr);

            return this.visitThis(exprLet.sub);
        }


        @Override
        public Expr visit(ExprQt exprQt) throws Err {
            Expr current = exprQt;
            List<QuantifierDecl> quantifiers = new ArrayList<>();
            List<Decl> disjunctions = new ArrayList<>();

            while (true) {
                while (current instanceof ExprUnary && ((ExprUnary) current).op == NOOP)
                    current = ((ExprUnary) current).sub;

                if (current instanceof ExprQt) {
                    ExprQt.Op op = ((ExprQt) current).op;

                    for (Decl decl : ((ExprQt) current).decls) {
                        Set<String> dependentFields = new StreamFieldNames().visitThis(decl.expr).collect(Collectors.toSet());
                        if (decl.disjoint != null)
                            disjunctions.add(decl);

                        decl.names.forEach(name -> {
                            int depth = 0;

                            for (QuantifierDecl q : quantifiers) {
                                if (dependentFields.contains(q.fieldName())) {
                                    depth = Integer.max(depth, q.depth + 1);
                                }
                            }
                            quantifiers.add(new QuantifierDecl(depth, name, decl.expr, op));
                        });
                    }
                    current = ((ExprQt) current).sub;
                } else
                    break;
            }

            Collections.sort(quantifiers);

            List<Map.Entry<String, Expr>> name_rollbacks = new ArrayList<>();

            quantifiers.forEach(x -> {
                if (x.canMigrateName()) {
                    String new_label = "ref" + var_counter++;
                    String old_label = x.migrateName(new_label);
                    if (old_label != null) {
                        Expr old = var_context.put(old_label, x.name);
                        if (old == null)
                            name_rollbacks.add(null);
                        else name_rollbacks.add(Map.entry(old_label, old));
                    }
                }
            });

            Expr result = this.visitThis(current);
            if (!disjunctions.isEmpty()) {
                result = ExprList.make(Pos.UNKNOWN, Pos.UNKNOWN, ExprList.Op.AND, Stream.concat(
                        disjunctions.stream().map(
                                dec -> ExprList.make(
                                        dec.disjoint, dec.disjoint2,
                                        ExprList.Op.DISJOINT,
                                        dec.names.stream()
                                                .map(this::visitThis)
                                                .sorted(Comparator.comparing(Expr::toString))
                                                .toList())
                        ).sorted(Comparator.comparing(Expr::toString)), Stream.of(result)).toList());
            }

            for (int i = quantifiers.size() - 1; i >= 0; i--) {
                QuantifierDecl qtfDecl = quantifiers.get(i);
                Map.Entry<String, Expr> rollback = name_rollbacks.get(i);
                if (rollback != null) {
                    var_context.put(rollback.getKey(), rollback.getValue());
                }
                Expr normalizedType = visitThis(qtfDecl.type);

                Decl d = new Decl(null, null, null, null, List.of(qtfDecl.name), normalizedType);

                result = qtfDecl.quantifier.make(null, null, List.of(d), result);
            }

            return result;
        }

        @Override
        public Expr visit(ExprUnary exprUnary) throws Err {
            //NOOP ARE KEEPED FOR PROPER POSITION MAPPING
            return exprUnary.op.make(exprUnary.pos, this.visitThis(exprUnary.sub));
        }

        @Override
        public Expr visit(ExprVar exprVar) throws Err {
            return Optional.ofNullable(var_context.get(exprVar.label)).orElse(exprVar);
        }

        @Override
        public Expr visit(Sig sig) throws Err {
            return sig;
        }

        @Override
        public Expr visit(Sig.Field field) throws Err {
            return field;
        }
    }

    private static class StreamFieldNames extends VisitReturn<Stream<String>> {

        @Override
        public Stream<String> visit(ExprBinary exprBinary) throws Err {
            return Stream.concat(visitThis(exprBinary.left), visitThis(exprBinary.right));
        }

        @Override
        public Stream<String> visit(ExprList exprList) throws Err {
            return exprList.args.stream().flatMap(this::visitThis);
        }

        @Override
        public Stream<String> visit(ExprCall exprCall) throws Err {
            if (exprCall.fun.pos.sameFile(exprCall.pos))
                return visitThis(exprCall.fun.getBody());
            return Stream.of();
        }

        @Override
        public Stream<String> visit(ExprConstant exprConstant) throws Err {
            return Stream.of();
        }

        @Override
        public Stream<String> visit(ExprITE exprITE) throws Err {
            return Stream.of(visitThis(exprITE.cond), visitThis(exprITE.left), visitThis(exprITE.right)).flatMap(i -> i);
        }

        @Override
        public Stream<String> visit(ExprLet exprLet) throws Err {
            return Stream.concat(visitThis(exprLet.expr), visitThis(exprLet.sub));
        }

        @Override
        public Stream<String> visit(ExprQt exprQt) throws Err {
            return Stream.concat(exprQt.decls.stream().map(x -> x.expr).flatMap(this::visitThis), visitThis(exprQt.sub));
        }

        @Override
        public Stream<String> visit(ExprUnary exprUnary) throws Err {
            return visitThis(exprUnary.sub);
        }

        @Override
        public Stream<String> visit(ExprVar exprVar) throws Err {
            return Stream.of(exprVar.label);
        }

        @Override
        public Stream<String> visit(Sig sig) throws Err {
            return Stream.of();
        }

        @Override
        public Stream<String> visit(Sig.Field field) throws Err {
            return visitThis(field.decl().expr);
        }
    }

}
