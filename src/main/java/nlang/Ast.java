package nlang;

import java.util.List;

public abstract class Ast {

    public abstract static class Node {
        public final int pos;

        Node(int pos) {
            this.pos = pos;
        }
    }

    public static final class Program extends Node {
        public final List<Stmt> stmts;

        public Program(List<Stmt> stmts, int pos) {
            super(pos);
            this.stmts = stmts;
        }
    }

    public abstract static class Stmt extends Node {
        Stmt(int pos) {
            super(pos);
        }
    }

    public static final class Def extends Stmt {
        public final String name;
        public final Formula body;

        public Def(String name, Formula body, int pos) {
            super(pos);
            this.name = name;
            this.body = body;
        }
    }

    public static final class Assert extends Stmt {
        public final Formula body;

        public Assert(Formula body, int pos) {
            super(pos);
            this.body = body;
        }
    }

    public abstract static class Formula extends Node {
        Formula(int pos) {
            super(pos);
        }
    }

    public static final class Bin extends Formula {
        public final String op;
        public final Formula left;
        public final Formula right;

        public Bin(String op, Formula left, Formula right, int pos) {
            super(pos);
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    public static final class Not extends Formula {
        public final Formula body;

        public Not(Formula body, int pos) {
            super(pos);
            this.body = body;
        }
    }

    public static final class Quant extends Formula {
        public final String quant;
        public final String var;
        public final NType type;
        public final Formula body;

        public Quant(String quant, String var, NType type, Formula body, int pos) {
            super(pos);
            this.quant = quant;
            this.var = var;
            this.type = type;
            this.body = body;
        }
    }

    public static final class Call extends Formula {
        public final String word;
        public final List<Arg> args;

        public Call(String word, List<Arg> args, int pos) {
            super(pos);
            this.word = word;
            this.args = args;
        }
    }

    public static final class Arg {
        public final String role;
        public final Term term;
        public final int pos;

        public Arg(String role, Term term, int pos) {
            this.role = role;
            this.term = term;
            this.pos = pos;
        }
    }

    public static final class PropName extends Formula {
        public final String name;

        public PropName(String name, int pos) {
            super(pos);
            this.name = name;
        }
    }

    public static final class TrueLit extends Formula {
        public TrueLit(int pos) {
            super(pos);
        }
    }

    public static final class FalseLit extends Formula {
        public FalseLit(int pos) {
            super(pos);
        }
    }

    public static final class TimeScope extends Formula {
        public final String op;
        public final Term arg;
        public final Formula body;

        public TimeScope(String op, Term arg, Formula body, int pos) {
            super(pos);
            this.op = op;
            this.arg = arg;
            this.body = body;
        }
    }

    public static final class TimeRel extends Formula {
        public final String op;
        public final List<Term> args;

        public TimeRel(String op, List<Term> args, int pos) {
            super(pos);
            this.op = op;
            this.args = args;
        }
    }

    public static final class Rel extends Formula {
        public final String op;
        public final Term left;
        public final Term right;

        public Rel(String op, Term left, Term right, int pos) {
            super(pos);
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    public abstract static class Term extends Node {
        Term(int pos) {
            super(pos);
        }
    }

    public static final class WordTerm extends Term {
        public final char prefix;
        public final String word;

        public WordTerm(char prefix, String word, int pos) {
            super(pos);
            this.prefix = prefix;
            this.word = word;
        }
    }

    public static final class VarTerm extends Term {
        public final String name;

        public VarTerm(String name, int pos) {
            super(pos);
            this.name = name;
        }
    }

    public static final class LitTerm extends Term {
        public final Token.K kind;
        public final String text;
        public final String value;
        public final String from;
        public final String to;

        public LitTerm(Token.K kind, String text, String value, int pos) {
            this(kind, text, value, null, null, pos);
        }

        public LitTerm(Token.K kind, String text, String value, String from, String to, int pos) {
            super(pos);
            this.kind = kind;
            this.text = text;
            this.value = value;
            this.from = from;
            this.to = to;
        }
    }

    public static final class Iota extends Term {
        public final String var;
        public final NType type;
        public final Formula body;

        public Iota(String var, NType type, Formula body, int pos) {
            super(pos);
            this.var = var;
            this.type = type;
            this.body = body;
        }
    }

    public static final class Card extends Term {
        public final Term set;

        public Card(Term set, int pos) {
            super(pos);
            this.set = set;
        }
    }

    public static final class SetComp extends Term {
        public final String var;
        public final NType type;
        public final Formula body;

        public SetComp(String var, NType type, Formula body, int pos) {
            super(pos);
            this.var = var;
            this.type = type;
            this.body = body;
        }
    }

    public static final class BoolBox extends Term {
        public final Formula body;

        public BoolBox(Formula body, int pos) {
            super(pos);
            this.body = body;
        }
    }
}
