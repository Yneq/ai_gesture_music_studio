package com.vance.gesturemusicstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 對應 music_events 資料表。
 * 每一筆代表圓形音階 UI 上一次 note on(進入新音階區域)。
 * note 存音名(例如 "C4"、"D4"),instrument 存樂器名稱(piano/guitar/synth/drum)。
 */
@Entity
@Table(name = "music_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String note;

    @Column(nullable = false, length = 20)
    private String instrument;

    @Column(nullable = false)
    private Integer volume;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
