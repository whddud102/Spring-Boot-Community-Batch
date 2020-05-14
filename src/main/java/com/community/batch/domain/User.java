package com.community.batch.domain;

import com.community.batch.domain.enums.Grade;
import com.community.batch.domain.enums.SocialType;
import com.community.batch.domain.enums.UserStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;


@Getter
@EqualsAndHashCode(of = {"idx", "email"})
@NoArgsConstructor
@Entity
@Table
public class User implements Serializable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column
    private String name;

    @Column
    private String password;

    @Column
    private String email;
    
    @Column
    private String principal;   // OAuth2 인증으로 제공 받은 키 값
    
    @Column
    @Enumerated(EnumType.STRING)
    private SocialType socialType;      // 어떤 소셜 미디어로 인증 받았는지 여부

    @Column
    @Enumerated(EnumType.STRING)
    private UserStatus status;      // 휴면 여부

    @Column
    @Enumerated(EnumType.STRING)
    private Grade grade;    // 회원 등급

    @Column
    private LocalDateTime createdDate;

    @Column
    private LocalDateTime updatedDate;

    @Builder
    public User(String name, String password, String email, String principal, SocialType socialType, LocalDateTime createdDate, LocalDateTime updateDate) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.principal = principal;
        this.socialType = socialType;
        this.createdDate = createdDate;
        this.updatedDate = updateDate;
    }

    /**
     * User를 휴면 상태로 전환
     * @return User 객체
     */
    public User setInactive() {
        status = UserStatus.INACTIVE;
        return this;
    }

}
