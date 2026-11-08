# -*- coding: utf-8 -*-
import os
import cv2
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision

def test_user_images_tasks():
    user_images_dir = r"C:\Users\Jordan Guaman\.gemini\antigravity\brain\9cb8de02-156f-4cf8-92ce-7cabde3880c6\.user_uploaded"
    model_path = r"C:\Users\Jordan Guaman\Desktop\AR-LenguaDeSenas\android_app\app\src\main\assets\hand_landmarker.task"

    if not os.path.exists(model_path):
        print(f"Modelo no encontrado en: {model_path}")
        return

    base_options = python.BaseOptions(model_asset_path=model_path)
    options = vision.HandLandmarkerOptions(
        base_options=base_options,
        num_hands=2,
        min_hand_detection_confidence=0.25,
        min_hand_presence_confidence=0.25,
        min_tracking_confidence=0.25
    )
    detector = vision.HandLandmarker.create_from_options(options)

    images = [os.path.join(user_images_dir, f) for f in os.listdir(user_images_dir) if f.endswith(('.jpg', '.png'))]
    print(f"Total imágenes: {len(images)}. Probando con MediaPipe Tasks...")

    for img_path in images[-6:]:
        img_name = os.path.basename(img_path)
        img_cv = cv2.imread(img_path)
        if img_cv is None:
            continue

        # Recortar solo la región de la pantalla del monitor (dentro del recuadro verde)
        # La captura completa es un screenshot vertical del teléfono 720x1600.
        # En la imagen del teléfono, la cámara de video de YouTube está en la región central:
        h, w, _ = img_cv.shape
        # Región de interés central (la cámara del video):
        roi = img_cv[int(h * 0.30) : int(h * 0.70), int(w * 0.10) : int(w * 0.90)]
        roi_rgb = cv2.cvtColor(roi, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=roi_rgb)

        detection_result = detector.detect(mp_image)
        if detection_result.hand_landmarks:
            num_hands = len(detection_result.hand_landmarks)
            print(f"[+] {img_name}: ¡EXITOSO! Detectó {num_hands} mano(s) en la región de la cámara.")
            for i, hand in enumerate(detection_result.hand_landmarks):
                print(f"    Mano {i+1}: Muñeca en (x={hand[0].x:.2f}, y={hand[0].y:.2f})")
        else:
            # Probar imagen completa
            full_rgb = cv2.cvtColor(img_cv, cv2.COLOR_BGR2RGB)
            mp_full = mp.Image(image_format=mp.ImageFormat.SRGB, data=full_rgb)
            res_full = detector.detect(mp_full)
            if res_full.hand_landmarks:
                print(f"[+] {img_name}: Detectó {len(res_full.hand_landmarks)} mano(s) en imagen completa.")
            else:
                print(f"[-] {img_name}: No detectó manos ni en ROI ni en completa.")

if __name__ == '__main__':
    test_user_images_tasks()
