# -*- coding: utf-8 -*-
"""Red neuronal densa optimizada para clasificacion de señas estaticas en móviles"""

def build_static_mlp_model(input_features=63, num_classes=13):
    try:
        import tensorflow as tf
        from tensorflow.keras import layers, models, regularizers

        model = models.Sequential([
            layers.Input(shape=(input_features,), name="landmarks_input"),
            layers.Dense(128, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
            layers.BatchNormalization(),
            layers.Dropout(0.25),
            layers.Dense(64, activation='relu', kernel_regularizer=regularizers.l2(0.001)),
            layers.BatchNormalization(),
            layers.Dropout(0.2),
            layers.Dense(32, activation='relu'),
            layers.Dense(num_classes, activation='softmax', name="lsec_probabilities")
        ])

        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        return model
    except ImportError:
        return None
