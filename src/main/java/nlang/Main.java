package nlang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令行入口。只有三个功能：
 *
 * <pre>
 * nlang demo             测试程序：输出例句与反例，并统计通过数
 * nlang trans &lt;file.n&gt;   把 N 语言翻译成中文（- 表示从标准输入读）
 * nlang code &lt;编码&gt;       编码 → 词形，找不到输出 null
 * nlang word &lt;词形&gt;       词形 → 编码，找不到输出 null
 * </pre>
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            Repl.run();
            return;
        }
        switch (args[0]) {
            case "demo":
            case "自检":
                Demo.run();
                return;
            case "test":
            case "测试":
            case "例句":
                System.out.println(SentenceBank.listing());
                return;
            case "trans":
            case "translate":
            case "翻译":
                translate(args);
                return;
            case "code":
                System.out.println(orNull(Lexicon.codeToWord(firstArg(args))));
                return;
            case "word":
                System.out.println(orNull(Lexicon.wordToCode(firstArg(args))));
                return;
            case "-h":
            case "--help":
            case "help":
                usage();
                return;
            default:
                System.err.println("未知命令：" + args[0]);
                usage();
                System.exit(2);
        }
    }

    // ------------------------------------------------------------------ 翻译

    private static void translate(String[] args) {
        String src;
        try {
            if (args.length >= 2 && !"-".equals(args[1])) {
                Path p = resolve(args[1]);
                if (p == null) {
                    System.err.println("找不到文件：" + args[1]);
                    System.exit(2);
                    return;
                }
                src = Files.readString(p, StandardCharsets.UTF_8);
            } else {
                if (args.length < 2) {
                    System.err.println("（从标准输入读取，输入完按 Ctrl-D）");
                }
                src = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("读取失败：" + e.getMessage());
            System.exit(2);
            return;
        }

        Result r = compile(src);
        if (r.hasError()) {
            report(src, r.diags);
            System.exit(1);
            return;
        }
        String cn = new Renderer(r.lexicon).gloss(r.program);
        if (!cn.isEmpty()) {
            System.out.println(cn);
        }
    }

    /** 一次完整的前端流水线：词法 → 语法 → 静态检查。 */
    public static final class Result {
        public final Lexicon lexicon;
        public final Ast.Program program;
        public final List<Diagnostic> diags;

        Result(Lexicon lexicon, Ast.Program program, List<Diagnostic> diags) {
            this.lexicon = lexicon;
            this.program = program;
            this.diags = diags;
        }

        public boolean hasError() {
            for (Diagnostic d : diags) {
                if (d.isError()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static Result compile(String src) {
        List<Diagnostic> diags = new ArrayList<>();
        Lexicon lex = new Lexicon();
        List<Token> tokens = new Lexer(src, diags).lex();
        Ast.Program program;
        Diagnostic unbalanced = Brackets.check(src);
        if (unbalanced != null) {
            diags.add(unbalanced);
            program = new Ast.Program(new ArrayList<>(), 0);
        } else {
            program = new Parser(tokens, diags).parseProgram();
        }
        new Analyzer(lex, diags).analyze(program);
        return new Result(lex, program, diags);
    }

    // ------------------------------------------------------------------ 工具

    private static String firstArg(String[] args) {
        return args.length >= 2 ? args[1] : null;
    }

    private static String orNull(String s) {
        return s == null ? "null" : s;
    }

    /** 工作目录常常不是项目根，向上找 examples/ 里的同名文件。 */
    private static Path resolve(String arg) {
        Path direct = Path.of(arg);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        String[] dirs = {"examples", "../examples", "../../examples", "../../../examples"};
        String name = direct.getFileName() == null ? arg : direct.getFileName().toString();
        for (String d : dirs) {
            Path p = Path.of(d, name);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    /** 把诊断拼成可打印的文本（交互界面和命令行共用）。 */
    public static String formatDiags(String src, List<Diagnostic> diags) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diags) {
            if (!d.isError()) {
                continue;
            }
            int[] lc = Lexer.lineCol(src, d.pos);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("  ").append(lc[0]).append(':').append(lc[1]).append("  ")
                    .append(d.catZh()).append("  [").append(d.code).append("]  ")
                    .append(d.msg);
        }
        return sb.toString();
    }

    /** 可直接打印的翻译结果：成功给中文，失败给诊断。 */
    public static String translateOrReport(String src) {
        Result r = compile(src);
        if (r.hasError()) {
            return formatDiags(src, r.diags);
        }
        return new Renderer(r.lexicon).gloss(r.program);
    }

    private static void report(String src, List<Diagnostic> diags) {
        String text = formatDiags(src, diags);
        if (!text.isEmpty()) {
            System.err.println(text);
        }
    }

    private static void usage() {
        System.out.println("""
                N 语言翻译器

                不带参数启动就是交互模式（推荐）：
                  ./run.sh

                也可以直接给参数：
                  nlang test                输出测试内容（例句 + 反例）
                  nlang trans <file.n>      翻译一个 N 语言文件
                  nlang code  <编码>         编码 → 词形，找不到输出 null
                  nlang word  <词形>         词形 → 编码，找不到输出 null
                """);
    }

    private Main() {
    }
}
