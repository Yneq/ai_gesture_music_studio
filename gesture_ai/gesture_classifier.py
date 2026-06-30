"""
根據 21 個 landmark 座標，分類成離散指令手勢
(OPEN_HAND / FIST / THUMB_UP / OK / PEACE / POINT)，
並計算信心值。
也負責「食指位置 -> 圓形音階區域」的連續座標轉換。
"""

import math
from dataclasses import dataclass
from typing import Optional, Tuple

from config import (
    NOTE_ZONE_CENTER_X,
    NOTE_ZONE_CENTER_Y,
    NOTE_ZONE_INNER_RADIUS,
    NOTE_ZONE_OUTER_RADIUS,
    NOTES,
)

# MediaPipe 21 點索引
WRIST = 0
THUMB_TIP, THUMB_IP, THUMB_MCP = 4, 3, 2
INDEX_TIP, INDEX_PIP = 8, 6
MIDDLE_TIP, MIDDLE_PIP = 12, 10
RING_TIP, RING_PIP = 16, 14
PINKY_TIP, PINKY_PIP = 20, 18


@dataclass
class ClassificationResult:
    gesture: str
    confidence: float


class GestureClassifier:
    def classify(self, landmarks, handedness: str = "Right") -> ClassificationResult:
        """
        回傳 (gesture_name, confidence)。
        依特定手勢優先序評分，取信心值最高者。
        """
        candidates = [
            self._score_ok(landmarks),
            self._score_peace(landmarks),
            self._score_point(landmarks),
            self._score_thumb_up(landmarks, handedness),
            self._score_fist(landmarks, handedness),
            self._score_open_hand(landmarks, handedness),
        ]

        best = max(candidates, key=lambda c: c.confidence)
        if best.confidence < 0.5:
            return ClassificationResult(gesture="UNKNOWN", confidence=best.confidence)
        return best

    def map_to_note_zone(self, index_fingertip_xy: Tuple[float, float]) -> Optional[str]:
        """
        回傳食指目前所在的音階區域（例如 "C4"），不在圓盤範圍內回傳 None。
        index_fingertip_xy 為正規化座標 (0~1, 0~1)。
        """
        x, y = index_fingertip_xy
        dx = x - NOTE_ZONE_CENTER_X
        dy = y - NOTE_ZONE_CENTER_Y
        distance = math.sqrt(dx * dx + dy * dy)

        if distance < NOTE_ZONE_INNER_RADIUS or distance > NOTE_ZONE_OUTER_RADIUS:
            return None

        # 角度 0 在正右方，逆時針增加；偏移 -pi/2 讓正上方為第一個音
        angle = math.atan2(dy, dx) + math.pi / 2
        if angle < 0:
            angle += 2 * math.pi

        sector_size = 2 * math.pi / len(NOTES)
        index = int(angle / sector_size) % len(NOTES)
        return NOTES[index]

    def _finger_extended(self, landmarks, tip_idx: int, pip_idx: int) -> bool:
        return landmarks[tip_idx].y < landmarks[pip_idx].y

    def _thumb_extended(self, landmarks, handedness: str) -> bool:
        tip = landmarks[THUMB_TIP]
        ip = landmarks[THUMB_IP]
        if handedness == "Right":
            return tip.x > ip.x
        return tip.x < ip.x

    def _distance(self, a, b) -> float:
        return math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2)

    def _score_from_checks(self, gesture: str, checks: list[bool]) -> ClassificationResult:
        if not checks:
            return ClassificationResult(gesture=gesture, confidence=0.0)
        score = sum(1 for ok in checks if ok) / len(checks)
        return ClassificationResult(gesture=gesture, confidence=round(score, 2))

    def _score_ok(self, landmarks) -> ClassificationResult:
        thumb_index_dist = self._distance(landmarks[THUMB_TIP], landmarks[INDEX_TIP])
        checks = [
            thumb_index_dist < 0.05,
            not self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            not self._finger_extended(landmarks, RING_TIP, RING_PIP),
            not self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("OK", checks)

    def _score_peace(self, landmarks) -> ClassificationResult:
        checks = [
            self._finger_extended(landmarks, INDEX_TIP, INDEX_PIP),
            self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            not self._finger_extended(landmarks, RING_TIP, RING_PIP),
            not self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("PEACE", checks)

    def _score_point(self, landmarks) -> ClassificationResult:
        checks = [
            self._finger_extended(landmarks, INDEX_TIP, INDEX_PIP),
            not self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            not self._finger_extended(landmarks, RING_TIP, RING_PIP),
            not self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("POINT", checks)

    def _score_thumb_up(self, landmarks, handedness: str) -> ClassificationResult:
        checks = [
            self._thumb_extended(landmarks, handedness),
            not self._finger_extended(landmarks, INDEX_TIP, INDEX_PIP),
            not self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            not self._finger_extended(landmarks, RING_TIP, RING_PIP),
            not self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("THUMB_UP", checks)

    def _score_fist(self, landmarks, handedness: str) -> ClassificationResult:
        checks = [
            not self._thumb_extended(landmarks, handedness),
            not self._finger_extended(landmarks, INDEX_TIP, INDEX_PIP),
            not self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            not self._finger_extended(landmarks, RING_TIP, RING_PIP),
            not self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("FIST", checks)

    def _score_open_hand(self, landmarks, handedness: str) -> ClassificationResult:
        checks = [
            self._thumb_extended(landmarks, handedness),
            self._finger_extended(landmarks, INDEX_TIP, INDEX_PIP),
            self._finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP),
            self._finger_extended(landmarks, RING_TIP, RING_PIP),
            self._finger_extended(landmarks, PINKY_TIP, PINKY_PIP),
        ]
        return self._score_from_checks("OPEN_HAND", checks)
