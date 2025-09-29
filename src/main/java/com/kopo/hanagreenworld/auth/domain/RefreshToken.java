package com.kopo.hanagreenworld.auth.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens", 
       uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
@Getter
@NoArgsConstructor
public class RefreshToken extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public RefreshToken(Member member, String refreshToken) {
        this.member = member;
        this.refreshToken = refreshToken;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        this.isActive = true;
    }
}
