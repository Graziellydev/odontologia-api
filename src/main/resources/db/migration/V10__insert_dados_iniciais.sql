INSERT INTO procedimentos (nome, especialidade, exige_dente, exige_classe) VALUES
 ('Restauração',          'Dentística',  TRUE,  TRUE),
 ('Endodontia',           'Endodontia',  TRUE,  FALSE),
 ('Extração',             'Cirurgia',    TRUE,  FALSE),
 ('Raspagem periodontal', 'Periodontia', FALSE, FALSE),
 ('Profilaxia',           'Periodontia', FALSE, FALSE);

INSERT INTO templates_mensagem (codigo, descricao, conteudo) VALUES
 ('LEMBRETE_24H', 'Lembrete 24h', 'Sua consulta é amanhã. Responda 1 para CONFIRMAR ou 2 para CANCELAR.');