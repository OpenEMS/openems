package eu.chargetime.ocpp.utilities;
/*
ChargeTime.eu - Java OCA OCPP
Copyright (C) 2015-2016 Thomas Volden <tv@chargetime.eu>

MIT License

Copyright (C) 2016-2018 Thomas Volden

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/** Utilities for model classes. Used to validate values. */
public abstract class ModelUtil {

  /**
   * Check if a value is in a list of values.
   *
   * @param needle value we want to search for.
   * @param hayStack list of value that we search in.
   * @return true if value was found in list.
   */
  public static boolean isAmong(String needle, String... hayStack) {
    boolean found = false;
    if (hayStack != null) {
      for (String straw : hayStack) {
        if (found = isNullOrEqual(straw, needle)) {
          break;
        }
      }
    }
    return found;
  }

  /**
   * Compares two values.
   *
   * @param object1 Right value to compare.
   * @param object2 Left value to compare.
   * @return Both values are null or equal.
   */
  private static boolean isNullOrEqual(Object object1, Object object2) {
    boolean nullOrEqual = false;
    if (object1 == null && object2 == null) {
      nullOrEqual = true;
    } else if (object1 != null && object2 != null) {
      nullOrEqual = object1.equals(object2);
    }
    return nullOrEqual;
  }

  /**
   * Check if a string exceeds a given length.
   *
   * @param input The string to check.
   * @param maxLength The largest length accepted.
   * @return The string length does not exceed max length.
   */
  public static boolean validate(String input, int maxLength) {
    return input != null && input.length() <= maxLength;
  }
}
