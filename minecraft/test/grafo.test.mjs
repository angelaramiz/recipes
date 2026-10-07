import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { loadApp } from './helpers.mjs';

let api;
let R;

function seed() {
  R.recetas.length = 0;
  R.recetas.push(
    { id: 'r_small', nombre: 'Batery Small', ingredientes: [{ id: 'mat_fe', cantidad: 2 }], maquina: 'mesa', tiempo: 10, energia: 0 },
    { id: 'r_med', nombre: 'Batery Medium', ingredientes: [{ id: 'rec_r_small', cantidad: 4 }], maquina: 'horno', tiempo: 30, energia: 0 },
    { id: 'r_jumbo', nombre: 'Batery Jumbo', ingredientes: [{ id: 'rec_r_med', cantidad: 2 }, { id: 'rec_r_small', cantidad: 1 }], maquina: 'fundicion', tiempo: 60, energia: 0 }
  );
}

describe('grafo de flujo', () => {
  beforeEach(() => {
    const app = loadApp();
    api = app.api;
    R = api.state();
    seed();
  });

  it('niveles por profundidad desde la raiz', () => {
    const g = api.computeGrafo('r_jumbo');
    const nivel = {};
    g.nodos.forEach((n) => { nivel[n.id] = n.nivel; });
    assert.equal(nivel['r_jumbo'], 0);
    assert.equal(nivel['r_med'], 1);
    assert.equal(nivel['r_small'], 2);
  });

  it('subreceta compartida es un solo nodo con dos aristas', () => {
    const g = api.computeGrafo('r_jumbo');
    assert.equal(g.nodos.filter((n) => n.id === 'r_small').length, 1);
    const hacia = g.aristas.filter((a) => a.a === 'r_small');
    assert.equal(hacia.length, 2);
    assert.equal(hacia.find((a) => a.de === 'r_jumbo').cantidad, 1);
  });

  it('aristas llevan cantidad y nodos maquina y posicion', () => {
    const g = api.computeGrafo('r_med');
    const e = g.aristas.find((a) => a.de === 'r_med');
    assert.equal(e.cantidad, 4);
    const n = g.nodos.find((x) => x.id === 'r_med');
    assert.equal(n.maquina, 'horno');
    assert.ok(n.x >= 0 && n.y >= 0);
    // mismo nivel no se solapa en Y
    const ys = g.nodos.filter((x) => x.nivel === n.nivel).map((x) => x.y);
    assert.equal(new Set(ys).size, ys.length);
  });

  it('raiz desconocida da grafo vacio y no cuelga con ciclo', () => {
    const vacio = api.computeGrafo('nope');
    assert.equal(vacio.nodos.length, 0);
    assert.equal(vacio.aristas.length, 0);
    R.recetas.push({ id: 'r_a', nombre: 'A', ingredientes: [{ id: 'rec_r_a', cantidad: 1 }] });
    const g = api.computeGrafo('r_a');
    assert.equal(g.nodos.length, 1);
  });
});
