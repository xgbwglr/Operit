"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const ROOT = path.resolve(__dirname, "../../..");
const PACKAGE_DIR = path.join(ROOT, "examples", "universal_richtext");
const RUNNER = path.join(ROOT, "tools", "toolpkg", "toolpkg_hook_runner.js");

function runPrompt(payload, configDir) {
  const result = spawnSync(process.execPath, [
    RUNNER,
    "--source", PACKAGE_DIR,
    "--kind", "system_prompt",
    "--event", "after_compose_system_prompt",
    "--payload", JSON.stringify(payload),
    "--config-dir", configDir,
    "--pretty"
  ], { cwd: ROOT, encoding: "utf8" });
  assert.equal(result.status, 0, result.stderr || result.stdout);
  return JSON.parse(result.stdout);
}

test("injects the Chinese protocol into the shared system prompt", () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), "operit-richtext-zh-"));
  const output = runPrompt({ systemPrompt: "BASE_SYSTEM_PROMPT", useEnglish: false }, configDir);
  const prompt = output.results[0].result.systemPrompt;
  assert.match(prompt, /BASE_SYSTEM_PROMPT/);
  assert.match(prompt, /OPERIT_UNIVERSAL_RICHTEXT_PROTOCOL_V1/);
  assert.match(prompt, /标准 CommonMark Markdown/);
  assert.match(prompt, /Mermaid/);
  assert.match(prompt, /SVG/);
});

test("injects the English protocol when useEnglish is true", () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), "operit-richtext-en-"));
  const output = runPrompt({ systemPrompt: "BASE_SYSTEM_PROMPT", useEnglish: true }, configDir);
  const prompt = output.results[0].result.systemPrompt;
  assert.match(prompt, /Universal Rich Text Output Protocol/);
  assert.match(prompt, /CommonMark Markdown/);
  assert.match(prompt, /standalone/);
});

test("respects the persisted disabled setting", () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), "operit-richtext-off-"));
  const packageConfigDir = path.join(configDir, "com.operit.universal_richtext");
  fs.mkdirSync(packageConfigDir, { recursive: true });
  fs.writeFileSync(path.join(packageConfigDir, "settings.json"), JSON.stringify({ enabled: false }), "utf8");
  const output = runPrompt({ systemPrompt: "BASE_SYSTEM_PROMPT", useEnglish: false }, configDir);
  assert.equal(output.results[0].result, null);
});

test("input menu toggle persists enabled state and system prompt hook follows it", async () => {
  const previousToolPkg = global.ToolPkg;
  const previousTools = global.Tools;
  const fileStore = new Map();
  const registrations = {};
  global.ToolPkg = {
    getConfigDir() { return "/mock/universal_richtext"; },
    registerInputMenuTogglePlugin(definition) { registrations.menu = definition; },
    registerSystemPromptComposeHook(definition) { registrations.prompt = definition; }
  };
  global.Tools = {
    Files: {
      async read(filePath) {
        if (!fileStore.has(filePath)) throw new Error("file not found");
        return { content: fileStore.get(filePath) };
      },
      async write(filePath, content) {
        fileStore.set(filePath, String(content));
        return { successful: true };
      }
    }
  };

  try {
    const modulePath = require.resolve("../main.js");
    delete require.cache[modulePath];
    const plugin = require("../main.js");
    await plugin.registerToolPkg();

    const created = await registrations.menu.function({
      eventName: "input_menu_toggle",
      eventPayload: { action: "create" }
    });
    assert.equal(created.toggles[0].isChecked, true);

    await registrations.menu.function({
      eventName: "input_menu_toggle",
      eventPayload: { action: "toggle", toggleId: "universal_richtext" }
    });
    const afterToggle = await registrations.menu.function({
      eventName: "input_menu_toggle",
      eventPayload: { action: "create" }
    });
    assert.equal(afterToggle.toggles[0].isChecked, false);

    const promptResult = await registrations.prompt.function({
      eventName: "after_compose_system_prompt",
      eventPayload: { systemPrompt: "BASE_SYSTEM_PROMPT" }
    });
    assert.equal(promptResult, null);
  } finally {
    if (previousToolPkg === undefined) delete global.ToolPkg;
    else global.ToolPkg = previousToolPkg;
    if (previousTools === undefined) delete global.Tools;
    else global.Tools = previousTools;
  }
});
