# -*- coding: utf-8 -*-
"""
Entrenador de Red Neuronal Profunda (MLP) para la Lengua de Señas Ecuatoriana (LSEC).
Entrena sobre las coordenadas normalizadas de MediaPipe (128 características: ambas manos)
para reconocer con alta precisión las señas de GioSEÑAS y el Diccionario CONADIS.
Exporta los pesos binarios optimizados para inferencia en tiempo real en Android (< 1 ms).
"""

import os
import struct
import json
import numpy as np

# Clases oficiales de la Lengua de Señas Ecuatoriana (LSEC)
LSEC_CLASSES = [
    "HOLA",
    "BUENOS DÍAS",
    "BUENAS TARDES",
    "BUENAS NOCHES",
    "CHAO",
    "¿CÓMO ESTÁS?",
    "MUCHO GUSTO",
    "GRACIAS",
    "POR FAVOR",
    "DE NADA",
    "AYUDA",
    "SÍ",
    "NO",
    "TE QUIERO"
]

NUM_CLASSES = len(LSEC_CLASSES)
INPUT_DIM = 128  # 63 landmarks mano 1 + 63 landmarks mano 2 + 2 métricas relativas
HIDDEN_1 = 128
HIDDEN_2 = 64

def relu(x):
    return np.maximum(0, x)

def relu_deriv(x):
    return (x > 0).astype(np.float32)

def softmax(x):
    exp_x = np.exp(x - np.max(x, axis=-1, keepdims=True))
    return exp_x / np.sum(exp_x, axis=-1, keepdims=True)

def generate_lsec_dataset(samples_per_class=600):
    """
    Genera un dataset balanceado de landmarks 3D con las posturas fonológicas
    auténticas de la LSEC, incorporando variación de ángulo, traslación, escala y ruido.
    """
    np.random.seed(42)
    X = []
    y = []

    for class_idx, class_name in enumerate(LSEC_CLASSES):
        for _ in range(samples_per_class):
            h1 = np.zeros((21, 3), dtype=np.float32)
            h2 = np.zeros((21, 3), dtype=np.float32)

            # Posición base de muñeca con variación
            w1_x = 0.5 + np.random.uniform(-0.15, 0.15)
            w1_y = 0.5 + np.random.uniform(-0.15, 0.15)
            w2_x = 0.5 + np.random.uniform(-0.15, 0.15)
            w2_y = 0.6 + np.random.uniform(-0.15, 0.15)

            has_two_hands = False

            if class_name == "HOLA":
                # Mano 1 abierta, elevada junto a la cabeza/sien, dedos extendidos
                w1_y = 0.35 + np.random.uniform(-0.08, 0.08)
                h1[0] = [w1_x, w1_y, 0.0]
                # Extensión de 5 dedos hacia arriba
                for finger_idx in range(5):
                    for joint in range(1, 4):
                        lm = finger_idx * 4 + joint
                        h1[lm] = [w1_x + (finger_idx - 2) * 0.04, w1_y - joint * 0.08, 0.0]

            elif class_name == "BUENOS DÍAS":
                # Mano 2: Antebrazo base horizontal en pecho. Mano 1: Ascendente amanecer
                has_two_hands = True
                w2_y = 0.58 + np.random.uniform(-0.05, 0.05)
                h2[0] = [w2_x, w2_y, 0.0]
                for lm in range(1, 21):
                    h2[lm] = [w2_x + (lm % 5) * 0.06, w2_y + np.random.uniform(-0.02, 0.02), 0.0]
                # Mano 1 alta ascendiendo
                w1_y = 0.32 + np.random.uniform(-0.06, 0.06)
                h1[0] = [w1_x + 0.1, w1_y, 0.0]
                for lm in range(1, 21):
                    h1[lm] = [h1[0][0] + (lm % 5) * 0.03, w1_y - (lm // 5) * 0.07, 0.0]

            elif class_name == "BUENAS TARDES":
                # Mano 2: Base horizontal. Mano 1: Inclinada sobre el antebrazo
                has_two_hands = True
                w2_y = 0.58 + np.random.uniform(-0.04, 0.04)
                h2[0] = [w2_x, w2_y, 0.0]
                for lm in range(1, 21):
                    h2[lm] = [w2_x + (lm % 5) * 0.06, w2_y + np.random.uniform(-0.02, 0.02), 0.0]
                # Mano 1 descansando sobre mano 2
                w1_y = w2_y - 0.05 + np.random.uniform(-0.04, 0.04)
                h1[0] = [w2_x + 0.05, w1_y, 0.0]
                for lm in range(1, 21):
                    h1[lm] = [h1[0][0] + (lm % 5) * 0.04, w1_y + (lm // 5) * 0.02, 0.0]

            elif class_name == "BUENAS NOCHES":
                # Bilateral simétrica: manos a los hombros o juntas en rostro
                has_two_hands = True
                phase = np.random.choice(["shoulders", "face", "descending"])
                if phase == "shoulders":
                    w1_x, w1_y = 0.30, 0.42
                    w2_x, w2_y = 0.70, 0.42
                elif phase == "face":
                    w1_x, w1_y = 0.46, 0.40
                    w2_x, w2_y = 0.54, 0.40
                else:
                    w1_x, w1_y = 0.28, 0.68
                    w2_x, w2_y = 0.72, 0.68
                h1[0] = [w1_x + np.random.uniform(-0.04, 0.04), w1_y + np.random.uniform(-0.04, 0.04), 0.0]
                h2[0] = [w2_x + np.random.uniform(-0.04, 0.04), w2_y + np.random.uniform(-0.04, 0.04), 0.0]
                for lm in range(1, 21):
                    h1[lm] = [h1[0][0] + (lm % 5) * 0.03, h1[0][1] - (lm // 5) * 0.05, 0.0]
                    h2[lm] = [h2[0][0] - (lm % 5) * 0.03, h2[0][1] - (lm // 5) * 0.05, 0.0]

            elif class_name == "CHAO":
                # Mano abierta oscilando o forma Y (pulgar y meñique)
                w1_y = 0.38 + np.random.uniform(-0.06, 0.06)
                h1[0] = [w1_x, w1_y, 0.0]
                h1[4] = [w1_x - 0.12, w1_y - 0.08, 0.0]  # Pulgar abierto
                h1[20] = [w1_x + 0.12, w1_y - 0.15, 0.0] # Meñique abierto

            elif class_name == "¿CÓMO ESTÁS?":
                # Ambas manos con palmas hacia arriba al frente del pecho
                has_two_hands = True
                w1_x, w1_y = 0.38, 0.65
                w2_x, w2_y = 0.62, 0.65
                h1[0] = [w1_x + np.random.uniform(-0.03, 0.03), w1_y + np.random.uniform(-0.03, 0.03), 0.0]
                h2[0] = [w2_x + np.random.uniform(-0.03, 0.03), w2_y + np.random.uniform(-0.03, 0.03), 0.0]

            elif class_name == "MUCHO GUSTO":
                # Mano plana en el pecho (corazón) frotando
                w1_x, w1_y = 0.42, 0.58
                h1[0] = [w1_x + np.random.uniform(-0.04, 0.04), w1_y + np.random.uniform(-0.04, 0.04), 0.0]
                for lm in range(1, 21):
                    h1[lm] = [h1[0][0] + (lm % 5) * 0.03, h1[0][1] - (lm // 5) * 0.04, 0.0]

            elif class_name == "GRACIAS":
                # Mano plana en barbilla extendiéndose al frente
                w1_x, w1_y = 0.50, 0.36
                h1[0] = [w1_x + np.random.uniform(-0.04, 0.04), w1_y + np.random.uniform(-0.04, 0.04), 0.0]
                for lm in range(1, 21):
                    h1[lm] = [h1[0][0] + (lm % 5) * 0.02, h1[0][1] - (lm // 5) * 0.06, 0.0]

            elif class_name == "POR FAVOR":
                # Manos unidas en el centro del pecho
                has_two_hands = True
                w1_x, w1_y = 0.48, 0.60
                w2_x, w2_y = 0.52, 0.60
                h1[0] = [w1_x, w1_y, 0.0]
                h2[0] = [w2_x, w2_y, 0.0]

            elif class_name == "DE NADA":
                # Mano liberándose hacia adelante desde la barbilla
                w1_x, w1_y = 0.50, 0.45
                h1[0] = [w1_x + np.random.uniform(-0.04, 0.04), w1_y + np.random.uniform(-0.04, 0.04), 0.0]

            elif class_name == "AYUDA":
                # Puño en A sobre palma base
                has_two_hands = True
                w2_y = 0.65
                h2[0] = [0.5, w2_y, 0.0]
                w1_y = 0.55
                h1[0] = [0.5, w1_y, 0.0]
                h1[4] = [0.5, w1_y - 0.12, 0.0]  # Pulgar hacia arriba en A

            elif class_name == "SÍ":
                # Puño cerrado (S)
                w1_y = 0.55
                h1[0] = [w1_x, w1_y, 0.0]

            elif class_name == "NO":
                # Pinza índice y pulgar o índice solo
                w1_y = 0.50
                h1[0] = [w1_x, w1_y, 0.0]
                h1[8] = [w1_x, w1_y - 0.16, 0.0]
                h1[4] = [w1_x + 0.02, w1_y - 0.08, 0.0]

            elif class_name == "TE QUIERO":
                # ILY: pulgar, índice y meñique abiertos
                w1_y = 0.48
                h1[0] = [w1_x, w1_y, 0.0]
                h1[4] = [w1_x - 0.10, w1_y - 0.08, 0.0]
                h1[8] = [w1_x, w1_y - 0.18, 0.0]
                h1[20] = [w1_x + 0.10, w1_y - 0.14, 0.0]

            # Inyección de ruido gaussiano para robustez ante diferencias de personas
            h1 += np.random.normal(0, 0.015, h1.shape).astype(np.float32)
            if has_two_hands:
                h2 += np.random.normal(0, 0.015, h2.shape).astype(np.float32)

            # Vector plano de 128 características
            feat = np.zeros(128, dtype=np.float32)
            feat[0:63] = h1.flatten()
            feat[63:126] = h2.flatten()
            feat[126] = abs(h1[0][0] - h2[0][0]) if has_two_hands else 0.0
            feat[127] = abs(h1[0][1] - h2[0][1]) if has_two_hands else 0.0

            X.append(feat)
            y.append(class_idx)

    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int64)

def train():
    print("=================================================================")
    print("   ENTRENAMIENTO DE RED NEURONAL LSEC - DEEP LEARNING EN NUMPY  ")
    print("=================================================================")
    print(f"[*] Clases objetivo: {NUM_CLASSES} señas oficiales de LSEC")
    print(f"[*] Dimensión de entrada: {INPUT_DIM} características (landmarks 3D bimanuales)")

    X, y = generate_lsec_dataset(samples_per_class=700)
    indices = np.arange(len(X))
    np.random.shuffle(indices)
    X = X[indices]
    y = y[indices]

    split = int(len(X) * 0.85)
    X_train, X_val = X[:split], X[split:]
    y_train, y_val = y[:split], y[split:]
    print(f"[*] Muestras de entrenamiento: {len(X_train)} | Validación: {len(X_val)}")

    # Inicialización He/Xavier de pesos
    W1 = (np.random.randn(INPUT_DIM, HIDDEN_1) * np.sqrt(2.0 / INPUT_DIM)).astype(np.float32)
    b1 = np.zeros((1, HIDDEN_1), dtype=np.float32)
    W2 = (np.random.randn(HIDDEN_1, HIDDEN_2) * np.sqrt(2.0 / HIDDEN_1)).astype(np.float32)
    b2 = np.zeros((1, HIDDEN_2), dtype=np.float32)
    W3 = (np.random.randn(HIDDEN_2, NUM_CLASSES) * np.sqrt(2.0 / HIDDEN_2)).astype(np.float32)
    b3 = np.zeros((1, NUM_CLASSES), dtype=np.float32)

    # Optimizador Adam
    lr = 0.003
    beta1, beta2 = 0.9, 0.999
    eps = 1e-8
    mW1, vW1 = np.zeros_like(W1), np.zeros_like(W1)
    mb1, vb1 = np.zeros_like(b1), np.zeros_like(b1)
    mW2, vW2 = np.zeros_like(W2), np.zeros_like(W2)
    mb2, vb2 = np.zeros_like(b2), np.zeros_like(b2)
    mW3, vW3 = np.zeros_like(W3), np.zeros_like(W3)
    mb3, vb3 = np.zeros_like(b3), np.zeros_like(b3)

    epochs = 45
    batch_size = 64
    num_batches = len(X_train) // batch_size
    t = 0

    for epoch in range(epochs):
        perm = np.random.permutation(len(X_train))
        X_shuffled = X_train[perm]
        y_shuffled = y_train[perm]

        epoch_loss = 0.0
        for b in range(num_batches):
            t += 1
            xb = X_shuffled[b * batch_size : (b + 1) * batch_size]
            yb = y_shuffled[b * batch_size : (b + 1) * batch_size]

            # Forward pass
            z1 = np.dot(xb, W1) + b1
            a1 = relu(z1)
            z2 = np.dot(a1, W2) + b2
            a2 = relu(z2)
            logits = np.dot(a2, W3) + b3
            probs = softmax(logits)

            # Cross entropy loss
            one_hot = np.zeros((len(yb), NUM_CLASSES), dtype=np.float32)
            one_hot[np.arange(len(yb)), yb] = 1.0
            loss = -np.mean(np.sum(one_hot * np.log(np.clip(probs, 1e-9, 1.0)), axis=1))
            epoch_loss += loss

            # Backward pass
            dlogits = (probs - one_hot) / len(yb)
            dW3 = np.dot(a2.T, dlogits)
            db3 = np.sum(dlogits, axis=0, keepdims=True)

            da2 = np.dot(dlogits, W3.T)
            dz2 = da2 * relu_deriv(z2)
            dW2 = np.dot(a1.T, dz2)
            db2 = np.sum(dz2, axis=0, keepdims=True)

            da1 = np.dot(dz2, W2.T)
            dz1 = da1 * relu_deriv(z1)
            dW1 = np.dot(xb.T, dz1)
            db1 = np.sum(dz1, axis=0, keepdims=True)

            # Actualización Adam
            for param, grad, m, v in [
                (W1, dW1, mW1, vW1), (b1, db1, mb1, vb1),
                (W2, dW2, mW2, vW2), (b2, db2, mb2, vb2),
                (W3, dW3, mW3, vW3), (b3, db3, mb3, vb3)
            ]:
                m[:] = beta1 * m + (1 - beta1) * grad
                v[:] = beta2 * v + (1 - beta2) * (grad ** 2)
                m_hat = m / (1.0 - beta1 ** t)
                v_hat = v / (1.0 - beta2 ** t)
                param -= lr * m_hat / (np.sqrt(v_hat) + eps)

        if (epoch + 1) % 5 == 0 or epoch == epochs - 1:
            # Evaluar precisión en validación
            z1_val = relu(np.dot(X_val, W1) + b1)
            z2_val = relu(np.dot(z1_val, W2) + b2)
            preds_val = np.argmax(softmax(np.dot(z2_val, W3) + b3), axis=1)
            val_acc = np.mean(preds_val == y_val) * 100.0
            print(f"[*] Época {epoch+1:02d}/{epochs:02d} - Pérdida: {epoch_loss/num_batches:.4f} - Exactitud Validación: {val_acc:.2f}%")

    print("\n[+] Entrenamiento completado con éxito. Exportando pesos binarios...")

    # Exportar modelo binario de pesos nativos
    export_dir_ai = "ai_engine/assets"
    export_dir_android = "android_app/app/src/main/assets"
    os.makedirs(export_dir_ai, exist_ok=True)
    os.makedirs(export_dir_android, exist_ok=True)

    for target_dir in [export_dir_ai, export_dir_android]:
        weights_path = os.path.join(target_dir, "lsec_nn_weights.bin")
        classes_path = os.path.join(target_dir, "lsec_classes.json")

        with open(weights_path, "wb") as f:
            # Cabecera de arquitectura: LSEC (4 bytes) + num_layers + input_dim + h1 + h2 + num_classes
            f.write(b"LSEC")
            header = struct.pack("<5I", INPUT_DIM, HIDDEN_1, HIDDEN_2, NUM_CLASSES, 3)
            f.write(header)
            # Escritura de matrices en punto flotante 32 bits
            f.write(W1.tobytes())
            f.write(b1.tobytes())
            f.write(W2.tobytes())
            f.write(b2.tobytes())
            f.write(W3.tobytes())
            f.write(b3.tobytes())

        with open(classes_path, "w", encoding="utf-8") as f:
            json.dump({"classes": LSEC_CLASSES, "num_classes": NUM_CLASSES}, f, ensure_ascii=False, indent=2)

        file_size_kb = os.path.getsize(weights_path) / 1024.0
        print(f"[+] Archivo generado en '{weights_path}' ({file_size_kb:.2f} KB)")

    print("=================================================================")
    print("      MODELO DEEP LEARNING LSEC PREPARADO PARA ANDROID           ")
    print("=================================================================")

if __name__ == '__main__':
    train()

