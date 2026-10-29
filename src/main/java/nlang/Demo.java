package nlang;

/**
 * 测试程序：跑一批内置例句，把 N 语言翻译成中文打印出来；
 * 再跑一批「应当被拒绝」的输入，确认类型系统挡住了它们。
 *
 * <p>这份列表同时也是回归测试：改动词法、文法或签名表后先看这里的通过数。</p>
 */
public final class Demo {

    private static final String[] EXAMPLES = {
            "tapa(kp m paka).",
            "dape(kt m paku).",
            "dapi(km m paku).",
            "kapa(kt m puka).",
            "gapa(kn m paka, kl m puka).",
            "gape(gp m pika, gb m puka).",
            "gapi(gm m paki, gn m paku).",
            "zapa(gt m paki, gd m pako).",
            "tapo(kp m paka, gh m puka).",
            "tapu(kp m paka, kt m puka, kb m piki).",
            "tuke(gz m paka, gf \"Alice\").",
            "tuki(gz m puka, gg n kapo, gv 0.9, gs n hake).",
            "¬(dape(kt m paku)).",
            "(dape(kt m paku) ∧ kapa(kt m puka)).",
            "(dapi(km m paku) → dape(kt m paku)).",
            "∀x:A ( (x ∈ n bapa) → dape(kt x) ).",
            "∃x:A ( (x ∈ n bapa) ∧ dapi(km x) ).",
            "m paku ∈ n bapa.",
            "n bapa ⊆ n vapi.",
            "z({x:A : ((x ∈ n bapu) ∧ kapa(kt x))}) = 3.",
            "h(n hapa, tapa(kp m paka)).",
            "hh(n hape, tape(kp m piki)).",
            "∃x:K ( (hh(n hape, tapi(kk x, kt m puka)) ∧ hd(x, 3, n hapo)) ).",
            "tuku(gz m piko, gf bb(dape(kt m paku))).",
            "C := tapa(kp m paka).\nJ := dape(kt m piki).\n(C ∧ J).",
    };

    private static final String[][] REJECT = {
            {"tapu(kp m paka, kt m puka).", "缺少必需接收者 kb"},
            {"tapa(kp m paka, kp m piki).", "同一角色重复"},
            {"tapa(gv 3).", "未许可角色且缺少 kp"},
            {"tapu(kp m paka, kt n bapu, kb m piki).", "kt 要求 A，却给了集合 H<A>"},
            {"n bapa ∈ n vapi.", "集合不能当作 A 成员"},
            {"kapo(kt m puka).", "kapo 是维度词项，不是谓词"},
            {"gapo(ks bb(tapi(kt m puka)), kz m pete).", "原因必须是 K，不是 B"},
            {"tuke(gz m paka, gf m piki).", "名称参数要求字符串 S"},
            {"kapa(kt x).", "自由变量未绑定"},
            {"h(!d\"2026-10-26\", ⊤).", "日期 L 不是时刻 D"},
            {"∀x ( x ∈ n bapa ).", "绑定缺少显式类型"},
            {"tapa(kp n sapo).", "数字读音条目不能当核心词项"},
    };

    public static void run() {
        int pass = 0;
        int fail = 0;

        line("N 语言 → 中文   例句");
        int no = 0;
        for (String src : EXAMPLES) {
            no++;
            Main.Result r = Main.compile(src);
            if (r.hasError()) {
                fail++;
                System.out.println(pad(no) + "✗ " + flat(src));
                printDiags(src, r);
            } else {
                pass++;
                System.out.println(pad(no) + flat(src));
                String cn = new Renderer(r.lexicon).gloss(r.program);
                for (String l : cn.split("\n")) {
                    System.out.println("     " + l);
                }
            }
        }

        System.out.println();
        line("应当被拒绝的输入");
        no = 0;
        for (String[] rc : REJECT) {
            no++;
            Main.Result r = Main.compile(rc[0]);
            System.out.println(pad(no) + flat(rc[0]) + "   —— " + rc[1]);
            if (!r.hasError()) {
                fail++;
                System.out.println("     ✗ 没有报错，类型系统漏了这条");
                continue;
            }
            pass++;
            for (Diagnostic d : r.diags) {
                if (d.isError()) {
                    System.out.println("     ↳ " + d.catZh() + " [" + d.code + "] " + d.msg);
                    break;
                }
            }
        }

        System.out.println();
        line("统计");
        System.out.println("例句 " + EXAMPLES.length + " 条，反例 " + REJECT.length + " 条");
        System.out.println("通过 " + pass + " ／ 失败 " + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void printDiags(String src, Main.Result r) {
        for (Diagnostic d : r.diags) {
            if (!d.isError()) {
                continue;
            }
            int[] lc = Lexer.lineCol(src, d.pos);
            System.out.println("     ✗ " + lc[0] + ":" + lc[1] + " " + d.catZh()
                    + " [" + d.code + "] " + d.msg);
            break;
        }
    }

    private static String pad(int n) {
        return String.format("%3d. ", n);
    }

    /** 多行句子压成一行，方便对照。 */
    private static String flat(String s) {
        return s.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private static void line(String title) {
        StringBuilder sb = new StringBuilder("── ").append(title).append(' ');
        while (sb.length() < 74) {
            sb.append('─');
        }
        System.out.println(sb);
    }

    private Demo() {
    }
}
