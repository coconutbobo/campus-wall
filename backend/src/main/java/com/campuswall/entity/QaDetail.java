// 校园墙项目
// 数据库实体：对应一张表，字段和数据库设计文档里的 QaDetail 保持一致。
package com.campuswall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "qa_details")
@Getter
@Setter
@NoArgsConstructor
public class QaDetail {
    @Id
    private Long postId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    private boolean solved = false;

    private Long acceptedCommentId;
}
