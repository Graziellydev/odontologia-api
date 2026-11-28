INSERT INTO usuarios (nome, email, senha_hash, perfil, matricula_ou_registro) VALUES
    ('Ana Paula Martins', 'secretaria1@unicatolica.edu.br', '$2a$10$wYQoVRNoFkThW8rADr7.2.lu6BJBjb9tPvYVvOpXUZb6X3aFKcfE6', 'SECRETARIA', 'SEC202601'),
    ('Beatriz Costa Lima', 'secretaria2@unicatolica.edu.br', '$2a$10$Hegc2ZjSil1/tLstvoKe1eRd9l88zTakoaSI/e497bra/so.Fb9rO', 'SECRETARIA', 'SEC202602'),
    ('Mariana Costa Silva', 'mariana.costa@aluno.unicatolica.edu.br', '$2a$10$AYnpZs1l8wSLPGmdQ/j4nO7Bkw9jEJiCCiwptZfcg7A65696cGRFm', 'ALUNO', '20241001'),
    ('Lucas Mendes Oliveira', 'lucas.mendes@aluno.unicatolica.edu.br', '$2a$10$lZP47L0zSpIRPWC.h95C/uamOcipnd4n9kkZWSQs7G4E3OKJFAJcK', 'ALUNO', '20241002'),
    ('Gabriel Santos Rocha', 'gabriel.santos@aluno.unicatolica.edu.br', '$2a$10$EVbpYgnSCh6efmJkutMnx.wwjy3LK5/PtA4LHy7PaqPDkyNuKy52q', 'ALUNO', '20241003'),
    ('Camila Ferreira Alves', 'camila.ferreira@aluno.unicatolica.edu.br', '$2a$10$rUbphA3HIFbz3tn7VP4RnezaFY6Fy2lmaQXNZ49Prz.N5zg1P11EG', 'ALUNO', '20241004'),
    ('Pedro Henrique Sousa', 'pedro.sousa@aluno.unicatolica.edu.br', '$2a$10$LUZCrJgbzeQhjH.eR/7L5.1SUA1810yo1P5ncL5t2dx1qavAlDES6', 'ALUNO', '20241005'),
    ('Larissa Almeida Nogueira', 'larissa.almeida@aluno.unicatolica.edu.br', '$2a$10$0sZCrQztvDrlCd5Eo0bZROJdkUYMoX.uMwb3UYz5KrRjDkITq3Rfm', 'ALUNO', '20241006'),
    ('Rafael Lima Teixeira', 'rafael.lima@aluno.unicatolica.edu.br', '$2a$10$.rxSdA1utjOeQWkmzmI7EuuvpGtMDgMQ.6vPja0IApiOpbBPljtWO', 'ALUNO', '20241007'),
    ('Juliana Ribeiro Castro', 'juliana.ribeiro@aluno.unicatolica.edu.br', '$2a$10$XbHFIZ5XU/rtx/sKLWDu7eNf54wnBnmX/b79nv3ZXtnNnWLbXvnIe', 'ALUNO', '20241008'),
    ('Mateus Cavalcante Dias', 'mateus.cavalcante@aluno.unicatolica.edu.br', '$2a$10$kUF5Gf5wOws2gsPbWWkEF.kPEql.1M3cIkvWZLsQvZJ5C72D6DNky', 'ALUNO', '20241009'),
    ('Isabela Gomes Pereira', 'isabela.gomes@aluno.unicatolica.edu.br', '$2a$10$W.c4Rma6rdyouSYPfMEs3uBznF6o7WrICA1Eexxynwsok8466uA/W', 'ALUNO', '20241010'),
    ('Prof. Dr. Ricardo Vasconcelos', 'coordenacao.odonto@unicatolica.edu.br', '$2a$10$qaIH9hQiNjZD/0odttSJBOL/8h3bFLDdRO7Ju0jkdSiUtEWlTq/YK', 'COORDENADOR', 'COORD202601')
ON CONFLICT DO NOTHING;
