package com.nasigolang.ddbnb.member.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity(name = "Member")
@Table(name = "member")
@SequenceGenerator(name = "member_sequence_generator", sequenceName = "sequence_member_id", initialValue = 1, allocationSize = 50)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_sequence_generator")
    @Column(name = "member_id")
    private long memberId;

    @Column(name = "nickname", unique = true, nullable = false)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "reported_count", nullable = false)
    private int reportedCount;

    @Column(name = "social_login", nullable = false)
    private String socialLogin;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "access_token_expire_date", nullable = false)
    private long accessTokenExpireDate;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "refresh_token_expire_date", nullable = false)
    private long refreshTokenExpireDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "sign_date", nullable = false)
    private LocalDate signDate;

    @Column(name = "deleted_date")
    private LocalDate deletedDate;

    @Column(name = "preferred_area")
    private String preferredArea;

    @Column(name = "per_sitter_career")
    private String petStitterCarrer;

    @Column(name = "detailed_history")
    private String detailedHistory;

    @Column(name = "period")
    private String period;

    @Column(name = "star_point")
    private String starPoint;

    @Column(name = "status")
    private String status;

    @Column(name = "LAST_VISIT_DATE")
    private LocalDate lastVisitDate;
}