package nlang;

public final class Token {

    public enum K {
        WORD, VAR, NUMBER, STRING, DATE, TIME, URL, INTERVAL,
        SYMBOL, KEYWORD, PROPNAME, TYPE, EOF
    }

    public final K kind;
    public final String text;
    public final String value;
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
