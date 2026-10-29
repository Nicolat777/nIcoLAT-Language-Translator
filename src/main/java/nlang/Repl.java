package nlang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class Repl {

    private static String banner() {
        return """
            ==============================================
                欢迎来到N语言翻译器！
            ==============================================
             2004 / tapu       数字 ⇄ 单词
             那只狗饿了。        中文 → N 语言（例句库）
             dapi(km m paku).  N 语言 → 中文
             测试 / test       查看 %d 组例句对照表
             翻译 / translate   逐行翻译任意 N 语言句子
             帮助 / help       全部指令    退出 / exit  离开
            ----------------------------------------------""".formatted(SentenceBank.all().size());
    }

    public static void run() {
        System.out.println(banner());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        boolean bye = false;
        while (!bye) {
            System.out.print("> ");
            System.out.flush();
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                break;
            }
            if (line == null) {
                System.out.println();
                break;
            }
            try {
                bye = handle(line.strip(), in);
            } catch (IOException e) {
                break;
            } catch (RuntimeException e) {
                System.out.println("出错了：" + e.getMessage());
            }
        }
        System.out.println("再见！");
    }

    private static boolean handle(String raw, BufferedReader in) throws IOException {
        if (raw.isEmpty()) {
            return false;
        }
        String low = raw.toLowerCase(Locale.ROOT);

        switch (low) {
            case "退出":
            case "exit":
            case "quit":
            case "q":
                return true;
            case "帮助":
            case "help":
            case "h":
            case "?":
            case "？":
                help();
                return false;
            case "测试":
            case "test":
            case "例句":
            case "列表":
            case "examples":
                System.out.println(SentenceBank.listing());
                return false;
            case "自检":
            case "demo":
                Demo.run();
                return false;
            case "翻译":
            case "translate":
            case "trans":
                translateFromConsole(in);
                return false;
            default:
                break;
        }

        if (low.startsWith("翻译 ") || low.startsWith("translate ") || low.startsWith("trans ")) {
            translateFile(raw.substring(raw.indexOf(' ') + 1).strip());
            return false;
        }

        if (hasChinese(raw)) {
            String n = SentenceBank.zhToN(raw);
            if (n != null) {
                System.out.println(n);
                return false;
            }
        }

        String zh = SentenceBank.nToZh(raw);
        if (zh != null) {
            System.out.println(zh);
            return false;
        }

        String word = Lexicon.codeToWord(raw);
        if (word != null) {
            System.out.println(word);
            return false;
        }
        String code = Lexicon.wordToCode(raw);
        if (code != null) {
            System.out.println(code);
            return false;
        }

        if (looksLikeN(raw)) {
            String out = Main.translateOrReport(readStatement(raw, in));
            System.out.println(out.isEmpty() ? "（没有可翻译的内容）" : out);
            return false;
        }

        if (hasChinese(raw)) {
            SentenceBank.Pair near = SentenceBank.closest(raw);
            System.out.println("例句库里没有这句（共 " + SentenceBank.all().size() + " 句，输 测试 可看全部）。");
            if (near != null) {
                System.out.println("最接近的是第 " + near.no() + " 句：" + near.zh());
                System.out.println("  " + near.n());
            }
            return false;
        }

        System.out.println("null");
        System.out.println("（既不是例句、合法编码，也不是合法词形。输 帮助 看指令。）");
        return false;
    }

    private static String readStatement(String first, BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder(first);
        int guard = 0;
        while (guard++ < 500) {
            String text = sb.toString().stripTrailing();
            int depth = depthOf(text);
            if (depth < 0) {
                break;
            }
            if (depth == 0 && text.endsWith(".")) {
                break;
            }
            System.out.print("  | ");
            System.out.flush();
            String l = in.readLine();
            if (l == null || l.strip().isEmpty()) {
                break;
            }
            sb.append('\n').append(l);
        }
        return sb.toString();
    }

    private static int depthOf(String s) {
        int d = 0;
        boolean inStr = false;
        boolean esc = false;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
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
                d++;
            } else if (c == ')') {
                d--;
            }
        }
        return d;
    }

    private static boolean looksLikeN(String raw) {
        return raw.endsWith(".") || raw.indexOf('(') >= 0 || raw.contains("∈")
                || raw.contains("⊆") || raw.startsWith("¬") || raw.startsWith("∀")
                || raw.startsWith("∃") || raw.startsWith("h(") || raw.startsWith("hh(")
                || raw.startsWith("hl(") || raw.startsWith("hv(");
    }

    private static void translateFromConsole(BufferedReader in) throws IOException {
        System.out.println("请输入 N 语言句子，一行一句，空行结束：");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (true) {
            System.out.print("  | ");
            System.out.flush();
            String l = in.readLine();
            if (l == null || l.strip().isEmpty()) {
                break;
            }
            sb.append(l).append('\n');
            count++;
        }
        if (count == 0) {
            System.out.println("（没有输入，已取消）");
            return;
        }
        String out = Main.translateOrReport(sb.toString());
        System.out.println(out.isEmpty() ? "（没有可输出的内容）" : out);
    }

    private static void translateFile(String arg) {
        String name = arg.replace("\"", "").replace("'", "").strip();
        Path p = Path.of(name);
        if (!Files.isRegularFile(p)) {
            Path alt = Path.of("examples", name);
            if (Files.isRegularFile(alt)) {
                p = alt;
            } else {
                System.out.println("找不到文件：" + name);
                return;
            }
        }
        try {
            String src = Files.readString(p, StandardCharsets.UTF_8);
            System.out.println("── " + p + " ──");
            System.out.println(Main.translateOrReport(src));
        } catch (IOException e) {
            System.out.println("读取失败：" + e.getMessage());
        }
    }

    private static void help() {
        System.out.println("""
                例句库互查（共 %d 组）：
                  输入中文句子     → 对应的 N 语言     例：那只狗饿了。
                  输入 N 语言句子  → 对应的中文       例：dapi(km m paku).

                看例句：
                  测试 / test / 例句    打印全部 %d 组对照表

                查编码：
                  2004 / tapu           数字 ⇄ 单词，查不到输出 null

                翻译任意 N 语言（不限例句库）：
                  翻译 / translate       逐行输入，空行结束
                  翻译 examples/basic.n  翻译整个文件

                其他：
                  自检 / demo           类型检查演示（25 条例句 + 12 条反例）
                  帮助 / help           本说明
                  退出 / exit           离开""".formatted(
                SentenceBank.all().size(), SentenceBank.all().size()));
    }

    private static boolean hasChinese(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.UnicodeScript.of(s.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private Repl() {
    }
}
