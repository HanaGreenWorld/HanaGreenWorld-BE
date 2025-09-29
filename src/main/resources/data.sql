-- 테스트 사용자 데이터
INSERT INTO members (member_id, login_id, email, password, name, role, is_active, created_at, updated_at) VALUES
(1, 'test_leader', 'leader@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '김팀장', 'USER', true, NOW(), NOW()),
(2, 'test_member1', 'member1@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '이멤버', 'USER', true, NOW(), NOW()),
(3, 'test_member2', 'member2@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '박멤버', 'USER', true, NOW(), NOW());

-- 테스트 팀 데이터
INSERT INTO teams (id, name, slogan, invite_code, created_at, updated_at) VALUES
(1, '그린워리어즈', '지구를 지키는 우리의 여정! 🌱', 'GG-1234', NOW(), NOW());

-- 팀 멤버 관계 데이터
INSERT INTO member_teams (id, member_id, team_id, member_role, is_active, created_at, updated_at) VALUES
(1, 1, 1, 'LEADER', true, NOW(), NOW()),
(2, 2, 1, 'MEMBER', true, NOW(), NOW()),
(3, 3, 1, 'MEMBER', true, NOW(), NOW());

-- 팀 점수 데이터
INSERT INTO team_scores (id, team_id, total_points, monthly_points, carbon_saved_kg, completed_challenges, created_at, updated_at) VALUES
(1, 1, 1500, 450, 25.5, 8, NOW(), NOW());

-- 사용자 프로필 데이터
INSERT INTO member_profiles (id, member_id, profile_image_url, bio, created_at, updated_at) VALUES
(1, 1, 'https://via.placeholder.com/150/4CAF50/FFFFFF?text=김팀장', '환경을 사랑하는 팀장입니다! 🌱', NOW(), NOW()),
(2, 2, 'https://via.placeholder.com/150/2196F3/FFFFFF?text=이멤버', '친환경 라이프를 실천하고 있어요! 🌿', NOW(), NOW()),
(3, 3, 'https://via.placeholder.com/150/FF9800/FFFFFF?text=박멤버', '지구를 위한 작은 실천을 시작했어요! 🌍', NOW(), NOW());

-- 포인트 거래 내역 (팀 활동으로 인한 포인트 적립)
INSERT INTO point_transactions (id, member_id, points_amount, point_transaction_type, category, description, occurred_at, created_at, updated_at) VALUES
(1, 1, 100, 'EARN', 'WALKING', '일일 걷기 챌린지 완료', NOW(), NOW(), NOW()),
(2, 1, 50, 'EARN', 'QUIZ', '환경 퀴즈 정답', NOW(), NOW(), NOW()),
(3, 2, 80, 'EARN', 'WALKING', '일일 걷기 챌린지 완료', NOW(), NOW(), NOW()),
(4, 2, 30, 'EARN', 'QUIZ', '환경 퀴즈 정답', NOW(), NOW(), NOW()),
(5, 3, 90, 'EARN', 'WALKING', '일일 걷기 챌린지 완료', NOW(), NOW(), NOW()),
(6, 3, 40, 'EARN', 'QUIZ', '환경 퀴즈 정답', NOW(), NOW(), NOW());

-- 챌린지 데이터
INSERT INTO challenges (id, title, description, points, team_score, is_team_challenge, is_active, created_at, updated_at) VALUES
(1, '일일 걷기 챌린지', '하루 10,000보 걷기', 100, 50, true, true, NOW(), NOW()),
(2, '환경 퀴즈', '환경 관련 퀴즈 풀기', 50, 25, false, true, NOW(), NOW()),
(3, '플라스틱 줄이기', '일회용 플라스틱 사용 줄이기', 200, 100, true, true, NOW(), NOW());

-- 챌린지 기록 데이터
INSERT INTO challenge_records (id, member_id, challenge_id, team_id, activity_date, image_url, step_count, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), 'https://via.placeholder.com/300x200/4CAF50/FFFFFF?text=Walking+Challenge', 12000, NOW(), NOW()),
(2, 2, 1, 1, NOW(), 'https://via.placeholder.com/300x200/2196F3/FFFFFF?text=Walking+Challenge', 10500, NOW(), NOW()),
(3, 3, 1, 1, NOW(), 'https://via.placeholder.com/300x200/FF9800/FFFFFF?text=Walking+Challenge', 11000, NOW(), NOW()),
(4, 1, 2, 1, NOW(), NULL, NULL, NOW(), NOW()),
(5, 2, 2, 1, NOW(), NULL, NULL, NOW(), NOW()),
(6, 3, 2, 1, NOW(), NULL, NULL, NOW(), NOW());

-- 걷기 기록 데이터
INSERT INTO walking_records (id, member_id, activity_date, activity_amount, carbon_saved, points_awarded, created_at, updated_at) VALUES
(1, 1, NOW(), 12000, 0.5, 100, NOW(), NOW()),
(2, 2, NOW(), 10500, 0.4, 80, NOW(), NOW()),
(3, 3, NOW(), 11000, 0.45, 90, NOW(), NOW());

-- 퀴즈 기록 데이터
INSERT INTO quiz_records (id, member_id, activity_date, is_correct, points_awarded, created_at, updated_at) VALUES
(1, 1, NOW(), true, 50, NOW(), NOW()),
(2, 2, NOW(), true, 30, NOW(), NOW()),
(3, 3, NOW(), true, 40, NOW(), NOW());

-- 팀 채팅 설정
INSERT INTO team_chat_settings (id, team_id, is_enabled, max_messages_per_day, created_at, updated_at) VALUES
(1, 1, true, 100, NOW(), NOW());

-- 팀 채팅 메시지 (샘플)
INSERT INTO team_chat_messages (id, team_id, sender_id, message_text, message_type, sent_at, is_deleted, created_at, updated_at) VALUES
(1, 1, 1, '안녕하세요! 그린워리어즈 팀에 오신 것을 환영합니다! 🌱', 'TEXT', NOW(), false, NOW(), NOW()),
(2, 1, 2, '팀장님 안녕하세요! 함께 환경을 지켜나가요! 🌿', 'TEXT', NOW(), false, NOW(), NOW()),
(3, 1, 3, '저도 열심히 참여하겠습니다! 🌍', 'TEXT', NOW(), false, NOW(), NOW()),
(4, 1, 1, '오늘 걷기 챌린지 완료하신 분들 수고하셨습니다! 👏', 'TEXT', NOW(), false, NOW(), NOW());