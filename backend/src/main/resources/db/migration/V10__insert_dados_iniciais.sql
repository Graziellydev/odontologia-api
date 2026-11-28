INSERT INTO procedimentos (nome, especialidade, exige_dente, exige_classe) VALUES
 ('Restauração',          'Dentística',  TRUE,  TRUE),
 ('Endodontia',           'Endodontia',  TRUE,  FALSE),
 ('Extração',             'Cirurgia',    TRUE,  FALSE),
 ('Raspagem periodontal', 'Periodontia', FALSE, FALSE),
 ('Profilaxia',           'Periodontia', FALSE, FALSE);

INSERT INTO templates_mensagem (codigo, descricao, conteudo) VALUES
 ('LEMBRETE_24H',           'Lembrete 24h com confirmação',
  'Olá, {{nome_paciente}}! Sua consulta na Clínica de Odontologia da UniCatólica é amanhã, {{data}}, turno {{turno}}. Responda 1 para CONFIRMAR ou 2 para CANCELAR.'),
 ('COMPROVANTE_AGENDAMENTO','Consulta agendada',
  'Olá, {{nome_paciente}}! Sua consulta foi agendada para {{data}}, turno {{turno}}. Tolerância de espera: 20 minutos.'),
 ('AVISO_REMARCACAO',       'Consulta remarcada',
  'Olá, {{nome_paciente}}! Sua consulta foi remarcada para {{data}}, turno {{turno}}.'),
 ('AVISO_CANCELAMENTO',     'Consulta cancelada',
  'Olá, {{nome_paciente}}! Sua consulta de {{data}} foi cancelada. Você permanece na fila de espera.'),
 ('AVISO_FALTA',            'Aviso de falta',
  'Olá, {{nome_paciente}}! Registramos sua ausência em {{data}}. Faltas sem justificativa: {{faltas}} (a 3ª gera desligamento).'),
 ('CONVITE_REAGENDAMENTO',  'Convite para remarcar após falta',
  'Olá, {{nome_paciente}}! Deseja remarcar sua consulta? Responda 1 para SIM ou 2 para NÃO.'),
 ('AVISO_DESLIGAMENTO',     'Desligamento por faltas',
  'Olá, {{nome_paciente}}! Seu atendimento foi encerrado. Motivo: {{motivo}}.'),
 ('OFERTA_VAGA_DESISTENCIA','Vaga de última hora',
  'Olá, {{nome_paciente}}! Surgiu uma vaga hoje, {{data}}, turno {{turno}}. Responda 1 para ACEITAR.'),
 ('PESQUISA_SATISFACAO',    'Pesquisa rápida e opcional',
  'Olá, {{nome_paciente}}! Como foi seu atendimento? Responda com uma nota de 1 (ruim) a 5 (ótimo). Se quiser, envie também um comentário.');
