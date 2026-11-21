import { api } from '../api.js';

export function entrar(identificador, senha) {
  return api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ identificador, senha }),
  });
}

export function solicitarRecuperacao(identificador) {
  return api('/auth/esqueci-senha', {
    method: 'POST',
    body: JSON.stringify({ identificador }),
  });
}

export function redefinirSenha(identificador, codigo, novaSenha) {
  return api('/auth/redefinir-senha', {
    method: 'POST',
    body: JSON.stringify({ identificador, codigo, novaSenha }),
  });
}

export function sairDaSessao() {
  return api('/auth/logout', { method: 'POST' });
}
