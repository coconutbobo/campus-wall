// 校园墙项目
// 数据库实体：对应一张表，字段和数据库设计文档里的 Campus 保持一致。
package com.campuswall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campuses")
@Getter
@Setter
@NoArgsConstructor
public class Campus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(length = 200)
    private String description;
}
