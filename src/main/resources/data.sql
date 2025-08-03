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

-- Sample tasks removed - only user-created tasks will be shown

-- Update existing tasks to use correct status values
UPDATE tasks SET status = 'BACKLOG' WHERE status = 'Backlog';
UPDATE tasks SET status = 'READY_TO_DEVELOP' WHERE status = 'A Fazer';
UPDATE tasks SET status = 'IN_PROGRESS' WHERE status = 'Em Progresso';
UPDATE tasks SET status = 'IN_REVIEW' WHERE status = 'Em Revisão';
UPDATE tasks SET status = 'DONE' WHERE status = 'Concluído';

-- Insert sample meeting minutes
INSERT INTO meeting_minutes (title, file_name, file_path, file_size, content_type, meeting_date, uploaded_at, project_id, uploaded_by) VALUES
('Reunião de Kickoff - E-commerce', 'kickoff-ecommerce.pdf', '/uploads/kickoff-ecommerce.pdf', 1024000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1),
('Sprint Planning #1', 'sprint-planning-1.pdf', '/uploads/sprint-planning-1.pdf', 512000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 2),
('Reunião de Design - App Mobile', 'design-meeting-app.pdf', '/uploads/design-meeting-app.pdf', 768000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 1),
('Apresentação do Projeto Website', 'apresentacao-website.pdf', '/uploads/apresentacao-website.pdf', 2048000, 'application/pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 2);
