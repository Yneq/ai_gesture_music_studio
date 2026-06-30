package com.vance.gesturemusicstudio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 對應 favorite_layout 資料表。
 * layoutJson 存使用者自訂的圓形音階排列(JSON 字串),
 * 之後若想用 PostgreSQL 的 JSONB 型別做查詢優化,可以再調整成 columnDefinition = "jsonb"。
 */
@Entity
@Table(name = "favorite_layout")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "layout_json", nullable = false, columnDefinition = "TEXT")
    private String layoutJson;
}
