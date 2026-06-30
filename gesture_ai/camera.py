"""
負責開啟 webcam、逐幀讀取畫面。
"""

import cv2


class Camera:
    def __init__(self, camera_index: int = 0, width: int = 640, height: int = 480):
        self.camera_index = camera_index
        self.width = width
        self.height = height
        self.cap = cv2.VideoCapture(camera_index)

        if not self.cap.isOpened():
            raise RuntimeError(f"無法開啟攝影機 (index={camera_index})")

        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

    def read_frame(self):
        """回傳目前畫面 (BGR numpy array)，讀取失敗回傳 None。"""
        success, frame = self.cap.read()
        if not success:
            return None
        return cv2.flip(frame, 1)

    def release(self):
        """釋放攝影機資源。"""
        if self.cap is not None:
            self.cap.release()
            self.cap = None
