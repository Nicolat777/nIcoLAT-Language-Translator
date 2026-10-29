package nlang;

/**
 * 词法记号。
 *
 * <p>N 语言规范 3.1 要求「最长合法记号」并且「概念词含元音，语法算子不含元音」。
 * 因此记号种类可以按形状划分：</p>
 * <ul>
 *   <li>WORD：CV 音节串（必然含元音），例如 bapu、tapu</li>
 *   <li>KEYWORD：纯辅音串，例如 n、m、kp、hb、bb（不可能与 WORD 混淆）</li>
 *   <li>VAR：x / y / w 及 x0 / x12</li>
 *   <li>TYPE / PROPNAME：大写字母，两类字母表互不相交（类型 A B N D E K M U S L O H，
 *       命题名 C J Q R W X Y）</li>
 *   <li>SYMBOL：形式逻辑与标点</li>
 *   <li>字面量：NUMBER / STRING / DATE / TIME / URL / INTERVAL</li>
 * </ul>
 */
public final class Token {

    public enum K {
        WORD, VAR, NUMBER, STRING, DATE, TIME, URL, INTERVAL,
        SYMBOL, KEYWORD, PROPNAME, TYPE, EOF
    }

    public final K kind;
    /** 原文切片（区间的原文、数值的原文等） */
    public final String text;
    /** 规范化后的值：字符串为解码结果，数值为既约分数形式，其余通常为 null */
    public final String value;
    /** 在源串中的字符下标，用于报错定位 */
    public final int pos;

    public Token(K kind, String text, String value, int pos) {
        this.kind = kind;
        this.text = text;
        this.value = value;
        this.pos = pos;
    }

    public boolean is(String s) {
        return text.equals(s);
    }

    public boolean is(K k) {
        return kind == k;
    }

    @Override
    public String toString() {
        return kind + "(" + text + ")";
    }
}
