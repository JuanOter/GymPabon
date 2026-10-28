-- =============================================================
-- GymPabon - sample data (fictitious, for testing and demos)
-- Run after schema.sql.
-- All demo users have the password: GymPabon2026
-- =============================================================
USE gympabon;

-- Plans
INSERT INTO plan (id, plan_type, name, monthly_price, description) VALUES
(1, 'BASIC',    'Básico',   70000,  'Acceso de lunes a viernes de 5:00 a. m. a 3:00 p. m. a máquinas y pesas.'),
(2, 'STANDARD', 'Estándar', 95000,  'Acceso en todo el horario del gimnasio a máquinas, pesas y clases grupales.'),
(3, 'PREMIUM',  'Premium',  140000, 'Acceso en todo el horario a todas las zonas, con entrenador personal asignado.');

-- System users (staff)
INSERT INTO app_user (id, document_number, first_name, last_name, phone, email, username, password_hash, role, active, last_login) VALUES
(1, '1065000001', 'Ricardo', 'Pabón', '300 600 0001', 'admin@gympabon.com',  'admin',  'pbkdf2$65536$1OBaFzfJvD/j4AZfjjCUHg==$3EwblG48XbrDUzTtwnTlUb+uVKa4xlXYZHvsoXzTPiE=', 'ADMIN',        TRUE,  '2026-10-27 16:02:00'),
(2, '1065000002', 'Laura',   'Mejía', '300 600 0002', 'lmejia@gympabon.com', 'lmejia', 'pbkdf2$65536$6zll8nQrtPfzX5T8pURcXQ==$pUs9RCeh8XY0jvUoKaVqHIEnNANgMco5hTEiSo0TAwk=', 'RECEPTIONIST', TRUE,  '2026-10-27 05:00:00'),
(3, '1065000003', 'Kevin',   'Ortiz', '300 600 0003', 'kortiz@gympabon.com', 'kortiz', 'pbkdf2$65536$x551cqA2BtoZqpOIlHb0pA==$pohnz6V53BVCIfI3seVPRsiBBSh1RKz7gwVKFCGb970=', 'RECEPTIONIST', TRUE,  '2026-10-27 13:00:00'),
(4, '1065000004', 'Mónica',  'Ruiz',  '300 600 0004', 'mruiz@gympabon.com',  'mruiz',  'pbkdf2$65536$mkYTqt8ELHPIv+McwfNIEg==$RCGGODGf+SNiRjEm5qNTBCJCN+yeuQ20c/Dt86+kqus=', 'RECEPTIONIST', FALSE, '2026-08-30 09:15:00');

-- Trainers
INSERT INTO trainer (id, document_number, first_name, last_name, phone, email, specialty, work_schedule, active) VALUES
(1, '1065100001', 'Ana',    'Gutiérrez', '320 555 0201', 'ana.gutierrez@gympabon.com',  'Entrenamiento funcional', 'Mañana · 5:00 a. m. a 1:00 p. m.',  TRUE),
(2, '1065100002', 'Camilo', 'Rojas',     '320 555 0202', 'camilo.rojas@gympabon.com',   'Pesas y fuerza',          'Tarde · 1:00 p. m. a 10:00 p. m.',  TRUE),
(3, '1065100003', 'Diego',  'Martínez',  '320 555 0203', 'diego.martinez@gympabon.com', 'Acondicionamiento físico','Tarde · 1:00 p. m. a 10:00 p. m.',  TRUE),
(4, '1065100004', 'Laura',  'Peña',      '320 555 0204', 'laura.pena@gympabon.com',     'Clases grupales y cardio','Mañana · 5:00 a. m. a 1:00 p. m.',  TRUE);

-- Clients
INSERT INTO client (id, document_number, first_name, last_name, phone, email, birth_date, registration_date, active) VALUES
(1, '1065123456', 'Carlos',     'Mendoza', '300 555 0101', 'carlos.mendoza@correo.com',  '1995-04-12', '2026-09-30', TRUE),
(2, '1065234567', 'Daniela',    'Ríos',    '301 555 0102', 'daniela.rios@correo.com',    '2001-08-03', '2026-10-01', TRUE),
(3, '1065345678', 'Andrés',     'Fuentes', '302 555 0103', 'andres.fuentes@correo.com',  '1990-11-20', '2026-10-02', TRUE),
(4, '1065456789', 'Valentina',  'Arias',   '304 555 0104', 'valentina.arias@correo.com', '1998-02-14', '2026-10-03', TRUE),
(5, '1065567890', 'Luis',       'Pacheco', '310 555 0105', 'luis.pacheco@correo.com',    '1987-06-30', '2026-09-21', TRUE),
(6, '1065678901', 'María José', 'Daza',    '311 555 0106', 'mariajose.daza@correo.com',  '2003-01-09', '2026-10-27', TRUE),
(7, '1065789012', 'Jorge',      'Castro',  '312 555 0107', 'jorge.castro@correo.com',    '1993-09-17', '2026-10-26', TRUE),
(8, '1065890000', 'Pedro',      'Gómez',   '313 555 0108', 'pedro.gomez@correo.com',     '1985-12-05', '2026-05-16', FALSE),
(9, '1065890123', 'Sofía',      'Herrera', '314 555 0109', 'sofia.herrera@correo.com',   '2000-07-22', '2026-10-27', TRUE);

-- Memberships
-- Value = monthly price x months x (1 - discount) + enrollment fee (first membership, except Premium).
-- End date = start date + months - 1 day.
INSERT INTO membership (id, client_id, plan_id, trainer_id, duration, start_date, end_date, enrollment_fee, total_amount, status, cancellation_reason) VALUES
(1,  1, 2, NULL, 'ONE_MONTH',    '2026-09-30', '2026-10-29', 20000, 115000, 'ACTIVE',          NULL),
(2,  2, 1, NULL, 'ONE_MONTH',    '2026-10-01', '2026-10-31', 20000,  90000, 'ACTIVE',          NULL),
(3,  3, 3, 4,    'ONE_MONTH',    '2026-10-02', '2026-11-01',     0, 140000, 'ACTIVE',          NULL),
(4,  3, 3, 4,    'ONE_MONTH',    '2026-11-02', '2026-12-01',     0, 140000, 'SCHEDULED',       NULL),
(5,  4, 2, NULL, 'ONE_MONTH',    '2026-10-03', '2026-11-02', 20000, 115000, 'ACTIVE',          NULL),
(6,  5, 1, NULL, 'ONE_MONTH',    '2026-09-21', '2026-10-20', 20000,  90000, 'EXPIRED',         NULL),
(7,  5, 1, NULL, 'ONE_MONTH',    '2026-10-23', '2026-11-22',     0,  70000, 'PENDING_PAYMENT', NULL),
(8,  6, 2, NULL, 'ONE_MONTH',    '2026-10-27', '2026-11-26', 20000, 115000, 'PENDING_PAYMENT', NULL),
(9,  7, 3, 2,    'THREE_MONTHS', '2026-10-26', '2027-01-25',     0, 399000, 'ACTIVE',          NULL),
(10, 8, 1, NULL, 'ONE_MONTH',    '2026-05-16', '2026-06-15', 20000,  90000, 'EXPIRED',         NULL),
(11, 9, 2, NULL, 'THREE_MONTHS', '2026-10-27', '2027-01-26', 20000, 290750, 'PENDING_PAYMENT', NULL);

-- Payments (membership 7 had a voided payment, so it is still pending)
INSERT INTO payment (id, membership_id, amount, method, payment_date, registered_by, voided, void_reason) VALUES
(1, 1,  115000, 'CASH',     '2026-09-30', 2, FALSE, NULL),
(2, 2,   90000, 'CASH',     '2026-10-01', 2, FALSE, NULL),
(3, 3,  140000, 'TRANSFER', '2026-10-02', 2, FALSE, NULL),
(4, 4,  140000, 'TRANSFER', '2026-10-25', 3, FALSE, NULL),
(5, 5,  115000, 'CASH',     '2026-10-03', 2, FALSE, NULL),
(6, 6,   90000, 'CASH',     '2026-09-21', 2, FALSE, NULL),
(7, 7,   70000, 'CASH',     '2026-10-23', 3, TRUE,  'Valor errado'),
(8, 9,  399000, 'CARD',     '2026-10-26', 3, FALSE, NULL),
(9, 10,  90000, 'CASH',     '2026-05-16', 2, FALSE, NULL);

-- Attendance of 27/10/2026 (Tuesday)
INSERT INTO attendance (client_id, date_time, allowed, rejection_reason, registered_by) VALUES
(1, '2026-10-27 06:10:00', TRUE,  NULL, 2),
(2, '2026-10-27 06:30:00', TRUE,  NULL, 2),
(3, '2026-10-27 07:05:00', TRUE,  NULL, 2),
(2, '2026-10-27 15:25:00', FALSE, 'Fuera del horario del plan Básico', 3),
(3, '2026-10-27 16:40:00', TRUE,  NULL, 3),
(4, '2026-10-27 16:58:00', TRUE,  NULL, 3),
(5, '2026-10-27 17:05:00', FALSE, 'Membresía vencida el 20/10/2026', 3),
(7, '2026-10-27 17:12:00', TRUE,  NULL, 3);
