package nlang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SentenceBank {

    public record Pair(int no, String zh, String n) {
    }

    private static final List<Pair> ALL;
    private static final Map<String, String> ZH_TO_N = new HashMap<>();
    private static final Map<String, String> N_TO_ZH = new HashMap<>();

    static {
        List<Pair> list = new ArrayList<>();
        add(list, "那只狗属于狗的集合。", "m paku ∈ n bapa.");
        add(list, "那只猫属于猫的集合。", "m paki ∈ n bape.");
        add(list, "那个杯子属于杯子的集合。", "m puka ∈ n bapu.");
        add(list, "所有狗都是哺乳动物。", "n bapa ⊆ n vapi.");
        add(list, "所有哺乳动物都是动物。", "n vapi ⊆ n vapa.");
        add(list, "那个杯子是红色的。", "kapa(kt m puka).");
        add(list, "那块石头是圆形的。", "kape(kt m pako).");
        add(list, "那份液体有甜味。", "kapu(kt m puku).");
        add(list, "人物甲醒着。", "dapa(kt m paka).");
        add(list, "那只猫在睡觉。", "dape(kt m paki).");
        add(list, "那只狗饿了。", "dapi(km m paku).");
        add(list, "人物甲正在行走。", "tapa(kp m paka).");
        add(list, "那只狗正在奔跑。", "tape(kp m paku).");
        add(list, "那个杯子正在掉落。", "tapi(kt m puka).");
        add(list, "人物甲正在制造那个杯子。", "tapo(kp m paka, gh m puka).");
        add(list, "人物甲正在把那个杯子交给人物乙。",
                "tapu(kp m paka, kt m puka, kb m piki).");
        add(list, "人物甲拥有那个杯子。", "gapa(kn m paka, kl m puka).");
        add(list, "对象 pika 是那个杯子的一部分。", "gape(gp m pika, gb m puka).");
        add(list, "那只猫与那只狗相邻。", "gapi(gm m paki, gn m paku).");
        add(list, "人物甲与人物乙有亲属关系。", "gapu(gm m paka, gn m piki).");
        add(list, "那只猫位于那块石头上方。", "zapa(gt m paki, gd m pako).");
        add(list, "那块石头位于那个杯子内部。", "zapi(gt m pako, gd m puka).");
        add(list, "那只猫位于那个杯子外部。", "zapo(gt m paki, gd m puka).");
        add(list, "那只狗没有在睡觉。", "¬dape(kt m paku).");
        add(list, "那只狗和那只猫都在睡觉。", "(dape(kt m paku) ∧ dape(kt m paki)).");
        add(list, "那只狗或那只猫在睡觉，也可能两只都在睡觉。",
                "(dape(kt m paku) ∨ dape(kt m paki)).");
        add(list, "如果那个杯子是红色的，那么人物甲拥有它。",
                "(kapa(kt m puka) → gapa(kn m paka, kl m puka)).");
        add(list, "至少有一只狗处于饥饿状态。", "∃x:A ((x ∈ n bapa) ∧ dapi(km x)).");
        add(list, "所讨论范围内的所有狗都在睡觉。", "∀x:A ((x ∈ n bapa) → dape(kt x)).");
        add(list, "所讨论范围内没有猫在睡觉。", "¬∃x:A ((x ∈ n bape) ∧ dape(kt x)).");
        add(list, "所讨论范围内恰好有一个红色杯子。", "∃!x:A ((x ∈ n bapu) ∧ kapa(kt x)).");
        add(list, "所讨论范围内共有三个红色杯子。",
                "z({x:A : ((x ∈ n bapu) ∧ kapa(kt x))}) = 3.");
        add(list, "人物甲正在把唯一的那个红色杯子交给人物乙。",
                "tapu(kp m paka, kt s(x:A, ((x ∈ n bapu) ∧ kapa(kt x))), kb m piki).");
        add(list, "那块石头的莫氏硬度为 7。", "tuki(gz m pako, gg n kapi, gv 7, gs n haka).");
        add(list, "那个杯子的透光率为 0.9。", "tuki(gz m puka, gg n kapo, gv 0.9, gs n hake).");
        add(list, "那份液体的体积为半个约定容量杯，即 125 毫升。",
                "tuki(gz m puku, gg n kipo, gv 1/2, gs n hako).");
        add(list, "那个杯子的透光率至少为 0.9。",
                "∃x:N (tuki(gz m puka, gg n kapo, gv x, gs n hake) ∧ (x ≥ 0.9)).");
        add(list, "在参照时刻，人物甲正在行走。", "h(n hapa, tapa(kp m paka)).");
        add(list, "人物乙在参照时刻之前的某个时刻跑过步。", "hh(n hape, tape(kp m piki)).");
        add(list, "在参照时刻之后的某个时刻，人物甲将处于制造那个杯子的过程中。",
                "hh(n hapi, tapo(kp m paka, gh m puka)).");
        add(list, "北京时间 2026 年 10 月 29 日上午 9 点至 10 点的整个区间内，那只狗始终在睡觉；不含 10 点整。",
                "hl(!h[!t\"2026-10-29T09:00:00+08:00\", !t\"2026-10-29T10:00:00+08:00\"], "
                        + "dape(kt m paku)).");
        add(list, "那个杯子有一次持续三秒的掉落事件，整次事件都发生在参照时刻之前。",
                "∃x:K (hh(n hape, tapi(kt m puka, kk x)) ∧ (hn(x, n hape) ∧ hd(x, 3, n hapo))).");
        add(list, "人物甲在过去至少有过两次不同的奔跑事件。",
                "∃x:K (∃y:K ((x ≠ y) ∧ (hh(n hape, tape(kp m paka, kk x)) ∧ "
                        + "hh(n hape, tape(kp m paka, kk y))))).");
        add(list, "对象 piko 是一条消息。", "m piko ∈ n fapo.");
        add(list, "对象 pike 是一份文本。", "m pike ∈ n fapa.");
        add(list, "那条消息以那份文本为内容。", "tuka(gz m piko, gf m pike).");
        add(list, "人物甲正在把那条消息交给人物乙。", "tapu(kp m paka, kt m piko, kb m piki).");
        add(list, "人物甲的一个外部名称是“Alice”。", "tuke(gz m paka, gf \"Alice\").");
        add(list, "那条消息关联的 URL 是所示地址。",
                "tuko(gz m piko, gf !l\"https://example.org/message/1\").");
        add(list, "那条消息表达的命题是“那只狗在睡觉”。",
                "tuku(gz m piko, gf bb(dape(kt m paku))).");

        add(list, "那只狗不是猫。", "m paku ∉ n bape.");
        add(list, "所有狗都是动物，但并非所有动物都是狗。", "n bapa ⊂ n vapa.");
        add(list, "所有车辆都是工具。", "n vapu ⊆ n vapo.");
        add(list, "杯子的总数为 3。", "z(n bapu) = 3.");
        add(list, "恰好有一个圆杯子。", "∃!x:A ((x ∈ n bapu) ∧ kape(kt x)).");
        add(list, "至少有一个杯子不是红色的。", "∃x:A ((x ∈ n bapu) ∧ ¬kapa(kt x)).");
        add(list, "所有杯子都不处于饥饿状态。", "∀x:A ((x ∈ n bapu) → ¬dapi(km x)).");
        add(list, "那次掉落事件导致了那次开启事件。", "gapo(ks m peta, kz m pete).");
        add(list, "参照时刻正落在那次掉落事件之内。", "ht(m peta, n hapa).");
        add(list, "那次掉落事件整体发生在参照时刻之前。", "hn(m peta, n hape).");
        add(list, "那次掉落事件持续 3 秒。", "hd(m peta, 3, n hapo).");
        add(list, "将来会有一次人物甲行走的事件。",
                "∃x:K (hh(n hapi, tapa(kk x, kp m paka))).");
        add(list, "在 2026 年 10 月 29 日 12 点整，人物甲正在行走。",
                "h(!t\"2026-10-29T12:00:00+08:00\", tapa(kp m paka)).");
        add(list, "2026 年 10 月 29 日上午 9 点到 10 点之间，人物乙跑过步。",
                "hh(!h[!t\"2026-10-29T09:00:00+08:00\", "
                        + "!t\"2026-10-29T10:00:00+08:00\"], tape(kp m piki)).");
        add(list, "人物甲没有醒着。", "¬dapa(kt m paka).");
        add(list, "人物甲醒着，而且不饿。", "(dapa(kt m paka) ∧ ¬dapi(km m paka)).");
        add(list, "那块石头在那个杯子外面。", "zapo(gt m pako, gd m puka).");
        add(list, "那只猫在杯子的正中心。", "zapu(gt m paki, gd m puka).");
        add(list, "人物甲制造了一条消息。", "tapo(kp m paka, gh m piko).");
        add(list, "那份文本以那条消息为内容。", "tuka(gz m pike, gf m piko).");
        add(list, "人物甲关联的网址是所示地址。",
                "tuko(gz m paka, gf !l\"https://example.org/p/1\").");
        add(list, "人物乙的一个外部名称是“Bob”。", "tuke(gz m piki, gf \"Bob\").");
        add(list, "那条消息表达的是“狗没有在睡觉”。",
                "tuku(gz m piko, gf bb(¬dape(kt m paku))).");
        add(list, "那条消息也是一份文本。", "m piko ∈ n fapa.");
        add(list, "至少存在一个红色杯子。", "∃x:A (x ∈ n bapu ∧ kapa(kt x)).");

        ALL = List.copyOf(list);
        for (Pair p : ALL) {
            ZH_TO_N.put(norm(p.zh), p.n);
            N_TO_ZH.put(norm(p.n), p.zh);
        }
    }

    private static void add(List<Pair> list, String zh, String n) {
        list.add(new Pair(list.size() + 1, zh, n));
    }

    public static List<Pair> all() {
        return ALL;
    }

    public static String zhToN(String zh) {
        return ZH_TO_N.get(norm(zh));
    }

    public static String nToZh(String n) {
        return N_TO_ZH.get(norm(n));
    }

    public static Pair closest(String zh) {
        String t = norm(zh);
        if (t.length() < 2) {
            return null;
        }
        for (Pair p : ALL) {
            String c = norm(p.zh);
            if (c.contains(t) || t.contains(c)) {
                return p;
            }
        }
        return null;
    }

    public static String norm(String s) {
        String t = s.strip()
                .replace('“', '"').replace('”', '"')
                .replace('「', '"').replace('」', '"')
                .replace('‘', '\'').replace('’', '\'');
        t = t.replaceAll("\\s+", "");
        t = t.replaceAll("[。．.]+$", "");
        t = t.replaceAll("[？?！!]+$", "");
        return t.toLowerCase(Locale.ROOT);
    }

    public static String listing() {
        StringBuilder sb = new StringBuilder();
        sb.append("── 例句对照表（共 ").append(ALL.size()).append(" 组）");
        while (sb.length() < 70) {
            sb.append('─');
        }
        sb.append('\n');
        for (Pair p : ALL) {
            sb.append(String.format("%3d. ", p.no)).append(p.zh).append('\n');
            sb.append("     ").append(p.n).append('\n');
        }
        sb.append("输入其中任意一句（中文或 N 语言）即可互查。");
        return sb.toString();
    }

    private SentenceBank() {
    }
}
