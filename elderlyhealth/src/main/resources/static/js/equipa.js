// Painel de enfermagem / administração: alertas ativos de todos os idosos e lista geral.

import { get } from './api.js';
import {
  CLASSIFICACAO, DESTINATARIO, anelContagem, dataHora, esc, estadoPill, haQuanto, icone, idade,
} from './ui.js';
import { abrirRegistoAtendimento } from './idoso.js';

const ESPERA_QUEDA = 60;

export async function vistaEquipa(ctx) {
  if (!ctx.equipa) { location.hash = '#/inicio'; return; }
  const main = ctx.conteudo;
  main.innerHTML = `
    <div class="cabecalho-pagina">
      <div><h1>Painel de enfermagem</h1><div class="resumo-numeros" data-numeros style="margin-top:.6rem"></div></div>
      <a class="btn btn--sec" href="#/inscrever">${icone('mais')}Inscrever idoso</a>
    </div>
    <section class="painel"><h2>Precisa de atenção</h2><div data-alertas></div></section>
    <section class="painel"><h2>Idosos</h2><div class="tabela-wrap" data-idosos></div></section>
    <section class="painel"><div class="painel__topo"><h2>Mensagens para a enfermagem</h2></div><ul class="linha-tempo" data-mensagens></ul></section>`;

  const st = { idosos: ctx.idosos, quedas: [], mensagens: [] };
  const alvo = (k) => main.querySelector(`[data-${k}]`);

  async function carregar() {
    const [idosos, quedas, mensagens] = await Promise.all([
      get('/api/idosos'), get('/api/quedas'), get('/api/notificacoes?destinatario=ENFERMEIRO'),
    ]);
    Object.assign(st, { idosos, quedas, mensagens });
  }

  function desenhar() {
    const nomes = Object.fromEntries(st.idosos.map((r) => [r.idoso.id, r.idoso]));
    const ativas = st.quedas.filter((q) => q.estado === 'AGUARDA_RESPOSTA_IDOSO' || q.estado === 'ENFERMEIRO_ALERTADO')
      .sort((a, b) => (a.estado === 'ENFERMEIRO_ALERTADO' ? -1 : 1) - (b.estado === 'ENFERMEIRO_ALERTADO' ? -1 : 1));
    const graves = st.idosos.filter((r) => r.ultimaMedicao?.classificacao === 'ALERTA_GRAVE');

    alvo('numeros').innerHTML = `
      <span><b>${st.idosos.length}</b>${st.idosos.length === 1 ? 'idoso acompanhado' : 'idosos acompanhados'}</span>
      <span><b>${ativas.length}</b>${ativas.length === 1 ? 'alerta de queda' : 'alertas de queda'}</span>
      <span><b>${graves.length}</b>com tensão grave</span>`;

    const linhasQueda = ativas.map((q) => {
      const i = nomes[q.idosoId] || {};
      if (q.estado === 'AGUARDA_RESPOSTA_IDOSO') {
        const restante = Math.max(0, Math.ceil(ESPERA_QUEDA - (Date.now() - new Date(q.detetadoEm).getTime()) / 1000));
        return `<div class="alerta-linha" data-detetado="${esc(q.detetadoEm)}">
          <div data-anel>${anelContagem(restante, ESPERA_QUEDA)}</div>
          <div><p class="titulo"><b>Possível queda: ${esc(i.nome)}</b></p>
            <p class="suave pequeno">Quarto ${esc(q.quarto || '—')}. À espera da resposta do idoso; se não responder, o alerta passa para a enfermagem.</p></div>
          <a class="btn btn--sec" href="#/idoso/${q.idosoId}">Abrir ficha</a></div>`;
      }
      return `<div class="alerta-linha" data-queda="${q.id}">
        <span class="icone-redondo icone-redondo--alerta">${icone('queda')}</span>
        <div><p><b>Queda: ${esc(i.nome)}, quarto ${esc(q.quarto || '—')}</b></p>
          <p class="suave pequeno">Alerta ${esc(haQuanto(q.enfermeiroAlertadoEm))}, ${q.escaladoPorTimeout ? 'sem resposta do idoso' : 'o idoso pediu ajuda'}. Info médica: ${esc(i.informacaoMedica || '—')}</p></div>
        <button class="btn" data-atendimento>Registar atendimento</button></div>`;
    });
    const linhasTensao = graves.map((r) => `<div class="alerta-linha alerta-linha--atencao">
      <span class="icone-redondo icone-redondo--atencao">${icone('pulso')}</span>
      <div><p><b>Tensão ${r.ultimaMedicao.sistolica}/${r.ultimaMedicao.diastolica}: ${esc(r.idoso.nome)}</b></p>
        <p class="suave pequeno">Quarto ${esc(r.idoso.quarto || '—')}, medição ${esc(haQuanto(r.ultimaMedicao.medidoEm))}.</p></div>
      <a class="btn btn--sec" href="#/idoso/${r.idoso.id}/tensao">Ver tensão</a></div>`);
    alvo('alertas').innerHTML = [...linhasQueda, ...linhasTensao].join('')
      || '<div class="vazio" style="padding-top:0"><b>Sem alertas ativos</b><p>Os alertas novos aparecem aqui automaticamente.</p></div>';
    alvo('alertas').querySelectorAll('[data-atendimento]').forEach((b) => b.addEventListener('click', () => {
      const q = st.quedas.find((x) => x.id === Number(b.closest('[data-queda]').dataset.queda));
      abrirRegistoAtendimento(q, ctx, nomes[q.idosoId], async () => { await carregar(); desenhar(); });
    }));

    alvo('idosos').innerHTML = st.idosos.length ? `<table>
      <thead><tr><th>Nome</th><th>Quarto</th><th>Idade</th><th>Última tensão</th><th>Alertas</th><th>Contactos</th></tr></thead>
      <tbody>${st.idosos.map((r) => {
        const t = r.ultimaMedicao;
        return `<tr class="linha-clicavel" data-ir="${r.idoso.id}">
          <td><a href="#/idoso/${r.idoso.id}"><b>${esc(r.idoso.nome)}</b></a></td>
          <td>${esc(r.idoso.quarto || '—')}</td>
          <td class="num">${idade(r.idoso.dataNascimento) ?? '—'}</td>
          <td>${t ? `<span class="num"><b>${t.sistolica}/${t.diastolica}</b></span> ${estadoPill(CLASSIFICACAO[t.classificacao])}` : '<span class="suave">—</span>'}</td>
          <td>${r.quedasAtivas ? `<span class="estado estado--alerta">${r.quedasAtivas} queda</span>` : '<span class="suave">—</span>'}</td>
          <td class="num">${r.contactosFamiliares}</td></tr>`;
      }).join('')}</tbody></table>`
      : '<div class="vazio"><b>Ainda não há idosos inscritos</b><a class="btn" href="#/inscrever">Inscrever idoso</a></div>';
    alvo('idosos').querySelectorAll('[data-ir]').forEach((tr) => tr.addEventListener('click', (e) => {
      if (e.target.closest('a')) return;
      location.hash = `#/idoso/${tr.dataset.ir}`;
    }));

    alvo('mensagens').innerHTML = st.mensagens.length ? st.mensagens.slice(0, 8).map((n) => `<li>
      <span class="icone-redondo ${/URGENTE|QUEDA/.test(n.mensagem) ? 'icone-redondo--alerta' : ''}">${icone(n.referencia?.startsWith('QUEDA') ? 'queda' : 'pulso')}</span>
      <div><p>${esc(n.mensagem)}</p><p><time>${esc(dataHora(n.criadaEm))}</time><span class="para">, ${esc(DESTINATARIO[n.destinatario].toLowerCase())}</span></p></div></li>`).join('')
      : `<li><span class="icone-redondo icone-redondo--neutro">${icone('mensagem')}</span><div><p>Sem mensagens para a enfermagem.</p></div></li>`;
  }

  await carregar();
  desenhar();
  const polling = setInterval(async () => { try { await carregar(); desenhar(); } catch { /* tenta de novo */ } }, 5000);
  const relogio = setInterval(() => {
    main.querySelectorAll('[data-detetado]').forEach((el) => {
      const r = Math.max(0, Math.ceil(ESPERA_QUEDA - (Date.now() - new Date(el.dataset.detetado).getTime()) / 1000));
      el.querySelector('[data-anel]').innerHTML = anelContagem(r, ESPERA_QUEDA);
    });
  }, 1000);
  ctx.aoSair(() => { clearInterval(polling); clearInterval(relogio); });
}
