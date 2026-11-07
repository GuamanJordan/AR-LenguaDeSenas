# -*- coding: utf-8 -*-
"""Red neuronal espacio-temporal (Conv1D + BiLSTM) para señas dinámicas LSEC"""

def build_dynamic_bilstm_model(sequence_length=30, num_features=63, num_classes=12):
    try:
        import tensorflow as tf
        from tensorflow.keras import layers, models

        model = models.Sequential([
            layers.Input(shape=(sequence_length, num_features), name="temporal_sequence_input"),
            layers.Conv1D(filters=64, kernel_size=3, padding='same', activation='relu'),
            layers.BatchNormalization(),
            layers.MaxPooling1D(pool_size=2),
            layers.Bidirectional(layers.LSTM(64, return_sequences=False)),
            layers.Dropout(0.3),
            layers.Dense(32, activation='relu'),
            layers.Dense(num_classes, activation='softmax', name="dynamic_probabilities")
        ])

        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.0008),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        return model
    except ImportError:
        return None
