// Utilitários de interface: escape, formatação, ícones, avisos e diálogos.

export const esc = (v) => String(v ?? '')
  .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;').replaceAll("'", '&#39;');

const P = {
  pulso: '<path d="M3 12h4l2-5 4 11 3-8 1.5 2H21"/>',
  queda: '<circle cx="13" cy="4.5" r="2"/><path d="M11 8l-3 5 4 2-2 6"/><path d="M8 13l-4 1"/><path d="M12 15l5 1 2 4"/>',
  pessoas: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c.6-3.6 3-5.6 6-5.6s5.4 2 6 5.6"/><circle cx="17" cy="9" r="2.5"/><path d="M16.5 14.3c2.4.2 4 1.9 4.5 4.7"/>',
  pasta: '<path d="M3 7.5A1.5 1.5 0 0 1 4.5 6H9l2 2h8.5A1.5 1.5 0 0 1 21 9.5v8a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z"/>',
  mensagem: '<path d="M4 5h16v11H9l-5 4z"/>',
  casa: '<path d="M4 11l8-6 8 6v8.5a.5.5 0 0 1-.5.5H15v-6H9v6H4.5a.5.5 0 0 1-.5-.5z"/>',
  sair: '<path d="M14 4h4.5a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1-1.5 1.5H14"/><path d="M10 8l-4 4 4 4"/><path d="M6 12h10"/>',
  mais: '<path d="M12 5v14M5 12h14"/>',
  enviar: '<path d="M12 16V4"/><path d="M7 9l5-5 5 5"/><path d="M5 15v4h14v-4"/>',
  descarregar: '<path d="M12 4v12"/><path d="M7 11l5 5 5-5"/><path d="M5 20h14"/>',
  lixo: '<path d="M5 7h14"/><path d="M9 7V5h6v2"/><path d="M7 7l1 12h8l1-12"/>',
  olho: '<path d="M2.5 12s3.5-6.5 9.5-6.5S21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="2.8"/>',
  visto: '<path d="M5 12.5l4.2 4.2L19 7"/>',
  fechar: '<path d="M6 6l12 12M18 6L6 18"/>',
  relogio: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  aviso: '<path d="M12 4l9 16H3z"/><path d="M12 10v4"/><path d="M12 17v.5"/>',
  equipa: '<path d="M12 3.5l7 3v5.2c0 4.3-3 7.6-7 8.8-4-1.2-7-4.5-7-8.8V6.5z"/><path d="M12 9v6M9 12h6"/>',
  servicos: '<rect x="4" y="4" width="7" height="7" rx="1.5"/><rect x="13" y="4" width="7" height="7" rx="1.5"/><rect x="4" y="13" width="7" height="7" rx="1.5"/><rect x="13" y="13" width="7" height="7" rx="1.5"/>',
  sensor: '<rect x="7" y="7" width="10" height="10" rx="2.5"/><path d="M4 9.5a8 8 0 0 1 0 5M20 9.5a8 8 0 0 1 0 5"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  jogo: '<rect x="4" y="4" width="16" height="16" rx="2"/><circle cx="9" cy="9" r="1.2"/><circle cx="15" cy="15" r="1.2"/><circle cx="15" cy="9" r="1.2"/><circle cx="9" cy="15" r="1.2"/>',
  calendario: '<rect x="4" y="5.5" width="16" height="14.5" rx="2"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4"/>',
  igreja: '<path d="M12 3v4M10 5h4"/><path d="M7 21V12l5-4 5 4v9"/><path d="M10.5 21v-4h3v4"/>',
  chave: '<path d="M14.5 4.5a5 5 0 0 0-5.7 6.7L4 16v4h4v-2h2v-2h2l1.3-1.3a5 5 0 0 0 1.2-10.2z"/>',
  refeicao: '<path d="M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10"/><path d="M16 21V3c2.2 1.3 3 3.6 3 6.5V13h-3"/>',
  coracao: '<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"/>',
};

export function icone(nome, classe = '') {
  return `<svg class="${classe}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[nome] || ''}</svg>`;
}

export const marcaSvg = `<svg viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="9" fill="#1E6B5C"/><path d="M5 17h6l2.5-6 4 11 3-8 1.5 3H27" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

/* ---------- Datas ---------- */

const fmtData = new Intl.DateTimeFormat('pt-PT', { day: 'numeric', month: 'short' });
const fmtHora = new Intl.DateTimeFormat('pt-PT', { hour: '2-digit', minute: '2-digit' });
const fmtCompleta = new Intl.DateTimeFormat('pt-PT', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });

export const dataHora = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  const hoje = new Date();
  const mesmoDia = d.toDateString() === hoje.toDateString();
  return mesmoDia ? `hoje, ${fmtHora.format(d)}` : `${fmtData.format(d)}, ${fmtHora.format(d)}`;
};
export const dataCompleta = (iso) => (iso ? fmtCompleta.format(new Date(iso)) : '');
export const dataCurta = (iso) => fmtData.format(new Date(iso));

export function haQuanto(iso) {
  const s = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 45) return 'agora mesmo';
  const m = Math.round(s / 60);
  if (m < 60) return `há ${m} min`;
  const h = Math.round(m / 60);
  if (h < 24) return `há ${h} h`;
  const d = Math.round(h / 24);
  return d === 1 ? 'ontem' : `há ${d} dias`;
}

export function idade(dataNascimento) {
  if (!dataNascimento) return null;
  const n = new Date(dataNascimento);
  const h = new Date();
  let a = h.getFullYear() - n.getFullYear();
  if (h.getMonth() < n.getMonth() || (h.getMonth() === n.getMonth() && h.getDate() < n.getDate())) a -= 1;
  return a;
}

export const tamanho = (bytes) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
};

export const telefone = (t) => (t && /^\d{9}$/.test(t) ? `${t.slice(0, 3)} ${t.slice(3, 6)} ${t.slice(6)}` : t || '');

/* ---------- Textos de domínio ---------- */

export const PAPEL = { FAMILIAR: 'Família', IDOSO: 'Idoso', ENFERMEIRO: 'Enfermagem', ADMIN: 'Administração' };
export const CLASSIFICACAO = {
  NORMAL: { texto: 'Normal', tom: '' },
  ALERTA_NAO_GRAVE: { texto: 'Fora do normal', tom: 'atencao' },
  ALERTA_GRAVE: { texto: 'Grave', tom: 'alerta' },
};
export const ESTADO_QUEDA = {
  AGUARDA_RESPOSTA_IDOSO: { texto: 'À espera do idoso', tom: 'alerta' },
  ENFERMEIRO_ALERTADO: { texto: 'Enfermagem alertada', tom: 'alerta' },
  CANCELADO: { texto: 'Cancelado: está bem', tom: 'neutro' },
  INCIDENTE_REGISTADO: { texto: 'Incidente registado', tom: '' },
};
export const CATEGORIA = {
  HISTORICO_CLINICO: 'Histórico clínico',
  EXAMES: 'Exames',
  RECEITAS: 'Receitas',
  RELATORIOS: 'Relatórios',
  OUTROS: 'Outros',
};
export const DESTINATARIO = { UTILIZADOR: 'Para quem fez o pedido', IDOSO: 'Para o idoso', FAMILIA: 'Para a família', ENFERMEIRO: 'Para a enfermagem' };

export const estadoPill = (info) => `<span class="estado ${info.tom ? `estado--${info.tom}` : ''}">${esc(info.texto)}</span>`;

/* ---------- Validações iguais às do back-end ---------- */

export function nifValido(bruto) {
  const nif = String(bruto || '').replace(/[\s.-]/g, '');
  if (!/^\d{9}$/.test(nif)) return false;
  if (!'1235689'.includes(nif[0]) && !['45', '70', '71', '72', '74', '75', '77', '79'].includes(nif.slice(0, 2))) return false;
  let soma = 0;
  for (let i = 0; i < 8; i += 1) soma += Number(nif[i]) * (9 - i);
  let c = 11 - (soma % 11);
  if (c >= 10) c = 0;
  return c === Number(nif[8]);
}

export function telefoneValido(bruto) {
  let t = String(bruto || '').replace(/[\s.-]/g, '');
  if (t.startsWith('+351')) t = t.slice(4);
  else if (t.startsWith('00351')) t = t.slice(5);
  return /^(9[1236]\d{7}|2\d{8})$/.test(t);
}

/* ---------- Avisos (toasts) ---------- */

export function avisar(texto, tipo = 'ok') {
  const caixa = document.getElementById('avisos');
  const el = document.createElement('div');
  el.className = `aviso ${tipo === 'erro' ? 'aviso--erro' : ''}`;
  el.innerHTML = `${icone(tipo === 'erro' ? 'aviso' : 'visto')}<p>${esc(texto)}</p>`;
  caixa.append(el);
  setTimeout(() => el.remove(), tipo === 'erro' ? 7000 : 4500);
}

/* ---------- Diálogo ---------- */

export function dialogo(html, { aoFechar } = {}) {
  const anterior = document.activeElement;
  const fundo = document.createElement('div');
  fundo.className = 'sobreposicao';
  fundo.innerHTML = `<div class="dialogo" role="dialog" aria-modal="true">${html}</div>`;
  document.body.append(fundo);
  const caixa = fundo.firstElementChild;

  const fechar = () => {
    document.removeEventListener('keydown', tecla);
    fundo.remove();
    aoFechar?.();
    anterior?.focus?.();
  };
  const tecla = (e) => { if (e.key === 'Escape') fechar(); };
  document.addEventListener('keydown', tecla);
  fundo.addEventListener('mousedown', (e) => { if (e.target === fundo) fechar(); });
  caixa.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', fechar));
  setTimeout(() => (caixa.querySelector('[autofocus], input, textarea, button.btn') || caixa).focus(), 30);
  return { caixa, fechar };
}

export function confirmar(titulo, texto, rotuloSim = 'Confirmar', perigo = false) {
  return new Promise((resolver) => {
    let resposta = false;
    const { caixa, fechar } = dialogo(`
      <h2>${esc(titulo)}</h2>
      <p class="suave">${esc(texto)}</p>
      <div class="acoes">
        <button class="btn btn--sec" data-fechar>Cancelar</button>
        <button class="btn ${perigo ? 'btn--perigo' : ''}" data-sim>${esc(rotuloSim)}</button>
      </div>`, { aoFechar: () => resolver(resposta) });
    caixa.querySelector('[data-sim]').addEventListener('click', () => { resposta = true; fechar(); });
  });
}

/** Desativa o botão enquanto a promessa corre (evita pedidos duplicados). */
export async function ocupado(botao, fn) {
  const texto = botao?.innerHTML;
  if (botao) { botao.disabled = true; }
  try { return await fn(); } finally { if (botao) { botao.disabled = false; botao.innerHTML = texto; } }
}

/** Anel de contagem decrescente (usado na queda e na confirmação de contacto). */
export function anelContagem(restante, total) {
  const r = 27;
  const c = 2 * Math.PI * r;
  const frac = Math.max(0, Math.min(1, restante / total));
  return `<div class="contagem" role="timer" aria-label="${restante} segundos">
    <svg viewBox="0 0 64 64"><circle class="fundo" cx="32" cy="32" r="${r}"/><circle class="frente" cx="32" cy="32" r="${r}" stroke-dasharray="${c.toFixed(1)}" stroke-dashoffset="${(c * (1 - frac)).toFixed(1)}"/></svg>
    <b class="num">${restante}</b></div>`;
}
