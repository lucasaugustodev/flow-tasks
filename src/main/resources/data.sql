-- Insert sample users (password is 'password123' encoded with BCrypt)
INSERT INTO users (username, email, password, full_name, created_at, updated_at, is_active) VALUES
('admin', 'admin@example.com', '$2a$10$Fo4IOGqJkEH5dstr.zV82ue1h25ifsd5fCiNv0EYMyFh.P45Dk0gW', 'Administrador', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('joao', 'joao@example.com', '$2a$10$Fo4IOGqJkEH5dstr.zV82ue1h25ifsd5fCiNv0EYMyFh.P45Dk0gW', 'João Silva', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('maria', 'maria@example.com', '$2a$10$Fo4IOGqJkEH5dstr.zV82ue1h25ifsd5fCiNv0EYMyFh.P45Dk0gW', 'Maria Santos', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('pedro', 'pedro@example.com', '$2a$10$Fo4IOGqJkEH5dstr.zV82ue1h25ifsd5fCiNv0EYMyFh.P45Dk0gW', 'Pedro Costa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert sample projects
INSERT INTO projects (name, description, status, start_date, created_at, updated_at, created_by) VALUES
('Sistema de E-commerce', 'Desenvolvimento de plataforma de vendas online', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('App Mobile', 'Aplicativo móvel para gestão de tarefas', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('Website Corporativo', 'Novo site institucional da empresa', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2);

-- Insert project users relationships
INSERT INTO project_users (project_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 4),
(3, 2), (3, 3), (3, 4);

-- Insert project access permissions
INSERT INTO project_access (project_id, user_id, role, granted_at, granted_by) VALUES
(1, 1, 'OWNER', CURRENT_TIMESTAMP, 1),
(1, 2, 'ADMIN', CURRENT_TIMESTAMP, 1),
(1, 3, 'MEMBER', CURRENT_TIMESTAMP, 1),
(2, 1, 'OWNER', CURRENT_TIMESTAMP, 1),
(2, 4, 'MEMBER', CURRENT_TIMESTAMP, 1),
(3, 2, 'OWNER', CURRENT_TIMESTAMP, 2),
(3, 3, 'ADMIN', CURRENT_TIMESTAMP, 2),
(3, 4, 'MEMBER', CURRENT_TIMESTAMP, 2);

-- Insert sample tasks
INSERT INTO tasks (title, description, status, priority, project_id, assigned_user_id, created_by, created_at, updated_at) VALUES
('Configurar ambiente de desenvolvimento', 'Configurar Docker, banco de dados e dependências', 'DONE', 'HIGH', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Criar modelo de dados', 'Definir entidades e relacionamentos do banco', 'DONE', 'HIGH', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Implementar autenticação', 'Sistema de login e registro de usuários', 'IN_PROGRESS', 'HIGH', 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Desenvolver catálogo de produtos', 'Listagem e busca de produtos', 'READY_TO_DEVELOP', 'MEDIUM', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Implementar carrinho de compras', 'Funcionalidade de adicionar/remover produtos', 'BACKLOG', 'MEDIUM', 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('Design da interface', 'Criar mockups e protótipos', 'IN_PROGRESS', 'HIGH', 2, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Configurar projeto React Native', 'Setup inicial do projeto mobile', 'READY_TO_DEVELOP', 'HIGH', 2, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Implementar navegação', 'Sistema de navegação entre telas', 'BACKLOG', 'MEDIUM', 2, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('Análise de requisitos', 'Levantamento das necessidades do cliente', 'DONE', 'HIGH', 3, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Criar layout responsivo', 'Design adaptável para diferentes dispositivos', 'IN_PROGRESS', 'HIGH', 3, 4, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Desenvolver páginas institucionais', 'Sobre, Contato, Serviços', 'READY_TO_DEVELOP', 'MEDIUM', 3, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample meeting minutes
INSERT INTO meeting_minutes (title, file_name, file_path, file_size, content_type, meeting_date, uploaded_at, project_id, uploaded_by) VALUES
('Reunião de Kickoff - E-commerce', 'kickoff-ecommerce.pdf', '/uploads/kickoff-ecommerce.pdf', 1024000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1),
('Sprint Planning #1', 'sprint-planning-1.pdf', '/uploads/sprint-planning-1.pdf', 512000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 2),
('Reunião de Design - App Mobile', 'design-meeting-app.pdf', '/uploads/design-meeting-app.pdf', 768000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 1),
('Apresentação do Projeto Website', 'apresentacao-website.pdf', '/uploads/apresentacao-website.pdf', 2048000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 2);
