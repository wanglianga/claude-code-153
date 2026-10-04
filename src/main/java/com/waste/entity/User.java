package com.waste.entity;

import com.waste.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户（业主/施工队/物业/清运公司/财务/管理员） */
@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(length = 32)
    private String phone;

    /** 施工队名称 / 清运公司名称（业主、物业、财务可为空） */
    @Column(length = 64)
    private String teamName;

    private LocalDateTime createdAt = LocalDateTime.now();
}
