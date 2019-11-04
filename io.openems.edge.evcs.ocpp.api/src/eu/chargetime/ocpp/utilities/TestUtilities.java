package eu.chargetime.ocpp.utilities;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/** Utilities for tests. Used to quickly create usefull objects. */
public final class TestUtilities {

  /**
   * Create a string of a given length.
   *
   * @param length the desired length.
   * @return A string of the desired length.
   */
  public static String aString(int length) {
    String lorem =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus bibendum eros vitae sapien metusa.";

    if (lorem.length() < length) {
      StringBuilder extender = new StringBuilder(lorem);
      while (extender.length() < length) {
        extender.append(lorem);
      }
      lorem = extender.toString();
    }

    return lorem.substring(0, length);
  }

  /**
   * Create an array of elements.
   *
   * @param objects Params of elements to add to the list.
   * @param <T> The type of the elements.
   * @return An array of the given elements.
   */
  public static <T> T[] aList(T... objects) {
    return objects;
  }

  /**
   * Create a String from an Array.
   *
   * @param delimiter Used to split the values.
   * @param array The array to print.
   * @return Formatted string, with array values.
   */
  public static String join(String delimiter, Object[] array) {
    StringBuilder output = new StringBuilder();

    for (Object current : array) output.append(String.format("%s%s", delimiter, current));

    return output.toString().substring(1);
  }
}
