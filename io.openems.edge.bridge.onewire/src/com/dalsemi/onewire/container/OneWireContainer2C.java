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

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;

//----------------------------------------------------------------------------

/**
 * <P>
 * 1-Wire&reg; container that encapsulates the functionality of the 1-Wire
 * family type <b>2C</B> (hex), Maxim Integrated Products part number:
 * <B>DS2890, 1-Wire Digital Potentiometer</B>.
 * </P>
 *
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Single element 256-position linear taper potentiometer
 * <LI>Supports potentiometer terminal working voltages up to 11V
 * <LI>Potentiometer terminal voltage independent of supply voltage
 * <LI>100k Ohm resistor element value
 * <LI>Operating temperature range from -40@htmlonly &#176C @endhtmlonly to
 * +85@htmlonly &#176C @endhtmlonly
 * <LI>2.8V - 6.0V operating voltage range
 * </UL>
 *
 * <H3>Data sheet</H3>
 *
 * <A HREF="http://pdfserv.maxim-ic.com/arpdf/DS2890.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS2890.pdf</A>
 *
 * @version 0.00, 28 Aug 2000
 * @author KLA
 */
public class OneWireContainer2C extends OneWireContainer implements PotentiometerContainer {

	// --------
	// -------- Variables
	// --------
	private final byte[] buffer = new byte[4];

	// --------
	// -------- Finals
	// --------

	/**
	 * DS2890 1-Wire Write Control Register command constant.
	 *
	 * The Write Control Register command is used to manipulate DS2890 state bits
	 * located in the Control Register.
	 */
	private static final byte WRITE_CONTROL = (byte) 0x55;

	/**
	 * DS2890 1-Wire Read Control Register command constant.
	 *
	 * The Read Control Register command is used to obtain both the Control Register
	 * and potentiometer Feature Register.
	 */
	private static final byte READ_CONTROL = (byte) 0xaa;

	/**
	 * DS2890 1-Wire Write Position command constant.
	 *
	 * The Write Position command is used to set the position of the currently
	 * addressed potentiometer wiper.
	 */
	private static final byte WRITE_POSITION = (byte) 0x0f;

	/**
	 * DS2890 1-Wire Read Position command constant.
	 *
	 * The Read Position command is used to obtain the wipre setting of the
	 * potentiometer currently addressed by the control register.
	 */
	private static final byte READ_POSITION = (byte) 0xf0;

	/**
	 * DS2890 1-Wire Increment command constant.
	 *
	 * The Increment command is used for a one step position increase of the
	 * currently addressed potentiometer wiper.
	 */
	private static final byte INCREMENT = (byte) 0xc3;

	/**
	 * DS2890 1-Wire Decrement command constant.
	 *
	 * The Decrement command is used for a one setp position decrease of the
	 * currently addressed potentiometer wiper.
	 */
	private static final byte DECREMENT = (byte) 0x99;

	// --------
	// -------- Constructors
	// --------

	/**
	 * Default constructor
	 */
	public OneWireContainer2C() {
	}

	/**
	 * Creates a container with a provided adapter object and the address of the
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer2C(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a container with a provided adapter object and the address of this
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer2C(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a container with a provided adapter object and the address of this
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer2C(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);
	}

	// --------
	// -------- Methods
	// --------

	/**
	 * Retrieves the Maxim Integrated Products part number of this 1-Wire device as
	 * a string. For example 'DS2890'.
	 *
	 * @return representation of this 1-Wire devices name
	 */
	@Override
	public String getName() {
		return "DS2890";
	}

	/**
	 * Retrieves the alternate Maxim Integrated Products part numbers or names. A
	 * family of MicroLan devices may have more than one part number depending on
	 * packaging.
	 *
	 * @return representation of the alternate names
	 */
	@Override
	public String getAlternateNames() {
		return "Digital Potentiometer";
	}

	/**
	 * Retrieves a short description of the function of this 1-Wire Device.
	 *
	 * @return representation of the function description
	 */
	@Override
	public String getDescription() {
		return "1-Wire linear taper digitally controlled potentiometer "
				+ "with 256 wiper positions.  0-11 Volt working range.";
	}

	/**
	 * Gets the maximum speed this 1-Wire device can communicate at.
	 *
	 * @return maximum speed this device can communicate at
	 */
	@Override
	public int getMaxSpeed() {
		return DSPortAdapter.SPEED_OVERDRIVE;
	}

	// --------
	// -------- Potentiometer Feature methods
	// --------

	/**
	 * Queries to see if this Potentiometer One Wire Device has linear potentiometer
	 * element(s) or logarithmic potentiometer element(s).
	 *
	 * @param state state buffer of the Potentiometer One Wire Device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return <CODE>true</CODE> if this device has linear potentiometer element(s);
	 *         <CODE>false</CODE> if this device has logarithmic potentiometer
	 *         element(s)
	 */
	@Override
	public boolean isLinear(byte[] state) {
		return (state[0] & 0x01) == 0x01;
	}

	/**
	 * Queries to see if this 1-Wire Potentiometer device's wiper settings are
	 * volatile or non-volatile.
	 *
	 * @param state state buffer of the 1-Wire Potentiometer device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return <CODE>true</CODE> if the wiper settings are volatile;
	 *         <CODE>false</CODE> if the wiper settings are non-volatile
	 */
	@Override
	public boolean wiperSettingsAreVolatile(byte[] state) {
		return (state[0] & 0x02) == 0x02;
	}

	/**
	 * Queries to see how many potentiometers this Potentiometer One Wire Device
	 * has.
	 *
	 * @param state state buffer of this 1-Wire Potentiometer device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return the number of potentiometers on this device
	 */
	@Override
	public int numberOfPotentiometers(byte[] state) {
		return (state[0] >> 2 & 0x03) + 1;
	}

	/**
	 * Queries to find the number of wiper settings that any wiper on this
	 * Potentiometer One Wire Device can have.
	 *
	 * @param state state buffer of this 1-Wire Potentiometer device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return number of wiper positions available
	 */
	@Override
	public int numberOfWiperSettings(byte[] state) {
		switch (state[0] & 0x30) {

		case 0x00:
			return 32;
		case 0x10:
			return 64;
		case 0x20:
			return 128;
		default:
			return 256;
		}
	}

	/**
	 * Queries to find the resistance value of the potentiometer.
	 *
	 * @param state state buffer of this 1-Wire Potentiometer device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return the resistance value in k-Ohms
	 */
	@Override
	public int potentiometerResistance(byte[] state) {
		switch (state[0] & 0xc0) {

		case 0x00:
			return 5;
		case 0x40:
			return 10;
		case 0x80:
			return 50;
		default:
			return 100;
		}
	}

	/**
	 * Gets the currently selected wiper number. All wiper actions affect this
	 * wiper. The number of wipers is the same as
	 * <CODE>numberOfPotentiometers()</CODE>.
	 *
	 * @param state state buffer of this 1-Wire Potentiometer device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return the current wiper number
	 */
	@Override
	public int getCurrentWiperNumber(byte[] state) {
		var wiper = state[1] & 0x03;
		var wiper_inverse = state[1] >> 2 & 0x03;

		if (wiper + wiper_inverse == 3) {
			return wiper;
		}

		return -1;
	}

	/**
	 * Sets the currently selected wiper number. All wiper actions affect this
	 * wiper. The number of wipers is the same as
	 * <CODE>numberOfPotentiometers()</CODE>.
	 *
	 * @param wiper_number wiper number to select for communication. Valid choices
	 *                     are 0 to 3
	 * @param state        state buffer of this 1-Wire Potentiometer device
	 *                     (returned by <CODE>readDevice()</CODE>)
	 */
	@Override
	public void setCurrentWiperNumber(int wiper_number, byte[] state) {
		if (wiper_number != (wiper_number & 0x03)) {
			return; // invalid, just skip it
		}

		var wiper_inverse = ~wiper_number;

		wiper_number = wiper_number | (wiper_inverse & 0x03) << 2;
		state[1] = (byte) (state[1] & 0xf0 | wiper_number & 0x0f);
	}

	/**
	 * Determines if this device's charge pump is enabled.
	 *
	 * @param state state buffer of this Potentiometer One Wire Device (returned by
	 *              <CODE>readDevice()</CODE>)
	 * @return <CODE>true</CODE> if it is enabled; <CODE>false</CODE> if not
	 */
	@Override
	public boolean isChargePumpOn(byte[] state) {
		return (state[1] & 0x40) == 0x40;
	}

	/**
	 * Sets this device's charge pump. This decreases the wiper's resistance, but
	 * increases the power consumption by the part. Vdd must be connected to use the
	 * charge pump (see the DS2890 datasheet for more information at
	 * <A HREF="http://www.dalsemi.com">www.dalsemi.com</A>)
	 *
	 * @param charge_pump_on <CODE>true</CODE> if you want to enable the charge pump
	 * @param state          state buffer of this Potentiometer One Wire Device
	 *                       (returned by <CODE>readDevice()</CODE>)
	 * @return <CODE>true</CODE> if the operation was successful; <CODE>false</CODE>
	 *         if there was an error
	 */
	@Override
	public void setChargePump(boolean charge_pump_on, byte[] state) {
		state[1] = (byte) (state[1] & 0xbf); // mask out the charge pump bit

		if (charge_pump_on) {
			state[1] = (byte) (state[1] | 0x40);
		}
	}

	/**
	 * Gets the current wiper position of this device. The wiper position is between
	 * 0 and 255, and describes the voltage output. The output lies between RH and
	 * RL.
	 *
	 * @return the wiper position between 0 and 255
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public int getWiperPosition() throws OneWireIOException, OneWireException {
		return this.readRegisters(READ_POSITION) & 0x0ff;
	}

	/**
	 * Sets the wiper position for the potentiometer.
	 *
	 * @param position the position to set the wiper. This value will be cast to a
	 *                 byte, only the 8 least significant bits matter.
	 * @return <CODE>true</CODE> if the operation was successful; <CODE>false</CODE>
	 *         otherwise
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public boolean setWiperPosition(int position) throws OneWireIOException, OneWireException {
		return this.writeTransaction(WRITE_POSITION, (byte) position);
	}

	/**
	 * Increments the wiper position.
	 *
	 * @param reselect increment/decrement can be called without resetting the part
	 *                 if the last call was an increment/decrement.
	 *                 <CODE>true</CODE> if you want to select the part (you must
	 *                 call with <CODE>true</CODE> after any other one-wire method)
	 * @return the new position of the wiper (0-255)
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public int increment(boolean reselect) throws OneWireIOException, OneWireException {
		return this.unitChange(INCREMENT, reselect);
	}

	/**
	 * Decrements the wiper position.
	 *
	 * @param reselect increment/decrement can be called without resetting the part
	 *                 if the last call was an increment/decrement.
	 *                 <CODE>true</CODE> if you want to select the part (you must
	 *                 call with <CODE>true</CODE> after any other one-wire method)
	 * @return the new position of the wiper (0-255)
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public int decrement(boolean reselect) throws OneWireIOException, OneWireException {
		return this.unitChange(DECREMENT, reselect);
	}

	/**
	 * Increments the wiper position after selecting the part.
	 *
	 * @return the new position of the wiper (0-255)
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public int increment() throws OneWireIOException, OneWireException {
		return this.unitChange(INCREMENT, true);
	}

	/**
	 * Decrements the wiper position after selecting the part.
	 *
	 * @return the new position of the wiper (0-255)
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public int decrement() throws OneWireIOException, OneWireException {
		return this.unitChange(DECREMENT, true);
	}

	/**
	 * Retrieves the 1-Wire device sensor state. This state is returned as a byte
	 * array. Pass this byte array to the static query and set methods. If the
	 * device state needs to be changed then call the <CODE>writeDevice</CODE> to
	 * finalize the one or more change.
	 *
	 * @return 1-Wire device sensor state
	 *
	 * @throws OneWireIOException Data was not read correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public byte[] readDevice() throws OneWireIOException, OneWireException {

		// format for the byte array is this:
		// byte 0: Feature register
		// (msb) bit 7 : Potentiometer resistance msb
		// bit 6 : Potentiometer resistance lsb
		// bit 5 : Number of Wiper Positions msb
		// bit 4 : Number of Wiper Positions lsb
		// bit 3 : Number of Potentiometers msb
		// bit 2 : Number of Potentiometers lsb
		// bit 1 : Wiper Setting Volatility
		// (lsb) bit 0 : Potentiometer Characteristic (lin/log)
		// byte 1: Control register
		// (msb) bit 7 : Reserved
		// bit 6 : Charge Pump Control
		// bit 5 : Reserved
		// bit 4 : Reserved
		// bit 3 : Inverted Wiper Number msb
		// bit 2 : Inverted Wiper Number lsb
		// bit 1 : Wiper Number msb
		// (lsb) bit 0 : Wiper Number lsb
		var state = new byte[2];

		this.doSpeed();

		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("Could not select the part!");
		}

		var buf = new byte[3];

		buf[0] = READ_CONTROL;
		buf[1] = buf[2] = (byte) 0x0ff;

		this.adapter.dataBlock(buf, 0, 3);

		state[0] = buf[1]; // feature
		state[1] = buf[2]; // control

		return state;
	}

	/**
	 * Writes the 1-Wire device sensor state that have been changed by the 'set'
	 * methods. It knows which registers have changed by looking at the bitmap
	 * fields appended to the state data.
	 *
	 * @param state byte array of clock register page contents
	 *
	 * @throws OneWireIOException Data was not written correctly
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {

		// here we want to write the control register, just state[1]
		if (!this.writeTransaction(WRITE_CONTROL, state[1])) {
			throw new OneWireIOException("Device may not have been present!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// Private Methods //
	//////////////////////////////////////////////////////////////////////

	/*
	 * This function handles reading of the registers for: 1. Finding the state of
	 * the charge pump 2. Finding the current location of the wiper
	 *
	 * Both of these operations send one command byte and receive two information
	 * bytes. The relevant information for both is stored in that second received
	 * byte.
	 */
	private synchronized int readRegisters(byte COMMAND) throws OneWireIOException, OneWireException {
		this.doSpeed();

		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("Could not select the part!");
		}

		this.buffer[0] = COMMAND;
		this.buffer[1] = this.buffer[2] = (byte) 0x0ff;

		this.adapter.dataBlock(this.buffer, 0, 3);

		return 0x0ff & this.buffer[2];
	}

	/*
	 * Handles the writing transactions, which are: 1. Setting the control register
	 * (ie the charge pump state) 2. Setting the wiper position
	 *
	 * Both of these operations have the same transaction process. The command byte
	 * and a value parameter are passed in (either the new control register or the
	 * new position) and the part echo's the value parameter. If the echo is correct
	 * (no transmission errors), the master sends a 96 (which means finish
	 * transaction). If the transaction succeeds, the part returns 0's, otherwise it
	 * returns 1's.
	 *
	 */
	private synchronized boolean writeTransaction(byte COMMAND, byte value)
			throws OneWireIOException, OneWireException {
		this.doSpeed();

		if (this.adapter.select(this.address)) {
			this.buffer[0] = COMMAND;
			this.buffer[1] = value;
			this.buffer[2] = (byte) 0x0ff;

			this.adapter.dataBlock(this.buffer, 0, 3);

			if (this.buffer[2] == value) {
				this.buffer[0] = (byte) 0x096;
				this.buffer[1] = (byte) 0x0ff;

				this.adapter.dataBlock(this.buffer, 0, 2);

				if (this.buffer[1] == 0) {
					return true;
				}
			}
		}

		return false;
	}

	/*
	 * This function handles the increment and decrement operations, including the
	 * contingent reset. You do not need to call reset between consecutive unit
	 * change commands. Both operations issue the command byte and then receive the
	 * new wiper position.
	 */
	private synchronized int unitChange(byte COMMAND, boolean reselect) throws OneWireIOException, OneWireException {
		if (reselect) {
			this.doSpeed(); // don't need to do this if we don't need to select
			this.adapter.select(this.address);
		}

		this.buffer[0] = COMMAND;
		this.buffer[1] = (byte) 0x0ff;

		this.adapter.dataBlock(this.buffer, 0, 2);

		return 0x0ff & this.buffer[1];
	}
}
// CHECKSTYLE:ON
