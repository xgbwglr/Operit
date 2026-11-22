# Universal RichText（通用富文本规范）

这是一个 Operit ToolPkg 插件，不是 APK 补丁，也不是 Markdown 渲染器。它通过 `registerSystemPromptComposeHook` 在统一系统提示词流程中注入跨模型输出规范。

## 功能
- 默认启用；可在聊天输入菜单的「富文本输出规范」开关中关闭/开启。
- 对所有经过 Operit 统一 System Prompt Compose 流程的模型注入相同规范。
- 指导模型使用标准 Markdown/GFM、LaTeX、代码块、Mermaid 和安全 SVG。
- 按 `useEnglish` 选择英文或中文规范。
- 使用 ToolPkg 配置目录保存开关状态；不调用网络、不申请额外权限。

## 重要边界
- 插件只指导模型如何组织输出，不会改变 Operit 的 Markdown/SVG 渲染器，也不会给不支持这些格式的模型增加能力。
- 模型是否遵循系统提示词仍取决于模型和服务商；不能保证每个模型每次都严格遵守。
- SVG 指令限制脚本、外部资源和嵌入 HTML，以兼容 Operit 的安全预览。
- 标题、引用、列表的具体颜色由 Operit 客户端主题和渲染器控制，模型不能通过此插件强制设置 UI 颜色。

## 安装
1. 在本仓库 GitHub Actions 中打开 `Package Universal RichText ToolPkg` 工作流。
2. 下载名为 `universal-richtext-toolpkg` 的构建产物，其中包含 `universal-richtext.toolpkg`。
3. 在 Operit 的 ToolPkg/插件管理界面导入本地 `.toolpkg` 文件并启用。
4. 打开任意聊天的输入菜单，确认「富文本输出规范」开关处于开启状态。

## 快速测试
向任意模型发送：

请分别输出一个二级标题、一段引用、一个三级嵌套列表、一个 LaTeX 公式、一张 Markdown 表格、一个 Python 代码块、一个 Mermaid flowchart，以及一个仅使用 rect/circle/text 的完整 SVG 图形。不要把整篇回答放进代码块。

预期：模型应尽量使用标准格式；Operit 的渲染结果仍取决于当前客户端版本。

## 开发测试
在仓库根目录执行：

```bash
node --test examples/universal_richtext/tests/plugin.test.cjs
```

ToolPkg API 基线：`1.0.0`。入口文件：`main.js`。