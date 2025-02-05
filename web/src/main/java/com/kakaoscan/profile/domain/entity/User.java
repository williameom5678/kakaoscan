package com.kakaoscan.profile.domain.entity;

import com.kakaoscan.profile.domain.enums.Role;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity(name = "tb_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User implements Serializable {
    /**
     * 이메일 아이디
     */
    @Id
    private String email;

    private long totalUseCount;
    /**
     * 권한
     */
    @Enumerated(EnumType.STRING)
    private Role role;
    /**
     * 수정 날짜
     */
    @UpdateTimestamp
    private LocalDateTime modifyDt;

    @CreationTimestamp
    private LocalDateTime createDt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserRequestUnlock requestUnlock;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserRequest request;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<UserHistory> historyList;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserLog> logList;

    public void setRole(Role role) {
        this.role = role;
    }

    public void incTotalUseCount() {
        this.totalUseCount++;
    }
}
