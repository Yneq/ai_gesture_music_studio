# AI Gesture Music Studio

用 webcam 手勢辨識即時演奏音樂的全端互動樂器專案。

架構:Python(MediaPipe 手勢辨識)→ Spring Boot(商業邏輯、JWT、WebSocket、PostgreSQL)→ Vue 3 Dashboard(Web Audio API 即時發聲)。

## 目前進度

- [x] **Step 1 — 建立專案架構**(目前位置)
- [ ] Step 2 — JWT 登入 / 註冊
- [ ] Step 3 — AI 手勢辨識(MediaPipe)
- [ ] Step 4 — WebSocket 即時同步(雙管線:指令型 + 演奏型)
- [ ] Step 5 — Web Audio API 播放器 + 圓形音階 UI
- [ ] Step 6 — Vue Dashboard
- [ ] Step 7 — Chart.js 統計

## 專案結構

```
ai-gesture-music-studio/
├── backend/        # Spring Boot 3 + Java 17 + PostgreSQL
├── frontend/        # Vue 3 + Vite + Tailwind v4 + Pinia + Chart.js
└── gesture_ai/       # Python + MediaPipe Hands + OpenCV
```

## Backend 啟動方式

需求:Java 17+、Maven、本機 PostgreSQL。

```bash
# 1. 建立資料庫
createdb gesture_music_studio

# 2. 設定環境變數(密碼不要寫進程式碼或 commit)
export DB_USERNAME=postgres
export DB_PASSWORD=你的密碼

# 3. 啟動(第一次啟動 Hibernate 會自動依照 Entity 建出 4 張表)
cd backend
mvn spring-boot:run
```

啟動後可以用 `curl http://localhost:8080/actuator/health` 之類的方式確認(目前還沒加 actuator,Step 1 只要 App 能正常啟動、不報錯即可)。

## Frontend 啟動方式

```bash
cd frontend
npm install
npm run dev
```

開 `http://localhost:5173` 應該會看到一個深色背景、寫著「AI Gesture Music Studio」、Camera status: offline 的畫面 —— 這代表 Vue Router、Pinia、Tailwind 都正常運作。

## Python AI Service

```bash
cd gesture_ai
pip install -r requirements.txt
python main.py
```

目前只是骨架,執行只會印出一行提示訊息,實際 MediaPipe 偵測邏輯在 Step 3 才會加入。

## 技術筆記

- 手勢事件分成**兩條獨立管線**:離散指令手勢(Play/Pause/Next,REST POST + 時間型 debounce)與演奏型手勢(食指連續位置,WebSocket + 區域變化型 debounce)。兩者使用不同的 debounce 策略,詳見專案討論記錄。
- `SecurityConfig` 目前是 Step 1 暫時版本(全部端點 permitAll),Step 2 會換成正式的 JWT 驗證。
- 資料庫密碼一律走環境變數,不寫死在 `application.yml` 裡。
