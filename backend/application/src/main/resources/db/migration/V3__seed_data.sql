-- ============================================================
-- V3 — Seed data: admin + cliente teste, 6 categorias, 18 produtos
-- ============================================================

-- Senhas reais (BCrypt cost 12):
--   admin@pethub.com   → Admin@123
--   cliente@teste.com  → Cliente@123

INSERT INTO usuarios (id, nome, email, senha_hash, tipo_usuario, ativo)
VALUES
    (1, 'Admin Pet Hub',  'admin@pethub.com',  '$2b$12$4F8Mj1oLV6MoX0RlEtTCzewFxbgY7itAYqHtwkK9SgOBAKO6.c91u', 'ADMIN',   TRUE),
    (2, 'Cliente Teste',  'cliente@teste.com', '$2b$12$w3XpcJno7ihDfZrxQL/VDOLSyKt1e3N.Ayw93ThZ8cgCikgEkdZy2', 'CLIENTE', TRUE);

SELECT setval('usuarios_id_seq', 2);

INSERT INTO usuario_roles (usuario_id, role) VALUES
    (1, 'ROLE_ADMIN_LOJA'),
    (2, 'ROLE_CLIENTE');

-- ─── Categorias ─────────────────────────────────────────────

INSERT INTO categorias (id, nome, slug, descricao, ordem) VALUES
    (1, 'Smart Collars',           'smart-collars',           'Coleiras inteligentes com GPS, monitor de atividade e saúde.',     1),
    (2, 'Comedouros Automáticos',  'comedouros-automaticos',  'Comedouros IoT com porções programadas e câmera integrada.',       2),
    (3, 'Câmeras Pet',             'cameras-pet',             'Câmeras de monitoramento com áudio bidirecional e visão noturna.', 3),
    (4, 'GPS Trackers',            'gps-trackers',            'Rastreadores GPS dedicados para pets aventureiros.',                4),
    (5, 'Fontes Inteligentes',     'fontes-inteligentes',     'Bebedouros com filtragem, sensor de nível e app companion.',       5),
    (6, 'Brinquedos Interativos',  'brinquedos-interativos',  'Brinquedos com IA, sensores e gamificação para pets.',             6);

SELECT setval('categorias_id_seq', 6);

-- ─── Produtos ──────────────────────────────────────────────
-- 3 por categoria. NCMs realistas para futura emissão de NF-e (Fase 7).

INSERT INTO produtos (id, sku, nome, descricao_curta, descricao_completa, marca, categoria_id, peso_kg, altura_cm, largura_cm, profundidade_cm, ncm, origem, specs, destacado) VALUES
    -- Smart Collars (cat 1)
    (1,  'COLLAR-PRO-001', 'PetTrack Pro Collar',          'Coleira GPS com monitor de atividade e saúde 24/7.', 'Coleira inteligente com GPS de precisão, monitoramento de batimento cardíaco, atividade, sono e localização em tempo real via app.', 'PetTech',    1, 0.085, 4.5, 28.0, 4.5, '85176259', 'IMPORTADO_DIRETO', '{"bateria_dias":7,"resistencia":"IP67","gps":true,"conectividade":"4G+BLE"}'::jsonb, TRUE),
    (2,  'COLLAR-LITE-002','SmartPaw Lite Collar',         'Coleira com Bluetooth e atividade básica.',           'Versão econômica com BLE, contador de passos e LED noturno. Bateria de até 14 dias.',                                                    'SmartPaw',   1, 0.060, 3.5, 24.0, 3.5, '85176259', 'NACIONAL',         '{"bateria_dias":14,"resistencia":"IP65","gps":false,"led_noturno":true}'::jsonb,    FALSE),
    (3,  'COLLAR-MED-003', 'VetCollar Health Monitor',     'Coleira clínica com biometria avançada.',            'Recomendada por veterinários. Monitora respiração, temperatura, ECG e gera relatórios médicos exportáveis.',                              'VetTech',    1, 0.095, 5.0, 30.0, 5.0, '85176259', 'IMPORTADO_DIRETO', '{"bateria_dias":5,"resistencia":"IP68","ecg":true,"relatorios_medicos":true}'::jsonb, TRUE),

    -- Comedouros (cat 2)
    (4,  'FEEDER-CAM-001', 'AutoFeeder Camera HD',         'Comedouro com câmera HD e dispensação programada.',  'Comedouro com câmera 1080p, dispensação programada de até 6 refeições/dia, gravador de áudio e fala bidirecional via app.',              'AutoFeed',   2, 2.500, 35.0, 22.0, 30.0, '84385000', 'IMPORTADO_DIRETO', '{"capacidade_litros":4,"camera":"1080p","refeicoes_dia":6,"audio_bidirecional":true}'::jsonb, TRUE),
    (5,  'FEEDER-BASIC-002','PetMeal Programável',         'Comedouro com timer simples para até 4 refeições.',  'Solução básica para programar 4 refeições diárias com porções ajustáveis.',                                                                  'PetMeal',    2, 1.800, 28.0, 20.0, 25.0, '84385000', 'NACIONAL',         '{"capacidade_litros":3,"refeicoes_dia":4}'::jsonb,                                  FALSE),
    (6,  'FEEDER-PRO-003', 'NutriPet AI Feeder',           'Comedouro com IA que aprende horários do pet.',      'IA detecta padrões de alimentação e ajusta porções automaticamente. Integração com app fitness pet.',                                      'NutriPet',   2, 3.000, 38.0, 24.0, 32.0, '84385000', 'IMPORTADO_DIRETO', '{"capacidade_litros":5,"ia_aprendizado":true,"camera":"2K","conectividade":"WiFi"}'::jsonb, FALSE),

    -- Câmeras Pet (cat 3)
    (7,  'CAM-360-001',    'PetView 360 Smart Camera',     'Câmera 360° com áudio e visão noturna IR.',         'Visão 360° motorizada, áudio bidirecional, detecção de latido e movimento, visão noturna IR. Armazenamento em nuvem opcional.',           'PetView',    3, 0.450, 12.0, 12.0, 15.0, '85258100', 'IMPORTADO_DIRETO', '{"resolucao":"2K","rotacao":"360","visao_noturna":true,"deteccao_latido":true}'::jsonb, TRUE),
    (8,  'CAM-MINI-002',   'PupCam Mini',                  'Câmera compacta para apartamentos.',                  'Câmera mini-pet com 1080p, áudio bidirecional e detecção básica de movimento.',                                                              'PupCam',     3, 0.180, 8.0, 8.0, 8.0, '85258100', 'NACIONAL',         '{"resolucao":"1080p","rotacao":"fixa","audio":true}'::jsonb,                         FALSE),
    (9,  'CAM-TREAT-003',  'PetView Treat Dispenser Cam',  'Câmera que dispensa petiscos remotamente.',          'Câmera 2K com dispensador integrado de petiscos via app. Modo "jogue petisco" e laser interativo.',                                          'PetView',    3, 1.200, 22.0, 18.0, 20.0, '85258100', 'IMPORTADO_DIRETO', '{"resolucao":"2K","dispensador_petisco":true,"laser_interativo":true}'::jsonb,       FALSE),

    -- GPS Trackers (cat 4)
    (10, 'GPS-MICRO-001',  'PetGPS Micro Tracker',         'Rastreador GPS compacto para coleiras existentes.',  'Anexa à coleira do pet. GPS 4G + WiFi + BLE com geofence configurável e alertas em tempo real.',                                            'PetGPS',     4, 0.040, 4.0, 4.0, 1.5, '85269100', 'IMPORTADO_DIRETO', '{"bateria_dias":10,"resistencia":"IP67","conectividade":"4G+WiFi+BLE","geofence":true}'::jsonb, TRUE),
    (11, 'GPS-RUGGED-002', 'AdventureTracker Pro',         'GPS robusto para pets aventureiros.',                'GPS de longo alcance com bateria de 30 dias, resistente a impacto e submersão. Ideal para cachorros de campo.',                              'Adventure',  4, 0.080, 5.5, 4.5, 2.0, '85269100', 'IMPORTADO_DIRETO', '{"bateria_dias":30,"resistencia":"IP69","ondas_lpwan":true}'::jsonb,                FALSE),
    (12, 'GPS-LITE-003',   'FindMyPet Lite',               'Rastreador BLE para alcance curto.',                 'Solução simples baseada em rede crowdsourced BLE. Ideal para gatos urbanos. Sem mensalidade.',                                                'FindMyPet',  4, 0.030, 3.5, 3.5, 1.0, '85269100', 'NACIONAL',         '{"bateria_dias":180,"tecnologia":"BLE","crowdsourced":true}'::jsonb,                 FALSE),

    -- Fontes Inteligentes (cat 5)
    (13, 'FOUNT-FILT-001', 'AquaPet Filtered Fountain',    'Fonte com filtragem em 3 estágios e sensor.',         'Filtros de carvão + algodão + resina trocáveis. Sensor de nível notifica o app quando é hora de reabastecer.',                              'AquaPet',    5, 1.400, 18.0, 22.0, 22.0, '84212100', 'IMPORTADO_DIRETO', '{"capacidade_litros":2.5,"filtragem_estagios":3,"sensor_nivel":true}'::jsonb,       FALSE),
    (14, 'FOUNT-UV-002',   'PureFlow UV Fountain',         'Fonte com esterilização por luz UV.',                 'UV-C esteriliza a água continuamente. Bomba silenciosa <30dB. Indicado para gatos exigentes.',                                                'PureFlow',   5, 1.600, 20.0, 24.0, 24.0, '84212100', 'IMPORTADO_DIRETO', '{"capacidade_litros":3,"uv_esterilizacao":true,"ruido_db":28}'::jsonb,              TRUE),
    (15, 'FOUNT-BASIC-003','PetFlow Basic',                'Fonte de água com bomba silenciosa.',                 'Versão simples com bomba silenciosa e fluxo ajustável.',                                                                                       'PetFlow',    5, 1.100, 16.0, 20.0, 20.0, '84212100', 'NACIONAL',         '{"capacidade_litros":2,"fluxo_ajustavel":true}'::jsonb,                              FALSE),

    -- Brinquedos Interativos (cat 6)
    (16, 'TOY-LASER-001',  'LaserCat Auto Play',           'Laser automatizado para gatos.',                      'Laser com 5 padrões de movimento e timer de 15min. Desliga sozinho para evitar sobrestimulação.',                                            'LaserCat',   6, 0.250, 12.0, 12.0, 12.0, '95030097', 'NACIONAL',         '{"padroes_movimento":5,"timer_minutos":15,"auto_shutoff":true}'::jsonb,             FALSE),
    (17, 'TOY-PUZZLE-002', 'BrainPet Puzzle Smart',        'Quebra-cabeça com IA que aumenta dificuldade.',       'Brinquedo que dispensa petiscos. IA aumenta a dificuldade conforme o pet resolve cada nível.',                                                'BrainPet',   6, 0.600, 8.0, 22.0, 22.0, '95030097', 'IMPORTADO_DIRETO', '{"ia_dificuldade_dinamica":true,"niveis":10,"dispensa_petisco":true}'::jsonb,       TRUE),
    (18, 'TOY-BALL-003',   'RollyBall Smart',              'Bola que rola sozinha e desvia obstáculos.',          'Bola autônoma com sensores de proximidade. Bateria recarregável via USB-C, 90min de uso por carga.',                                          'RollyBall',  6, 0.320, 7.0, 7.0, 7.0, '95030097', 'IMPORTADO_DIRETO', '{"bateria_minutos":90,"sensores_proximidade":true,"recarga":"USB-C"}'::jsonb,       FALSE);

SELECT setval('produtos_id_seq', 18);

-- ─── Imagens (3 por produto, primeira = principal) ─────────

INSERT INTO produto_imagens (produto_id, url, ordem, principal)
SELECT p.id, 'https://picsum.photos/seed/pethub-' || p.id || '-' || g.idx || '/800/600', g.idx, g.idx = 0
FROM produtos p
CROSS JOIN (VALUES (0), (1), (2)) AS g(idx);

-- ─── Preços vigentes (entre R$ 89,90 e R$ 1.299,00) ────────

INSERT INTO precos_vigentes (produto_id, valor_base, data_inicio, criado_por_id) VALUES
    (1,  799.90,  CURRENT_TIMESTAMP, 1),
    (2,  249.90,  CURRENT_TIMESTAMP, 1),
    (3,  1299.00, CURRENT_TIMESTAMP, 1),
    (4,  649.00,  CURRENT_TIMESTAMP, 1),
    (5,  189.90,  CURRENT_TIMESTAMP, 1),
    (6,  899.00,  CURRENT_TIMESTAMP, 1),
    (7,  549.00,  CURRENT_TIMESTAMP, 1),
    (8,  229.90,  CURRENT_TIMESTAMP, 1),
    (9,  699.00,  CURRENT_TIMESTAMP, 1),
    (10, 449.00,  CURRENT_TIMESTAMP, 1),
    (11, 879.00,  CURRENT_TIMESTAMP, 1),
    (12, 169.90,  CURRENT_TIMESTAMP, 1),
    (13, 319.00,  CURRENT_TIMESTAMP, 1),
    (14, 459.00,  CURRENT_TIMESTAMP, 1),
    (15, 149.90,  CURRENT_TIMESTAMP, 1),
    (16,  89.90,  CURRENT_TIMESTAMP, 1),
    (17, 289.00,  CURRENT_TIMESTAMP, 1),
    (18, 199.00,  CURRENT_TIMESTAMP, 1);
