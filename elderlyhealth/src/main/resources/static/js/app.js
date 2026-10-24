// ElderlyHealth — aplicação (router, sessão, barra lateral, entrar, criar conta, inscrever idoso).
// Fluxo do main.bpmn: Aceder à aplicação -> "Já tem conta?" -> Não: criar conta + inscrever idoso
// -> Realizar autenticação -> Menu principal -> feature escolhida -> Terminar sessão.

import { ErroApi, get, post, put, sessao } from './api.js';
import {
  PAPEL, avisar, esc, icone, marcaSvg, nifValido, ocupado, telefoneValido,
} from './ui.js';
import { vistaIdoso } from './idoso.js';
import { vistaEquipa } from './equipa.js';

const app = document.getElementById('app');
let limpezas = [];
const aoSair = (fn) => limpezas.push(fn);

const ROTAS = [
  { re: /^#\/entrar$/, publica: true, vista: vistaEntrar },
  { re: /^#\/criar-conta$/, publica: true, vista: vistaCriarConta },
  { re: /^#\/inicio$/, vista: vistaInicio },
  { re: /^#\/inscrever$/, vista: vistaInscrever },
  { re: /^#\/equipa$/, vista: (m, ctx) => vistaEquipa(ctx) },
  { re: /^#\/servicos$/, vista: vistaServicos },
  { re: /^#\/idoso\/(\d+)(?:\/([a-z]+))?$/, vista: (m, ctx) => vistaIdoso(ctx, Number(m[1]), m[2] || 'resumo') },
];

window.addEventListener('hashchange', navegar);
window.addEventListener('sessao-expirada', () => {
  avisar('A sessão terminou. Entre novamente.', 'erro');
  location.hash = '#/entrar';
});
navegar();

async function navegar() {
  limpezas.forEach((fn) => fn());
  limpezas = [];
  const hash = location.hash || '#/inicio';
  const rota = ROTAS.find((r) => r.re.test(hash));
  if (!rota) { location.hash = '#/inicio'; return; }
  const m = hash.match(rota.re);
  const s = sessao.obter();

  if (rota.publica) {
    if (s?.token && hash === '#/entrar') { location.hash = '#/inicio'; return; }
    rota.vista(m);
    window.scrollTo(0, 0);
    return;
  }
  if (!s?.token) { location.hash = '#/entrar'; return; }

  try {
    const ctx = await montarAplicacao(hash);
    await rota.vista(m, ctx);
  } catch (e) {
    if (e instanceof ErroApi && e.status === 401) return;
    const main = document.getElementById('conteudo');
    const html = `<div class="painel"><h2>Não foi possível abrir esta página</h2><p class="suave">${esc(e.message)}</p>
      <div><a class="btn btn--sec" href="#/inicio">Voltar ao início</a></div></div>`;
    if (main) main.innerHTML = html; else app.innerHTML = `<main class="conteudo">${html}</main>`;
  }
}

/* ---------------------------------------------------------------------------
   Barra lateral + área de conteúdo (todas as páginas com sessão iniciada)
--------------------------------------------------------------------------- */

function tomDoIdoso(r) {
  if (r.quedasAtivas > 0 || r.ultimaMedicao?.classificacao === 'ALERTA_GRAVE') return 'alerta';
  if (r.ultimaMedicao?.classificacao === 'ALERTA_NAO_GRAVE') return 'atencao';
  return 'calma';
}

async function montarAplicacao(hash) {
  const [utilizador, idosos] = await Promise.all([get('/api/auth/eu'), get('/api/idosos')]);
  const s = sessao.obter();
  sessao.guardar({ ...s, utilizador });
  const equipa = utilizador.papel === 'ENFERMEIRO' || utilizador.papel === 'ADMIN';
  const atual = (href) => (hash === href || hash.startsWith(`${href}/`) ? 'aria-current="page"' : '');

  const ligacoesIdosos = idosos.map((r) => {
    const tom = tomDoIdoso(r);
    return `<a class="idoso-ligacao" href="#/idoso/${r.idoso.id}" ${atual(`#/idoso/${r.idoso.id}`)}>
      <span>${esc(r.idoso.nome)}</span>
      <span style="display:inline-flex;align-items:center;gap:.5rem"><small>${esc(r.idoso.quarto || '')}</small>
      <span class="ponto ${tom !== 'calma' ? `ponto--${tom}` : ''}" title="${tom === 'alerta' ? 'Alerta ativo' : tom === 'atencao' ? 'Precisa de atenção' : 'Tudo calmo'}"></span></span></a>`;
  }).join('');

  app.innerHTML = `
    <div class="app">
      <aside class="lateral" id="lateral">
        <div class="lateral__topo">
          <a class="marca" href="#/inicio">${marcaSvg}<span>ElderlyHealth</span></a>
          <button class="btn btn--sec btn--icone menu-movel" aria-expanded="false" aria-controls="lateral" data-menu>${icone('menu')}<span class="vh">Abrir menu</span></button>
        </div>
        <div class="lateral__conta"><b>${esc(utilizador.nome)}</b><span>${esc(PAPEL[utilizador.papel])}</span></div>
        ${equipa ? `<nav aria-label="Equipa"><a href="#/equipa" ${atual('#/equipa')}>${icone('equipa')}Painel de enfermagem</a></nav>` : ''}
        <nav aria-label="Idosos">
          <h2>${equipa ? 'Todos os idosos' : 'Os seus idosos'}</h2>
          ${ligacoesIdosos || '<p class="pequeno" style="padding:0 .7rem;color:#9fbdb4">Ainda sem idosos inscritos.</p>'}
          <a href="#/inscrever" ${atual('#/inscrever')}>${icone('mais')}Inscrever idoso</a>
        </nav>
        <nav aria-label="Outros"><a href="#/servicos" ${atual('#/servicos')}>${icone('servicos')}Mais serviços</a></nav>
        <div class="lateral__fim">
          <button class="ligacao" data-sair>${icone('sair')}Terminar sessão</button>
        </div>
      </aside>
      <main class="conteudo" id="conteudo"><div class="carregar">A carregar…</div></main>
    </div>`;

  const lateral = app.querySelector('#lateral');
  const botaoMenu = app.querySelector('[data-menu]');
  botaoMenu.addEventListener('click', () => {
    const aberta = lateral.classList.toggle('aberta');
    botaoMenu.setAttribute('aria-expanded', String(aberta));
  });
  app.querySelector('[data-sair]').addEventListener('click', terminarSessao);

  return { utilizador, idosos, equipa, aoSair, conteudo: app.querySelector('#conteudo'), tomDoIdoso };
}

async function terminarSessao() {
  try { await post('/api/auth/logout'); } catch { /* sessão já inválida */ }
  sessao.limpar();
  location.hash = '#/entrar';
  avisar('Sessão terminada.');
}

/* ---------------------------------------------------------------------------
   Entrar
--------------------------------------------------------------------------- */

const DEMO = [
  { rotulo: 'Família (João Silva)', email: 'joao.silva@mail.pt', password: 'familia2026' },
  { rotulo: 'Enfermagem', email: 'enfermeira@elderlyhealth.pt', password: 'enfermagem2026' },
  { rotulo: 'Administração', email: 'admin@elderlyhealth.pt', password: 'admin2026' },
];

const tracoEntrada = `<svg class="entrada__traco" viewBox="0 0 800 130" preserveAspectRatio="none" aria-hidden="true">
  <path class="sombra" d="M0 78 H210 L232 78 L250 40 L272 112 L292 22 L314 92 L330 78 H520 L540 78 L556 58 L574 96 L590 78 H800"/>
  <path d="M0 78 H210 L232 78 L250 40 L272 112 L292 22 L314 92 L330 78 H520 L540 78 L556 58 L574 96 L590 78 H800"/></svg>`;

function vistaEntrar() {
  app.innerHTML = `
    <div class="entrada">
      <section class="entrada__mensagem">
        <a class="marca" href="#/entrar">${marcaSvg}<span>ElderlyHealth</span></a>
        <div>
          <h1 class="entrada__titulo">Saber que está tudo bem.</h1>
          <p class="entrada__texto">Acompanhe a tensão arterial, as quedas e os documentos de quem está ao cuidado do hospital, e receba um alerta quando é preciso.</p>
        </div>
        ${tracoEntrada}
        <p class="entrada__rodape">Protótipo do projeto MSP 2026/27, FCT NOVA.</p>
      </section>
      <section class="entrada__form">
        <form class="cartao-form" novalidate>
          <div>
            <h1>Entrar</h1>
            <p class="suave" style="margin-top:.4rem">Use o e-mail e a palavra-passe da sua conta.</p>
          </div>
          <div class="mensagem-erro" hidden data-erro></div>
          <label class="campo"><span>E-mail</span><input name="email" type="email" autocomplete="username" required></label>
          <label class="campo"><span>Palavra-passe</span><input name="password" type="password" autocomplete="current-password" required></label>
          <button class="btn btn--grande" type="submit">Entrar</button>
          <p>Ainda não tem conta? <a href="#/criar-conta">Criar conta e inscrever um idoso</a></p>
          <div class="demo">
            <p>Contas de demonstração (protótipo)</p>
            <div class="demo__lista">${DEMO.map((d, i) => `<button type="button" class="chip" data-demo="${i}" aria-pressed="false">${esc(d.rotulo)}</button>`).join('')}</div>
          </div>
        </form>
      </section>
    </div>`;

  const form = app.querySelector('form');
  const erro = form.querySelector('[data-erro]');
  form.querySelectorAll('[data-demo]').forEach((b) => b.addEventListener('click', () => {
    const d = DEMO[Number(b.dataset.demo)];
    form.email.value = d.email;
    form.password.value = d.password;
    form.querySelectorAll('[data-demo]').forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
  }));
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    erro.hidden = true;
    if (!form.email.value.trim() || !form.password.value) {
      erro.textContent = 'Preencha o e-mail e a palavra-passe.';
      erro.hidden = false;
      return;
    }
    await ocupado(form.querySelector('[type=submit]'), async () => {
      try {
        const s = await post('/api/auth/login', { email: form.email.value, password: form.password.value });
        sessao.guardar(s);
        location.hash = s.utilizador.papel === 'ENFERMEIRO' || s.utilizador.papel === 'ADMIN' ? '#/equipa' : '#/inicio';
      } catch (ex) {
        erro.textContent = ex.message;
        erro.hidden = false;
        form.password.select();
      }
    });
  });
  form.email.focus();
}

/* ---------------------------------------------------------------------------
   Criar conta (público) e Inscrever idoso (com sessão)
--------------------------------------------------------------------------- */

function vistaCriarConta() {
  app.innerHTML = `
    <div class="assistente-pagina">
      <div class="assistente-topo">
        <a class="marca" href="#/entrar">${marcaSvg}<span>ElderlyHealth</span></a>
        <a href="#/entrar">Já tenho conta</a>
      </div>
      <div id="assistente"></div>
    </div>`;
  assistente(app.querySelector('#assistente'), {
    comConta: true,
    aoConcluir: (idosoId) => { location.hash = `#/idoso/${idosoId}`; },
  });
}

function vistaInscrever(m, ctx) {
  ctx.conteudo.innerHTML = '<div id="assistente"></div>';
  assistente(ctx.conteudo.querySelector('#assistente'), {
    comConta: false,
    utilizador: ctx.utilizador,
    aoConcluir: (idosoId) => { location.hash = `#/idoso/${idosoId}`; },
  });
}

/**
 * Assistente que segue o subprocesso "Registar conta do idoso":
 * dados (NIF validado) -> preferências + contacto de emergência (em paralelo no back-end) -> registado.
 */
function assistente(alvo, { comConta, utilizador, aoConcluir }) {
  const passos = [
    ...(comConta ? [{ id: 'conta', nome: 'A sua conta' }] : []),
    { id: 'idoso', nome: 'Dados do idoso' },
    { id: 'extra', nome: 'Preferências e emergência' },
  ];
  const st = { i: 0, conta: { papel: 'FAMILIAR' }, dados: {}, extra: {}, pedidoId: null, idoso: null };

  const cabecalho = (titulo, texto) => `
    <div><h1>${esc(titulo)}</h1>${texto ? `<p class="suave" style="margin-top:.6rem;max-width:60ch">${esc(texto)}</p>` : ''}</div>
    <ol class="passos" style="--n:${passos.length}">
      ${passos.map((p, k) => `<li data-estado="${st.idoso || k < st.i ? 'feito' : k === st.i ? 'atual' : ''}"><span>${esc(p.nome)}</span></li>`).join('')}
    </ol>`;

  const erroHtml = '<div class="mensagem-erro" hidden data-erro></div>';
  const mostrarErro = (form, msg) => {
    const el = form.querySelector('[data-erro]');
    el.textContent = msg;
    el.hidden = false;
    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  };

  async function garantirPedido() {
    if (st.pedidoId) return;
    const pedido = await post('/api/registos');
    st.pedidoId = pedido.id;
    if (st.dados.nif) {
      const r = await put(`/api/registos/${st.pedidoId}/dados`, st.dados);
      if (r.estado === 'NEGADO') { st.pedidoId = null; throw new Error(r.motivo); }
    }
  }

  function desenhar() {
    if (st.idoso) return desenharConcluido();
    const passo = passos[st.i].id;
    if (passo === 'conta') desenharConta();
    if (passo === 'idoso') desenharIdoso();
    if (passo === 'extra') desenharExtra();
    alvo.querySelector('input, select, textarea')?.focus();
  }

  function desenharConta() {
    const c = st.conta;
    alvo.innerHTML = `<div class="assistente">
      ${cabecalho('Criar conta', 'Primeiro a sua conta de acesso. A seguir inscreve o idoso que vai acompanhar.')}
      <form novalidate>
        ${erroHtml}
        <fieldset>
          <legend>Quem vai usar esta conta?</legend>
          <div class="escolhas">
            <label class="escolha"><input type="radio" name="papel" value="FAMILIAR" ${c.papel !== 'IDOSO' ? 'checked' : ''}><span><b>Sou familiar ou cuidador</b><i>Acompanho um ou mais idosos</i></span></label>
            <label class="escolha"><input type="radio" name="papel" value="IDOSO" ${c.papel === 'IDOSO' ? 'checked' : ''}><span><b>Sou o próprio idoso</b><i>Vou usar a aplicação para mim</i></span></label>
          </div>
        </fieldset>
        <div class="grelha-campos">
          <label class="campo largo"><span>Nome completo</span><input name="nome" autocomplete="name" value="${esc(c.nome)}" required></label>
          <label class="campo"><span>E-mail</span><input name="email" type="email" autocomplete="email" value="${esc(c.email)}" required></label>
          <label class="campo"><span>Palavra-passe</span><input name="password" type="password" autocomplete="new-password" required><small>Pelo menos 8 caracteres, com letras e números.</small></label>
        </div>
        <div class="acoes"><a class="esq" href="#/entrar">Cancelar</a><button class="btn btn--grande" type="submit">Criar conta e continuar</button></div>
      </form></div>`;
    const form = alvo.querySelector('form');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const papel = form.papel.value;
      Object.assign(st.conta, { nome: form.nome.value.trim(), email: form.email.value.trim(), papel });
      const pw = form.password.value;
      if (!st.conta.nome) return mostrarErro(form, 'Indique o seu nome.');
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(st.conta.email)) return mostrarErro(form, 'O e-mail não parece válido.');
      if (pw.length < 8 || !/[0-9]/.test(pw) || !/[A-Za-z]/.test(pw)) return mostrarErro(form, 'A palavra-passe tem de ter pelo menos 8 caracteres, com letras e números.');
      await ocupado(form.querySelector('[type=submit]'), async () => {
        try {
          const s = await post('/api/auth/registar', { ...st.conta, password: pw });
          sessao.guardar(s);
          if (papel === 'IDOSO' && !st.dados.nome) st.dados.nome = st.conta.nome;
          st.i += 1;
          desenhar();
        } catch (ex) { mostrarErro(form, ex.message); }
      });
    });
  }

  function desenharIdoso() {
    const d = st.dados;
    const titulo = comConta ? 'Inscrever o idoso' : 'Inscrever idoso';
    const texto = utilizador && (utilizador.papel === 'ENFERMEIRO' || utilizador.papel === 'ADMIN')
      ? 'Inscrição feita pela receção. O NIF é validado antes de criar a conta.'
      : 'Estes dados ficam na ficha do idoso. O NIF é validado antes de criar a conta.';
    alvo.innerHTML = `<div class="assistente">
      ${cabecalho(titulo, texto)}
      <form novalidate>
        ${erroHtml}
        <div class="grelha-campos">
          <label class="campo largo"><span>Nome do idoso</span><input name="nome" value="${esc(d.nome)}" required></label>
          <label class="campo"><span>NIF</span><input name="nif" inputmode="numeric" maxlength="11" value="${esc(d.nif)}" required><small>9 dígitos</small></label>
          <label class="campo"><span>Data de nascimento</span><input name="dataNascimento" type="date" value="${esc(d.dataNascimento)}" required></label>
          <label class="campo"><span>Quarto</span><input name="quarto" value="${esc(d.quarto)}" placeholder="Ex.: Q204"></label>
          <label class="campo largo"><span>Informação médica</span><textarea name="informacaoMedica" placeholder="Doenças, medicação habitual, mobilidade…">${esc(d.informacaoMedica)}</textarea></label>
        </div>
        <div class="acoes">
          ${comConta ? '' : '<a class="esq" href="#/inicio">Cancelar</a>'}
          <button class="btn btn--grande" type="submit">Continuar</button>
        </div>
      </form></div>`;
    const form = alvo.querySelector('form');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const novo = {
        nome: form.nome.value.trim(),
        nif: form.nif.value.replace(/\s/g, ''),
        dataNascimento: form.dataNascimento.value,
        quarto: form.quarto.value.trim() || null,
        informacaoMedica: form.informacaoMedica.value.trim() || null,
      };
      Object.assign(st.dados, novo);
      form.nif.setAttribute('aria-invalid', 'false');
      if (!novo.nome) return mostrarErro(form, 'Indique o nome do idoso.');
      if (!nifValido(novo.nif)) { form.nif.setAttribute('aria-invalid', 'true'); return mostrarErro(form, 'O NIF não é válido. Confirme os 9 dígitos.'); }
      if (!novo.dataNascimento || new Date(novo.dataNascimento) >= new Date()) return mostrarErro(form, 'Indique uma data de nascimento válida.');
      await ocupado(form.querySelector('[type=submit]'), async () => {
        try {
          st.pedidoId = null; // dados novos: começa um pedido de registo novo
          await garantirPedido();
          st.i += 1;
          desenhar();
        } catch (ex) { mostrarErro(form, ex.message); }
      });
    });
  }

  function desenharExtra() {
    const x = st.extra;
    alvo.innerHTML = `<div class="assistente">
      ${cabecalho('Preferências e contacto de emergência', `Para ${st.dados.nome}. O contacto de emergência é avisado se houver um problema grave.`)}
      <form novalidate>
        ${erroHtml}
        <fieldset>
          <legend>Preferências</legend>
          <div class="grelha-campos">
            <label class="campo"><span>Dieta</span><input name="dieta" value="${esc(x.dieta)}" placeholder="Ex.: hipossódica, sem açúcar"></label>
            <label class="campo"><span>Alergias</span><input name="alergias" value="${esc(x.alergias)}" placeholder="Separe por vírgulas"></label>
            <label class="campo largo"><span>Observações</span><input name="observacoes" value="${esc(x.observacoes)}" placeholder="Ex.: prefere visitas à tarde"></label>
          </div>
        </fieldset>
        <fieldset>
          <legend>Contacto de emergência</legend>
          <div class="grelha-campos">
            <label class="campo"><span>Nome</span><input name="ceNome" value="${esc(x.ceNome ?? (comConta ? st.conta.nome : ''))}" required></label>
            <label class="campo"><span>Telefone</span><input name="ceTelefone" type="tel" value="${esc(x.ceTelefone)}" placeholder="912 345 678" required></label>
            <label class="campo"><span>Parentesco</span><input name="ceParentesco" value="${esc(x.ceParentesco)}" placeholder="Ex.: Filho"></label>
          </div>
        </fieldset>
        <div class="acoes">
          <button class="btn btn--sec esq" type="button" data-voltar>Voltar</button>
          <button class="btn btn--grande" type="submit">Concluir inscrição</button>
        </div>
      </form></div>`;
    const form = alvo.querySelector('form');
    form.querySelector('[data-voltar]').addEventListener('click', () => { st.i -= 1; desenhar(); });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      Object.assign(st.extra, {
        dieta: form.dieta.value.trim(), alergias: form.alergias.value.trim(), observacoes: form.observacoes.value.trim(),
        ceNome: form.ceNome.value.trim(), ceTelefone: form.ceTelefone.value.trim(), ceParentesco: form.ceParentesco.value.trim(),
      });
      if (!st.extra.ceNome) return mostrarErro(form, 'Indique o nome do contacto de emergência.');
      if (!telefoneValido(st.extra.ceTelefone)) return mostrarErro(form, 'O telefone do contacto de emergência não é válido (ex.: 912 345 678).');
      const enviar = async () => {
        await garantirPedido();
        await put(`/api/registos/${st.pedidoId}/preferencias`, {
          dieta: st.extra.dieta || null,
          alergias: st.extra.alergias ? st.extra.alergias.split(',').map((a) => a.trim()).filter(Boolean) : [],
          observacoes: st.extra.observacoes || null,
        });
        return put(`/api/registos/${st.pedidoId}/contacto-emergencia`, {
          nome: st.extra.ceNome, telefone: st.extra.ceTelefone, parentesco: st.extra.ceParentesco || null,
        });
      };
      await ocupado(form.querySelector('[type=submit]'), async () => {
        try {
          let r;
          try {
            r = await enviar();
          } catch (ex) {
            if (!(ex instanceof ErroApi) || (ex.status !== 410 && ex.status !== 409)) throw ex;
            st.pedidoId = null; // o pedido expirou (timer de 20 min): repete com os mesmos dados
            r = await enviar();
          }
          if (r.estado === 'NEGADO') { st.pedidoId = null; return mostrarErro(form, r.motivo); }
          st.idoso = { id: r.idosoId, nome: st.dados.nome };
          desenhar();
        } catch (ex) { mostrarErro(form, ex.message); }
      });
    });
  }

  function desenharConcluido() {
    alvo.innerHTML = `<div class="assistente">
      ${cabecalho('Inscrição concluída', '')}
      <div class="painel concluido">
        <div class="selo">${icone('visto')}</div>
        <h2>${esc(st.idoso.nome)} já está na plataforma</h2>
        <p class="suave" style="max-width:56ch">A ficha foi criada, juntamente com a pasta de documentos. Pode agora acompanhar a tensão arterial, os alertas de queda, os contactos familiares e os documentos.</p>
        <button class="btn btn--grande" data-abrir>Abrir a ficha de ${esc(st.idoso.nome.split(' ')[0])}</button>
      </div></div>`;
    alvo.querySelector('[data-abrir]').addEventListener('click', () => aoConcluir(st.idoso.id));
    alvo.querySelector('[data-abrir]').focus();
  }

  desenhar();
}

/* ---------------------------------------------------------------------------
   Início e Mais serviços
--------------------------------------------------------------------------- */

function vistaInicio(m, ctx) {
  if (ctx.equipa) { location.hash = '#/equipa'; return; }
  if (ctx.idosos.length === 1) { location.replace(`#/idoso/${ctx.idosos[0].idoso.id}`); return; }
  const primeiro = ctx.utilizador.nome.split(' ')[0];
  if (ctx.idosos.length === 0) {
    ctx.conteudo.innerHTML = `
      <div class="cabecalho-pagina"><h1>Olá, ${esc(primeiro)}</h1></div>
      <div class="painel">
        <div class="vazio">
          <b>Ainda não acompanha nenhum idoso</b>
          <p>Inscreva o idoso que quer acompanhar. Leva cerca de dois minutos.</p>
          <a class="btn btn--grande" href="#/inscrever">${icone('mais')}Inscrever idoso</a>
        </div>
      </div>`;
    return;
  }
  ctx.conteudo.innerHTML = `
    <div class="cabecalho-pagina"><h1>Olá, ${esc(primeiro)}</h1></div>
    <div class="painel">
      <h2>Os seus idosos</h2>
      <ul class="lista">${ctx.idosos.map((r) => {
        const tom = ctx.tomDoIdoso(r);
        const t = r.ultimaMedicao;
        return `<li>
          <span class="icone-redondo ${tom !== 'calma' ? `icone-redondo--${tom}` : ''}" style="width:2.8rem;height:2.8rem">${icone(tom === 'alerta' ? 'aviso' : 'coracao')}</span>
          <div><p class="titulo">${esc(r.idoso.nome)}</p>
            <p class="detalhe"><span>Quarto ${esc(r.idoso.quarto || '—')}</span>${t ? `<span>Tensão ${t.sistolica}/${t.diastolica}</span>` : ''}${r.quedasAtivas ? '<span>Alerta de queda ativo</span>' : ''}</p></div>
          <a class="btn btn--sec" href="#/idoso/${r.idoso.id}">Abrir ficha</a></li>`;
      }).join('')}</ul>
    </div>`;
}

function vistaServicos(m, ctx) {
  const familia = [
    ['jogo', 'Requisitar jogo de tabuleiro', 'Pedir um jogo para a sala de convívio.'],
    ['calendario', 'Marcar visita ao idoso', 'Escolher dia e hora e receber o código de acesso.'],
    ['igreja', 'Inscrever na missa', 'Reservar lugar na missa semanal da capela.'],
    ['chave', 'Fazer pedido de manutenção', 'Avisar de uma avaria no quarto.'],
  ];
  const gestao = [
    ['refeicao', 'Pedidos de refeição', 'Refeições especiais com validação médica.'],
    ['sair', 'Checkout de um idoso', 'Alta hospitalar e arquivo do processo.'],
    ['calendario', 'Gestão de turnos', 'Escalas da equipa de enfermagem.'],
  ];
  const lista = (itens) => `<ul class="servicos">${itens.map(([i, t, d]) => `
    <li><span class="icone-redondo">${icone(i)}</span><div><b>${esc(t)}</b><span class="suave pequeno">${esc(d)}</span></div></li>`).join('')}</ul>`;
  ctx.conteudo.innerHTML = `
    <div class="cabecalho-pagina"><div><h1>Mais serviços</h1>
      <p class="suave" style="margin-top:.5rem">Estes serviços fazem parte do menu principal e ficam disponíveis no Sprint 2.</p></div></div>
    <section class="painel painel--discreto"><h2>Para a família</h2>${lista(familia)}</section>
    ${ctx.utilizador.papel === 'ADMIN' ? `<section class="painel painel--discreto"><h2>Administração</h2>${lista(gestao)}</section>` : ''}`;
}
