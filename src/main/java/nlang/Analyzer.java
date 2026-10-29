package nlang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 静态检查：词典/名称解析 → 变量绑定 → 参数签名与类型 → literal 值域（规范 5.3 的检查顺序）。
 *
 * <p>这里刻意不做求值。规范 5.4 / 5.6 明确：类型正确不等于陈述为真；
 * iota 匹配 0 个或多个对象属于「求值未定义」，不能冒充静态类型错误。
 * 所以求值器（下一阶段）才负责 IOTA_EMPTY / IOTA_MULTIPLE / UNDECIDABLE。</p>
 */
public final class Analyzer {

    private final Lexicon lex;
    private final List<Diagnostic> diags;
    private final Deque<Map<String, NType>> scopes = new ArrayDeque<>();
    private final Set<String> definedProps = new HashSet<>();

    public Analyzer(Lexicon lex, List<Diagnostic> diags) {
        this.lex = lex;
        this.diags = diags;
    }

    public void analyze(Ast.Program program) {
        scopes.clear();
        scopes.push(new HashMap<>());
        definedProps.clear();
        for (Ast.Stmt s : program.stmts) {
            if (s instanceof Ast.Def d) {
                if (definedProps.contains(d.name)) {
                    err(Diagnostic.Cat.TYPE, "DUPLICATE_PROPNAME", d.pos,
                            "命题名 " + d.name + " 只能定义一次");
                }
                checkFormula(d.body);
                definedProps.add(d.name);
            } else if (s instanceof Ast.Assert a) {
                checkFormula(a.body);
            }
        }
    }

    // ------------------------------------------------------------------ 公式

    private void checkFormula(Ast.Formula f) {
        if (f instanceof Ast.Bin b) {
            checkFormula(b.left);
            checkFormula(b.right);
        } else if (f instanceof Ast.Not n) {
            checkFormula(n.body);
        } else if (f instanceof Ast.Quant q) {
            if (!validBindingType(q.type, q.pos, "量词")) {
                return;
            }
            if (!"∀".equals(q.quant) && !"∃".equals(q.quant) && q.type.equals(NType.B)) {
                err(Diagnostic.Cat.TYPE, "QUANT_B_UNSUPPORTED", q.pos,
                        "∃! 与 s 的绑定类型不能为 B：唯一性需要对象身份判定");
                return;
            }
            scopes.push(new HashMap<>());
            bind(q.var, q.type);
            checkFormula(q.body);
            scopes.pop();
        } else if (f instanceof Ast.Call c) {
            checkCall(c);
        } else if (f instanceof Ast.PropName p) {
            if (!definedProps.contains(p.name)) {
                err(Diagnostic.Cat.UNREGISTERED, "PROPNAME_NOT_DEFINED", p.pos,
                        "命题名 " + p.name + " 尚未定义（必须先定义后使用，不允许递归或前向引用）");
            }
        } else if (f instanceof Ast.TrueLit || f instanceof Ast.FalseLit) {
            // 恒真 / 恒假
        } else if (f instanceof Ast.TimeScope ts) {
            NType want = "hh".equals(ts.op) || "hl".equals(ts.op) ? NType.E : NType.D;
            requireType(ts.arg, want, "时间作用域 " + ts.op);
            checkFormula(ts.body);
        } else if (f instanceof Ast.TimeRel tr) {
            checkTimeRel(tr);
        } else if (f instanceof Ast.Rel r) {
            checkRel(r);
        }
    }

    private void checkCall(Ast.Call c) {
        Lexicon.Entry e = lex.word(c.word);
        if (e == null) {
            err(Diagnostic.Cat.UNREGISTERED, "UNREGISTERED_WORD", c.pos,
                    "词形「" + c.word + "」未在词典注册（能转换成词形不等于已拥有词义）");
            for (Ast.Arg a : c.args) {
                checkTerm(a.term);
            }
            return;
        }
        if (e.kind != Lexicon.Kind.PREDICATE) {
            err(Diagnostic.Cat.TYPE, "NOT_A_PREDICATE", c.pos,
                    "「" + c.word + "」是" + kindZh(e.kind) + "，不是谓词，不能后接括号调用");
            for (Ast.Arg a : c.args) {
                checkTerm(a.term);
            }
            return;
        }

        Set<String> seen = new HashSet<>();
        for (Ast.Arg a : c.args) {
            Lexicon.Sig sig = find(e, a.role);
            if (sig == null) {
                err(Diagnostic.Cat.SIGNATURE, "UNKNOWN_ROLE", a.pos,
                        "谓词 " + c.word + " 的签名不允许角色 " + a.role);
            }
            if (!seen.add(a.role)) {
                err(Diagnostic.Cat.SIGNATURE, "DUPLICATE_ROLE", a.pos,
                        "同一角色 " + a.role + " 在 V2 中每项最大出现次数为 1");
            }
            NType actual = checkTerm(a.term);
            if (sig != null && actual != null && !sig.type.equals(actual)) {
                err(Diagnostic.Cat.TYPE, "ROLE_TYPE_MISMATCH", a.term.pos,
                        "角色 " + a.role + " 期待 " + sig.type + "（" + sig.type.zhName()
                                + "），实际是 " + actual + "（" + actual.zhName() + "）");
            }
        }
        for (Lexicon.Sig s : e.sig) {
            if (s.required && !seen.contains(s.role)) {
                err(Diagnostic.Cat.SIGNATURE, "MISSING_ROLE", c.pos,
                        "谓词 " + c.word + " 缺少必需角色 " + s.role + ":" + s.type
                                + "（未知的必需参数必须显式量化，不能省略）");
            }
        }
    }

    private void checkTimeRel(Ast.TimeRel tr) {
        switch (tr.op) {
            case "hb":
            case "hk":
                requireType(tr.args.get(0), NType.D, tr.op + " 第一项");
                requireType(tr.args.get(1), NType.D, tr.op + " 第二项");
                break;
            case "hm":
                requireType(tr.args.get(0), NType.E, "hm 第一项");
                requireType(tr.args.get(1), NType.D, "hm 第二项");
                break;
            case "ht":
                requireType(tr.args.get(0), NType.K, "ht 第一项");
                requireType(tr.args.get(1), NType.D, "ht 第二项");
                break;
            case "hn":
                requireType(tr.args.get(0), NType.K, "hn 第一项");
                requireType(tr.args.get(1), NType.E, "hn 第二项");
                break;
            case "hd":
                requireType(tr.args.get(0), NType.K, "hd 第一项");
                requireType(tr.args.get(1), NType.N, "hd 第二项");
                requireType(tr.args.get(2), NType.U, "hd 第三项");
                break;
            default:
                break;
        }
    }

    private void checkRel(Ast.Rel r) {
        NType l = checkTerm(r.left);
        NType rt = checkTerm(r.right);
        if (l == null || rt == null) {
            return;
        }
        switch (r.op) {
            case "=":
            case "≠":
                if (l.equals(NType.B) || rt.equals(NType.B)) {
                    err(Diagnostic.Cat.TYPE, "EQ_ON_PROPOSITION", r.pos,
                            "= 与 ≠ 不比较命题身份；命题之间用 ↔ 表达逻辑等价");
                } else if (!l.equals(rt)) {
                    err(Diagnostic.Cat.TYPE, "REL_TYPE_MISMATCH", r.pos,
                            "两侧类型必须相同：左 " + l + "，右 " + rt);
                }
                break;
            case "∈":
            case "∉":
                if (!rt.isSet()) {
                    err(Diagnostic.Cat.TYPE, "MEMBER_OF_NON_SET", r.pos,
                            r.op + " 的右侧必须是集合 H<T>，实际是 " + rt);
                } else if (!rt.elem.equals(l)) {
                    err(Diagnostic.Cat.TYPE, "MEMBER_TYPE_MISMATCH", r.pos,
                            l + " 不能作为 " + rt + " 的成员");
                }
                break;
            case "⊂":
            case "⊆":
                if (!l.isSet() || !rt.isSet()) {
                    err(Diagnostic.Cat.TYPE, "SUBSET_OF_NON_SET", r.pos,
                            r.op + " 两侧都必须是集合 H<T>，实际是 " + l + " 与 " + rt);
                } else if (!l.elem.equals(rt.elem)) {
                    err(Diagnostic.Cat.TYPE, "SUBSET_TYPE_MISMATCH", r.pos,
                            "两集合元素类型必须相同：" + l + " 与 " + rt);
                }
                break;
            case "<":
            case "≤":
            case ">":
            case "≥":
                if (!l.equals(NType.N) || !rt.equals(NType.N)) {
                    err(Diagnostic.Cat.TYPE, "ORDER_TYPE_MISMATCH", r.pos,
                            r.op + " 只作用于 N，实际是 " + l + " 与 " + rt);
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ 词项

    /** @return 词项类型；出错时返回 null（已记录诊断） */
    private NType checkTerm(Ast.Term t) {
        if (t instanceof Ast.WordTerm w) {
            return wordTermType(w);
        }
        if (t instanceof Ast.VarTerm v) {
            NType ty = lookup(v.name);
            if (ty == null) {
                err(Diagnostic.Cat.TYPE, "UNBOUND_VARIABLE", v.pos,
                        "自由变量 " + v.name + " 未绑定（量词、集合构造、iota 只绑定各自括号内的变量）");
            }
            return ty;
        }
        if (t instanceof Ast.LitTerm l) {
            switch (l.kind) {
                case NUMBER: return NType.N;
                case STRING: return NType.S;
                case DATE: return NType.L;
                case TIME: return NType.D;
                case URL: return NType.O;
                case INTERVAL: return NType.E;
                default: return null;
            }
        }
        if (t instanceof Ast.Iota iota) {
            if (!validBindingType(iota.type, iota.pos, "iota")) {
                return null;
            }
            if (iota.type.equals(NType.B)) {
                err(Diagnostic.Cat.TYPE, "IOTA_B_UNSUPPORTED", iota.pos,
                        "s 的绑定类型不能为 B：唯一性需要对象身份判定");
                return null;
            }
            scopes.push(new HashMap<>());
            bind(iota.var, iota.type);
            checkFormula(iota.body);
            scopes.pop();
            return iota.type;
        }
        if (t instanceof Ast.Card c) {
            NType ty = checkTerm(c.set);
            if (ty == null) {
                return NType.N;
            }
            if (!ty.isSet()) {
                err(Diagnostic.Cat.TYPE, "CARD_OF_NON_SET", c.pos,
                        "z 只接受有限集合 H<T>，实际是 " + ty);
            }
            return NType.N;
        }
        if (t instanceof Ast.SetComp sc) {
            if (!validBindingType(sc.type, sc.pos, "集合构造")) {
                return null;
            }
            scopes.push(new HashMap<>());
            bind(sc.var, sc.type);
            checkFormula(sc.body);
            scopes.pop();
            return NType.set(sc.type);
        }
        if (t instanceof Ast.BoolBox b) {
            checkFormula(b.body);
            return NType.B;
        }
        return null;
    }

    private NType wordTermType(Ast.WordTerm w) {
        if (w.prefix == 'm') {
            Lexicon.Name n = lex.name(w.word);
            if (n == null) {
                err(Diagnostic.Cat.UNREGISTERED, "UNREGISTERED_NAME", w.pos,
                        "名称「" + w.word + "」不在名称表中（解析前必须先知道名称对应的类型）");
                return null;
            }
            return n.type;
        }
        Lexicon.Entry e = lex.word(w.word);
        if (e == null) {
            err(Diagnostic.Cat.UNREGISTERED, "UNREGISTERED_WORD", w.pos,
                    "词形「" + w.word + "」未在词典注册");
            return null;
        }
        if (e.kind == Lexicon.Kind.NUMERAL) {
            err(Diagnostic.Cat.TYPE, "NUMERAL_AS_TERM", w.pos,
                    "「" + w.word + "」是数字读音条目，核心词项位置不接受它；"
                            + "导入旧文本时应转换为数值 " + e.concept);
            return null;
        }
        if (e.kind == Lexicon.Kind.PREDICATE) {
            err(Diagnostic.Cat.TYPE, "PREDICATE_AS_TERM", w.pos,
                    "「" + w.word + "」是谓词而不是可引用的词项对象");
            return null;
        }
        return e.type;
    }

    // ------------------------------------------------------------------ 工具

    private void requireType(Ast.Term t, NType want, String where) {
        NType actual = checkTerm(t);
        if (actual != null && !want.equals(actual)) {
            err(Diagnostic.Cat.TYPE, "TYPE_MISMATCH", t.pos,
                    where + " 期待 " + want + "（" + want.zhName() + "），实际是 " + actual
                            + "（" + actual.zhName() + "）");
        }
    }

    private boolean validBindingType(NType t, int pos, String where) {
        if (t == null) {
            return false;
        }
        if (t.isSet() && t.containsProposition()) {
            err(Diagnostic.Cat.TYPE, "SET_OF_PROPOSITION", pos,
                    "H<B> 以及任何嵌套含 B 的集合在 V2 核心暂不开放（" + where + "）");
            return false;
        }
        return true;
    }

    private Lexicon.Sig find(Lexicon.Entry e, String role) {
        for (Lexicon.Sig s : e.sig) {
            if (s.role.equals(role)) {
                return s;
            }
        }
        return null;
    }

    private void bind(String var, NType t) {
        scopes.peek().put(var, t);
    }

    private NType lookup(String var) {
        for (Map<String, NType> scope : scopes) {
            NType t = scope.get(var);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private void err(Diagnostic.Cat cat, String code, int pos, String msg) {
        diags.add(new Diagnostic(cat, code, pos, msg));
    }

    private static String kindZh(Lexicon.Kind k) {
        switch (k) {
            case SET: return "集合词项";
            case DIMENSION: return "测量维度词项";
            case UNIT: return "单位或标度词项";
            case CONTEXT: return "上下文词项";
            case NUMERAL: return "数字读音条目";
            default: return "非谓词词项";
        }
    }
}
