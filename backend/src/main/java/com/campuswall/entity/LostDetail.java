// 校园墙项目
// 数据库实体：对应一张表，字段和数据库设计文档里的 LostDetail 保持一致。
package com.campuswall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lost_details")
@Getter
@Setter
@NoArgsConstructor
public class LostDetail {
    @Id
    private Long postId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(length = 16)
    private String kind;

    @Column(length = 128)
    private String location;

    private LocalDateTime happenedAt;
}
