// SFC 编译校验：parse + compileScript + compileTemplate + compileStyle
import { readFileSync } from 'node:fs';
import { parse, compileScript, compileTemplate, compileStyle } from '@vue/compiler-sfc';

const files = process.argv.slice(2);
let failed = 0;

for (const file of files) {
  const source = readFileSync(file, 'utf8');
  const { errors, descriptor } = parse(source, { filename: file });
  if (errors.length) {
    failed++;
    console.error(`[PARSE FAIL] ${file}`);
    for (const e of errors) console.error('  ', e.message);
    continue;
  }
  const id = 'sfc-check';
  try {
    if (descriptor.script || descriptor.scriptSetup) {
      compileScript(descriptor, { id });
    }
    if (descriptor.template) {
      const res = compileTemplate({
        source: descriptor.template.content,
        filename: file,
        id,
      });
      if (res.errors.length) throw new Error(res.errors.map((e) => e.message || e).join('; '));
    }
    for (const style of descriptor.styles) {
      const res = compileStyle({
        source: style.content,
        filename: file,
        id,
        scoped: style.scoped,
      });
      if (res.errors.length) throw new Error(res.errors.map((e) => e.message || e).join('; '));
    }
    console.log(`[OK] ${file}`);
  } catch (err) {
    failed++;
    console.error(`[COMPILE FAIL] ${file}`);
    console.error('  ', err.message);
  }
}

process.exit(failed ? 1 : 0);
