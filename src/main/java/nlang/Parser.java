package nlang;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归下降语法分析器，实现规范 4.2 的 BNF：
 *
 * <pre>
 * 程序   ::= ε | 语句 程序
 * 语句   ::= 定义 | 公式 "."
 * 定义   ::= 命题名 ":=" 公式 "."
 * 公式   ::= 蕴含
 * 蕴含   ::= 析取 (("→" | "↔") 析取)*
 * 析取   ::= 合取 ("∨" 合取)*
 * 合取   ::= 否定 ("∧" 否定)*
 * 否定   ::= "¬" 否定 | 原子
 * </pre>
 *
 * <p>递归下降的好处：优先级直接体现在函数层次里，报错可以精确到「这里期待什么」，
 * 而且后面加语法糖（比如中文关键词）只要改几个分支，不需要重新生成语法表。</p>
 */
public final class Parser {

    private final List<Token> toks;
    private final List<Diagnostic> diags;
    private int p = 0;

    public Parser(List<Token> toks, List<Diagnostic> diags) {
        this.toks = toks;
        this.diags = diags;
    }

    /** 语法错误时抛出，用于跳过当前语句继续分析。 */
    private static final class Bail extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Bail() {
            super(null, null, false, false);
        }
    }

    public Ast.Program parseProgram() {
        List<Ast.Stmt> stmts = new ArrayList<>();
        while (!peek().is(Token.K.EOF)) {
            int before = p;
            try {
                stmts.add(parseStatement());
            } catch (Bail b) {
                sync();
                if (p == before) {
                    p++;
                }
            }
        }
        return new Ast.Program(stmts, 0);
    }

    private void sync() {
        while (!peek().is(Token.K.EOF)) {
            if (peek().is(".")) {
                p++;
                return;
            }
            p++;
        }
    }

    // ------------------------------------------------------------------ 语句

    private Ast.Stmt parseStatement() {
        if (peek().is(Token.K.PROPNAME) && peek(1).is(":=")) {
            Token n = next();
            next(); // :=
            Ast.Formula body = parseFormula();
            expect(".");
            return new Ast.Def(n.text, body, n.pos);
        }
        Ast.Formula f = parseFormula();
        expect(".");
        return new Ast.Assert(f, f.pos);
    }

    // ------------------------------------------------------------------ 公式

    private Ast.Formula parseFormula() {
        return parseImplication();
    }

    private Ast.Formula parseImplication() {
        Ast.Formula left = parseDisjunction();
        while (peek().is("→") || peek().is("↔")) {
            Token op = next();
            Ast.Formula right = parseDisjunction();
            left = new Ast.Bin(op.text, left, right, op.pos);
        }
        return left;
    }

    private Ast.Formula parseDisjunction() {
        Ast.Formula left = parseConjunction();
        while (peek().is("∨")) {
            Token op = next();
            left = new Ast.Bin("∨", left, parseConjunction(), op.pos);
        }
        return left;
    }

    private Ast.Formula parseConjunction() {
        Ast.Formula left = parseNegation();
        while (peek().is("∧")) {
            Token op = next();
            left = new Ast.Bin("∧", left, parseNegation(), op.pos);
        }
        return left;
    }

    private Ast.Formula parseNegation() {
        if (peek().is("¬")) {
            Token t = next();
            return new Ast.Not(parseNegation(), t.pos);
        }
        return parseAtom();
    }

    private static final List<String> TIME_SCOPES = List.of("h", "hh", "hl", "hv");
    private static final List<String> TIME_RELS = List.of("hb", "hk", "hm", "ht", "hn", "hd");

    private Ast.Formula parseAtom() {
        Token t = peek();

        if (t.is("(")) {
            next();
            Ast.Formula f = parseFormula();
            expect(")");
            return f;
        }
        if (t.is(Token.K.PROPNAME)) {
            next();
            return new Ast.PropName(t.text, t.pos);
        }
        if (t.is("⊤")) {
            next();
            return new Ast.TrueLit(t.pos);
        }
        if (t.is("⊥")) {
            next();
            return new Ast.FalseLit(t.pos);
        }
        if (t.is("∀") || t.is("∃") || t.is("∃!")) {
            next();
            String var = expectKind(Token.K.VAR, "变量").text;
            expect(":");
            NType type = parseType();
            expect("(");
            Ast.Formula body = parseFormula();
            expect(")");
            return new Ast.Quant(t.text, var, type, body, t.pos);
        }
        if (t.is(Token.K.KEYWORD) && TIME_SCOPES.contains(t.text)) {
            next();
            expect("(");
            Ast.Term arg = parseTerm();
            expect(",");
            Ast.Formula body = parseFormula();
            expect(")");
            return new Ast.TimeScope(t.text, arg, body, t.pos);
        }
        if (t.is(Token.K.KEYWORD) && TIME_RELS.contains(t.text)) {
            next();
            expect("(");
            int arity = "hd".equals(t.text) ? 3 : 2;
            List<Ast.Term> args = new ArrayList<>();
            for (int k = 0; k < arity; k++) {
                if (k > 0) {
                    expect(",");
                }
                args.add(parseTerm());
            }
            expect(")");
            return new Ast.TimeRel(t.text, args, t.pos);
        }
        if (t.is(Token.K.WORD)) {
            next();
            List<Ast.Arg> args = parseArgs();
            return new Ast.Call(t.text, args, t.pos);
        }
        if (startsTerm(t)) {
            Ast.Term left = parseTerm();
            Token rel = peek();
            if (rel.is(Token.K.SYMBOL) && isRelation(rel.text)) {
                next();
                Ast.Term right = parseTerm();
                return new Ast.Rel(rel.text, left, right, rel.pos);
            }
            syntax(rel, "词项「" + describe(left) + "」后缺少关系符号（= ≠ ∈ ∉ ⊂ ⊆ < ≤ > ≥），"
                    + "单独的词项不是公式");
        }
        syntax(t, "无法识别的公式起始记号「" + t.text + "」");
        throw new Bail();
    }

    private static boolean isRelation(String s) {
        return "=≠∈∉⊂⊆<≤>≥".contains(s) && s.length() == 1;
    }

    private List<Ast.Arg> parseArgs() {
        expect("(");
        List<Ast.Arg> args = new ArrayList<>();
        if (peek().is(")")) {
            next();
            return args;
        }
        while (true) {
            Token role = peek();
            if (!(role.is(Token.K.KEYWORD) && Lexer.ROLES.contains(role.text))) {
                syntax(role, "参数必须以角色标签开头（kp kt kb … ），实际是「" + role.text + "」");
                throw new Bail();
            }
            next();
            Ast.Term term = parseTerm();
            args.add(new Ast.Arg(role.text, term, role.pos));
            if (peek().is(",")) {
                next();
                if (peek().is(")")) {
                    syntax(peek(), "不允许尾随逗号");
                    throw new Bail();
                }
                continue;
            }
            break;
        }
        expect(")");
        return args;
    }

    private static boolean startsTerm(Token t) {
        switch (t.kind) {
            case VAR:
            case NUMBER:
            case STRING:
            case DATE:
            case TIME:
            case URL:
            case INTERVAL:
                return true;
            case KEYWORD:
                return "n".equals(t.text) || "m".equals(t.text) || "s".equals(t.text)
                        || "z".equals(t.text) || "bb".equals(t.text);
            case SYMBOL:
                return "{".equals(t.text);
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------ 词项

    private Ast.Term parseTerm() {
        Token t = peek();
        switch (t.kind) {
            case VAR:
                next();
                return new Ast.VarTerm(t.text, t.pos);
            case NUMBER:
            case STRING:
            case DATE:
            case TIME:
            case URL:
                next();
                return new Ast.LitTerm(t.kind, t.text, t.value, t.pos);
            case INTERVAL:
                next();
                return new Ast.LitTerm(t.kind, t.text, null,
                        firstIntervalPart(t.text), lastIntervalPart(t.text), t.pos);
            case KEYWORD:
                switch (t.text) {
                    case "n":
                    case "m": {
                        next();
                        Token w = expectKind(Token.K.WORD, "词形");
                        return new Ast.WordTerm(t.text.charAt(0), w.text, t.pos);
                    }
                    case "s": {
                        next();
                        expect("(");
                        String var = expectKind(Token.K.VAR, "变量").text;
                        expect(":");
                        NType type = parseType();
                        expect(",");
                        Ast.Formula body = parseFormula();
                        expect(")");
                        return new Ast.Iota(var, type, body, t.pos);
                    }
                    case "z": {
                        next();
                        expect("(");
                        Ast.Term set = parseTerm();
                        expect(")");
                        return new Ast.Card(set, t.pos);
                    }
                    case "bb": {
                        next();
                        expect("(");
                        Ast.Formula body = parseFormula();
                        expect(")");
                        return new Ast.BoolBox(body, t.pos);
                    }
                    default:
                        break;
                }
                break;
            case SYMBOL:
                if (t.is("{")) {
                    next();
                    String var = expectKind(Token.K.VAR, "变量").text;
                    expect(":");
                    NType type = parseType();
                    expect(":");
                    Ast.Formula body = parseFormula();
                    expect("}");
                    return new Ast.SetComp(var, type, body, t.pos);
                }
                break;
            default:
                break;
        }
        syntax(t, "期待词项（n 词形、m 词形、变量、字面量、s(...)、z(...)、{...}、bb(...)），"
                + "实际是「" + t.text + "」");
        throw new Bail();
    }

    private NType parseType() {
        Token t = peek();
        if (!t.is(Token.K.TYPE)) {
            syntax(t, "期待类型注记（A B N D E K M U S L O 或 H<T>），实际是「" + t.text + "」");
            throw new Bail();
        }
        next();
        if ("H".equals(t.text)) {
            expect("<");
            NType inner = parseType();
            expect(">");
            return NType.set(inner);
        }
        NType ty = NType.atom(t.text);
        if (ty == null) {
            syntax(t, "未定义的原子类型「" + t.text + "」");
            throw new Bail();
        }
        return ty;
    }

    // ------------------------------------------------------------------ 记号流

    private static String firstIntervalPart(String raw) {
        int a = raw.indexOf("!t");
        if (a < 0) {
            return null;
        }
        int q1 = raw.indexOf('"', a);
        int q2 = q1 < 0 ? -1 : raw.indexOf('"', q1 + 1);
        return (q1 < 0 || q2 < 0) ? null : raw.substring(q1 + 1, q2);
    }

    private static String lastIntervalPart(String raw) {
        int b = raw.lastIndexOf("!t");
        if (b < 0) {
            return null;
        }
        int q1 = raw.indexOf('"', b);
        int q2 = q1 < 0 ? -1 : raw.indexOf('"', q1 + 1);
        return (q1 < 0 || q2 < 0) ? null : raw.substring(q1 + 1, q2);
    }

    private Token peek() {
        return toks.get(Math.min(p, toks.size() - 1));
    }

    private Token peek(int ahead) {
        return toks.get(Math.min(p + ahead, toks.size() - 1));
    }

    private Token next() {
        Token t = peek();
        if (!t.is(Token.K.EOF)) {
            p++;
        }
        return t;
    }

    private Token expect(String s) {
        Token t = peek();
        if (!t.is(s)) {
            syntax(t, "期待「" + s + "」，实际是「" + t.text + "」");
            throw new Bail();
        }
        return next();
    }

    private Token expectKind(Token.K k, String what) {
        Token t = peek();
        if (!t.is(k)) {
            syntax(t, "期待" + what + "，实际是「" + t.text + "」");
            throw new Bail();
        }
        return next();
    }

    private void syntax(Token at, String msg) {
        if (at.is(Token.K.EOF)) {
            msg = msg.replace("「」", "「输入末尾」");
        }
        diags.add(new Diagnostic(Diagnostic.Cat.SYNTAX, "SYN_UNEXPECTED", at.pos, msg));
    }

    private static String describe(Ast.Node n) {
        if (n instanceof Ast.WordTerm w) {
            return w.prefix + " " + w.word;
        }
        if (n instanceof Ast.VarTerm v) {
            return v.name;
        }
        if (n instanceof Ast.LitTerm l) {
            return l.text;
        }
        return n.getClass().getSimpleName();
    }
}
