// 校园墙项目
// 数据库实体：对应一张表，字段和数据库设计文档里的 SecondhandDetail 保持一致。
package com.campuswall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "secondhand_details")
@Getter
@Setter
@NoArgsConstructor
public class SecondhandDetail {
    @Id
    private Long postId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    private BigDecimal price;

    @Column(length = 64)
    private String conditionText;
}
