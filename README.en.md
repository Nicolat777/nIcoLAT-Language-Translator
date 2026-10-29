**English** · [中文](README.md)

# N Language (V2) Translator

A parser, static checker and Chinese renderer for **N Language**, built from the
language's own rule book (`newLanguage_V2.pdf`).

Pure Java, zero third-party dependencies — `javac` compiles it, `./run.sh` runs it,
and it ships with an interactive shell.

N Language is a deliberately engineered formal language with six stacked layers:

```
CODE → WORD → OPERATOR → FORM → EXPRESSION → SEMANTICS
```

This project turns those six layers into a working compiler front end, then adds a
rendering layer that reads a program back as Chinese.

> The output language is Chinese. The interactive shell accepts **both Chinese and
> English** commands.

---

## Features

| Feature | What it does |
|---|---|
| **Sentence bank** | 75 curated Chinese ⇄ N Language pairs, looked up in both directions |
| **N Language → Chinese** | Any valid sentence goes through the real parser and gets rendered |
| **Codes ⇄ words** | `2004 ⇄ tapu`; malformed input yields `null` |
| **Static checking** | Lexing, parsing, name resolution, binding, role signatures, types — 10 diagnostic categories with line/column |
| **Interactive shell** | A menu on startup, no flags to remember |

---

## Quick start

Requires **JDK 17+** (developed on OpenJDK 23). No Maven, no network needed.

```sh
git clone <your-repo-url>
cd N语言
./run.sh
```

```
==============================================
    欢迎来到N语言翻译器！
==============================================
 2004 / tapu       数字 ⇄ 单词
 那只狗饿了。        中文 → N 语言（例句库）
 dapi(km m paku).  N 语言 → 中文
 测试 / test       查看 75 组例句对照表
 翻译 / translate   逐行翻译任意 N 语言句子
 帮助 / help       全部指令    退出 / exit  离开
----------------------------------------------
> 
```

### In IntelliJ IDEA

1. `File → Open` and pick the **project root** (the folder containing `pom.xml`)
2. `Settings → Editor → File Encodings` — set all three to **UTF-8**
   (the sources contain `∀ ∃ ∧ ∨ ↔ ⊤ ⊥`; GBK will break the build)
3. Run `nlang.Main` **without arguments** to enter the interactive shell

---

## Interactive commands

| Input | Result |
|---|---|
| `那只狗饿了。` | `dapi(km m paku).` (Chinese → N Language) |
| `dapi(km m paku).` | `那只狗饿了。` (N Language → Chinese) |
| `2004` / `tapu` | `tapu` / `2004` |
| `测试` · `test` · `例句` | print all 75 pairs |
| `翻译` · `translate` | read N Language line by line, blank line ends input |
| `翻译 examples/basic.n` | translate a whole file |
| `自检` · `demo` | type-checker demo (25 sentences + 12 rejections) |
| `帮助` · `help` | command list |
| `退出` · `exit` | quit |

Multi-line sentences can be pasted as a block: the shell keeps reading (`|` prompt)
until the parens balance and the statement ends with a period.

---

## Command line

No arguments starts the interactive shell; with arguments it runs one task:

```sh
./run.sh                          # interactive shell
./run.sh test                     # print the sentence bank
./run.sh trans examples/basic.n   # translate a file
./run.sh trans -                  # read from stdin
./run.sh code 42C4B1             # → kinume
./run.sh word kinume             # → 42C4B1
```

On failure `trans` prints diagnostics to stderr and exits with code `1`.

---

## Sentence bank

75 pairs, every N Language side verified by the checker. Coverage: set relations,
boolean properties, states, actions, space, connectives, quantification, counting,
iota, measurement, temporal scope, events and causation, information objects.

Matching rules — **this is a lookup table, not a translator**:

- whitespace removed, trailing `。 . ？ !` dropped, curly/straight quotes unified,
  Latin case ignored
- whole-sentence match only; a paraphrase misses
- on a miss it suggests the closest entry

Add your own in `src/main/java/nlang/SentenceBank.java`:

```java
add(list, "我的中文句子。", "tapa(kp m paka).");
```

---

## Project layout

```
N语言/
├── run.sh                      one-command compile + run, no Maven
├── pom.xml                     Maven metadata (no dependencies)
├── newLanguage_V2.pdf          the language specification
├── docs/usage.md               user guide (commands, adding words, diagnostics)
├── examples/
│   ├── basic.n                 five starter sentences
│   ├── s1.n / s2.n             two long nested sentences (quantifiers, events, time)
│   ├── a3_time.n               measurement and time
│   ├── input.n                 scratch pad
│   └── reject.n                intentionally invalid, shows the diagnostics
└── src/main/java/nlang/
    ├── Main.java               entry point and pipeline
    ├── Repl.java               interactive shell
    ├── Brackets.java           paren-balance pre-check
    ├── Lexer.java              lexer: CV word forms, consonant-only operators, literals
    ├── Parser.java             recursive descent (the BNF in spec §4.2)
    ├── Ast.java                syntax tree
    ├── Analyzer.java           dictionary / binding / signature / type checking
    ├── NType.java              types and H<T>
    ├── Lexicon.java            dictionary, name table, code ↔ word conversion
    ├── SentenceBank.java       75 sentence pairs
    ├── Renderer.java           N Language → Chinese
    ├── Literals.java           rationals, date/time/URL/string validation
    ├── Diagnostic.java         the 10 diagnostic categories
    ├── Demo.java               self-check and example walkthrough
    └── Token.java              tokens
```

Roughly 3.5k lines of Java across 15 files.

---

## Scope

**Implemented**

- Lexing: longest-token rule, CV word forms, consonant-only operators, six literal
  kinds with domain validation
- Parsing: the full BNF — `s(...)`, `z(...)`, `{x:T : F}`, `bb(...)`, `hh/hl/hv`,
  `hb…hd`
- Semantics: dictionary and name table kept apart, role signatures, variable binding,
  type matching
- Output: Chinese rendering
- Tools: code conversion, sentence bank, paren pre-check, multi-line input

**Deliberately out of scope**

- Evaluation: models, worlds, reference time `r`, uniqueness for `s`, finiteness
  for `z`, event time ranges. The spec's `IOTA_EMPTY` / `IOTA_MULTIPLE` /
  `UNDECIDABLE` exist as enum values but nothing produces them yet.
- Automatic Chinese → N Language translation (only whole-sentence lookup)
- An external dictionary (it is hard-coded in `Lexicon`; the spec asks for
  versioning and migration records)

---

## Design notes

**Why hand-written recursive descent instead of ANTLR.** The token set is
"shape-separable": only vowels make a word, consonant-only strings are always
operators, and the uppercase alphabets for types (`A B N D E K M U S L O H`) and
proposition names (`C J Q R W X Y`) are disjoint. The lexer needs no backtracking
and fits in ~60 lines. Diagnostics also have to land in the spec's ten categories,
which a generated parser's messages would only need wrapping anyway.

**Why the sentence bank matches whole sentences.** Chinese and N Language are not
isomorphic — one Chinese sentence can map to several valid N programs (word order,
quantification) and vice versa. Real Chinese → N translation needs a translation
policy and a template engine; that is out of scope. Better to say "no such
sentence" than to guess.

**Why check parens first.** In recursive descent a single missing `)` pushes the
error far away from its cause. A pre-check makes "1 missing `)`" a first-class
diagnostic.

---

## Gotchas found in the spec

1. **`㲗` is a missing glyph** — the PDF renders "logical equivalence" as `㲗`;
   it should be `↔`. The lexer accepts `↔ ≡ 㲗` and normalises to `↔`.
2. **`62 ﬁ` is a ligature** — it should be `fi`.
3. **Codes and numbers are different layers** — `1004` has nothing to do with
   `n bapu`; `8000–8004` are pronunciation entries only and must be rejected in
   term position.
4. **`n` / `m` are not casts** — `n bapu ∈ n bapa` must be rejected.
5. **`H<B>` is switched off**, and neither `∃!` nor `s` may bind a `B`.
6. **Precedence** — `¬` binds tightest and is right-associative; `→ ↔` are loosest
   and left-associative.
7. **Period vs decimal point** — `3.` is "number + end of statement", `3.5` is a
   decimal.
8. **Variables carry digits** — `x0`, `x12` are single tokens, not `x` + `0`.
9. **Quantifiers bind only inside their own parens** — so when `∃y:K` mentions `x`,
   the `x` quantifier must enclose it.
10. **"absent / unreported / unknown / undefined" are four different things** — an
    evaluator must model them separately.

---

## Verification

```sh
./run.sh test    # the 75-pair table
./run.sh demo    # 25 sentences + 12 rejections → 37 pass, 0 fail
```

Also: all 75 pairs compile individually, and `javac -Xlint:all --release 17`
reports no warnings.

---

## License

Not specified yet. If you publish this, consider adding a `LICENSE`
(MIT or Apache-2.0 are common choices).
