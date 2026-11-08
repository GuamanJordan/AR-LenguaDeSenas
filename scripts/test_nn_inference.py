# -*- coding: utf-8 -*-
"""
Prueba las predicciones del modelo de red neuronal sobre las fotos reales del usuario.
"""
import os
import cv2
import numpy as np
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision

def test_inference_on_user_photos():
    model_path = r"C:\Users\Jordan Guaman\Desktop\AR-LenguaDeSenas\android_app\app\src\main\assets\hand_landmarker.task"
    weights_path = r"C:\Users\Jordan Guaman\Desktop\AR-LenguaDeSenas\android_app\app\src\main\assets\lsec_nn_weights.bin"
    user_images_dir = r"C:\Users\Jordan Guaman\.gemini\antigravity\brain\9cb8de02-156f-4cf8-92ce-7cabde3880c6\.user_uploaded"

    base_options = python.BaseOptions(model_asset_path=model_path)
    options = vision.HandLandmarkerOptions(
        base_options=base_options,
        num_hands=2,
        min_hand_detection_confidence=0.25,
        min_hand_presence_confidence=0.25,
        min_tracking_confidence=0.25
    )
    detector = vision.HandLandmarker.create_from_options(options)

    # Cargar pesos del modelo binario
    with open(weights_path, "rb") as f:
        magic = f.read(4)
        import struct
        header = f.read(20)
        in_dim, h1, h2, num_classes, layers = struct.unpack("<5I", header)
        w1 = np.frombuffer(f.read(in_dim * h1 * 4), dtype=np.float32).reshape(in_dim, h1)
        b1 = np.frombuffer(f.read(h1 * 4), dtype=np.float32).reshape(1, h1)
        w2 = np.frombuffer(f.read(h1 * h2 * 4), dtype=np.float32).reshape(h1, h2)
        b2 = np.frombuffer(f.read(h2 * 4), dtype=np.float32).reshape(1, h2)
        w3 = np.frombuffer(f.read(h2 * num_classes * 4), dtype=np.float32).reshape(h2, num_classes)
        b3 = np.frombuffer(f.read(num_classes * 4), dtype=np.float32).reshape(1, num_classes)

    classes = [
        "HOLA", "BUENOS DÍAS", "BUENAS TARDES", "BUENAS NOCHES", "CHAO",
        "¿CÓMO ESTÁS?", "MUCHO GUSTO", "GRACIAS", "POR FAVOR", "DE NADA",
        "AYUDA", "SÍ", "NO", "TE QUIERO"
    ]

    images = [os.path.join(user_images_dir, f) for f in os.listdir(user_images_dir) if f.endswith(('.jpg', '.png'))]

    print("=================================================================")
    print("      TESTEO DIRECTO DEL MODELO SOBRE CAPTURAS REALES DEL USUARIO")
    print("=================================================================")

    for img_path in images[-6:]:
        img_name = os.path.basename(img_path)
        img_cv = cv2.imread(img_path)
        if img_cv is None:
            continue

        h, w, _ = img_cv.shape
        roi = img_cv[int(h * 0.30) : int(h * 0.70), int(w * 0.10) : int(w * 0.90)]
        roi_rgb = cv2.cvtColor(roi, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=roi_rgb)
        res = detector.detect(mp_image)

        if not res.hand_landmarks:
            full_rgb = cv2.cvtColor(img_cv, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=full_rgb)
            res = detector.detect(mp_image)

        if res.hand_landmarks:
            hands_count = len(res.hand_landmarks)
            h1 = np.array([[lm.x, lm.y, lm.z] for lm in res.hand_landmarks[0]]).flatten()
            h2 = np.array([[lm.x, lm.y, lm.z] for lm in res.hand_landmarks[1]]).flatten() if hands_count > 1 else np.zeros(63, dtype=np.float32)

            feat = np.zeros(128, dtype=np.float32)
            feat[:63] = h1
            feat[63:126] = h2
            if hands_count > 1:
                feat[126] = abs(h1[0] - h2[0])
                feat[127] = abs(h1[1] - h2[1])

            # Inferencia
            z1 = np.maximum(0, np.dot(feat, w1) + b1)
            z2 = np.maximum(0, np.dot(z1, w2) + b2)
            logits = np.dot(z2, w3) + b3
            exp_l = np.exp(logits - np.max(logits))
            probs = exp_l / np.sum(exp_l)
            pred_idx = np.argmax(probs)
            pred_class = classes[pred_idx]
            conf = probs[0][pred_idx]

            print(f"[*] {img_name} ({hands_count} mano(s)): ➔ PREDICCIÓN: '{pred_class}' ({conf*100:.1f}%)")
        else:
            print(f"[-] {img_name}: No se detectaron manos en la captura.")

if __name__ == '__main__':
    test_inference_on_user_photos()

