import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { loadApp } from './helpers.mjs';

let api;
let R; // live refs: R.recetas, R.materiales

function seed() {
  R.recetas.length = 0;
  R.recetas.push(
    { id: 'r_small', nombre: 'Batery Small', ingredientes: [{ id: 'mat_fe', cantidad: 2 }, { id: 'mat_rd', cantidad: 1 }], maquina: 'mesa', tiempo: 10, energia: 5 },
    { id: 'r_med', nombre: 'Batery Medium', ingredientes: [{ id: 'rec_r_small', cantidad: 4 }], maquina: 'horno', tiempo: 30, energia: 20 },
    { id: 'r_old', nombre: 'Receta Vieja', ingredientes: [{ id: 'mat_au', cantidad: 1 }] }
  );
}

describe('recetas anidadas y planos', () => {
  beforeEach(() => {
    const app = loadApp();
    api = app.api;
    R = api.state();
    seed();
  });

  it('nombreIngrediente resuelve mat_ y rec_ a nombres', () => {
    assert.equal(api.nombreIngrediente({ id: 'mat_fe', cantidad: 2 }).nombre, 'Hierro');
    assert.equal(api.nombreIngrediente({ id: 'rec_r_small', cantidad: 4 }).nombre, 'Batery Small');
    assert.equal(api.nombreIngrediente({ id: 'rec_r_small', cantidad: 4 }).esReceta, true);
    assert.equal(api.nombreIngrediente({ id: 'mat_fe', cantidad: 2 }).esReceta, false);
  });

  it('resolverReceta acumula materiales, tiempo y energia multinivel', () => {
    const med = R.recetas.find((r) => r.id === 'r_med');
    const res = api.resolverReceta(med, 1);
    const fe = res.materiales.find((m) => m.codigo === 'fe');
    const rd = res.materiales.find((m) => m.codigo === 'rd');
    assert.equal(fe.cantidad, 8);   // 4 x 2
    assert.equal(rd.cantidad, 4);   // 4 x 1
    assert.equal(res.tiempo, 70);   // 30 + 4x10
    assert.equal(res.energia, 40);  // 20 + 4x5
  });

  it('maquinas acumuladas por id para el cuello de botella', () => {
    const med = R.recetas.find((r) => r.id === 'r_med');
    const res = api.resolverReceta(med, 1);
    const mesa = res.maquinas.find((m) => m.id === 'mesa');
    const horno = res.maquinas.find((m) => m.id === 'horno');
    assert.equal(mesa.tiempo, 40);  // 4x10 > horno 30 -> cuello de botella
    assert.equal(horno.tiempo, 30);
  });

  it('detectarCiclo frena ciclo directo y transitivo', () => {
    assert.equal(api.detectarCiclo([{ id: 'rec_r_med', cantidad: 1 }], 'r_med'), true);
    R.recetas.push({ id: 'r_a', nombre: 'A', ingredientes: [{ id: 'rec_r_b', cantidad: 1 }] });
    R.recetas.push({ id: 'r_b', nombre: 'B', ingredientes: [{ id: 'rec_r_a', cantidad: 1 }] });
    assert.equal(api.detectarCiclo([{ id: 'rec_r_b', cantidad: 1 }], 'r_a'), true);
    assert.equal(api.detectarCiclo([{ id: 'mat_fe', cantidad: 1 }], 'r_a'), false);
  });

  it('accesores con defaults para datos viejos sin plano', () => {
    const old = R.recetas.find((r) => r.id === 'r_old');
    assert.equal(api.maquinaDe(old), 'manual');
    assert.equal(api.tiempoDe(old), 0);
    assert.equal(api.energiaDe(old), 0);
    assert.equal(api.nombreMaquina('inexistente'), 'Crafteo manual');
  });

  it('renderRecetas muestra nombres, no IDs crudos', () => {
    const box = { html: '', set innerHTML(v) { this.html = v; }, get innerHTML() { return this.html; }, querySelectorAll: () => [] };
    const app = loadApp({ elements: { recetasList: box } });
    const st = app.api.state();
    st.recetas.push(
      { id: 'r_small', nombre: 'Batery Small', ingredientes: [{ id: 'mat_fe', cantidad: 2 }], maquina: 'mesa', tiempo: 10, energia: 0 },
      { id: 'r_med', nombre: 'Batery Medium', ingredientes: [{ id: 'rec_r_small', cantidad: 4 }], maquina: 'manual', tiempo: 0, energia: 0 }
    );
    app.api.renderRecetas();
    assert.match(box.html, /2 x Hierro/);
    assert.match(box.html, /4 x Batery Small/);
    assert.doesNotMatch(box.html, /mat_fe/);
    assert.doesNotMatch(box.html, /rec_r_small/);
  });
});
