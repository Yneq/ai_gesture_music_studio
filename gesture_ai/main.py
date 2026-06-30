"""
AI Service 進入點：整合 camera -> detector -> classifier -> sender。
Step 4：接上 GestureSender，把辨識結果同步到 Spring Boot 後端。
"""

import math
from typing import Optional

import cv2
import numpy as np

from config import (
    AI_SERVICE_PASSWORD,
    AI_SERVICE_USERNAME,
    BACKEND_HTTP_BASE_URL,
    BACKEND_WS_URL,
    CAMERA_INDEX,
    COMMAND_GESTURES,
    CONFIDENCE_THRESHOLD,
    DEFAULT_INSTRUMENT,
    DEFAULT_VOLUME,
    FRAME_HEIGHT,
    FRAME_WIDTH,
    NOTE_ZONE_CENTER_X,
    NOTE_ZONE_CENTER_Y,
    NOTE_ZONE_INNER_RADIUS,
    NOTE_ZONE_OUTER_RADIUS,
    NOTES,
    PREVIEW_WINDOW_NAME,
    SHOW_NOTE_ZONE_OVERLAY,
)
from camera import Camera
from detector import HandDetector
from gesture_classifier import INDEX_TIP, GestureClassifier
from sender import GestureSender


def _sector_polygon(center, inner_r, outer_r, start_a, end_a, n=24):
    """產生環形扇區的多邊形頂點（numpy int32 陣列）。"""
    angles = [start_a + (end_a - start_a) * t / n for t in range(n + 1)]
    outer_pts = [(int(center[0] + outer_r * math.cos(a)),
                  int(center[1] + outer_r * math.sin(a))) for a in angles]
    inner_pts = [(int(center[0] + inner_r * math.cos(a)),
                  int(center[1] + inner_r * math.sin(a))) for a in reversed(angles)]
    return np.array(outer_pts + inner_pts, dtype=np.int32)


def _draw_note_zone_overlay(frame):
    """在預覽畫面上繪製圓形音階區域：偶數扇區 65%、奇數扇區 30% 不透明。"""
    h, w = frame.shape[:2]
    cx = int(NOTE_ZONE_CENTER_X * w)
    cy = int(NOTE_ZONE_CENTER_Y * h)
    center = (cx, cy)
    outer_r = int(NOTE_ZONE_OUTER_RADIUS * min(w, h))
    inner_r = int(NOTE_ZONE_INNER_RADIUS * min(w, h))
    sector_count = len(NOTES)
    sector_angle = 2 * math.pi / sector_count
    fill_color = (60, 65, 80)

    # Pass 1: 奇數扇區 30% 不透明
    layer = frame.copy()
    for i in range(sector_count):
        if i % 2 != 0:
            start_a = sector_angle * i - math.pi / 2
            pts = _sector_polygon(center, inner_r, outer_r, start_a, start_a + sector_angle)
            cv2.fillPoly(layer, [pts], fill_color)
    cv2.addWeighted(layer, 0.30, frame, 0.70, 0, frame)

    # Pass 2: 偶數扇區 65% 不透明
    layer = frame.copy()
    for i in range(sector_count):
        if i % 2 == 0:
            start_a = sector_angle * i - math.pi / 2
            pts = _sector_polygon(center, inner_r, outer_r, start_a, start_a + sector_angle)
            cv2.fillPoly(layer, [pts], fill_color)
    cv2.addWeighted(layer, 0.65, frame, 0.35, 0, frame)

    # 分隔線、標籤、圓框直接畫在合成後的 frame 上
    for i, note in enumerate(NOTES):
        start_a = sector_angle * i - math.pi / 2
        mid_a = start_a + sector_angle / 2

        cv2.line(
            frame,
            (int(cx + inner_r * math.cos(start_a)), int(cy + inner_r * math.sin(start_a))),
            (int(cx + outer_r * math.cos(start_a)), int(cy + outer_r * math.sin(start_a))),
            (100, 110, 140), 1,
        )

        label_r = (inner_r + outer_r) // 2
        lx = int(cx + label_r * math.cos(mid_a))
        ly = int(cy + label_r * math.sin(mid_a))
        cv2.putText(frame, note, (lx - 14, ly + 5),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.42, (220, 225, 240), 1, cv2.LINE_AA)

    cv2.circle(frame, center, outer_r, (80, 180, 255), 2)
    cv2.circle(frame, center, inner_r, (80, 180, 255), 1)


def _draw_hud(frame, gesture: str, confidence: float, note_zone: Optional[str], connected: bool):
    """繪製手勢與音階 HUD。"""
    status = "後端已連線" if connected else "未連線後端"
    lines = [
        f"Gesture: {gesture}",
        f"Confidence: {confidence:.2f}",
        f"Note zone: {note_zone or '-'}",
        f"Threshold: {CONFIDENCE_THRESHOLD}",
        f"Backend: {status}",
        "Press Q to quit",
    ]
    y = 30
    for line in lines:
        cv2.putText(
            frame,
            line,
            (10, y),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.6,
            (0, 255, 128),
            2,
            cv2.LINE_AA,
        )
        y += 28


def main():
    camera = Camera(camera_index=CAMERA_INDEX, width=FRAME_WIDTH, height=FRAME_HEIGHT)
    detector = HandDetector()
    classifier = GestureClassifier()
    sender = GestureSender(BACKEND_HTTP_BASE_URL, BACKEND_WS_URL)

    connected = False
    if AI_SERVICE_USERNAME and AI_SERVICE_PASSWORD:
        connected = sender.login(AI_SERVICE_USERNAME, AI_SERVICE_PASSWORD)
    else:
        print("[main] 未設定 AI_SERVICE_USERNAME / AI_SERVICE_PASSWORD，以離線模式執行。")
        print("       啟動前請執行: export AI_SERVICE_USERNAME=xxx AI_SERVICE_PASSWORD=yyy")

    print("AI 手勢辨識已啟動。對著 webcam 比手勢，按 Q 離開。")

    try:
        while True:
            frame = camera.read_frame()
            if frame is None:
                print("讀取畫面失敗，結束程式。")
                break

            detection = detector.detect(frame)
            gesture = "NO_HAND"
            confidence = 0.0
            note_zone = None

            if detection is not None:
                result = classifier.classify(detection.landmarks, detection.handedness)
                gesture = result.gesture
                confidence = result.confidence

                index_tip = detection.landmarks[INDEX_TIP]
                note_zone = classifier.map_to_note_zone((index_tip.x, index_tip.y))

                frame = detector.draw_landmarks(frame, detection)

                if confidence >= CONFIDENCE_THRESHOLD:
                    if gesture in COMMAND_GESTURES:
                        sender.send_gesture_command(gesture, confidence)
                    elif gesture == "POINT" and note_zone is not None:
                        sender.send_note_event(note_zone, DEFAULT_INSTRUMENT, DEFAULT_VOLUME)

            # 手離開圓盤時重置，讓同一音可以重新觸發
            if note_zone is None:
                sender.reset_note_zone()

            if SHOW_NOTE_ZONE_OVERLAY:
                _draw_note_zone_overlay(frame)

            _draw_hud(frame, gesture, confidence, note_zone, connected)

            cv2.imshow(PREVIEW_WINDOW_NAME, frame)
            key = cv2.waitKey(1) & 0xFF
            if key in (ord("q"), ord("Q")):
                break
    finally:
        camera.release()
        detector.close()
        cv2.destroyAllWindows()
        print("AI Service 已停止。")


if __name__ == "__main__":
    main()
