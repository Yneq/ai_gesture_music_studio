"""
包裝 MediaPipe HandLandmarker（Tasks API），
負責偵測手部並回傳 21 個 landmark 座標。
"""

from dataclasses import dataclass
from pathlib import Path
from typing import Optional
from urllib.request import urlretrieve

import cv2
import mediapipe as mp
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import (
    HandLandmarker,
    HandLandmarkerOptions,
    HandLandmarksConnections,
    RunningMode,
)

MODEL_PATH = Path(__file__).resolve().parent / "models" / "hand_landmarker.task"
MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/"
    "hand_landmarker/float16/1/hand_landmarker.task"
)


def _ensure_model():
    if MODEL_PATH.exists():
        return
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    print(f"下載 MediaPipe 模型至 {MODEL_PATH} ...")
    urlretrieve(MODEL_URL, MODEL_PATH)
    print("模型下載完成。")


@dataclass
class HandDetection:
    """單手偵測結果。"""

    landmarks: list  # 21 個 NormalizedLandmark (x, y, z 皆為 0~1)
    handedness: str  # "Left" 或 "Right"


class HandDetector:
    def __init__(
        self,
        max_num_hands: int = 1,
        min_detection_confidence: float = 0.7,
        min_tracking_confidence: float = 0.5,
    ):
        _ensure_model()

        options = HandLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(MODEL_PATH)),
            running_mode=RunningMode.IMAGE,
            num_hands=max_num_hands,
            min_hand_detection_confidence=min_detection_confidence,
            min_hand_presence_confidence=min_tracking_confidence,
            min_tracking_confidence=min_tracking_confidence,
        )
        self._landmarker = HandLandmarker.create_from_options(options)
        self._connections = HandLandmarksConnections.HAND_CONNECTIONS

    def detect(self, frame) -> Optional[HandDetection]:
        """
        輸入 BGR 影格，回傳第一隻手的 landmark；未偵測到則回傳 None。
        """
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_frame)
        results = self._landmarker.detect(mp_image)

        if not results.hand_landmarks or not results.handedness:
            return None

        landmarks = results.hand_landmarks[0]
        handedness = results.handedness[0][0].category_name

        return HandDetection(landmarks=landmarks, handedness=handedness)

    def draw_landmarks(self, frame, detection: HandDetection):
        """在畫面上繪製手部骨架（供預覽用）。"""
        if detection is None:
            return frame

        annotated = frame.copy()
        h, w = annotated.shape[:2]
        points = [
            (int(lm.x * w), int(lm.y * h))
            for lm in detection.landmarks
        ]

        for connection in self._connections:
            start_idx = connection.start
            end_idx = connection.end
            cv2.line(
                annotated,
                points[start_idx],
                points[end_idx],
                (0, 255, 0),
                2,
            )

        for x, y in points:
            cv2.circle(annotated, (x, y), 4, (0, 200, 255), -1)

        return annotated

    def close(self):
        self._landmarker.close()
