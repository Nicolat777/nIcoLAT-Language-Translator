package nlang;

public final class NType {

    public static final NType A = new NType("A", null);
    public static final NType B = new NType("B", null);
    public static final NType N = new NType("N", null);
    public static final NType D = new NType("D", null);
    public static final NType E = new NType("E", null);
    public static final NType K = new NType("K", null);
    public static final NType M = new NType("M", null);
    public static final NType U = new NType("U", null);
    public static final NType S = new NType("S", null);
    public static final NType L = new NType("L", null);
    public static final NType O = new NType("O", null);

    public final String atom;
    public final NType elem;

    private NType(String atom, NType elem) {
        this.atom = atom;
        this.elem = elem;
    }

    public static NType set(NType elem) {
        return new NType(null, elem);
    }

    public static NType atom(String name) {
        switch (name) {
            case "A": return A;
            case "B": return B;
            case "N": return N;
            case "D": return D;
            case "E": return E;
            case "K": return K;
            case "M": return M;
            case "U": return U;
            case "S": return S;
            case "L": return L;
            case "O": return O;
            default: return null;
        }
    }

    public boolean isSet() {
        return elem != null;
    }

    public boolean containsProposition() {
        return isSet() ? elem.containsProposition() : "B".equals(atom);
    }

    public String zhName() {
        if (isSet()) {
            return "集合<" + elem + ">";
        }
        switch (atom) {
            case "A": return "个体 A";
            case "B": return "命题 B";
            case "N": return "有理数 N";
            case "D": return "时刻 D";
            case "E": return "区间 E";
            case "K": return "事件 K";
            case "M": return "测量维度 M";
            case "U": return "单位 U";
            case "S": return "文本 S";
            case "L": return "日历日期 L";
            case "O": return "URL O";
            default: return atom;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NType)) {
            return false;
        }
        NType t = (NType) o;
        if (atom != null) {
            return atom.equals(t.atom);
        }
        return t.elem != null && elem.equals(t.elem);
    }

    @Override
    public int hashCode() {
        return atom != null ? atom.hashCode() : 31 * elem.hashCode() + 7;
    }

    @Override
    public String toString() {
        return isSet() ? "H<" + elem + ">" : atom;
    }
}
