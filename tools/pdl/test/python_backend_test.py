#!/usr/bin/env python3
#
# Copyright (C) 2015 The Android Open Source Project
#
# Tests the generated python backend against standard PDL
# constructs, with matching input vectors.
import unittest
import parser


class Scalar_BitFieldTest(unittest.TestCase):

    def test_(self):
        result = parser.Scalar_BitField.parse_all([0x80, 0x3, 0x83, 0x2, 0x82, 0x1, 0x81, 0x0])
        self.assertEqual(result.a, 0)
        self.assertEqual(result.c, 0x1020304050607)


class Enum_BitFieldTest(unittest.TestCase):

    def test_(self):
        result = parser.Enum_BitField.parse_all([0x81, 0x7, 0x5, 0x4, 0x3, 0x2, 0x1, 0x0])
        self.assertEqual(result.a, parser.Enum7.A)
        self.assertEqual(result.c, 0x20406080a0f)

        result = parser.Enum_BitField.parse_all([0x82, 0x7, 0x5, 0x4, 0x3, 0x2, 0x1, 0x0])
        self.assertEqual(result.a, parser.Enum7.B)
        self.assertEqual(result.c, 0x20406080a0f)


def main():
    suite = unittest.TestLoader().loadTestsFromName(__name__)
    unittest.TextTestRunner(verbosity=3).run(suite)


if __name__ == '__main__':
    main()
