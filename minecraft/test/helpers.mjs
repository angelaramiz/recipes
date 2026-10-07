import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

// Carga sistem_recipe.html (fuente de verdad), extrae el <script> y lo ejecuta
// en un sandbox con DOM/localStorage falsos. init() queda diferido
// (readyState 'loading') asi que solo se exponen las funciones puras
// via window.__mcTest.
const root = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const htmlPath = process.env.APP_HTML || path.join(root, 'sistem_recipe.html');

export function loadApp(opts) {
  const html = readFileSync(htmlPath, 'utf8');
  const m = html.match(/<script>([\s\S]*)<\/script>/);
  if (!m) throw new Error('no <script> block found');
  const store = {};
  const win = {};
  const listeners = {};
  const elements = (opts && opts.elements) || {};
  const documentStub = {
    readyState: 'loading',
    addEventListener: (ev, fn) => { listeners[ev] = fn; },
    querySelector: () => null,
    querySelectorAll: () => [],
    getElementById: (id) => (id in elements ? elements[id] : null),
    createElement: () => null
  };
  const sandbox = {
    window: win,
    document: documentStub,
    localStorage: {
      getItem: (k) => (k in store ? store[k] : null),
      setItem: (k, v) => { store[k] = String(v); },
      removeItem: (k) => { delete store[k]; }
    },
    console
  };
  vm.createContext(sandbox);
  vm.runInContext(m[1], sandbox, { filename: 'app.js' });
  if (!win.__mcTest) throw new Error('__mcTest hook missing');
  return { api: win.__mcTest, store, listeners };
}
