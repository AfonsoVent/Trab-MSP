// Comunicação com o back-end (mesma origem: o Spring Boot serve esta página e a API).

const CHAVE = 'elderlyhealth.sessao';

export const sessao = {
  obter() {
    try { return JSON.parse(sessionStorage.getItem(CHAVE)); } catch { return null; }
  },
  guardar(valor) {
    try { sessionStorage.setItem(CHAVE, JSON.stringify(valor)); } catch { /* modo privado */ }
  },
  limpar() {
    try { sessionStorage.removeItem(CHAVE); } catch { /* ignorar */ }
  },
};

export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

export async function api(metodo, caminho, corpo, { formulario } = {}) {
  const s = sessao.obter();
  const headers = {};
  if (s?.token) headers.Authorization = `Bearer ${s.token}`;
  let body;
  if (formulario) {
    body = formulario;
  } else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(corpo);
  }

  let resposta;
  try {
    resposta = await fetch(caminho, { method: metodo, headers, body });
  } catch {
    throw new ErroApi(0, 'Sem ligação ao servidor. Confirme que o back-end está a correr.');
  }

  if (resposta.status === 401 && !caminho.startsWith('/api/auth/')) {
    sessao.limpar();
    window.dispatchEvent(new CustomEvent('sessao-expirada'));
  }
  if (!resposta.ok) {
    let mensagem = `Erro ${resposta.status}`;
    try {
      const erro = await resposta.json();
      if (erro?.mensagem) mensagem = erro.mensagem;
    } catch { /* corpo vazio */ }
    throw new ErroApi(resposta.status, mensagem);
  }
  if (resposta.status === 204) return null;
  const tipo = resposta.headers.get('content-type') || '';
  return tipo.includes('application/json') ? resposta.json() : resposta.blob();
}

export const get = (c) => api('GET', c);
export const post = (c, corpo) => api('POST', c, corpo ?? {});
export const put = (c, corpo) => api('PUT', c, corpo);
export const apagar = (c) => api('DELETE', c);
export const enviarFicheiro = (c, formulario) => api('POST', c, undefined, { formulario });
