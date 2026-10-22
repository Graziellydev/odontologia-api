package br.com.unicatolica.odontologia_api.model;

/**
 * Enumeração que define os perfis de acesso permitidos no sistema.
 * Reflete exatamente a trava do banco PostgreSQL: CHECK (perfil IN ('ALUNO','COORDENADOR','SECRETARIA'))
 */
public enum Perfil {
    ALUNO,
    COORDENADOR,
    SECRETARIA
}
