package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.*;

import java.util.List;

public class ExprStringify {
    public static String rawStringify(Expr e) {
        return new ExprStringifyVisitReturn().visitThis(e);
    }

    public static String stringify(Expr e) {
        String res = rawStringify(e);
        if ("true".equals(res))
            return "";
        return res;
    }

    private static class ExprStringifyVisitReturn extends VisitReturn<String> {

        @Override
        public String visit(ExprBinary exprBinary) throws Err {
            return "(%s %s %s)".formatted(visitThis(exprBinary.left), exprBinary.op.toString(), visitThis(exprBinary.right));
        }

        @Override
        public String visit(ExprList exprList) throws Err {
            List<String> list = exprList.args.stream().map(this::visitThis).toList();

            return switch (exprList.op) {
                case AND -> '(' + Util.lineCSV(" && ", list) + ')';
                case OR -> '(' + Util.lineCSV(" || ", list) + ')';
                case DISJOINT -> "disj[" + Util.lineCSV(",", list) + "]";
                default -> exprList.op + "[" + Util.lineCSV(",", list) + "]";
            };
        }

        @Override
        public String visit(ExprCall exprCall) throws Err {
            List<String> arguments = exprCall.args.stream().map(this::visitThis).toList();
            return exprCall.fun.label.replace("this/", "") + "[" + Util.lineCSV(",", arguments) + "]";
        }

        @Override
        public String visit(ExprConstant exprConstant) throws Err {
            return exprConstant.toString();
        }

        @Override
        public String visit(ExprITE exprITE) throws Err {
            String cond = this.visitThis(exprITE.cond);
            String left = this.visitThis(exprITE.left);
            String right = this.visitThis(exprITE.right);

            return "(%s implies %s else %s)".formatted(cond, left, right);
        }

        @Override
        public String visit(ExprLet exprLet) throws Err {
            return "(let %s=%s {%s})".formatted(visitThis(exprLet.var), visitThis(exprLet.expr), visitThis(exprLet.sub));
        }

        @Override
        public String visit(ExprQt exprQt) throws Err {
            String decString = Util.lineCSV(",", exprQt.decls.stream().map(e -> Util.lineCSV(",", e.names.stream().map(x -> x.label).toList()) + ":" + visitThis(e.expr)).toList());
            String sub = visitThis(exprQt.sub);

            if (exprQt.op == ExprQt.Op.COMPREHENSION)
                return "{" + decString + "|" + sub + "}";
            else return "(" + exprQt.op + " " + decString + "|" + sub + ")";
        }

        @Override
        public String visit(ExprUnary exprUnary) throws Err {
            String sub = this.visitThis(exprUnary.sub);
            return switch (exprUnary.op) {
                case SOMEOF -> "(some " + sub + ')';
                case LONEOF -> "(lone " + sub + ')';
                case ONEOF -> "(one " + sub + ')';
                case SETOF -> "(set " + sub + ')';
                case EXACTLYOF -> "(exactly " + sub + ')';
                case CAST2INT -> "int[" + sub + "]";
                case CAST2SIGINT -> "Int[" + sub + "]";
                case PRIME -> "(" + sub + ")'";
                case NOOP -> sub;
                default -> '(' + exprUnary.op.toString() + ' ' + sub + ")";
            };
        }

        @Override
        public String visit(ExprVar exprVar) throws Err {
            return exprVar.label.replace("this/", "");
        }

        @Override
        public String visit(Sig sig) throws Err {
            return sig.label.replace("this/", "");
        }

        @Override
        public String visit(Sig.Field field) throws Err {
            return "(" + field.sig.label.replace("this/", "") + " <: " + field.label.replace("this/", "") + ")";
        }
    }
}
