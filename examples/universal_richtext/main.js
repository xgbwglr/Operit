"use strict";

const DEFAULT_ENABLED = true;
const SETTINGS_FILE = "settings.json";
const TOGGLE_ID = "universal_richtext";
const MENU_HOOK_ID = "universal_richtext_menu";
const PROMPT_HOOK_ID = "universal_richtext_system_prompt";
const PROMPT_MARKER = "[OPERIT_UNIVERSAL_RICHTEXT_PROTOCOL_V1]";

const PROMPT_ZH = "【Operit 通用富文本输出规范 v1】\n你必须在回答中遵循以下通用输出协议。目标是让不同模型通过 Operit 使用同一套标准格式，并获得稳定、易读的显示效果。\n\n一、基本原则\n- 内容准确、推理完整优先于装饰；只在有助于理解时使用富文本。\n- 使用标准 CommonMark Markdown 与 GFM 扩展，不使用模型专有标签、自定义组件、JSX 或依赖客户端私有语法的格式。\n- 不要把整篇回答包进一个代码块；正文、公式、代码、图表各自使用合适结构。\n- 兼顾手机屏幕：段落适中、表格简洁、代码行不要无意义地过长。\n\n二、标题与强调\n- 按层级使用 #、##、### 标题，不跳级，不滥用标题。\n- 使用 **粗体** 突出结论和关键词，*斜体* 作轻度强调，`行内代码` 表示变量、函数、命令和文件名。\n- 使用 > 引用块呈现重要提示、定理条件、风险和注意事项。\n- 不要用 HTML/CSS 手工指定文字颜色。标题层级、引用和列表标记的颜色由 Operit 当前主题决定；用语义结构表达层级即可。\n\n三、列表、任务和表格\n- 普通列表使用 - 或 *；有步骤时使用 1. 2. 3.。\n- 嵌套列表使用一致缩进：每深入一层增加两个空格；不要用手打圆点、特殊空格伪造列表。\n- 任务清单使用 GFM 语法：- [ ] 未完成；- [x] 已完成。不要用 Unicode 方框替代。\n- 对比和结构化数据使用标准 Markdown 表格；列数保持一致，单元格简短，避免超宽表格。\n\n四、数学公式\n- 使用标准 LaTeX：行内公式使用 \\\\( ... \\\\)，独立公式使用 \\\\[ ... \\\\]。\n- 多行推导可使用 aligned、cases、matrix 等标准环境；正确处理上下标、分式、根号、积分、求和和希腊字母。\n- 公式前后解释符号和推导依据；不要把 LaTeX 放进普通代码块，也不要输出未经转义的原始公式命令。\n\n五、代码块\n- 完整代码使用带语言标识的围栏代码块，例如 ```python、```kotlin、```java、```cpp、```json、```bash、```javascript。\n- 每个代码块只放一种语言或一种文件内容；多个文件分别标明文件名。\n- 代码应完整、可运行；不要用省略号代替关键实现。代码块外说明用途、依赖和运行方法。\n\n六、Mermaid 图\n- 需要表达流程、状态或关系时，可使用独立的 ```mermaid 代码块。\n- 优先使用标准 flowchart TD、flowchart LR、sequenceDiagram 等语法；节点文字简洁，连线明确。\n- 避免非标准扩展和过度复杂的单张图；必要时拆分。\n\n七、SVG 矢量图\n- 用户要求示意图、函数图像、物理轨迹、结构图或实验装置图时，若 SVG 能帮助理解，输出独立的 ```svg 代码块。\n- SVG 必须是完整、独立、格式正确的 XML 文档，包含 xmlns=\"http://www.w3.org/2000/svg\"、明确的 width、height 和 viewBox；所有标签闭合。\n- 使用明确的 fill、stroke、stroke-width、font-size 和对比度良好的颜色，不依赖客户端默认颜色或透明背景。\n- 优先使用 rect、circle、ellipse、line、path、polygon、polyline、text、tspan、g、defs、linearGradient、radialGradient 和 marker 等常见元素。\n- 为兼容 Operit 的安全预览，禁止 JavaScript、事件属性（如 onload）、foreignObject、嵌入 HTML、外部图片、外部字体、外部链接、DOCTYPE、ENTITY、CSS 外部 url() 和网络/文件资源。\n- 数学/物理图必须依据真实公式和坐标绘制，标出单位、关键点和坐标轴；不要把不精确示意图冒充精确曲线。\n- 若无法可靠生成完整 SVG，明确说明限制并提供可核验的替代方案，不要输出残缺 SVG。\n\n八、链接与输出组织\n- 超链接使用 [描述](https://...)，只提供真实且相关的链接。\n- 复杂回答按“结论—分析/推导—结果—注意事项”组织；普通对话保持自然简洁。\n- 不要为了展示格式而强行添加表格、Mermaid 或 SVG。\n\n九、最终自检\n- 标题层级是否合理？列表缩进是否一致？表格列数是否匹配？\n- 数学公式是否为标准 LaTeX？代码块是否标注正确语言？\n- Mermaid 是否为标准语法？SVG 是否完整、闭合、无脚本和外部依赖？\n- 是否保持内容准确、适合手机阅读？\n\n最终原则：语义结构优先于手工装饰；标准格式优先于专有格式；准确性优先于排版效果。";
const PROMPT_EN = "[Operit Universal Rich Text Output Protocol v1]\nFollow this provider-neutral output protocol so responses from different models render consistently in Operit.\n\n1. General rules\n- Accuracy and completeness come before decoration. Use rich formatting only when it improves comprehension.\n- Use standard CommonMark Markdown and GFM. Do not use proprietary model tags, custom components, JSX, or client-private syntax.\n- Never wrap the entire answer in one code block. Keep prose, formulas, code, and diagrams in their appropriate blocks.\n- Optimize for phone screens: moderate paragraphs, concise tables, and readable code lines.\n\n2. Headings and emphasis\n- Use #, ##, and ### in a logical hierarchy without skipping levels or overusing headings.\n- Use **bold** for key conclusions, *italic* for light emphasis, and `inline code` for identifiers, commands, variables, and filenames.\n- Use > blockquotes for important notes, assumptions, risks, and cautions.\n- Do not force text colors with HTML/CSS. Operit applies theme-aware colors to semantic heading, quote, and list structures.\n\n3. Lists, tasks, and tables\n- Use - or * for unordered lists and 1. 2. 3. for ordered steps.\n- Indent each nested list level consistently by two spaces. Do not imitate lists with manually typed bullet symbols or special spaces.\n- Use GFM task syntax: - [ ] pending and - [x] completed. Do not substitute Unicode checkbox characters.\n- Use standard Markdown tables for concise comparisons and structured data. Keep column counts consistent and cells short.\n\n4. Math\n- Use standard LaTeX: \\\\( ... \\\\) for inline math and \\\\[ ... \\\\] for display math.\n- Use standard environments such as aligned, cases, and matrix for multi-line derivations. Format subscripts, superscripts, fractions, roots, integrals, sums, and Greek letters correctly.\n- Explain symbols and reasoning around equations. Do not put LaTeX in ordinary code fences.\n\n5. Code\n- Put complete code in fenced blocks with an accurate language label, such as ```python, ```kotlin, ```java, ```cpp, ```json, ```bash, or ```javascript.\n- Keep each block to one language or file. Label separate files clearly.\n- Provide complete, runnable code; do not replace essential implementation with ellipses. Explain dependencies and how to run it outside the code block.\n\n6. Mermaid\n- For processes, states, and relationships, use a separate ```mermaid block.\n- Prefer standard flowchart TD, flowchart LR, or sequenceDiagram syntax. Keep labels concise and connections explicit.\n- Avoid nonstandard extensions and overly large diagrams; split complex diagrams when needed.\n\n7. SVG\n- When a diagram, function plot, physics trajectory, structure, or apparatus benefits from vector graphics, provide a separate ```svg block.\n- SVG must be a complete, standalone, well-formed XML document with xmlns=\"http://www.w3.org/2000/svg\", explicit width, height, and viewBox, and properly closed elements.\n- Set explicit fill, stroke, stroke-width, font-size, and sufficient contrast. Do not rely on inherited client colors or transparent backgrounds.\n- Prefer common elements: rect, circle, ellipse, line, path, polygon, polyline, text, tspan, g, defs, linearGradient, radialGradient, and marker.\n- For Operit's safe preview, never include JavaScript, event attributes such as onload, foreignObject, embedded HTML, external images/fonts/links, DOCTYPE, ENTITY, external CSS url(), or network/file resources.\n- Mathematical and physics diagrams must follow the actual equations and coordinates, with labeled axes, units, and key points. Never present an inaccurate sketch as an exact plot.\n- If a valid complete SVG cannot be produced reliably, state the limitation and provide a verifiable alternative instead of malformed SVG.\n\n8. Links and organization\n- Use [descriptive link text](https://...) only for real, relevant URLs.\n- Organize substantial answers as conclusion, explanation/derivation, result, and caveats. Keep casual replies natural and concise.\n- Do not force tables, Mermaid, or SVG into answers that do not need them.\n\n9. Final self-check\n- Are heading levels and list indentation consistent? Do tables have matching columns?\n- Are formulas valid LaTeX and code blocks labeled correctly?\n- Is Mermaid standard syntax? Is SVG complete, well-formed, script-free, and self-contained?\n- Is the answer accurate and readable on a phone?\n\nCore principle: semantic structure over manual decoration; standard formats over proprietary formats; accuracy over visual effects.";

function getSettingsPath() {
  return String(ToolPkg.getConfigDir()) + "/" + SETTINGS_FILE;
}

async function readEnabled() {
  try {
    const result = await Tools.Files.read(getSettingsPath());
    const settings = JSON.parse(String((result && result.content) || "{}"));
    if (settings && typeof settings.enabled === "boolean") return settings.enabled;
  } catch (_error) {
    // First install or unreadable settings: use the manifest default.
  }
  return DEFAULT_ENABLED;
}

async function writeEnabled(enabled) {
  const settings = { enabled: !!enabled, schemaVersion: 1 };
  await Tools.Files.write(getSettingsPath(), JSON.stringify(settings, null, 2));
}

function isEnglish(event) {
  const payload = (event && event.eventPayload) || {};
  return payload.useEnglish === true;
}

async function onInputMenuToggle(event) {
  const payload = (event && event.eventPayload) || {};
  if (payload.action === "create") {
    const enabled = await readEnabled();
    return {
      toggles: [{
        id: TOGGLE_ID,
        title: isEnglish(event) ? "Rich Text Protocol" : "富文本输出规范",
        description: isEnglish(event)
          ? "Adds standard Markdown, LaTeX, Mermaid, and safe SVG output guidance to the model system prompt."
          : "向模型系统提示词注入通用 Markdown、LaTeX、Mermaid 和安全 SVG 输出规范。",
        isChecked: enabled,
        slot: "general"
      }]
    };
  }
  if (payload.action === "toggle" && payload.toggleId === TOGGLE_ID) {
    const enabled = await readEnabled();
    await writeEnabled(!enabled);
  }
  return null;
}

async function onSystemPromptCompose(event) {
  const stage = String((event && (event.eventName || event.event)) || "");
  if (stage !== "after_compose_system_prompt") return null;
  if (!(await readEnabled())) return null;
  const payload = (event && event.eventPayload) || {};
  const currentPrompt = String(payload.systemPrompt || "");
  if (currentPrompt.indexOf(PROMPT_MARKER) >= 0) return null;
  const protocol = (isEnglish(event) ? PROMPT_EN : PROMPT_ZH);
  const separator = currentPrompt.trim() ? currentPrompt.replace(/\s+$/, "") + "\n\n" : "";
  return { systemPrompt: separator + PROMPT_MARKER + "\n" + protocol };
}

function registerToolPkg() {
  ToolPkg.registerInputMenuTogglePlugin({ id: MENU_HOOK_ID, function: onInputMenuToggle });
  ToolPkg.registerSystemPromptComposeHook({ id: PROMPT_HOOK_ID, function: onSystemPromptCompose });
  return true;
}

if (typeof exports !== "undefined") {
  exports.registerToolPkg = registerToolPkg;
  exports.onInputMenuToggle = onInputMenuToggle;
  exports.onSystemPromptCompose = onSystemPromptCompose;
}
