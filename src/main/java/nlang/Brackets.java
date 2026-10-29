package nlang;

public final class Brackets {

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
