"""
把辨識結果送到 Spring Boot 後端：
- 離散指令手勢 -> POST /api/gesture（時間型 debounce，同一手勢 500ms 內只送一次）
- 演奏音符事件 -> POST /api/music-events（zone-change debounce，指尖移到新音才送）
  後端收到後立即透過 STOMP /topic/notes 廣播給 Vue Dashboard

Step 4 實作：登入取 JWT → 非同步 REST POST → 雙重 debounce
"""

import threading
import time

import requests

from config import BACKEND_HTTP_BASE_URL, GESTURE_DEBOUNCE_COOLDOWN_MS


class GestureSender:
    def __init__(self, http_base_url: str, ws_url: str):
        self.http_base_url = http_base_url
        self._token: str | None = None
        self._last_gesture_ms: dict[str, int] = {}
        self._last_note_zone: str | None = None
        self._lock = threading.Lock()

    # ------------------------------------------------------------------
    # 公開 API
    # ------------------------------------------------------------------

    def login(self, username: str, password: str) -> bool:
        """向後端登入取得 JWT。啟動時呼叫一次即可。"""
        try:
            resp = requests.post(
                f"{self.http_base_url}/auth/login",
                json={"username": username, "password": password},
                timeout=5,
            )
            if resp.ok:
                self._token = resp.json().get("token")
                print(f"[Sender] 後端登入成功（user: {username}）")
                return True
            print(f"[Sender] 登入失敗 HTTP {resp.status_code}: {resp.text[:120]}")
        except requests.exceptions.ConnectionError:
            print("[Sender] 無法連線後端，請確認 Spring Boot 已啟動。")
        except Exception as e:
            print(f"[Sender] 登入例外: {e}")
        return False

    def send_gesture_command(self, gesture: str, confidence: float) -> None:
        """
        POST /api/gesture。
        同一手勢在 GESTURE_DEBOUNCE_COOLDOWN_MS 毫秒內只送一次（時間型 debounce）。
        """
        now_ms = self._now_ms()
        with self._lock:
            last = self._last_gesture_ms.get(gesture, 0)
            if now_ms - last < GESTURE_DEBOUNCE_COOLDOWN_MS:
                return
            self._last_gesture_ms[gesture] = now_ms

        threading.Thread(
            target=self._post,
            args=(
                f"{self.http_base_url}/gesture",
                {"gesture": gesture, "confidence": confidence},
            ),
            daemon=True,
        ).start()

    def send_note_event(self, note: str, instrument: str, volume: int) -> None:
        """
        POST /api/music-events。
        只有當音階區域改變時才送（zone-change debounce）。
        後端會將事件存入 DB 並廣播到 /topic/notes。
        """
        with self._lock:
            if note == self._last_note_zone:
                return
            self._last_note_zone = note

        threading.Thread(
            target=self._post,
            args=(
                f"{self.http_base_url}/music-events",
                {"note": note, "instrument": instrument, "volume": volume},
            ),
            daemon=True,
        ).start()

    def reset_note_zone(self) -> None:
        """手離開圓盤時呼叫，讓同一音可以重新觸發。"""
        with self._lock:
            self._last_note_zone = None

    # ------------------------------------------------------------------
    # 內部工具
    # ------------------------------------------------------------------

    def _now_ms(self) -> int:
        return int(time.monotonic() * 1000)

    def _headers(self) -> dict:
        if self._token:
            return {"Authorization": f"Bearer {self._token}"}
        return {}

    def _post(self, url: str, payload: dict) -> None:
        if not self._token:
            return
        try:
            requests.post(url, json=payload, headers=self._headers(), timeout=3)
        except Exception:
            pass
