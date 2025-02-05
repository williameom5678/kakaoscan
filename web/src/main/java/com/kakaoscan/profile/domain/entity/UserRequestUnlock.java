package com.kakaoscan.profile.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "tb_req_unlock")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class UserRequestUnlock implements Serializable {
    /**
     * 이메일 아이디
     */
    @Id
    private String email;
    /**
     * 신청 메세지
     */
    private String message;
    /**
     * 수정 날짜
     */
    @UpdateTimestamp
    private LocalDateTime modifyDt;

    @OneToOne
    @JoinColumn(name = "email")
    private User user;
}
