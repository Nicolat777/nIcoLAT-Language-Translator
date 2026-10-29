package nlang;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 手写词法分析器。
 *
 * <p>为什么手写而不是 ANTLR：N 语言的记号集是「形状可分」的（含元音的才是词），
 * 而且要求「最长合法记号 + 完整消费输入」，报错要精确到规范 5.8 的十类诊断。
 * 手写 60 行就能做到，而且不受词法器生成器的歧义规则限制。</p>
 */
public final class Lexer {

    /** 纯辅音语法算子（规范 3.1、3.3、3.4）。与含元音的词形天然不冲突。 */
    private static final Set<String> KEYWORDS = Set.of(
            "n", "m", "s", "z", "bb",
            "h", "hh", "hl", "hv",
            "hb", "hk", "hm", "ht", "hn", "hd",
            "kp", "kt", "kb", "kk", "kf", "kh", "km", "kn", "kl", "ks", "kz",
            "gp", "gb", "gt", "gd", "gm", "gn", "gh", "gz", "gf", "gg", "gv", "gs");

    public static final Set<String> ROLES = Set.of(
            "kp", "kt", "kb", "kk", "kf", "kh", "km", "kn", "kl", "ks", "kz",
            "gp", "gb", "gt", "gd", "gm", "gn", "gh", "gz", "gf", "gg", "gv", "gs");

    private static final String CONS = "pbtdkgfvszhmnl";
    private static final String VOW = "aeiou";
    private static final String PROPN = "CJQRWXY";
    private static final String TYPE_LETTERS = "ABNDEK M U S L O H".replace(" ", "");

    private final String src;
    private final int len;
    private int i = 0;
    private final List<Token> out = new ArrayList<>();
    private final List<Diagnostic> diags;

    public Lexer(String src, List<Diagnostic> diags) {
        this.src = src;
        this.len = src.length();
        this.diags = diags;
    }

    public List<Token> lex() {
        while (i < len) {
            char c = src.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                i++;
                continue;
            }
            int start = i;
            if (c == '!') {
                literal(start);
            } else if (c == '"') {
                string(start);
            } else if (Character.isDigit(c)
                    || (c == '-' && i + 1 < len && Character.isDigit(src.charAt(i + 1)))) {
                number(start);
            } else if (c >= 'a' && c <= 'z') {
                lowerWord(start);
            } else if (c >= 'A' && c <= 'Z') {
                upper(start);
            } else if (!symbol(start)) {
                err(Diagnostic.Cat.LEX, "LEX_CHAR", start, "非法字符 '" + c + "'");
                i++;
            }
        }
        out.add(new Token(Token.K.EOF, "", null, len));
        return out;
    }

    // ------------------------------------------------------------------ 字母

    private void lowerWord(int start) {
        while (i < len && isLower(src.charAt(i))) {
            i++;
        }
        // 变量可以带数字后缀（x0、x12），数字也属于这个记号
        if (i - start == 1 && "xyw".indexOf(src.charAt(start)) >= 0
                && i < len && Character.isDigit(src.charAt(i))) {
            while (i < len && Character.isDigit(src.charAt(i))) {
                i++;
            }
            String v = src.substring(start, i);
            if (!isVarForm(v)) {
                err(Diagnostic.Cat.LEX, "LEX_VARIABLE", start,
                        "变量名不合法「" + v + "」：数字后缀不能有前导零");
                return;
            }
        }
        String w = src.substring(start, i);
        if (isVarForm(w)) {
            out.add(new Token(Token.K.VAR, w, null, start));
            return;
        }
        if (isWordForm(w)) {
            out.add(new Token(Token.K.WORD, w, null, start));
            return;
        }
        if (KEYWORDS.contains(w)) {
            out.add(new Token(Token.K.KEYWORD, w, null, start));
            return;
        }
        err(Diagnostic.Cat.LEX, "LEX_UNREGISTERED_CONSONANTS", start,
                "未登记的辅音串「" + w + "」：语法算子只能取自规范 3.1 的固定表");
    }

    private void upper(int start) {
        while (i < len && src.charAt(i) >= 'A' && src.charAt(i) <= 'Z') {
            i++;
        }
        String w = src.substring(start, i);
        if (w.length() == 1) {
            char c = w.charAt(0);
            if (PROPN.indexOf(c) >= 0) {
                out.add(new Token(Token.K.PROPNAME, w, null, start));
                return;
            }
            if (TYPE_LETTERS.indexOf(c) >= 0) {
                out.add(new Token(Token.K.TYPE, w, null, start));
                return;
            }
        }
        err(Diagnostic.Cat.LEX, "LEX_UNKNOWN_LETTERS", start, "未登记的大写字母串「" + w + "」");
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    /** (?:[pbtdkgfvszhmnl][aeiou])+ */
    private static boolean isWordForm(String w) {
        if (w.length() < 2 || w.length() % 2 != 0) {
            return false;
        }
        for (int k = 0; k < w.length(); k++) {
            char c = w.charAt(k);
            if (k % 2 == 0 ? CONS.indexOf(c) < 0 : VOW.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    /** [xyw](?:0|[1-9][0-9]*)? */
    private static boolean isVarForm(String w) {
        char c = w.charAt(0);
        if ("xyw".indexOf(c) < 0) {
            return false;
        }
        if (w.length() == 1) {
            return true;
        }
        String rest = w.substring(1);
        if ("0".equals(rest)) {
            return true;
        }
        return rest.matches("[1-9][0-9]*");
    }

    // ------------------------------------------------------------------ 数值

    private void number(int start) {
        StringBuilder raw = new StringBuilder();
        if (src.charAt(i) == '-') {
            raw.append('-');
            i++;
        }
        int intStart = i;
        while (i < len && Character.isDigit(src.charAt(i))) {
            i++;
        }
        String intPart = src.substring(intStart, i);
        if (intPart.isEmpty()) {
            err(Diagnostic.Cat.LEX, "LEX_NUMBER", start, "缺少整数部分");
            return;
        }
        if (intPart.length() > 1 && intPart.charAt(0) == '0') {
            err(Diagnostic.Cat.LITERAL, "LITERAL_LEADING_ZERO", start, "数值不允许前导零");
            return;
        }
        raw.append(intPart);

        if (i < len && src.charAt(i) == '/') {
            i++;
            raw.append('/');
            int denStart = i;
            while (i < len && Character.isDigit(src.charAt(i))) {
                i++;
            }
            String den = src.substring(denStart, i);
            if (den.isEmpty()) {
                err(Diagnostic.Cat.LITERAL, "LITERAL_FRACTION", start, "分数缺少分母");
                return;
            }
            raw.append(den);
            if (!den.matches("0*[1-9][0-9]*")) {
                err(Diagnostic.Cat.LITERAL, "LITERAL_FRACTION", start,
                        "分母必须为正整数且不能为零");
                return;
            }
        } else if (i + 1 < len && src.charAt(i) == '.' && Character.isDigit(src.charAt(i + 1))) {
            i++;
            raw.append('.');
            int fracStart = i;
            while (i < len && Character.isDigit(src.charAt(i))) {
                i++;
            }
            raw.append(src, fracStart, i);
        } else if (i < len && src.charAt(i) == '.') {
            // 语句结束句点：不吞掉
            out.add(new Token(Token.K.NUMBER, raw.toString(), canon(raw.toString(), start), start));
            return;
        }

        String text = raw.toString();
        String canon = canon(text, start);
        if (canon != null) {
            out.add(new Token(Token.K.NUMBER, text, canon, start));
        }
    }

    private String canon(String text, int pos) {
        try {
            return Literals.Num.parse(text).canon();
        } catch (RuntimeException e) {
            err(Diagnostic.Cat.LITERAL, "LITERAL_NUMBER", pos, e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ 字符串

    private void string(int start) {
        QStr q = readQuoted();
        if (q != null) {
            out.add(new Token(Token.K.STRING, q.raw, q.value, start));
        }
    }

    private record QStr(String raw, String value) {
    }

    /** 当前位置是开引号；返回引号内的原文与解码值。 */
    private QStr readQuoted() {
        int open = i;
        i++; // "
        StringBuilder raw = new StringBuilder();
        while (i < len) {
            char c = src.charAt(i);
            if (c == '"') {
                i++;
                String r = raw.toString();
                List<String> errs = new ArrayList<>();
                String v = Literals.decode(r, errs);
                if (v == null) {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_STRING", open,
                            "字符串不合法：" + (errs.isEmpty() ? "未知原因" : errs.get(0)));
                    return null;
                }
                return new QStr(r, v);
            }
            if (c == '\\') {
                if (i + 1 >= len) {
                    break;
                }
                raw.append(c).append(src.charAt(i + 1));
                i += 2;
                continue;
            }
            raw.append(c);
            i++;
        }
        err(Diagnostic.Cat.LITERAL, "LITERAL_STRING_UNTERMINATED", open, "字符串缺少结束引号");
        return null;
    }

    // ------------------------------------------------------------------ 字面量

    private void literal(int start) {
        i++; // '!'
        if (i >= len) {
            err(Diagnostic.Cat.LITERAL, "LITERAL_PREFIX", start, "'!' 之后缺少字面量内容");
            return;
        }
        char k = src.charAt(i);
        switch (k) {
            case 'd': {
                i++;
                QStr q = expectQuoted(start, "!d");
                if (q == null) {
                    return;
                }
                if (!Literals.validDate(q.value)) {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_DATE", start,
                            "日期不合法（公历，年份 0001–9999）：\"" + q.value + "\"");
                    return;
                }
                out.add(new Token(Token.K.DATE, src.substring(start, i), q.value, start));
                return;
            }
            case 't': {
                i++;
                QStr q = expectQuoted(start, "!t");
                if (q == null) {
                    return;
                }
                if (Literals.parseTime(q.value) == null) {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_TIME", start,
                            "时刻不合法（需 YYYY-MM-DDThh:mm:ss 加 Z 或 ±hh:mm）：\"" + q.value + "\"");
                    return;
                }
                out.add(new Token(Token.K.TIME, src.substring(start, i), q.value, start));
                return;
            }
            case 'l': {
                i++;
                QStr q = expectQuoted(start, "!l");
                if (q == null) {
                    return;
                }
                if (!Literals.validUrl(q.value)) {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_URL", start,
                            "URL 不合法（只接受主机非空的 http/https 绝对地址）：\"" + q.value + "\"");
                    return;
                }
                out.add(new Token(Token.K.URL, src.substring(start, i), q.value, start));
                return;
            }
            case 'h': {
                i++;
                if (i >= len || src.charAt(i) != '[') {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_INTERVAL", start, "区间字面量应为 !h[时刻,时刻]");
                    recover(start);
                    return;
                }
                i++; // [
                String a = timeLiteral(start);
                if (a == null) {
                    return;
                }
                skipSpace();
                if (i >= len || src.charAt(i) != ',') {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_INTERVAL", start, "区间字面量缺少逗号");
                    recover(start);
                    return;
                }
                i++;
                String b = timeLiteral(start);
                if (b == null) {
                    return;
                }
                skipSpace();
                if (i >= len || src.charAt(i) != ']') {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_INTERVAL", start, "区间字面量缺少右方括号");
                    recover(start);
                    return;
                }
                i++;
                if (!Literals.validInterval(a, b)) {
                    err(Diagnostic.Cat.LITERAL, "LITERAL_INTERVAL_ORDER", start,
                            "区间要求起点严格早于终点");
                    return;
                }
                out.add(new Token(Token.K.INTERVAL, src.substring(start, i), null, start));
                return;
            }
            default:
                err(Diagnostic.Cat.LITERAL, "LITERAL_PREFIX", start,
                        "未知字面量前缀 !" + k + "（只允许 !d !t !l !h）");
                recover(start);
        }
    }

    /** 读取 !t"..."，返回原始时刻字符串。 */
    private String timeLiteral(int outerStart) {
        skipSpace();
        if (i + 1 >= len || src.charAt(i) != '!' || src.charAt(i + 1) != 't') {
            err(Diagnostic.Cat.LITERAL, "LITERAL_INTERVAL", outerStart,
                    "区间端点必须是时刻字面量 !t\"...\"");
            recover(outerStart);
            return null;
        }
        i += 2;
        QStr q = expectQuoted(outerStart, "!t");
        if (q == null) {
            return null;
        }
        if (Literals.parseTime(q.value) == null) {
            err(Diagnostic.Cat.LITERAL, "LITERAL_TIME", outerStart,
                    "区间端点时刻不合法：\"" + q.value + "\"");
            return null;
        }
        return q.value;
    }

    private QStr expectQuoted(int start, String what) {
        skipSpace();
        if (i >= len || src.charAt(i) != '"') {
            err(Diagnostic.Cat.LITERAL, "LITERAL_QUOTED", start,
                    what + " 之后必须紧跟双引号字符串");
            recover(start);
            return null;
        }
        return readQuoted();
    }

    private void skipSpace() {
        while (i < len && Character.isWhitespace(src.charAt(i))) {
            i++;
        }
    }

    /** 出错后跳到下一个可能的安全点，避免死循环。 */
    private void recover(int start) {
        if (i <= start) {
            i = start + 1;
        }
        while (i < len) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ')' || c == ']'
                    || c == '}' || c == '.') {
                return;
            }
            i++;
        }
    }

    // ------------------------------------------------------------------ 符号

    private boolean symbol(int start) {
        // 长的先匹配
        if (src.startsWith(":=", i)) {
            i += 2;
            out.add(new Token(Token.K.SYMBOL, ":=", null, start));
            return true;
        }
        if (src.startsWith("∃!", i)) {
            i += 2;
            out.add(new Token(Token.K.SYMBOL, "∃!", null, start));
            return true;
        }
        if (src.startsWith("<=", i) || src.startsWith(">=", i)) {
            // 规范使用 ≤ ≥；这里顺手接受键盘写法，统一归一为 Unicode
            String s = src.substring(i, i + 2);
            i += 2;
            out.add(new Token(Token.K.SYMBOL, "<=".equals(s) ? "≤" : "≥", null, start));
            return true;
        }
        char c = src.charAt(i);
        String one = String.valueOf(c);
        if ("¬∧∨→↔≡㲗=≠∈∉⊂⊆<≤>≥⊤⊥(){}:,.".indexOf(c) >= 0) {
            i++;
            if (c == '㲗' || c == '≡') {
                one = "↔"; // 规范书 PDF 中该符号字形缺失，规则是「逻辑等价」
            }
            out.add(new Token(Token.K.SYMBOL, one, null, start));
            return true;
        }
        if (c == '∀' || c == '∃') {
            i++;
            out.add(new Token(Token.K.SYMBOL, one, null, start));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ 诊断

    private void err(Diagnostic.Cat cat, String code, int pos, String msg) {
        diags.add(new Diagnostic(cat, code, pos, msg));
    }

    /** 把字符下标换算成 1-based 行列号。 */
    public static int[] lineCol(String src, int pos) {
        int line = 1;
        int col = 1;
        int limit = Math.min(pos, src.length());
        for (int k = 0; k < limit; k++) {
            if (src.charAt(k) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new int[]{line, col};
    }
}
