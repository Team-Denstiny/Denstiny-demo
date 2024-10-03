package com.example.board;

import com.example.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "board", indexes = @Index(name = "board_idx_board_id", columnList = "board_id"))
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "category", nullable = false)
    private Integer category;

    @ColumnDefault("0")
    @Column(name = "view_count",nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @ColumnDefault("0")
    @Column(name = "comment_count",nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @ColumnDefault("0")
    @Column(name = "heart_count",nullable = false)
    @Builder.Default
    private Integer heartCount = 0;

    @Column(name = "writer", nullable = false)
    private Long writer;

    // 다대일 양방향 참조 필요시 사용
//    @OneToMany(mappedBy = "board")
//    private List<BoardImageEntity> boardImages = new ArrayList<>();
}
