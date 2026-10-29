# 使用手册

## 一、启动

```sh
./run.sh
```

```
==============================================
    欢迎来到N语言翻译器！
==============================================
 2004 / tapu       数字 ⇄ 单词
 那只狗饿了。        中文 → N 语言（例句库）
 dapi(km m paku).  N 语言 → 中文
 测试 / test       查看 例句对照表
 翻译 / translate   逐行翻译任意 N 语言句子
 帮助 / help       全部指令    退出 / exit  离开
----------------------------------------------
> 
```

---

## 二、例句库：中文 ⇄ N 语言（核心功能）

库里是 75 组对照句子（来源：规范书附录 A + 补充例句），
**输入中文出 N 语言，输入 N 语言出中文**：

```
> 那只狗饿了。
dapi(km m paku).

> dapi(km m paku).
那只狗饿了。

> 人物甲正在把那个杯子交给人物乙。
tapu(kp m paka, kt m puka, kb m piki).

> 在参照时刻，人物甲正在行走。
h(n hapa, tapa(kp m paka)).
```

看全部 75 组：

```
> 测试
── 例句对照表（共 75 组）──────────────
  1. 那只狗属于狗的集合。
     m paku ∈ n bapa.
  2. 那只猫属于猫的集合。
     m paki ∈ n bape.
  ...
```

### 匹配规则：整句匹配，不是语义理解

中文侧会先做**归一化**再比对：

- 去掉所有空格、换行
- 去掉句末的 `。` `.` `？` `!`
- 中文引号 `“ ”` 与英文 `" "` 视为相同
- 英文部分忽略大小写

所以下面这些都是同一句：

```
那只狗饿了。 / 那只狗饿了 / 那只狗饿了！
```

但**换个说法就查不到**，这是句子库不是翻译器：

```
> 那只狗很开心。
例句库里没有这句（输 测试 可看全部）。
```

如果句子里包含库里某句，会给出"最接近的一句"提示：

```
> 人物甲
例句库里没有这句（输 测试 可看全部）。
最接近的是第 12 句：人物甲正在行走。
  tapa(kp m paka).
```

### 自己加例句

打开 [SentenceBank.java](../src/main/java/nlang/SentenceBank.java)，照着写一行：

```java
add(list, "我的中文句子。", "tapa(kp m paka).");
```

N 语言那条必须是能通过检查的合法句子，否则查出来也用不了。

---

## 三、查编码

```
> 2004
tapu

> tapu
2004

> 9999
null
```

先当编码试，再当词形试；都不是就输出 `null`。

---

## 四、翻译任意 N 语言句子

例句库之外的句子会走真正的解析器（词法 → 语法 → 类型检查 → 中文）：

```
> ∀x:A ( (x ∈ n bapa) → dape(kt x) ).
对所有 x:A，（如果（x属于所有狗），那么（x在睡觉））。
```

逐行输入多句：

```
> 翻译
请输入 N 语言句子，一行一句，空行结束：
  | tapa(kp m paka).
  | dape(kt m paku).
  |
人物甲行走。
狗个体在睡觉。
```

多行句子也能整段粘贴：括号没闭合或还没写句点，程序会继续读（提示 `| `），
直到这条语句完整为止。

也可以翻译整个文件：`翻译 examples/basic.n`。

括号错位时会给准确提示，而不是一串莫名其妙的语法错误：

```
23:3  语法错误  [SYN_UNCLOSED_PAREN]  括号没有闭合：从第 1 行开始的表达式还缺 1 个「)」
```

---

## 五、写句子的规则

`词形(角色 词项, 角色 词项, …).` —— 结尾句点不能少。

- `n` 加词形 = 集合（`n bapu` 所有杯子），`m` 加词形 = 个体（`m puka` 杯子个体）
- 角色顺序随意，`tapu(kb m piki, kt m puka, kp m paka).` 和正序等价
- 角色只能从这 24 个里选：`kp kt kb kk kf kh km kn kl ks kz gp gb gt gd gm gn gh gz gf gg gv gs`

可用的专名：

```
m paka 人物甲   m piki 人物乙   m puka 杯子     m paku 狗
m paki 猫       m pako 石头     m pika 杯子部件  m piko 消息
m pike 文本     m puku 液体     m peta 掉落事件  m pete 开启事件
```

常用句型：

```
tapa(kp m paka).                          人物甲行走。
dape(kt m paku).                          狗个体在睡觉。
kapa(kt m puka).                          杯子个体是红色的。
gapa(kn m paka, kl m puka).               人物甲拥有杯子个体。
tapu(kp m paka, kt m puka, kb m piki).    人物甲把杯子个体交给人物乙。
zapa(gt m paki, gd m pako).               猫个体位于石头个体上方。
tuki(gz m puka, gg n kapo, gv 0.9, gs n hake).   杯子个体的透光率为 9/10 比例。
¬(dape(kt m paku)).                       并非（狗个体在睡觉）。
∀x:A ( (x ∈ n bapa) → dape(kt x) ).       所有狗都在睡觉。
∃x:A ( (x ∈ n bapa) ∧ dapi(km x) ).       存在一只饥饿的狗。
h(n hapa, tapa(kp m paka)).               在参照时刻，人物甲行走。
```

---

## 六、不开交互模式时（给脚本用）

```sh
./run.sh test                     # 打印 例句对照表
./run.sh trans examples/basic.n   # 翻译一个文件
./run.sh trans -                  # 从标准输入读
./run.sh code 2004                # tapu
./run.sh word kinume              # 42C4B1
```

---

## 七、加词

打开 [Lexicon.java](../src/main/java/nlang/Lexicon.java)：

```java
// bootstrapWords() 里
set("1005", "bapu", "碗");
pred("2005", "tapa", "洗碗", "{kp}洗{kt}",
        req("kp", NType.A), req("kt", NType.A), opt("kk", NType.K));

// bootstrapNames() 里
name("pita", NType.A, "碗个体");
```

`req` 必需角色、`opt` 可选角色；模板里的 `{角色}` 会被替换成中文。
加完往 `Demo.EXAMPLES` 和 `SentenceBank` 各补一条，以后改代码有回归保护。

---

## 八、报错怎么读

格式：`行:列  类别  [诊断码]  说明`

| 诊断码 | 意思 | 怎么改 |
|---|---|---|
| `MISSING_ROLE` | 缺必需角色 | 补上该角色 |
| `DUPLICATE_ROLE` | 角色重复 | 每个角色最多 1 次 |
| `UNKNOWN_ROLE` | 签名不允许这个角色 | 查谓词的签名 |
| `ROLE_TYPE_MISMATCH` | 角色类型不符 | 如 `kt` 要个体 `A`，别给 `n bapu` |
| `NOT_A_PREDICATE` | 名词当谓词用 | `kapo(...)` 不行 |
| `UNBOUND_VARIABLE` | 自由变量 | 变量只在 `∀x:A ( … )` 等括号内有效 |
| `MEMBER_TYPE_MISMATCH` | 集合当成员 | 个体用 `m …` |
| `UNREGISTERED_WORD` | 词形未注册 | 加进 `Lexicon` |
| `LITERAL_*` | 字面量不合法 | 日期/时刻/URL/分数 |
| `SYN_UNEXPECTED` | 语法错 | 看"期待 X，实际 Y" |
