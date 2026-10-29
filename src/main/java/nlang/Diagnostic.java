package nlang;

public final class Diagnostic {

    public enum Cat {
        LEX,
        SYNTAX,
        UNREGISTERED,
        TYPE,
        SIGNATURE,
        LITERAL,
        CONTEXT,
        UNDEFINED,
        MODEL,
        UNDECIDABLE,
        NOTE
    }

    public final Cat cat;
    public final String code;
    public final int pos;
    public final String msg;

    public Diagnostic(Cat cat, String code, int pos, String msg) {
        this.cat = cat;
        this.code = code;
        this.pos = pos;
        this.msg = msg;
    }

    public String catZh() {
        switch (cat) {
            case LEX: return "词法错误";
            case SYNTAX: return "语法错误";
            case UNREGISTERED: return "未注册名称";
            case TYPE: return "类型错误";
            case SIGNATURE: return "签名错误";
            case LITERAL: return "无效 literal";
            case CONTEXT: return "缺少上下文";
            case UNDEFINED: return "未定义项";
            case MODEL: return "模型不一致";
            case UNDECIDABLE: return "无法判定";
            default: return "提示";
        }
    }

    public boolean isError() {
        return cat != Cat.NOTE;
    }

    @Override
    public String toString() {
        return cat + "/" + code + "@" + pos + ": " + msg;
    }
}
