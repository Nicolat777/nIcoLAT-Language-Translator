package nlang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文译写：把 N 语言的语法树读成中文句子。
 *
 * <p>注意这不是「求值」——它只反映结构，不代表这句话为真。
 * 真值需要模型、世界、参照时刻，属于下一阶段的工作。</p>
 */
public final class Renderer {

    private final Lexicon lex;

    public Renderer(Lexicon lex) {
        this.lex = lex;
    }

    /** 一句一行，按语句顺序输出中文。 */
    public String gloss(Ast.Program p) {
        StringBuilder sb = new StringBuilder();
        for (Ast.Stmt s : p.stmts) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            if (s instanceof Ast.Def d) {
                sb.append("〔定义 ").append(d.name).append("〕").append(gloss(d.body));
            } else if (s instanceof Ast.Assert a) {
                sb.append(gloss(a.body)).append('。');
            }
        }
        return sb.toString();
    }

    private String gloss(Ast.Formula f) {
        if (f instanceof Ast.Bin b) {
            switch (b.op) {
                case "∧":
                    return "（" + gloss(b.left) + "）并且（" + gloss(b.right) + "）";
                case "∨":
                    return "（" + gloss(b.left) + "）或者（" + gloss(b.right) + "）";
                case "→":
                    return "如果（" + gloss(b.left) + "），那么（" + gloss(b.right) + "）";
                case "↔":
                    return "（" + gloss(b.left) + "）当且仅当（" + gloss(b.right) + "）";
                default:
                    return "（" + gloss(b.left) + "）" + b.op + "（" + gloss(b.right) + "）";
            }
        }
        if (f instanceof Ast.Not n) {
            return "并非（" + gloss(n.body) + "）";
        }
        if (f instanceof Ast.Quant q) {
            String head;
            if ("∀".equals(q.quant)) {
                head = "对所有 " + q.var + ":" + q.type + "，";
            } else if ("∃!".equals(q.quant)) {
                head = "恰好存在一个 " + q.var + ":" + q.type + "，使得";
            } else {
                head = "存在 " + q.var + ":" + q.type + "，使得";
            }
            return head + "（" + gloss(q.body) + "）";
        }
        if (f instanceof Ast.Call c) {
            return glossCall(c);
        }
        if (f instanceof Ast.PropName pn) {
            return "命题 " + pn.name;
        }
        if (f instanceof Ast.TrueLit) {
            return "恒真";
        }
        if (f instanceof Ast.FalseLit) {
            return "恒假";
        }
        if (f instanceof Ast.TimeScope ts) {
            String head;
            switch (ts.op) {
                case "h":
                    head = "在时刻（" + gloss(ts.arg) + "），";
                    break;
                case "hh":
                    head = "在区间（" + gloss(ts.arg) + "）内的某一时刻，";
                    break;
                case "hl":
                    head = "在区间（" + gloss(ts.arg) + "）内的每个时刻，";
                    break;
                default:
                    head = "以（" + gloss(ts.arg) + "）为参照时刻，";
                    break;
            }
            return head + "（" + gloss(ts.body) + "）";
        }
        if (f instanceof Ast.TimeRel tr) {
            return glossTimeRel(tr);
        }
        if (f instanceof Ast.Rel r) {
            return glossRel(r);
        }
        return "?";
    }

    private String glossCall(Ast.Call c) {
        Map<String, String> slot = new LinkedHashMap<>();
        for (Ast.Arg a : c.args) {
            slot.putIfAbsent(a.role, gloss(a.term));
        }
        Lexicon.Entry e = lex.word(c.word);
        if (e == null) {
            return "未注册谓词 " + c.word + "(" + String.join(", ", slot.values()) + ")";
        }
        if (e.gloss == null) {
            return e.concept + "(" + String.join(", ", slot.values()) + ")";
        }
        String out = e.gloss;
        for (Map.Entry<String, String> en : slot.entrySet()) {
            out = out.replace("{" + en.getKey() + "}", en.getValue());
        }
        // 可选角色缺省时，模板里剩下的占位符直接去掉
        return out.replaceAll("\\{[a-z]{2}\\}", "");
    }

    private String glossTimeRel(Ast.TimeRel tr) {
        List<String> a = new ArrayList<>();
        for (Ast.Term t : tr.args) {
            a.add(gloss(t));
        }
        switch (tr.op) {
            case "hb": return a.get(0) + " 严格早于 " + a.get(1);
            case "hk": return a.get(0) + " 严格晚于 " + a.get(1);
            case "hm": return a.get(1) + " 属于区间 " + a.get(0);
            case "ht": return "时刻 " + a.get(1) + " 落在事件 " + a.get(0) + " 的时间范围内";
            case "hn": return "事件 " + a.get(0) + " 的时间范围完整包含于 " + a.get(1);
            case "hd": return "事件 " + a.get(0) + " 持续 " + a.get(1) + " " + a.get(2);
            default: return tr.op + "(" + String.join(", ", a) + ")";
        }
    }

    private String glossRel(Ast.Rel r) {
        String l = gloss(r.left);
        String rt = gloss(r.right);
        switch (r.op) {
            case "=": return l + "等于" + rt;
            case "≠": return l + "不等于" + rt;
            case "∈": return l + "是" + rt + "中的一个";
            case "∉": return l + "不是" + rt + "中的任何一个";
            case "⊂": return l + "是" + rt + "的真子集";
            case "⊆": return l + "是" + rt + "的子集";
            case "<": return l + "小于" + rt;
            case "≤": return l + "小于或等于" + rt;
            case ">": return l + "大于" + rt;
            case "≥": return l + "大于或等于" + rt;
            default: return l + " " + r.op + " " + rt;
        }
    }

    private String gloss(Ast.Term t) {
        if (t instanceof Ast.WordTerm w) {
            if (w.prefix == 'm') {
                Lexicon.Name n = lex.name(w.word);
                return n == null ? "m " + w.word : n.concept;
            }
            Lexicon.Entry e = lex.word(w.word);
            if (e == null) {
                return "n " + w.word;
            }
            if (e.kind == Lexicon.Kind.SET) {
                return "所有" + e.concept;
            }
            return e.gloss != null ? e.gloss : e.concept;
        }
        if (t instanceof Ast.VarTerm v) {
            return v.name;
        }
        if (t instanceof Ast.LitTerm l) {
            switch (l.kind) {
                case NUMBER: return l.value != null ? l.value : l.text;
                case STRING: return "「" + l.value + "」";
                case DATE: return l.value + "（日历日期）";
                case TIME: return l.value;
                case URL: return l.value;
                case INTERVAL: return "区间[" + l.from + ", " + l.to + ")";
                default: return l.text;
            }
        }
        if (t instanceof Ast.Iota i) {
            return "唯一满足（" + gloss(i.body) + "）的 " + i.var + ":" + i.type + " 对象";
        }
        if (t instanceof Ast.Card c) {
            return "（" + gloss(c.set) + "）的元素个数";
        }
        if (t instanceof Ast.SetComp sc) {
            return "所有满足（" + gloss(sc.body) + "）的 " + sc.var + ":" + sc.type;
        }
        if (t instanceof Ast.BoolBox b) {
            return "命题「" + gloss(b.body) + "」";
        }
        return "?";
    }
}
