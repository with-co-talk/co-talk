-- 회원 탈퇴 FK 회귀 테스트용 제약 조건.
-- 엔티티에는 @ManyToOne/@ForeignKey 매핑이 없어 ddl-auto: create-drop으로는
-- 외래키 제약이 생성되지 않는다. 운영 스키마(V1__init_schema.sql)에 존재하는
-- 실제 FK 제약을 H2에 동일하게 추가하여, 회원 탈퇴 시 FK 위반이 실제로
-- 발생/해소되는지를 검증할 수 있도록 한다.

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_sender
    FOREIGN KEY (sender_id) REFERENCES users(id);

ALTER TABLE message_reactions
    ADD CONSTRAINT fk_message_reactions_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE message_reactions
    ADD CONSTRAINT fk_message_reactions_message
    FOREIGN KEY (message_id) REFERENCES messages(id);

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_reported_user
    FOREIGN KEY (reported_user_id) REFERENCES users(id);

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_reporter
    FOREIGN KEY (reporter_id) REFERENCES users(id);

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_reported_msg
    FOREIGN KEY (reported_message_id) REFERENCES messages(id);

ALTER TABLE hidden_friends
    ADD CONSTRAINT fk_hidden_friends_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE hidden_friends
    ADD CONSTRAINT fk_hidden_friends_friend
    FOREIGN KEY (friend_id) REFERENCES users(id);
