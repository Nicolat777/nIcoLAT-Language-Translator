package nlang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Lexicon {

    public enum Kind { SET, PREDICATE, DIMENSION, UNIT, CONTEXT, NUMERAL }

    public static final class Sig {
        public final String role;
        public final NType type;
        public final boolean required;

        Sig(String role, NType type, boolean required) {
            this.role = role;
            this.type = type;
            this.required = required;
        }
    }

    public static final class Entry {
        public final String code;
        public final String word;
        public final String concept;
        public final Kind kind;
        public final NType type;
        public final List<Sig> sig;
        public final String gloss;

        Entry(String code, String word, String concept, Kind kind, NType type,
              List<Sig> sig, String gloss) {
            this.code = code;
            this.word = word;
            this.concept = concept;
            this.kind = kind;
            this.type = type;
            this.sig = sig == null ? List.of() : sig;
            this.gloss = gloss;
        }
    }

    public static final class Name {
        public final String word;
        public final NType type;
        public final String concept;

        Name(String word, NType type, String concept) {
            this.word = word;
            this.type = type;
            this.concept = concept;
        }
    }

    public final Map<String, Entry> byWord = new LinkedHashMap<>();
    public final Map<String, Entry> byCode = new LinkedHashMap<>();
    public final Map<String, Name> names = new LinkedHashMap<>();

    public Lexicon() {
        bootstrapWords();
        bootstrapNames();
    }

    public Entry word(String w) {
        return byWord.get(w);
    }

    public Entry code(String c) {
        return byCode.get(c);
    }

    public Name name(String w) {
        return names.get(w);
    }

    private void set(String code, String word, String concept) {
        put(new Entry(code, word, concept, Kind.SET, NType.set(NType.A), null, concept));
    }

    private void term(String code, String word, String concept, Kind kind, NType type, String gloss) {
        put(new Entry(code, word, concept, kind, type, null, gloss));
    }

    private void pred(String code, String word, String concept, String gloss, Sig... sigs) {
        put(new Entry(code, word, concept, Kind.PREDICATE, NType.B, List.of(sigs), gloss));
    }

    private void put(Entry e) {
        byWord.put(e.word, e);
        byCode.put(e.code, e);
    }

    private static Sig req(String role, NType t) {
        return new Sig(role, t, true);
    }

    private static Sig opt(String role, NType t) {
        return new Sig(role, t, false);
    }

    private void bootstrapWords() {
        set("1000", "bapa", "狗");
        set("1001", "bape", "猫");
        set("1002", "bapi", "树");
        set("1003", "bapo", "石头");
        set("1004", "bapu", "杯子");
        set("6000", "fapa", "文本");
        set("6001", "fape", "图像");
        set("6002", "fapi", "录音");
        set("6003", "fapo", "消息");
        set("6004", "fapu", "指令");
        set("7000", "vapa", "动物");
        set("7001", "vape", "植物");
        set("7002", "vapi", "哺乳动物");
        set("7003", "vapo", "工具");
        set("7004", "vapu", "车辆");

        term("4002", "kapi", "硬度", Kind.DIMENSION, NType.M, "硬度");
        term("4003", "kapo", "透光率", Kind.DIMENSION, NType.M, "透光率");
        term("4203", "kipo", "体积", Kind.DIMENSION, NType.M, "体积");

        term("A000", "hapa", "参照时刻", Kind.CONTEXT, NType.D, "参照时刻 r");
        term("A001", "hape", "早于 r 的区间", Kind.CONTEXT, NType.E, "参照时刻 r 之前");
        term("A002", "hapi", "晚于 r 的区间", Kind.CONTEXT, NType.E, "参照时刻 r 之后");

        term("A003", "hapo", "秒", Kind.UNIT, NType.U, "秒");
        term("A004", "hapu", "固定日长 86400 秒", Kind.UNIT, NType.U, "日");
        term("A040", "haka", "莫氏硬度标度", Kind.UNIT, NType.U, "莫氏硬度");
        term("A041", "hake", "无量纲比例", Kind.UNIT, NType.U, "比例");
        term("A042", "haki", "升", Kind.UNIT, NType.U, "升");
        term("A043", "hako", "250mL 容量杯", Kind.UNIT, NType.U, "杯(250mL)");

        term("8000", "sapa", "0", Kind.NUMERAL, NType.N, "0");
        term("8001", "sape", "1", Kind.NUMERAL, NType.N, "1");
        term("8002", "sapi", "2", Kind.NUMERAL, NType.N, "2");
        term("8003", "sapo", "3", Kind.NUMERAL, NType.N, "3");
        term("8004", "sapu", "1/2", Kind.NUMERAL, NType.N, "1/2");

        pred("2000", "tapa", "行走", "{kp}行走", req("kp", NType.A), opt("kk", NType.K), opt("kf", NType.A));
        pred("2001", "tape", "奔跑", "{kp}奔跑", req("kp", NType.A), opt("kk", NType.K), opt("kf", NType.A));
        pred("2002", "tapi", "掉落", "{kt}掉落", req("kt", NType.A), opt("kk", NType.K), opt("kf", NType.A));
        pred("2003", "tapo", "制造并产生产品", "{kp}制造{gh}",
                req("kp", NType.A), req("gh", NType.A), opt("kk", NType.K), opt("kh", NType.A), opt("kf", NType.A));
        pred("2004", "tapu", "把主题物交给接收者", "{kp}把{kt}交给{kb}",
                req("kp", NType.A), req("kt", NType.A), req("kb", NType.A), opt("kk", NType.K), opt("kf", NType.A));
        pred("2403", "tupo", "从非开启转为开启", "{kt}从非开启状态转为开启", req("kt", NType.A), opt("kk", NType.K), opt("kf", NType.A));

        pred("3000", "dapa", "清醒", "{kt}是清醒的", req("kt", NType.A));
        pred("3001", "dape", "睡眠", "{kt}在睡觉", req("kt", NType.A));
        pred("3002", "dapi", "饥饿", "{km}感到饥饿", req("km", NType.A));
        pred("3003", "dapo", "处于开启状态", "{kt}处于开启状态", req("kt", NType.A));
        pred("3004", "dapu", "处于关闭状态", "{kt}处于关闭状态", req("kt", NType.A));

        pred("4000", "kapa", "红色", "{kt}是红色的", req("kt", NType.A));
        pred("4001", "kape", "圆形", "{kt}是圆形的", req("kt", NType.A));
        pred("4004", "kapu", "有甜味", "{kt}有甜味", req("kt", NType.A));

        pred("5000", "gapa", "拥有", "{kn}拥有{kl}", req("kn", NType.A), req("kl", NType.A));
        pred("5001", "gape", "部分与整体", "{gp}是{gb}的组成部分", req("gp", NType.A), req("gb", NType.A));
        pred("5002", "gapi", "相邻", "{gm}与{gn}相邻", req("gm", NType.A), req("gn", NType.A));
        pred("5003", "gapo", "原因事件导致结果事件", "{ks}导致{kz}", req("ks", NType.K), req("kz", NType.K));
        pred("5004", "gapu", "亲属关系", "{gm}与{gn}存在亲属关系", req("gm", NType.A), req("gn", NType.A));

        pred("9000", "zapa", "位于参照物上方", "{gt}位于{gd}上方", req("gt", NType.A), req("gd", NType.A));
        pred("9001", "zape", "位于参照物下方", "{gt}位于{gd}下方", req("gt", NType.A), req("gd", NType.A));
        pred("9002", "zapi", "位于参照物内部", "{gt}位于{gd}内部", req("gt", NType.A), req("gd", NType.A));
        pred("9003", "zapo", "位于参照物外部", "{gt}位于{gd}外部", req("gt", NType.A), req("gd", NType.A));
        pred("9004", "zapu", "位于参照物指定区域中心", "{gt}位于{gd}指定区域的中心", req("gt", NType.A), req("gd", NType.A));

        pred("2440", "tuka", "以另一信息对象为内容", "{gz}以{gf}为内容", req("gz", NType.A), req("gf", NType.A));
        pred("2441", "tuke", "对象的外部名称字符串", "{gz}的名称是{gf}", req("gz", NType.A), req("gf", NType.S));
        pred("2442", "tuki", "测量关系", "{gz}的{gg}为 {gv} {gs}",
                req("gz", NType.A), req("gg", NType.M), req("gv", NType.N), req("gs", NType.U));
        pred("2443", "tuko", "关联 URL", "{gz}关联地址{gf}", req("gz", NType.A), req("gf", NType.O));
        pred("2444", "tuku", "命题内容", "{gz}表达命题：{gf}", req("gz", NType.A), req("gf", NType.B));
    }

    private void bootstrapNames() {
        name("paka", NType.A, "人物甲");
        name("piki", NType.A, "人物乙");
        name("puka", NType.A, "杯子个体");
        name("paku", NType.A, "狗个体");
        name("paki", NType.A, "猫个体");
        name("pako", NType.A, "石头个体");
        name("pika", NType.A, "杯子部件");
        name("piko", NType.A, "消息对象");
        name("pike", NType.A, "文本对象");
        name("puku", NType.A, "一份液体");
        name("peta", NType.K, "一次掉落事件");
        name("pete", NType.K, "一次开启事件");
    }

    private void name(String word, NType type, String concept) {
        names.put(word, new Name(word, type, concept));
    }

    public List<Entry> entries(Kind kind) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byWord.values()) {
            if (e.kind == kind) {
                out.add(e);
            }
        }
        return out;
    }

    private static final String CODE_CHARS = "0123456789ABCD";
    private static final String CONS = "pbtdkgfvszhmnl";
    private static final String VOW = "aeiou";

    public static String codeToWord(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (c.isEmpty() || c.length() % 2 != 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < c.length(); k += 2) {
            int hi = CODE_CHARS.indexOf(c.charAt(k));
            char lo = c.charAt(k + 1);
            if (hi < 0 || lo < '0' || lo > '4') {
                return null;
            }
            sb.append(CONS.charAt(hi)).append(VOW.charAt(lo - '0'));
        }
        return sb.toString();
    }

    public static String wordToCode(String word) {
        if (word == null) {
            return null;
        }
        String w = word.trim().toLowerCase(java.util.Locale.ROOT);
        if (w.length() < 2 || w.length() % 2 != 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < w.length(); k += 2) {
            int ci = CONS.indexOf(w.charAt(k));
            int vi = VOW.indexOf(w.charAt(k + 1));
            if (ci < 0 || vi < 0) {
                return null;
            }
            sb.append(CODE_CHARS.charAt(ci)).append(vi);
        }
        return sb.toString();
    }
}
