-- Demo password: demo12345 (BCrypt). Change it before exposing this application.
INSERT INTO users (id, name, email, password) VALUES (1, 'Demo User', 'demo@taskflow.local', '$2y$10$2BK47jXdvr0IgHKvCac1nexSD1PICW9tT5Lnzc/26beQVnC04.nAa');
INSERT INTO projects (id, name, description, owner_id) VALUES (1, 'Launch plan', 'A sample project for exploring TaskFlow.', 1);
INSERT INTO tasks (id, title, description, status, priority, due_date, assignee_id, project_id) VALUES (1, 'Plan the launch', 'Write down the first milestones.', 'TODO', 'HIGH', CURRENT_DATE + 7, 1, 1), (2, 'Review designs', 'Collect feedback from the team.', 'IN_PROGRESS', 'MEDIUM', CURRENT_DATE + 3, 1, 1);
INSERT INTO task_activities (task_id, actor_id, previous_status, new_status) VALUES (1, 1, NULL, 'TODO'), (2, 1, NULL, 'TODO'), (2, 1, 'TODO', 'IN_PROGRESS');
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('projects', 'id'), (SELECT MAX(id) FROM projects));
SELECT setval(pg_get_serial_sequence('tasks', 'id'), (SELECT MAX(id) FROM tasks));
