package nlang;

/**
 * 诊断信息。
 *
 * <p>规范 5.8 / 附录 B 要求实现至少要区分：词法错误、语法错误、未注册名称、类型错误、
 * 签名错误、无效 literal、缺少上下文、未定义项、模型不一致、无法判定。
 * 这十类就是下面 Cat 的取值，编译器/翻译器的所有报错都必须落在这十类里，
 * 这样上层 IDE 才能给出稳定的一致提示。</p>
 */
public final class Diagnostic {

    public enum Cat {
        LEX,          // 词法错误
        SYNTAX,       // 语法错误
        UNREGISTERED, // 未注册名称
        TYPE,         // 类型错误
        SIGNATURE,    // 签名错误（角色缺失/重复/未知）
        LITERAL,      // 无效 literal
        CONTEXT,      // 缺少上下文（参照时刻等）
        UNDEFINED,    // 未定义项（iota 求值失败）
        MODEL,        // 模型不一致
        UNDECIDABLE,  // 无法判定
        NOTE          // 提示（不影响通过）
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
