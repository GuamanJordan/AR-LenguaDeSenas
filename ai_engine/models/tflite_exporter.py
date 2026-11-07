# -*- coding: utf-8 -*-
import os

class TFLiteModelExporter:
    """Convierte modelos Keras a TensorFlow Lite con cuantizacion FP16 e INT8"""

    @staticmethod
    def export_fp16(keras_model, output_path):
        import tensorflow as tf
        converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
        tflite_model = converter.convert()

        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        with open(output_path, "wb") as f:
            f.write(tflite_model)
        return len(tflite_model)
