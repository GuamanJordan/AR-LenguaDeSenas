# -*- coding: utf-8 -*-
import unittest
from core_logic.gloss_mapper import LsecGlossMapper

class TestLsecGlossMapper(unittest.TestCase):
    def setUp(self):
        self.mapper = LsecGlossMapper()

    def test_greeting_translation(self):
        result = self.mapper.translate_to_glosses("Hola, buenos días")
        self.assertEqual(result, ["HOLA", "BUENO", "DIA"])

    def test_gratitude_translation(self):
        result = self.mapper.translate_to_glosses("Muchas gracias")
        self.assertEqual(result, ["GRACIAS", "MUCHO"])

    def test_negation_placement(self):
        result = self.mapper.translate_to_glosses("No entiendo")
        self.assertIn("NO", result)
        self.assertEqual(result[-1], "NO")

    def test_emergency_query(self):
        result = self.mapper.translate_to_glosses("Necesito ayuda")
        self.assertIn("NECESITAR", result)
        self.assertIn("AYUDAR", result)

if __name__ == '__main__':
    unittest.main()
