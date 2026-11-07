# -*- coding: utf-8 -*-
import unittest
from database.db_manager import LsecDatabaseManager

class TestLsecDatabaseManager(unittest.TestCase):
    def setUp(self):
        self.db = LsecDatabaseManager("database/test_lsec.sqlite")

    def test_categories_count(self):
        cats = self.db.get_all_categories()
        self.assertGreaterEqual(len(cats), 5)

    def test_search_gloss(self):
        results = self.db.search_gloss_by_text("gracias")
        self.assertGreater(len(results), 0)
        self.assertEqual(results[0]["lsec_gloss"], "GRACIAS")

if __name__ == '__main__':
    unittest.main()
