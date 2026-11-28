INSERT INTO templates_mensagem (codigo, descricao, conteudo)
VALUES
    ('COMPROVANTE_AGENDAMENTO', 'Comprovante de agendamento',
     'Olá, {{nome_paciente}}! Sua consulta foi agendada para {{data}}, turno {{turno}}.'),
    ('AVISO_REMARCACAO', 'Aviso de remarcação',
     'Olá, {{nome_paciente}}! Sua consulta foi remarcada para {{data}}, turno {{turno}}.'),
    ('AVISO_CANCELAMENTO', 'Aviso de cancelamento',
     'Olá, {{nome_paciente}}! Sua consulta de {{data}} foi cancelada. Você retornará à fila de espera.')
ON CONFLICT (codigo) DO NOTHING;
