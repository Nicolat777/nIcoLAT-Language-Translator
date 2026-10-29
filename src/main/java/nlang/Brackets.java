package nlang;

/**
 * 括号平衡预检查。
 *
 * <p>递归下降一旦少个括号，报错会出现在很远的地方（"期待「)」，实际是「.」"），
 * 极难定位。所以在解析前先扫一遍：少了就直说缺几个、从哪行开始；
 * 多了就直说第几行第几列多了一个。</p>
 *
 * <p>字符串（含 !d"…" !t"…" !l"…"）里的括号不计数。</p>
 */
public final class Brackets {

    /** @return 有问题的诊断；平衡返回 null */
    public static Diagnostic check(String src) {
        int depth = 0;
        int line = 1;
        int col = 0;
        int openLine = 1;
        boolean inStr = false;
        boolean esc = false;

        for (int k = 0; k < src.length(); k++) {
            char c = src.charAt(k);
            if (c == '\n') {
                line++;
                col = 0;
                inStr = false;
                esc = false;
                continue;
            }
            col++;
            if (inStr) {
                if (esc) {
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
            } else if (c == '(') {
                if (depth == 0) {
                    openLine = line;
                }
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return new Diagnostic(Diagnostic.Cat.SYNTAX, "SYN_EXTRA_PAREN", k,
                            "第 " + line + " 行第 " + col + " 列多了一个「)」，括号不匹配");
                }
            }
        }
        if (depth > 0) {
            return new Diagnostic(Diagnostic.Cat.SYNTAX, "SYN_UNCLOSED_PAREN", src.length(),
                    "括号没有闭合：从第 " + openLine + " 行开始的表达式还缺 " + depth + " 个「)」");
        }
        return null;
    }

    private Brackets() {
    }
}
