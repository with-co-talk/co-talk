package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.ChatRoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 ChatRoom과 매핑된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    private String name;

    @Column(length = 500)
    private String announcement;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoom.ChatRoomType type;
}
