// Ficha do idoso: faixa de estado + secções (resumo, tensão, quedas, contactos, documentos, mensagens).

import {
  ErroApi, api, apagar, enviarFicheiro, get, post,
} from './api.js';
import {
  CATEGORIA, CLASSIFICACAO, DESTINATARIO, ESTADO_QUEDA, anelContagem, avisar, confirmar, dataCompleta,
  dataCurta, dataHora, dialogo, esc, estadoPill, haQuanto, icone, idade, ocupado, tamanho, telefone,
  telefoneValido,
} from './ui.js';

const ESPERA_QUEDA = 60; // segundos — igual a elderlyhealth.queda.timeout-resposta
const ESPERA_CONTACTO = 60; // segundos — igual a elderlyhealth.contacto.timeout-confirmacao

const SECOES = [
  { id: 'resumo', nome: 'Resumo', icone: 'casa', vista: secaoResumo },
  { id: 'tensao', nome: 'Tensão arterial', icone: 'pulso', vista: secaoTensao },
  { id: 'quedas', nome: 'Quedas', icone: 'queda', vista: secaoQuedas },
  { id: 'contactos', nome: 'Contactos', icone: 'pessoas', vista: secaoContactos },
  { id: 'documentos', nome: 'Documentos', icone: 'pasta', vista: secaoDocumentos },
  { id: 'mensagens', nome: 'Mensagens', icone: 'mensagem', vista: secaoMensagens },
];

const quedaAtiva = (q) => q.estado === 'AGUARDA_RESPOSTA_IDOSO' || q.estado === 'ENFERMEIRO_ALERTADO';
const primeiroNome = (n) => String(n || '').split(' ')[0];
const tracosDesenhados = new Set(); // a linha da faixa só se desenha (animada) uma vez por idoso

export async function vistaIdoso(ctx, id, secaoId) {
  const secao = SECOES.find((s) => s.id === secaoId) || SECOES[0];
  const main = ctx.conteudo;
  main.innerHTML = `<div id="faixa"></div>
    <nav class="separadores" aria-label="Secções da ficha"></nav>
    <section id="secao" style="display:grid;gap:1.4rem"></section>`;

  const st = {
    ctx, id, secao, resumo: null, medicoes: [], quedas: [], desenhada: false,
    faixa: main.querySelector('#faixa'), nav: main.querySelector('.separadores'), alvo: main.querySelector('#secao'),
    atualizar: null, // função de atualização da secção (polling)
  };
  st.recarregar = async () => {
    await carregar(st);
    desenharFaixa(st);
    desenharSeparadores(st);
    await st.atualizar?.();
  };

  await carregar(st);
  desenharFaixa(st);
  desenharSeparadores(st);
  await secao.vista(st);

  const polling = setInterval(async () => {
    try {
      await carregar(st);
      desenharFaixa(st);
      desenharSeparadores(st);
      await st.atualizar?.();
    } catch { /* falha temporária: tenta outra vez no próximo ciclo */ }
  }, 5000);
  const relogio = setInterval(() => atualizarContagens(st), 1000);
  ctx.aoSair(() => { clearInterval(polling); clearInterval(relogio); });
}

async function carregar(st) {
  const [resumo, medicoes, quedas] = await Promise.all([
    get(`/api/idosos/${st.id}`),
    get(`/api/tensao-arterial/idosos/${st.id}`),
    get(`/api/quedas?idosoId=${st.id}`),
  ]);
  Object.assign(st, { resumo, medicoes, quedas: quedas.sort((a, b) => b.id - a.id) });
}

/* ---------------------------------------------------------------------------
   Faixa de estado
--------------------------------------------------------------------------- */

function desenharFaixa(st) {
  const { idoso, ultimaMedicao: t } = st.resumo;
  const ativa = st.quedas.find(quedaAtiva);
  let tom = 'calma';
  let estado = 'Tudo calmo';
  if (t?.classificacao === 'ALERTA_NAO_GRAVE') { tom = 'atencao'; estado = 'Tensão fora do normal'; }
  if (t?.classificacao === 'ALERTA_GRAVE') { tom = 'alerta'; estado = t.sistolica < 90 ? 'Tensão muito baixa' : 'Tensão muito alta'; }
  if (ativa) { tom = 'alerta'; estado = ativa.estado === 'ENFERMEIRO_ALERTADO' ? 'Enfermagem a caminho' : 'Possível queda detetada'; }

  const anos = idade(idoso.dataNascimento);
  st.faixa.innerHTML = `
    <header class="faixa faixa--${tom}">
      <div>
        <p class="faixa__estado"><span class="ponto"></span>${esc(estado)}</p>
        <h1>${esc(idoso.nome)}</h1>
        <p class="faixa__dados">
          ${idoso.quarto ? `<span>Quarto <b>${esc(idoso.quarto)}</b></span>` : ''}
          ${anos ? `<span><b>${anos}</b> anos</span>` : ''}
          <span><b>${st.resumo.contactosFamiliares}</b> ${st.resumo.contactosFamiliares === 1 ? 'contacto familiar' : 'contactos familiares'}</span>
        </p>
      </div>
      <div class="faixa__tensao">
        ${t ? `<p class="rotulo">Última tensão arterial</p>
          <p class="valor">${t.sistolica}/${t.diastolica}<small>mmHg</small></p>
          <p class="quando">${esc(haQuanto(t.medidoEm))}${t.pulso ? `, pulso ${t.pulso}` : ''}</p>`
        : '<p class="rotulo">Ainda sem medições de tensão</p>'}
      </div>
      ${ativa ? blocoQueda(st, ativa) : ''}
      ${tracoTensao(st.medicoes, !st.desenhada && !tracosDesenhados.has(st.id))}
    </header>`;
  st.desenhada = true;
  tracosDesenhados.add(st.id);
  ligarAcoesQueda(st, st.faixa);
}

function tracoTensao(medicoes, animar) {
  const ultimas = medicoes.slice(-16);
  if (ultimas.length < 2) return '<svg class="faixa__traco" viewBox="0 0 1000 74" aria-hidden="true"></svg>';
  const vals = ultimas.map((m) => m.sistolica);
  const min = Math.min(...vals) - 8;
  const max = Math.max(...vals) + 8;
  const pts = vals.map((v, i) => {
    const x = (i / (vals.length - 1)) * 1000;
    const y = 66 - ((v - min) / (max - min)) * 56;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
  return `<svg class="faixa__traco" viewBox="0 0 1000 74" preserveAspectRatio="none" aria-hidden="true">
    <polygon class="area" points="0,74 ${pts} 1000,74"/>
    <polyline class="${animar ? 'desenho' : ''}" points="${pts}"/></svg>`;
}

function restanteQueda(q) {
  const passou = (Date.now() - new Date(q.detetadoEm).getTime()) / 1000;
  return Math.max(0, Math.ceil(ESPERA_QUEDA - passou));
}

function blocoQueda(st, q) {
  const equipa = st.ctx.equipa;
  const nome = primeiroNome(st.resumo.idoso.nome);
  if (q.estado === 'AGUARDA_RESPOSTA_IDOSO') {
    const pergunta = st.ctx.utilizador.papel === 'IDOSO' ? 'Está bem?' : `${nome} está bem?`;
    return `<div class="alerta-queda" data-queda="${q.id}" data-detetado="${esc(q.detetadoEm)}">
      ${anelContagem(restanteQueda(q), ESPERA_QUEDA)}
      <div><h2>${esc(pergunta)}</h2>
        <p class="suave">O sensor detetou uma possível queda ${esc(haQuanto(q.detetadoEm))}. Sem resposta, a enfermagem é alertada automaticamente.</p></div>
      <div class="acoes">
        <button class="btn btn--sec btn--grande" data-responder="false">Estou bem</button>
        <button class="btn btn--perigo btn--grande" data-responder="true">Preciso de ajuda</button>
      </div></div>`;
  }
  return `<div class="alerta-queda" data-queda="${q.id}">
    <span class="icone-redondo icone-redondo--alerta" style="width:64px;height:64px">${icone('equipa')}</span>
    <div><h2>A enfermagem foi alertada</h2>
      <p class="suave">${q.escaladoPorTimeout ? 'Não houve resposta ao alerta de queda' : 'Foi pedida ajuda depois da queda'}. Alerta enviado ${esc(dataHora(q.enfermeiroAlertadoEm))}${q.quarto ? `, quarto ${esc(q.quarto)}` : ''}.</p></div>
    <div class="acoes">${equipa ? '<button class="btn btn--grande" data-atendimento>Registar atendimento</button>' : ''}</div></div>`;
}

function ligarAcoesQueda(st, raiz) {
  raiz.querySelectorAll('[data-responder]').forEach((b) => b.addEventListener('click', async () => {
    const id = b.closest('[data-queda]').dataset.queda;
    const precisaAjuda = b.dataset.responder === 'true';
    await ocupado(b, async () => {
      try {
        await post(`/api/quedas/${id}/resposta`, { precisaAjuda });
        avisar(precisaAjuda ? 'Pedido de ajuda enviado. A enfermagem foi alertada.' : 'Alerta cancelado. Ainda bem que está tudo bem.');
        await st.recarregar();
      } catch (e) { avisar(e.message, 'erro'); await st.recarregar(); }
    });
  }));
  raiz.querySelectorAll('[data-atendimento]').forEach((b) => b.addEventListener('click', () => {
    const id = Number(b.closest('[data-queda]').dataset.queda);
    abrirRegistoAtendimento(st.quedas.find((q) => q.id === id), st.ctx, st.resumo.idoso, st.recarregar);
  }));
}

function atualizarContagens(st) {
  st.faixa.querySelectorAll('[data-detetado]').forEach((el) => {
    const restante = restanteQueda({ detetadoEm: el.dataset.detetado });
    const anel = el.querySelector('.contagem');
    if (anel) anel.outerHTML = anelContagem(restante, ESPERA_QUEDA);
  });
}

/** Diálogo da enfermagem: "Receber relatório de atendimento" -> "Registar incidente". */
export function abrirRegistoAtendimento(queda, ctx, idoso, depois) {
  const { caixa, fechar } = dialogo(`
    <div class="cabeca"><span class="icone-redondo icone-redondo--alerta" style="width:3rem;height:3rem">${icone('queda')}</span>
      <div><h2>Registar atendimento</h2><p class="suave">${esc(idoso?.nome || '')}${queda.quarto ? `, quarto ${esc(queda.quarto)}` : ''}</p></div></div>
    <form novalidate style="display:grid;gap:1rem">
      <div class="mensagem-erro" hidden data-erro></div>
      <label class="campo"><span>Enfermeiro(a)</span><input name="enfermeiro" value="${esc(ctx.utilizador.nome)}" required></label>
      <label class="campo"><span>O que aconteceu</span><textarea name="relatorio" required placeholder="Estado em que encontrou o idoso, o que foi feito, se precisa de vigilância…"></textarea>
        <small>O relatório fica guardado nos documentos do idoso, em Relatórios.</small></label>
      <div class="acoes"><button type="button" class="btn btn--sec" data-fechar>Cancelar</button><button class="btn" type="submit">Registar incidente</button></div>
    </form>`);
  const form = caixa.querySelector('form');
  form.relatorio.focus();
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const erro = form.querySelector('[data-erro]');
    if (!form.enfermeiro.value.trim() || !form.relatorio.value.trim()) {
      erro.textContent = 'Preencha o nome e o relatório.';
      erro.hidden = false;
      return;
    }
    await ocupado(form.querySelector('[type=submit]'), async () => {
      try {
        await post(`/api/quedas/${queda.id}/relatorio`, { enfermeiro: form.enfermeiro.value.trim(), relatorio: form.relatorio.value.trim() });
        fechar();
        avisar('Incidente registado e guardado nos documentos.');
        await depois?.();
      } catch (ex) { erro.textContent = ex.message; erro.hidden = false; }
    });
  });
}

function desenharSeparadores(st) {
  const ativas = st.quedas.filter(quedaAtiva).length;
  st.nav.innerHTML = SECOES.map((s) => `
    <a href="#/idoso/${st.id}/${s.id}" ${s.id === st.secao.id ? 'aria-current="page"' : ''}>${esc(s.nome)}
      ${s.id === 'quedas' && ativas ? `<span class="contador" aria-label="${ativas} ativas">${ativas}</span>` : ''}</a>`).join('');
}

/* ---------------------------------------------------------------------------
   Resumo
--------------------------------------------------------------------------- */

const ICONE_REF = { QUEDA: ['queda', 'alerta'], TENSAO: ['pulso', ''], CONTACTO: ['pessoas', ''], REGISTO: ['visto', ''] };

function itemNotificacao(n) {
  const [ic, tom] = ICONE_REF[(n.referencia || '').split('#')[0]] || ['mensagem', 'neutro'];
  const tomFinal = /URGENTE|QUEDA:/.test(n.mensagem) ? 'alerta' : tom;
  return `<li><span class="icone-redondo ${tomFinal ? `icone-redondo--${tomFinal}` : ''}">${icone(ic)}</span>
    <div><p>${esc(n.mensagem)}</p><p><time datetime="${esc(n.criadaEm)}">${esc(dataHora(n.criadaEm))}</time><span class="para">, ${esc((DESTINATARIO[n.destinatario] || '').toLowerCase())}</span></p></div></li>`;
}

async function secaoResumo(st) {
  const i = st.resumo.idoso;
  const p = i.preferencias || {};
  const ce = i.contactoEmergencia;
  st.alvo.innerHTML = `
    <div class="duas-colunas">
      <section class="painel">
        <div class="painel__topo"><h2>Últimos acontecimentos</h2><a href="#/idoso/${st.id}/mensagens">Ver todas as mensagens</a></div>
        <ul class="linha-tempo" data-acontecimentos><li><span></span><p class="suave">A carregar…</p></li></ul>
      </section>
      <section class="painel">
        <h2>Ficha</h2>
        <dl class="ficha">
          <dt>Nascimento</dt><dd>${i.dataNascimento ? esc(new Date(i.dataNascimento).toLocaleDateString('pt-PT')) : '—'}</dd>
          <dt>NIF</dt><dd class="num">${esc(i.nif)}</dd>
          <dt>Informação médica</dt><dd>${esc(i.informacaoMedica || '—')}</dd>
          <dt>Dieta</dt><dd>${esc(p.dieta || '—')}</dd>
          <dt>Alergias</dt><dd>${p.alergias?.length ? esc(p.alergias.join(', ')) : 'Nenhuma indicada'}</dd>
          <dt>Observações</dt><dd>${esc(p.observacoes || '—')}</dd>
          <dt>Emergência</dt><dd>${ce ? `${esc(ce.nome)}${ce.parentesco ? ` (${esc(ce.parentesco)})` : ''}<br><a href="tel:${esc(ce.telefone)}">${esc(telefone(ce.telefone))}</a>` : '—'}</dd>
        </dl>
      </section>
    </div>`;
  const lista = st.alvo.querySelector('[data-acontecimentos]');
  st.atualizar = async () => {
    const notas = await get(`/api/notificacoes?idosoId=${st.id}`);
    lista.innerHTML = notas.length
      ? notas.slice(0, 7).map(itemNotificacao).join('')
      : '<li><span class="icone-redondo icone-redondo--neutro">' + icone('mensagem') + '</span><div><p>Sem acontecimentos recentes.</p><p class="suave pequeno">Alertas, medições e confirmações aparecem aqui.</p></div></li>';
  };
  await st.atualizar();
}

/* ---------------------------------------------------------------------------
   Tensão arterial
--------------------------------------------------------------------------- */

function grafico(medicoes) {
  const dados = medicoes.slice(-20);
  if (dados.length < 2) return '<div class="vazio"><b>Ainda não há medições suficientes</b><p>O gráfico aparece a partir da segunda medição.</p></div>';
  const W = 760; const H = 280; const pl = 46; const pr = 14; const pt = 14; const pb = 36;
  const yMin = 40; const yMax = 200;
  const x = (k) => pl + (k / (dados.length - 1)) * (W - pl - pr);
  const y = (v) => pt + (1 - (Math.min(yMax, Math.max(yMin, v)) - yMin) / (yMax - yMin)) * (H - pt - pb);
  const linha = (campo) => dados.map((m, k) => `${x(k).toFixed(1)},${y(m[campo]).toFixed(1)}`).join(' ');
  const cor = { NORMAL: '#1e6b5c', ALERTA_NAO_GRAVE: '#c8861d', ALERTA_GRAVE: '#ad3127' };
  const passo = Math.ceil(dados.length / 7);
  const refs = [60, 90, 140, 180].map((v) => `<line class="ref" x1="${pl}" x2="${W - pr}" y1="${y(v)}" y2="${y(v)}"/><text class="eixo" x="${pl - 8}" y="${y(v) + 4}" text-anchor="end">${v}</text>`).join('');
  const etiquetas = dados.map((m, k) => (k % passo === 0 || k === dados.length - 1
    ? `<text class="eixo" x="${x(k)}" y="${H - 10}" text-anchor="middle">${esc(dataCurta(m.medidoEm))}</text>` : '')).join('');
  const pontos = dados.map((m, k) => `<circle cx="${x(k).toFixed(1)}" cy="${y(m.sistolica).toFixed(1)}" r="${m.classificacao === 'NORMAL' ? 4 : 6}" fill="${cor[m.classificacao]}"><title>${m.sistolica}/${m.diastolica} mmHg, ${esc(dataHora(m.medidoEm))}</title></circle>`).join('');
  return `<svg class="grafico" viewBox="0 0 ${W} ${H}" role="img" aria-label="Evolução da tensão arterial nas últimas ${dados.length} medições">
    <rect class="zona" x="${pl}" y="${y(140)}" width="${W - pl - pr}" height="${y(90) - y(140)}" rx="4"/>
    ${refs}${etiquetas}
    <polyline class="diastolica" points="${linha('diastolica')}"/>
    <polyline class="sistolica" points="${linha('sistolica')}"/>
    ${pontos}</svg>`;
}

async function secaoTensao(st) {
  st.alvo.innerHTML = `
    <section class="painel">
      <div class="painel__topo"><h2>Evolução</h2>
        <div class="legenda"><span><i></i>Sistólica</span><span><i class="d"></i>Diastólica</span><span><i class="z"></i>Sistólica normal (90 a 140)</span></div></div>
      <div data-grafico></div>
    </section>
    <section class="painel simulador">
      <h2>${icone('sensor')}Simulador do tensiómetro</h2>
      <p class="suave pequeno">No protótipo não há tensiómetro físico: envie aqui uma medição, como o dispositivo faria. O CPS classifica-a e avisa quem for preciso.</p>
      <div class="predefinidos">
        <button type="button" class="chip" data-pre="120,80,70">Normal 120/80</button>
        <button type="button" class="chip" data-pre="152,96,82">Fora do normal 152/96</button>
        <button type="button" class="chip" data-pre="186,122,98">Grave 186/122</button>
      </div>
      <form class="linha-campos" novalidate>
        <label class="campo"><span>Sistólica</span><input name="sistolica" type="number" inputmode="numeric" min="40" max="300" value="128"></label>
        <label class="campo"><span>Diastólica</span><input name="diastolica" type="number" inputmode="numeric" min="20" max="200" value="82"></label>
        <label class="campo"><span>Pulso</span><input name="pulso" type="number" inputmode="numeric" min="20" max="250" value="72"></label>
        <button class="btn" type="submit">${icone('enviar')}Enviar medição</button>
      </form>
    </section>
    <section class="painel">
      <h2>Medições</h2>
      <div class="tabela-wrap" data-tabela></div>
    </section>`;

  const form = st.alvo.querySelector('form');
  st.alvo.querySelectorAll('[data-pre]').forEach((b) => b.addEventListener('click', () => {
    const [s, d, p] = b.dataset.pre.split(',');
    form.sistolica.value = s; form.diastolica.value = d; form.pulso.value = p;
  }));
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const corpo = {
      idosoId: st.id,
      dispositivoId: `simulador-${st.resumo.idoso.quarto || st.id}`,
      sistolica: Number(form.sistolica.value),
      diastolica: Number(form.diastolica.value),
      pulso: form.pulso.value ? Number(form.pulso.value) : null,
    };
    await ocupado(form.querySelector('[type=submit]'), async () => {
      try {
        const m = await post('/api/tensao-arterial/medicoes', corpo);
        const txt = { NORMAL: 'Tensão normal. O idoso foi avisado.', ALERTA_NAO_GRAVE: 'Tensão fora do normal. Foram enviadas recomendações ao idoso.', ALERTA_GRAVE: 'Tensão grave. A enfermagem foi alertada.' };
        avisar(txt[m.classificacao], m.classificacao === 'ALERTA_GRAVE' ? 'erro' : 'ok');
        await st.recarregar();
      } catch (ex) { avisar(ex.message, 'erro'); }
    });
  });

  st.atualizar = async () => {
    st.alvo.querySelector('[data-grafico]').innerHTML = grafico(st.medicoes);
    const linhas = [...st.medicoes].reverse();
    st.alvo.querySelector('[data-tabela]').innerHTML = linhas.length ? `<table>
      <thead><tr><th>Quando</th><th>Tensão</th><th>Pulso</th><th>Estado</th><th>Recomendações</th></tr></thead>
      <tbody>${linhas.slice(0, 40).map((m) => `<tr>
        <td>${esc(dataHora(m.medidoEm))}</td>
        <td><span class="grande">${m.sistolica}/${m.diastolica}</span> <span class="suave pequeno">mmHg</span></td>
        <td class="num">${m.pulso ?? '—'}</td>
        <td>${estadoPill(CLASSIFICACAO[m.classificacao])}</td>
        <td class="pequeno">${m.recomendacoes?.length ? esc(m.recomendacoes.join('; ')) : '<span class="suave">—</span>'}</td></tr>`).join('')}</tbody></table>`
      : '<div class="vazio"><b>Sem medições</b><p>Use o simulador acima para enviar a primeira.</p></div>';
  };
  await st.atualizar();
}

/* ---------------------------------------------------------------------------
   Quedas
--------------------------------------------------------------------------- */

async function secaoQuedas(st) {
  st.alvo.innerHTML = `
    <section class="painel">
      <h2>Registo de quedas</h2>
      <ul class="lista" data-lista></ul>
    </section>
    <section class="painel simulador">
      <h2>${icone('sensor')}Simulador do sensor de quedas</h2>
      <p class="suave pequeno">Simula o sensor do quarto a detetar uma queda. O idoso tem ${ESPERA_QUEDA} segundos para responder; se não responder, a enfermagem é alertada.</p>
      <div><button class="btn btn--perigo" data-simular>${icone('queda')}Simular queda</button></div>
    </section>`;

  st.alvo.querySelector('[data-simular]').addEventListener('click', async (e) => {
    await ocupado(e.currentTarget, async () => {
      try {
        await post('/api/quedas', { idosoId: st.id, sensorId: `sensor-${st.resumo.idoso.quarto || st.id}` });
        avisar('Queda simulada. Veja o alerta no topo da ficha.');
        await st.recarregar();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      } catch (ex) { avisar(ex.message, 'erro'); }
    });
  });

  const lista = st.alvo.querySelector('[data-lista]');
  st.atualizar = async () => {
    lista.innerHTML = st.quedas.length ? st.quedas.map((q) => {
      const info = ESTADO_QUEDA[q.estado];
      return `<li data-queda="${q.id}">
        <span class="icone-redondo ${quedaAtiva(q) ? 'icone-redondo--alerta' : 'icone-redondo--neutro'}" style="width:2.8rem;height:2.8rem">${icone('queda')}</span>
        <div>
          <p class="titulo">Queda detetada ${esc(dataHora(q.detetadoEm))} ${estadoPill(info)}</p>
          <p class="detalhe">${q.sensorId ? `<span>Sensor ${esc(q.sensorId)}</span>` : ''}${q.quarto ? `<span>Quarto ${esc(q.quarto)}</span>` : ''}
            ${q.enfermeiroAlertadoEm ? `<span>Enfermagem alertada ${esc(dataHora(q.enfermeiroAlertadoEm))}${q.escaladoPorTimeout ? ' (sem resposta)' : ''}</span>` : ''}</p>
          ${q.relatorio ? `<p class="pequeno" style="margin-top:.4rem"><b>${esc(q.enfermeiro)}:</b> ${esc(q.relatorio)}</p>` : ''}
        </div>
        <div class="acoes-linha">${q.estado === 'ENFERMEIRO_ALERTADO' && st.ctx.equipa ? '<button class="btn btn--pequeno" data-atendimento>Registar atendimento</button>' : ''}</div>
      </li>`;
    }).join('') : '<li><span></span><div class="vazio" style="padding:0"><b>Sem quedas registadas</b><p>Quando o sensor detetar uma queda, ela aparece aqui.</p></div><span></span></li>';
    ligarAcoesQueda(st, lista);
  };
  await st.atualizar();
}

/* ---------------------------------------------------------------------------
   Contactos familiares
--------------------------------------------------------------------------- */

async function secaoContactos(st) {
  st.alvo.innerHTML = `
    <div class="duas-colunas">
      <section class="painel">
        <h2>Contactos familiares</h2>
        <ul class="lista" data-lista></ul>
      </section>
      <section class="painel">
        <h2>Adicionar contacto</h2>
        <form novalidate style="display:grid;gap:1rem">
          <div class="mensagem-erro" hidden data-erro></div>
          <label class="campo"><span>Nome</span><input name="nome" required></label>
          <label class="campo"><span>Parentesco</span><input name="parentesco" placeholder="Ex.: Neta" required></label>
          <label class="campo"><span>Telefone</span><input name="telefone" type="tel" placeholder="912 345 678" required></label>
          <label class="campo"><span>E-mail <span class="suave">(opcional)</span></span><input name="email" type="email"></label>
          <p class="suave pequeno">Depois de enviar, tem ${ESPERA_CONTACTO} segundos para confirmar a adição.</p>
          <div><button class="btn" type="submit">${icone('mais')}Adicionar contacto</button></div>
        </form>
      </section>
    </div>`;
  const lista = st.alvo.querySelector('[data-lista]');
  const form = st.alvo.querySelector('form');
  const erro = form.querySelector('[data-erro]');

  st.atualizar = async () => {
    const contactos = await get(`/api/idosos/${st.id}/contactos-familiares`);
    lista.innerHTML = contactos.length ? contactos.map((c) => `<li>
      <span class="icone-redondo" style="width:2.8rem;height:2.8rem">${icone('pessoas')}</span>
      <div><p class="titulo">${esc(c.nome)}</p>
        <p class="detalhe"><span>${esc(c.parentesco)}</span><a href="tel:${esc(c.telefone)}">${esc(telefone(c.telefone))}</a>${c.email ? `<a href="mailto:${esc(c.email)}">${esc(c.email)}</a>` : ''}</p></div>
      <span></span></li>`).join('')
      : '<li><span></span><div class="vazio" style="padding:0"><b>Ainda sem contactos familiares</b><p>Adicione quem deve ser avisado em caso de alerta.</p></div><span></span></li>';
  };
  await st.atualizar();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    erro.hidden = true;
    const dados = {
      nome: form.nome.value.trim(), parentesco: form.parentesco.value.trim(),
      telefone: form.telefone.value.trim(), email: form.email.value.trim() || null,
    };
    if (!dados.nome || !dados.parentesco) { erro.textContent = 'Preencha o nome e o parentesco.'; erro.hidden = false; return; }
    if (!telefoneValido(dados.telefone)) { erro.textContent = 'O telefone não é válido (ex.: 912 345 678).'; erro.hidden = false; return; }
    if (dados.email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(dados.email)) { erro.textContent = 'O e-mail não é válido.'; erro.hidden = false; return; }
    await ocupado(form.querySelector('[type=submit]'), async () => {
      try {
        const pedido = await post(`/api/idosos/${st.id}/contactos-familiares/pedidos`, dados);
        pedirConfirmacao(st, pedido, () => form.reset());
      } catch (ex) { erro.textContent = ex.message; erro.hidden = false; }
    });
  });
}

/** Event-based gateway do BPMN: confirmação do utilizador ou timer de 1 minuto. */
function pedirConfirmacao(st, pedido, aoConfirmar) {
  const d = pedido.dados;
  let decidido = false;
  let relogio;
  const { caixa, fechar } = dialogo(`
    <div class="cabeca"><div data-anel>${anelContagem(ESPERA_CONTACTO, ESPERA_CONTACTO)}</div>
      <div><h2>Confirmar novo contacto</h2><p class="suave">Para ${esc(st.resumo.idoso.nome)}</p></div></div>
    <dl class="ficha"><dt>Nome</dt><dd>${esc(d.nome)}</dd><dt>Parentesco</dt><dd>${esc(d.parentesco)}</dd>
      <dt>Telefone</dt><dd>${esc(telefone(d.telefone))}</dd>${d.email ? `<dt>E-mail</dt><dd>${esc(d.email)}</dd>` : ''}</dl>
    <div class="mensagem-erro" hidden data-erro></div>
    <div class="acoes" data-botoes><button class="btn btn--sec" data-nao>Cancelar</button><button class="btn" data-sim autofocus>Confirmar contacto</button></div>`,
  { aoFechar: () => { clearInterval(relogio); if (!decidido) responder(false, true); } });

  const anel = caixa.querySelector('[data-anel]');
  const inicio = new Date(pedido.criadoEm).getTime();
  const tick = () => {
    const restante = Math.max(0, Math.ceil(ESPERA_CONTACTO - (Date.now() - inicio) / 1000));
    anel.innerHTML = anelContagem(restante, ESPERA_CONTACTO);
    if (restante === 0) {
      clearInterval(relogio);
      decidido = true;
      caixa.querySelector('[data-erro]').textContent = 'O tempo para confirmar terminou e o contacto não foi adicionado. Pode voltar a adicioná-lo.';
      caixa.querySelector('[data-erro]').hidden = false;
      caixa.querySelector('[data-botoes]').innerHTML = '<button class="btn" data-fechar>Fechar</button>';
      caixa.querySelector('[data-fechar]').addEventListener('click', fechar);
    }
  };
  relogio = setInterval(tick, 1000);

  async function responder(confirmado, silencioso = false) {
    decidido = true;
    try {
      await post(`/api/idosos/${st.id}/contactos-familiares/pedidos/${pedido.id}/confirmacao`, { confirmado });
      if (!silencioso) {
        avisar(confirmado ? `${d.nome} foi adicionado aos contactos familiares.` : 'Adição cancelada.');
      }
      if (confirmado) aoConfirmar?.();
      await st.recarregar();
    } catch (ex) {
      if (!silencioso) avisar(ex instanceof ErroApi && ex.status === 410 ? 'O tempo para confirmar terminou. Volte a adicionar o contacto.' : ex.message, 'erro');
    }
  }
  caixa.querySelector('[data-sim]').addEventListener('click', async (e) => {
    await ocupado(e.currentTarget, () => responder(true));
    fechar();
  });
  caixa.querySelector('[data-nao]').addEventListener('click', async () => {
    await responder(false);
    fechar();
  });
}

/* ---------------------------------------------------------------------------
   Documentos
--------------------------------------------------------------------------- */

function tipoBadge(nome) {
  const ext = nome.split('.').pop().toLowerCase();
  const classe = ext === 'pdf' ? 'pdf' : ['png', 'jpg', 'jpeg'].includes(ext) ? 'img' : ['txt', 'csv'].includes(ext) ? 'txt' : '';
  return `<span class="tipo-ficheiro ${classe ? `tipo-ficheiro--${classe}` : ''}" aria-hidden="true">${esc(ext.toUpperCase().slice(0, 4))}</span>`;
}

async function secaoDocumentos(st) {
  let filtro = null;
  st.alvo.innerHTML = `
    <section class="painel">
      <div class="painel__topo"><div><h2>Documentos</h2>
        <p class="suave pequeno" style="margin-top:.25rem">Pasta de ${esc(st.resumo.idoso.nome)}: <span class="num">storage/idosos/idoso-${esc(st.resumo.idoso.nif)}/</span></p></div></div>
      <div class="zona-largar" data-zona>
        <span class="icone-redondo">${icone('enviar')}</span>
        <div><b>Arraste ficheiros para aqui</b><p class="suave pequeno">PDF, imagens, Word, Excel ou texto, até 10 MB.</p></div>
        <div class="controlos">
          <label class="vh" for="categoria-envio">Guardar em</label>
          <select id="categoria-envio" data-categoria>${Object.entries(CATEGORIA).map(([k, v]) => `<option value="${k}">${esc(v)}</option>`).join('')}</select>
          <label class="btn btn--sec" style="cursor:pointer">Escolher ficheiros<input type="file" multiple hidden data-ficheiro accept=".pdf,.png,.jpg,.jpeg,.txt,.doc,.docx,.xls,.xlsx,.csv,.odt"></label>
        </div>
      </div>
      <div class="filtros" data-filtros role="group" aria-label="Filtrar por categoria"></div>
      <ul class="lista" data-lista></ul>
    </section>`;

  const zona = st.alvo.querySelector('[data-zona]');
  const lista = st.alvo.querySelector('[data-lista]');
  const filtros = st.alvo.querySelector('[data-filtros]');
  const seletor = st.alvo.querySelector('[data-categoria]');
  const base = `/api/idosos/${st.id}/documentos`;

  async function enviar(ficheiros) {
    for (const f of ficheiros) {
      const fd = new FormData();
      fd.append('categoria', seletor.value);
      fd.append('ficheiro', f);
      try {
        const d = await enviarFicheiro(base, fd);
        avisar(`${d.nome} guardado em ${CATEGORIA[d.categoria]}.`);
      } catch (ex) { avisar(`${f.name}: ${ex.message}`, 'erro'); }
    }
    await desenharLista();
  }

  st.alvo.querySelector('[data-ficheiro]').addEventListener('change', (e) => { enviar([...e.target.files]); e.target.value = ''; });
  ['dragenter', 'dragover'].forEach((t) => zona.addEventListener(t, (e) => { e.preventDefault(); zona.classList.add('ativa'); }));
  ['dragleave', 'drop'].forEach((t) => zona.addEventListener(t, (e) => { e.preventDefault(); zona.classList.remove('ativa'); }));
  zona.addEventListener('drop', (e) => enviar([...e.dataTransfer.files]));

  async function desenharLista() {
    const todos = await get(base);
    const contagem = (k) => todos.filter((d) => d.categoria === k).length;
    filtros.innerHTML = [['', 'Todos', todos.length], ...Object.entries(CATEGORIA).map(([k, v]) => [k, v, contagem(k)])]
      .map(([k, v, n]) => `<button type="button" class="chip" data-filtro="${k}" aria-pressed="${(filtro || '') === k}">${esc(v)}<span>${n}</span></button>`).join('');
    filtros.querySelectorAll('[data-filtro]').forEach((b) => b.addEventListener('click', () => { filtro = b.dataset.filtro || null; desenharLista(); }));

    const visiveis = todos.filter((d) => !filtro || d.categoria === filtro);
    lista.innerHTML = visiveis.length ? visiveis.map((d) => `<li data-cat="${d.categoria}" data-nome="${esc(d.nome)}">
      ${tipoBadge(d.nome)}
      <div><p class="titulo">${esc(d.nome)}</p>
        <p class="detalhe"><span>${esc(CATEGORIA[d.categoria])}</span><span>${esc(tamanho(d.tamanhoBytes))}</span><span title="${esc(dataCompleta(d.carregadoEm))}">${esc(dataHora(d.carregadoEm))}</span></p></div>
      <div class="acoes-linha">
        <button class="btn btn--sec btn--icone" data-abrir title="Abrir">${icone('olho')}<span class="vh">Abrir ${esc(d.nome)}</span></button>
        <button class="btn btn--sec btn--icone" data-descarregar title="Descarregar">${icone('descarregar')}<span class="vh">Descarregar ${esc(d.nome)}</span></button>
        <button class="btn btn--sec btn--icone" data-apagar title="Apagar">${icone('lixo')}<span class="vh">Apagar ${esc(d.nome)}</span></button>
      </div></li>`).join('')
      : `<li><span></span><div class="vazio" style="padding:0"><b>${filtro ? `Sem documentos em ${esc(CATEGORIA[filtro])}` : 'A pasta está vazia'}</b><p>Arraste um ficheiro para a zona acima para o guardar.</p></div><span></span></li>`;

    lista.querySelectorAll('li[data-nome]').forEach((li) => {
      const url = `${base}/${li.dataset.cat}/${encodeURIComponent(li.dataset.nome)}`;
      li.querySelector('[data-abrir]').addEventListener('click', async () => {
        const janela = window.open('', '_blank');
        try {
          const blob = await api('GET', `${url}?inline=true`);
          const u = URL.createObjectURL(blob);
          if (janela) janela.location = u; else window.location.assign(u);
        } catch (ex) { janela?.close(); avisar(ex.message, 'erro'); }
      });
      li.querySelector('[data-descarregar]').addEventListener('click', async () => {
        try {
          const blob = await api('GET', url);
          const a = document.createElement('a');
          a.href = URL.createObjectURL(blob);
          a.download = li.dataset.nome;
          document.body.append(a);
          a.click();
          a.remove();
          setTimeout(() => URL.revokeObjectURL(a.href), 4000);
        } catch (ex) { avisar(ex.message, 'erro'); }
      });
      li.querySelector('[data-apagar]').addEventListener('click', async () => {
        if (!await confirmar('Apagar documento?', `${li.dataset.nome} vai ser apagado da pasta do idoso. Não é possível desfazer.`, 'Apagar', true)) return;
        try {
          await apagar(url);
          avisar('Documento apagado.');
          await desenharLista();
        } catch (ex) { avisar(ex.message, 'erro'); }
      });
    });
  }
  st.atualizar = null; // não interromper uma escolha de ficheiro com o polling
  await desenharLista();
}

/* ---------------------------------------------------------------------------
   Mensagens (o que o CPS enviou a cada participante)
--------------------------------------------------------------------------- */

async function secaoMensagens(st) {
  let filtro = '';
  st.alvo.innerHTML = `<section class="painel">
    <div><h2>Mensagens enviadas pelo sistema</h2><p class="suave pequeno" style="margin-top:.25rem">Tudo o que o CPS comunicou sobre ${esc(st.resumo.idoso.nome)}: ao idoso, à família e à enfermagem.</p></div>
    <div class="filtros" data-filtros role="group" aria-label="Filtrar por destinatário"></div>
    <ul class="linha-tempo" data-lista></ul></section>`;
  const lista = st.alvo.querySelector('[data-lista]');
  const filtros = st.alvo.querySelector('[data-filtros]');
  st.atualizar = async () => {
    const notas = await get(`/api/notificacoes?idosoId=${st.id}`);
    filtros.innerHTML = [['', 'Todas'], ['IDOSO', 'Idoso'], ['FAMILIA', 'Família'], ['ENFERMEIRO', 'Enfermagem'], ['UTILIZADOR', 'Pedidos']]
      .map(([k, v]) => `<button type="button" class="chip" data-f="${k}" aria-pressed="${filtro === k}">${esc(v)}</button>`).join('');
    filtros.querySelectorAll('[data-f]').forEach((b) => b.addEventListener('click', () => { filtro = b.dataset.f; st.atualizar(); }));
    const vis = notas.filter((n) => !filtro || n.destinatario === filtro);
    lista.innerHTML = vis.length ? vis.map(itemNotificacao).join('')
      : `<li><span class="icone-redondo icone-redondo--neutro">${icone('mensagem')}</span><div><p>Sem mensagens.</p></div></li>`;
  };
  await st.atualizar();
}
