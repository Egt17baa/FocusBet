// Pruebas de app/src/main/assets/bridge.js en Chromium (Playwright preinstalado).
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const here = path.dirname(fileURLToPath(import.meta.url));
const bridge = readFileSync(path.join(here, '../app/src/main/assets/bridge.js'), 'utf8');

const browser = await chromium.launch();
const page = await browser.newPage();
let failed = 0;
async function load(name) {
  await page.goto('file://' + path.join(here, 'fixtures', name));
  await page.evaluate(bridge);
}
async function test(name, fn) {
  try { await fn(); console.log('ok   ' + name); }
  catch (e) { failed++; console.log('FAIL ' + name + '\n     ' + e.message); }
}

await test('snapshotListsIdsAndNames', async () => {
  await load('tramite.html');
  const s = JSON.parse(await page.evaluate('CH.snapshot()'));
  assert.ok(s.ids.includes('tramiteGrupo[1]'), JSON.stringify(s.ids));
  assert.ok(s.ids.includes('btnAceptar'));
  assert.match(s.text, /INTERNET CITA PREVIA/);
});

await test('snapshotFlagsVisibleCaptchaImageOnly', async () => {
  await load('datos.html');
  let s = JSON.parse(await page.evaluate('CH.snapshot()'));
  assert.ok(!s.ids.includes('__captchaimg'));
  await page.evaluate("document.querySelector('img.img-thumbnail').style.display='block'");
  s = JSON.parse(await page.evaluate('CH.snapshot()'));
  assert.ok(s.ids.includes('__captchaimg'));
});

await test('selectByTextIgnoresAccentsAndHyphenation', async () => {
  await load('tramite.html');
  const r = await page.evaluate('CH.run([["selectByText","tramiteGrupo[1]","TOMA DE HUELLAS"],["click","btnAceptar"]])');
  assert.equal(r, 'ok');
  assert.equal(await page.evaluate("document.getElementsByName('tramiteGrupo[1]')[0].value"), '4010');
  assert.equal(await page.evaluate("document.getElementsByName('tramiteGrupo[0]')[0].value"), '-1');
  assert.equal(await page.evaluate('window.accepted'), true);
});

await test('selectByTextPrefersExactMatch', async () => {
  await load('datos.html');
  assert.equal(await page.evaluate('CH.run([["selectByText","txtPaisNac","Perú"]])'), 'ok');
  assert.equal(await page.evaluate("document.getElementById('txtPaisNac').value"), '1');
});

await test('selectByTextMissingReturnsFalse', async () => {
  await load('datos.html');
  const r = await page.evaluate('CH.run([["selectByText","txtPaisNac","ATLANTIDA"],["click","btnEnviar"]])');
  assert.equal(r, 'missing:txtPaisNac');
  assert.equal(await page.evaluate('window.sent'), undefined, 'no debe pulsar tras fallo');
});

await test('fillPersonalSetsValuesAndFiresChange', async () => {
  await load('datos.html');
  await page.evaluate("window.changes=0; document.getElementById('txtIdCitado').addEventListener('change',()=>window.changes++)");
  const r = await page.evaluate(`CH.run([["check","rdbTipoDocNie"],["setValue","txtIdCitado","X1234567L"],["setValue","txtDesCitado","O'Brien \\"</script>"],["selectByText","txtPaisNac","PERU"],["click","btnEnviar"]])`);
  assert.equal(r, 'ok');
  assert.equal(await page.evaluate("document.getElementById('rdbTipoDocNie').checked"), true);
  assert.equal(await page.evaluate("document.getElementById('txtIdCitado').value"), 'X1234567L');
  assert.equal(await page.evaluate("document.getElementById('txtDesCitado').value"), `O'Brien "</script>`);
  assert.equal(await page.evaluate('window.changes'), 1);
  assert.equal(await page.evaluate('window.sent'), true);
});

await test('missingIdReported', async () => {
  await load('datos.html');
  assert.equal(await page.evaluate('CH.run([["click","btnNoExiste"]])'), 'missing:btnNoExiste');
});

await test('optionalSelectNeverFails', async () => {
  await load('datos.html');
  assert.equal(await page.evaluate('CH.run([["trySelectByText","sede","CUALQUIER"]])'), 'ok');
});

await test('debugWorksOnOpaqueDocument', async () => {
  await page.goto('about:blank');
  await page.evaluate(bridge);
  const d = JSON.parse(await page.evaluate('CH.debug()'));
  assert.equal(d.href, 'about:blank');
});

await test('clickAnyUsesIdThenVisibleText', async () => {
  await page.setContent('<h1>x</h1><input type="button" value="Presentación con Cl@ve" onclick="window.con=1"><button onclick="window.sin=1">Presentación  sin Cl@ve</button>');
  await page.evaluate(bridge);
  assert.equal(await page.evaluate('CH.run([["clickAny","btnEntrar","SIN CL@VE"]])'), 'ok');
  assert.equal(await page.evaluate('window.sin'), 1);
  assert.equal(await page.evaluate('window.con'), undefined);
});

await test('controlsListsFormElements', async () => {
  await load('datos.html');
  const c = await page.evaluate('CH.controls()');
  assert.match(c, /input#txtIdCitado/);
  assert.match(c, /select#txtPaisNac/);
  assert.match(c, /input#btnEnviar="Aceptar"/);
});

await test('selectByTextAlreadySelectedFiresNoChange', async () => {
  await load('tramite.html');
  await page.evaluate("window.ch=0; document.getElementById('sede').addEventListener('change',()=>window.ch++)");
  assert.equal(await page.evaluate('CH.run([["trySelectByText","sede","CUALQUIER OFICINA"]])'), 'ok');
  assert.equal(await page.evaluate('window.ch'), 0);
});

await test('needChoiceOnlyFailsWhenRealOptionsAndNoneChosen', async () => {
  await page.setContent('<select id="subtramite"></select>');
  await page.evaluate(bridge);
  assert.equal(await page.evaluate('CH.run([["needChoice","subtramite"]])'), 'ok');
  await page.setContent('<select id="subtramite"><option value="">Seleccione</option><option value="1">A</option><option value="2">B</option></select>');
  await page.evaluate(bridge);
  assert.equal(await page.evaluate('CH.run([["needChoice","subtramite"]])'), 'missing:subtramite');
  await page.evaluate("document.getElementById('subtramite').value='2'");
  assert.equal(await page.evaluate('CH.run([["needChoice","subtramite"]])'), 'ok');
  assert.equal(await page.evaluate('CH.run([["needChoice","noExiste"]])'), 'ok');
});

await browser.close();
console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
process.exit(failed ? 1 : 0);
