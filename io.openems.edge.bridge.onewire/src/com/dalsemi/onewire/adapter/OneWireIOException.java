// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Maxim Integrated Products, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL MAXIM INTEGRATED PRODUCTS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Maxim Integrated Products
 * shall not be used except as stated in the Maxim Integrated Products
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package com.dalsemi.onewire.adapter;

// imports
import com.dalsemi.onewire.OneWireException;

/**
 * This exception is thrown when there is an IO error communicating on on the
 * 1-Wire Network. For instance, when a network error occurs when calling the
 * putBit(boolean) method.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
@SuppressWarnings({ "serial" })
public class OneWireIOException extends OneWireException {

	/**
	 * Constructs a <code>OneWireIOException</code> with no detail message.
	 */
	public OneWireIOException() {
	}

	/**
	 * Constructs a <code>OneWireIOException</code> with the specified detail
	 * message.
	 *
	 * @param desc the detail message description
	 */
	public OneWireIOException(String desc) {
		super(desc);
	}
}
// CHECKSTYLE:ON
