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

package com.dalsemi.onewire.container;

import com.dalsemi.onewire.adapter.DSPortAdapter;

//----------------------------------------------------------------------------

/**
 * <P>
 * 1-Wire container for temperature iButton which measures temperatures from
 * -55@htmlonly &#176C @endhtmlonly to +125@htmlonly &#176C @endhtmlonly,
 * DS1822. This container encapsulates the functionality of the iButton family
 * type <B>22</B> (hex)
 * </P>
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Measures temperatures from -55@htmlonly &#176C @endhtmlonly to
 * +125@htmlonly &#176C @endhtmlonly. Fahrenheit equivalent is -67@htmlonly
 * &#176F @endhtmlonly to +257@htmlonly &#176F @endhtmlonly
 * <LI>Power supply range is 3.0V to 5.5V
 * <LI>Zero standby power
 * <LI>+/- 2@htmlonly &#176C @endhtmlonly accuracy from -10@htmlonly
 * &#176C @endhtmlonly to +85@htmlonly &#176C @endhtmlonly
 * <LI>Thermometer resolution programmable from 9 to 12 bits
 * <LI>Converts 12-bit temperature to digital word in 750 ms (max.)
 * <LI>User-definable, nonvolatile temperature alarm settings
 * <LI>Alarm search command identifies and addresses devices whose temperature
 * is outside of programmed limits (temperature alarm condition)
 * <LI>Software compatible with DS18B20 (family type <B>28</B> hex)
 * </UL>
 *
 * <H3>Usage</H3>
 *
 * <DL>
 * <DD>See the usage example in
 * {@link com.dalsemi.onewire.container.TemperatureContainer
 * TemperatureContainer} for temperature specific operations.
 * </DL>
 *
 * <H3>DataSheet</H3>
 * <DL>
 * <DD><A HREF="http://pdfserv.maxim-ic.com/arpdf/DS1822.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS1822.pdf</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.container.TemperatureContainer
 *
 * @version 1.10, 26 September 2001
 * @author DalSemi
 */
public class OneWireContainer22 extends OneWireContainer28 implements TemperatureContainer {

	// -------------------------------------------------------------------------
	// -------- Static Final Variables
	// -------------------------------------------------------------------------

	/**
	 * Creates an empty <code>OneWireContainer22</code>. Must call
	 * <code>setupContainer()</code> before using this new container.
	 * <p>
	 *
	 * This is one of the methods to construct a <code>OneWireContainer22</code>.
	 * The others are through creating a <code>OneWireContainer22</code> with
	 * parameters.
	 *
	 * @see #OneWireContainer22(DSPortAdapter,byte[])
	 * @see #OneWireContainer22(DSPortAdapter,long)
	 * @see #OneWireContainer22(DSPortAdapter,String)
	 */
	public OneWireContainer22() {
	}

	/**
	 * Creates a <code>OneWireContainer22</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer22</code>.
	 * The others are through creating a <code>OneWireContainer22</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer22()
	 * @see #OneWireContainer22(DSPortAdapter,long)
	 * @see #OneWireContainer22(DSPortAdapter,String)
	 */
	public OneWireContainer22(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer22</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer22</code>.
	 * The others are through creating a <code>OneWireContainer22</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer22()
	 * @see #OneWireContainer22(DSPortAdapter,byte[])
	 * @see #OneWireContainer22(DSPortAdapter,String)
	 */
	public OneWireContainer22(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer22</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer22</code>.
	 * The others are through creating a <code>OneWireContainer22</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer22()
	 * @see #OneWireContainer22(DSPortAdapter,byte[])
	 * @see #OneWireContainer22(DSPortAdapter,long)
	 */
	public OneWireContainer22(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);
	}

	// --------
	// -------- Information methods
	// --------

	/**
	 * Retrieves the Maxim Integrated Products part number of this
	 * <code>OneWireContainer22</code> as a <code>String</code>. For example
	 * 'DS1822'.
	 *
	 * @return this <code>OneWireContainer22</code> name
	 */
	@Override
	public String getName() {
		return "DS1822";
	}

	/**
	 * Retrieves the alternate Maxim Integrated Products part numbers or names. A
	 * 'family' of 1-Wire Network devices may have more than one part number
	 * depending on packaging. There can also be nicknames such as 'Crypto iButton'.
	 *
	 * @return this <code>OneWireContainer22</code> alternate names
	 */
	@Override
	public String getAlternateNames() {
		return "";
	}

	/**
	 * Retrieves a short description of the function of this
	 * <code>OneWireContainer22</code> type.
	 *
	 * @return <code>OneWireContainer22</code> functional description
	 */
	@Override
	public String getDescription() {
		return """
				Digital thermometer measures temperatures from \
				-55C to 125C in 0.75 seconds (max).  +/- 2C \
				accuracy between -10C and 85C. Thermometer \
				resolution is programmable at 9, 10, 11, and 12 bits.\s""";
	}

	// --------
	// -------- Temperature Feature methods
	// --------

	// --------
	// -------- Temperature I/O Methods
	// --------

	// --------
	// -------- Temperature 'get' Methods
	// --------

	// --------
	// -------- Temperature 'set' Methods
	// --------

	// --------
	// -------- Custom Methods for this iButton Type
	// --------

}
// CHECKSTYLE:ON
