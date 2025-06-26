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

package com.dalsemi.onewire.utils;

import com.dalsemi.onewire.container.OneWireContainer;

/**
 * 1-Wire&reg; Network path element. Instances of this class are used to
 * represent a single branch of a complex 1-Wire network.
 *
 * <H3>Usage</H3>
 *
 * <DL>
 * <DD>
 * <H4>Example</H4> Enumerate through the 1-wire path elements in the 1-Wire
 * path 'path' and print information:
 *
 * <PRE>
 *  <CODE>
 *   OWPathElement path_element;
 *
 *   // enumerature through the path elements
 *   for (Enumeration path_enum = path.getAllOWPathElements();
 *           path_enum.hasMoreElements(); )
 *   {
 *
 *      // cast the enum as a OWPathElement
 *      path_element = (OWPathElement)path_enum.nextElement();
 *
 *      // print info
 *      System.out.println("Address: " + path_element.getContainer().getAddressAsString());
 *      System.out.println("Channel number: " + path_element.getChannel());
 *   }
 * </CODE>
 * </PRE>
 * </DL>
 *
 * @see com.dalsemi.onewire.utils.OWPath
 * @see com.dalsemi.onewire.container.OneWireContainer
 *
 * @version 0.00, 18 September 2000
 * @author DS
 */
public class OWPathElement {

	// --------
	// -------- Variables
	// --------

	/** OneWireContainer of the path element */
	private final OneWireContainer owc;

	/** Channel the path is on */
	private final int channel;

	// --------
	// -------- Constructors
	// --------

	/**
	 * Create a new 1-Wire path element.
	 *
	 * @param owcInstance   device that is the path element. Must implement
	 *                      SwitchContainer.
	 * @param channelNumber channel number of the 1-Wire path
	 */
	public OWPathElement(OneWireContainer owcInstance, int channelNumber) {
		this.owc = owcInstance;
		this.channel = channelNumber;
	}

	/**
	 * Get the 1-Wire container for this 1-Wire path element.
	 *
	 * @return OneWireContainer of this 1-Wire path element
	 *
	 * @see com.dalsemi.onewire.container.OneWireContainer
	 */
	public OneWireContainer getContainer() {
		return this.owc;
	}

	/**
	 * Get the channel number of this 1-Wire path element.
	 *
	 * @return channel number of this element
	 */
	public int getChannel() {
		return this.channel;
	}
}
// CHECKSTYLE:ON
