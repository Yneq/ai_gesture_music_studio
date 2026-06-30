package com.vance.gesturemusicstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 對應 gesture_history 資料表。
 * 只記錄「離散指令手勢」(OPEN_HAND/FIST/THUMB_UP...),
 * 不包含圓形音階的連續演奏事件 -> 那一類存在 MusicEvent。
 */
@Entity
@Table(name = "gesture_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GestureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String gesture;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
