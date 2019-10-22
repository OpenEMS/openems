
/*---------------------------------------------------------------------------
 * Copyright (C) 1999-2006 Maxim Integrated Products, All Rights Reserved.
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

// imports
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.adapter.*;
import java.util.*;

//----------------------------------------------------------------------------

/**
 * <P>
 * 1-Wire&reg; container for a 1-Wire programmable resolution digital
 * thermometer with "sequence detect" and PIO, the DS28EA00. This container
 * encapsulates the functionality of the iButton family type <B>42</B> (hex)
 * </P>
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Standard and Overdrive 1-Wire speed
 * <LI>Improved 1-Wire interface with hysteresis and glitch filter
 * <LI>Two general-purpose IO pins
 * <LI>Chain function sharing the IO pins to detect physical sequence of devices
 * in network
 * <LI>Can be powered from data line. Power supply range is 3.0V to 5.5V
 * <LI>Measures temperatures from –40°C to +85°C
 * <LI>@htmlonly &#177 @endhtmlonly 0.5°C accuracy from –10°C to +85°C
 * <LI>@htmlonly &#177 @endhtmlonly 2°C accuracy from –40°C to +85°C
 * <LI>Thermometer resolution is user-selectable from 9 to 12 bits
 * <LI>Converts temperature to 12-bit digital word in 750ms (max.)
 * <LI>User-definable nonvolatile (NV) alarm threshold settings/user bytes
 * <LI>Alarm search command to quickly identify devices whose temperature is
 * outside of programmed limits
 * <LI>Software-compatible and pin-compatible with the DS28EA00
 * </UL>
 *
 * <H3>Usage</H3>
 *
 * <DL>
 * <DD>See the temperature usage example in
 * {@link com.dalsemi.onewire.container.TemperatureContainer
 * TemperatureContainer} for temperature specific operations.
 * </DL>
 * 
 * <DL>
 * <DD>See the switch usage example in
 * {@link com.dalsemi.onewire.container.SwitchContainer SwitchContainer} for
 * switch specific operations.
 * </DL>
 *
 * <H3>DataSheet</H3>
 * <DL>
 * <DD><A HREF="http://www.maxim-ic.com/DS28EA00">
 * http://www.maxim-ic.com/DS28EA00</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.container.TemperatureContainer
 *
 * @version 1.00, 15 September 2006
 * @author BH
 */
public class OneWireContainer42 extends OneWireContainer implements TemperatureContainer, SwitchContainer {

	// -------------------------------------------------------------------------
	// -------- Static Final Variables
	// -------------------------------------------------------------------------

	/** DS28EA00 writes data to scratchpad command */
	public static final byte WRITE_SCRATCHPAD_COMMAND = (byte) 0x4E;

	/** DS28EA00 reads data from scratchpad command */
	public static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;

	/** DS28EA00 copys data from scratchpad to E-squared memory command */
	public static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x48;

	/** DS28EA00 converts temperature command */
	public static final byte CONVERT_TEMPERATURE_COMMAND = (byte) 0x44;

	/** DS28EA00 recalls E-squared memory command */
	public static final byte RECALL_E2MEMORY_COMMAND = (byte) 0xB8;

	/**
	 * DS28EA00 read power supply command. This command is used to determine if
	 * external power is supplied.
	 */
	public static final byte READ_POWER_SUPPLY_COMMAND = (byte) 0xB4;

	/** DS28EA00 12-bit resolution constant for CONFIG byte */
	public static final byte RESOLUTION_12_BIT = (byte) 0x7F;

	/** DS28EA00 11-bit resolution constant for CONFIG byte */
	public static final byte RESOLUTION_11_BIT = (byte) 0x5F;

	/** DS28EA00 10-bit resolution constant for CONFIG byte */
	public static final byte RESOLUTION_10_BIT = (byte) 0x3F;

	/** DS28EA00 9-bit resolution constant for CONFIG byte */
	public static final byte RESOLUTION_9_BIT = (byte) 0x1F;

	/** PIO Access read command */
	public static final byte PIO_ACCESS_READ = (byte) 0xF5;

	/** PIO Access read command */
	public static final byte PIO_ACCESS_WRITE = (byte) 0xA5; // Note the change from DS2413's 0x5A

	/**
	 * Creates an empty <code>OneWireContainer42</code>. Must call
	 * <code>setupContainer()</code> before using this new container.
	 * <p>
	 *
	 * This is one of the methods to construct a <code>OneWireContainer42</code>.
	 * The others are through creating a <code>OneWireContainer42</code> with
	 * parameters.
	 *
	 * @see #OneWireContainer42(DSPortAdapter,byte[])
	 * @see #OneWireContainer42(DSPortAdapter,long)
	 * @see #OneWireContainer42(DSPortAdapter,String)
	 */
	public OneWireContainer42() {
		super();
	}

	/**
	 * Creates a <code>OneWireContainer42</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer42</code>.
	 * The others are through creating a <code>OneWireContainer42</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer42()
	 * @see #OneWireContainer42(DSPortAdapter,long)
	 * @see #OneWireContainer42(DSPortAdapter,String)
	 */
	public OneWireContainer42(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer42</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer42</code>.
	 * The others are through creating a <code>OneWireContainer42</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer42()
	 * @see #OneWireContainer42(DSPortAdapter,byte[])
	 * @see #OneWireContainer42(DSPortAdapter,String)
	 */
	public OneWireContainer42(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer42</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer42</code>.
	 * The others are through creating a <code>OneWireContainer42</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer42()
	 * @see #OneWireContainer42(DSPortAdapter,byte[])
	 * @see #OneWireContainer42(DSPortAdapter,long)
	 */
	public OneWireContainer42(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Gets an enumeration of memory bank instances that implement one or more of
	 * the following interfaces: {@link com.dalsemi.onewire.container.MemoryBank
	 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
	 * PagedMemoryBank}, and {@link com.dalsemi.onewire.container.OTPMemoryBank
	 * OTPMemoryBank}.
	 * 
	 * @return <CODE>Enumeration</CODE> of memory banks
	 */
	public Enumeration<MemoryBank> getMemoryBanks() {
		Vector<MemoryBank> bank_vector = new Vector<>(3);

		// Status
		// bank_vector.addElement(new MemoryBankScratachTemp(this));

		// Temperature
		MemoryBankScratchTemp temp = new MemoryBankScratchTemp(this);
		temp.bankDescription = "Temperature";
		temp.generalPurposeMemory = false;
		temp.startPhysicalAddress = 0;
		temp.size = 2;
		temp.readWrite = false;
		temp.readOnly = true;
		temp.nonVolatile = false;
		temp.powerDelivery = true;
		bank_vector.addElement(temp);

		// Threshold
		temp = new MemoryBankScratchTemp(this);
		temp.bankDescription = "TH/TL Alarm Trip Points";
		temp.generalPurposeMemory = true;
		temp.startPhysicalAddress = 2;
		temp.size = 2;
		temp.readWrite = true;
		temp.readOnly = false;
		temp.nonVolatile = true;
		temp.powerDelivery = true;
		bank_vector.addElement(temp);

		// Elapsed Timer Meter
		temp = new MemoryBankScratchTemp(this);
		temp.bankDescription = "Status/Configuration";
		temp.generalPurposeMemory = false;
		temp.startPhysicalAddress = 4;
		temp.size = 1;
		temp.readWrite = true;
		temp.readOnly = false;
		temp.nonVolatile = true;
		temp.powerDelivery = true;
		bank_vector.addElement(temp);

		return bank_vector.elements();
	}

	// --------
	// -------- Information methods
	// --------

	/**
	 * Retrieves the Maxim Integrated Products part number of this
	 * <code>OneWireContainer42</code> as a <code>String</code>. For example
	 * 'DS28EA00'.
	 *
	 * @return this <code>OneWireContainer42</code> name
	 */
	public String getName() {
		return "DS28EA00";
	}

	/**
	 * Retrieves the alternate Maxim Integrated Products part numbers or names. A
	 * 'family' of 1-Wire Network devices may have more than one part number
	 * depending on packaging. There can also be nicknames such as 'Crypto iButton'.
	 *
	 * @return this <code>OneWireContainer42</code> alternate names
	 */
	public String getAlternateNames() {
		return "DS28EA00U+";
	}

	/**
	 * Retrieves a short description of the function of this device
	 * <code>OneWireContainer42</code> type.
	 *
	 * @return <code>OneWireContainer42</code> functional description
	 */
	public String getDescription() {
		return "Programmable resolution digital thermometer with "
				+ "'sequence detect' and 2 PIO channels. It measures "
				+ "temperature from -40°C to +85°C in 0.75 seconds (max). "
				+ "Its accuracy is ±0.5°C between -10°C and 85°C and "
				+ "±2°C accuracy from –40°C to +85°C. Thermometer " + "resolution is programmable at 9, 10, 11, and 12 "
				+ "bits. PIO channels can be used as generic channels "
				+ "or used in 'Chain' mode to detect the physical " + "sequence of devices in a 1-Wire network.";
	}

	/**
	 * Returns the maximum speed this iButton or 1-Wire device can communicate at.
	 *
	 * @return maximum speed
	 * @see DSPortAdapter#setSpeed
	 */
	public int getMaxSpeed() {
		return DSPortAdapter.SPEED_OVERDRIVE;
	}

	// --------
	// -------- Temperature Feature methods
	// --------

	/**
	 * Checks to see if this temperature measuring device has high/low trip alarms.
	 *
	 * @return <code>true</code> if this <code>OneWireContainer42</code> has
	 *         high/low trip alarms
	 *
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 */
	public boolean hasTemperatureAlarms() {
		return true;
	}

	/**
	 * Checks to see if this device has selectable temperature resolution.
	 *
	 * @return <code>true</code> if this <code>OneWireContainer42</code> has
	 *         selectable temperature resolution
	 *
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	public boolean hasSelectableTemperatureResolution() {
		return true;
	}

	/**
	 * Gets an array of available temperature resolutions in Celsius.
	 *
	 * @return byte array of available temperature resolutions in Celsius for this
	 *         <code>OneWireContainer42</code>. The minimum resolution is returned
	 *         as the first element and maximum resolution as the last element.
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #setTemperatureResolution
	 */
	public double[] getTemperatureResolutions() {
		double[] resolutions = new double[4];

		resolutions[0] = (double) 0.5; // 9-bit
		resolutions[1] = (double) 0.25; // 10-bit
		resolutions[2] = (double) 0.125; // 11-bit
		resolutions[3] = (double) 0.0625; // 12-bit

		return resolutions;
	}

	/**
	 * Gets the temperature alarm resolution in Celsius.
	 *
	 * @return temperature alarm resolution in Celsius for this
	 *         <code>OneWireContainer42</code>
	 *
	 * @see #hasTemperatureAlarms
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 *
	 */
	public double getTemperatureAlarmResolution() {
		return 1.0;
	}

	/**
	 * Gets the maximum temperature in Celsius.
	 *
	 * @return maximum temperature in Celsius for this
	 *         <code>OneWireContainer42</code>
	 *
	 * @see #getMinTemperature
	 */
	public double getMaxTemperature() {
		return 85.0;
	}

	/**
	 * Gets the minimum temperature in Celsius.
	 *
	 * @return minimum temperature in Celsius for this
	 *         <code>OneWireContainer42</code>
	 *
	 * @see #getMaxTemperature
	 */
	public double getMinTemperature() {
		return -40.0;
	}

	// --------
	// -------- Temperature I/O Methods
	// --------

	/**
	 * Performs a temperature conversion on <code>state</code> information.
	 *
	 * @param state byte array with device state information
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #getTemperature
	 */
	public void doTemperatureConvert(byte[] state) throws OneWireIOException, OneWireException {
		int msDelay = 750; // in milliseconds

		// select the device
		if (adapter.select(address)) {

			// Setup Power Delivery
			adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
			adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);
			// send the convert temperature command
			adapter.putByte(CONVERT_TEMPERATURE_COMMAND);

			// calculate duration of delay according to resolution desired
			switch (state[4]) {

			case RESOLUTION_9_BIT:
				msDelay = 94;
				break;
			case RESOLUTION_10_BIT:
				msDelay = 188;
				break;
			case RESOLUTION_11_BIT:
				msDelay = 375;
				break;
			case RESOLUTION_12_BIT:
				msDelay = 750;
				break;
			default:
				msDelay = 750;
			} // switch

			// delay for specified amount of time
			try {
				Thread.sleep(msDelay);
			} catch (InterruptedException e) {
			}

			// Turn power back to normal.
			adapter.setPowerNormal();

			// check to see if the temperature conversion is over
			if (adapter.getByte() != 0xFF)
				throw new OneWireIOException("OneWireContainer42-temperature conversion not complete");

			// return new converted temperature in "state" variable
			adapter.select(address);
			state = recallE2();

		} else {

			// device must not have been present
			throw new OneWireIOException("OneWireContainer42-device not present");
		}
	}

	// --------
	// -------- Temperature 'get' Methods
	// --------

	/**
	 * Gets the temperature value in Celsius from the <code>state</code> data
	 * retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information for this
	 *              <code>OneWireContainer42</code>
	 *
	 * @return temperature in Celsius from the last
	 *         <code>doTemperatureConvert()</code>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 *
	 * @see #doTemperatureConvert
	 */
	public double getTemperature(byte[] state) throws OneWireIOException {

		// Take these three steps:
		// 1) Make an 11-bit integer number out of MSB and LSB of the first 2 bytes from
		// scratchpad
		// 2) Divide final number by 16 to retrieve the floating point number.
		// 3) Afterwards, test for the following temperatures:
		// 0x07D0 = 125.0C
		// 0x0550 = 85.0C
		// 0x0191 = 25.0625C
		// 0x00A2 = 10.125C
		// 0x0008 = 0.5C
		// 0x0000 = 0.0C
		// 0xFFF8 = -0.5C
		// 0xFF5E = -10.125C
		// 0xFE6F = -25.0625C
		// 0xFC90 = -55.0C
		double theTemperature = (double) 0.0;
		int inttemperature = state[1]; // inttemperature is automatically sign extended here.

		inttemperature = (inttemperature << 8) | (state[0] & 0xFF); // this converts 2 bytes into integer
		theTemperature = (double) ((double) inttemperature / (double) 16); // converts integer to a double

		return (theTemperature);
	}

	/**
	 * Gets the specified temperature alarm value in Celsius from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     byte array with device state information
	 *
	 * @return temperature alarm trip values in Celsius for this
	 *         <code>OneWireContainer42</code>
	 *
	 * @see #hasTemperatureAlarms
	 * @see #setTemperatureAlarm
	 */
	public double getTemperatureAlarm(int alarmType, byte[] state) {
		return (double) state[alarmType == ALARM_LOW ? 3 : 2];
	}

	/**
	 * Gets the current temperature resolution in Celsius from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information
	 *
	 * @return temperature resolution in Celsius for this
	 *         <code>OneWireContainer42</code>
	 *
	 * @see #RESOLUTION_9_BIT
	 * @see #RESOLUTION_10_BIT
	 * @see #RESOLUTION_11_BIT
	 * @see #RESOLUTION_12_BIT
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	public double getTemperatureResolution(byte[] state) {
		double tempres = (double) 0.0;

		// calculate temperature resolution according to configuration byte
		switch (state[4]) {

		case RESOLUTION_9_BIT:
			tempres = (double) 0.5;
			break;
		case RESOLUTION_10_BIT:
			tempres = (double) 0.25;
			break;
		case RESOLUTION_11_BIT:
			tempres = (double) 0.125;
			break;
		case RESOLUTION_12_BIT:
			tempres = (double) 0.0625;
			break;
		default:
			tempres = (double) 0.0;
		} // switch

		return tempres;
	}

	// --------
	// -------- Temperature 'set' Methods
	// --------

	/**
	 * Sets the temperature alarm value in Celsius in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param alarmType  valid value: <code>ALARM_HIGH</code> or
	 *                   <code>ALARM_LOW</code>
	 * @param alarmValue alarm trip value in Celsius
	 * @param state      byte array with device state information
	 *
	 * @see #hasTemperatureAlarms
	 * @see #getTemperatureAlarm
	 */
	public void setTemperatureAlarm(int alarmType, double alarmValue, byte[] state)
			throws OneWireException, OneWireIOException {
		if ((alarmType != ALARM_LOW) && (alarmType != ALARM_HIGH))
			throw new IllegalArgumentException("Invalid alarm type.");

		if (alarmValue > 85.0 || alarmValue < -40.0)
			throw new IllegalArgumentException("Value for alarm not in accepted range.  Must be -40 C <-> +85 C.");

		state[(alarmType == ALARM_LOW) ? 3 : 2] = (byte) alarmValue;
	}

	/**
	 * Sets the current temperature resolution in Celsius in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param resolution temperature resolution in Celsius. Valid values are
	 *                   <code>RESOLUTION_9_BIT</code>,
	 *                   <code>RESOLUTION_10_BIT</code>,
	 *                   <code>RESOLUTION_11_BIT</code> and
	 *                   <code>RESOLUTION_12_BIT</code>.
	 * @param state      byte array with device state information
	 *
	 * @see #RESOLUTION_9_BIT
	 * @see #RESOLUTION_10_BIT
	 * @see #RESOLUTION_11_BIT
	 * @see #RESOLUTION_12_BIT
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 */
	public void setTemperatureResolution(double resolution, byte[] state) throws OneWireException {
		byte configbyte = RESOLUTION_12_BIT;

		synchronized (this) {

			// calculate configbyte from given resolution
			if (resolution == 0.5)
				configbyte = RESOLUTION_9_BIT;

			if (resolution == 0.25)
				configbyte = RESOLUTION_10_BIT;

			if (resolution == 0.125)
				configbyte = RESOLUTION_11_BIT;

			if (resolution == 0.0625)
				configbyte = RESOLUTION_12_BIT;

			state[4] = configbyte;
		}
	}

	/**
	 * Retrieves this <code>OneWireContainer42</code> state information. The state
	 * information is returned as a byte array. Pass this byte array to the
	 * '<code>get</code>' and '<code>set</code>' methods. If the device state needs
	 * to be changed, then call the <code>writeDevice()</code> to finalize the
	 * changes.
	 *
	 * @return <code>OneWireContainer42</code> state information. Device state looks
	 *         like this:
	 * 
	 *         <pre>
	 *   0 : temperature LSB
	 *   1 : temperature MSB
	 *   2 : trip high
	 *   3 : trip low
	 *   4 : configuration register (for resolution)
	 *   5 : reserved
	 *   6 : reserved
	 *   7 : reserved
	 *   8 : an 8 bit CRC of the previous 8 bytes
	 *   9 : PIO Status bit assignment to write (this is a "don't care" for a read)
	 *   10: PIO Status bit assignment to read
	 * 
	 * PIO Status Bit Assignment from PIO Access Write [A5H]:
	 *   b7-b2 = all ones      
	 *   b1=PIOB Pin State     
	 *   b0=PIOA Pin State   
	 * 
	 * PIO Status Bit Assignment from PIO Access Read [F5H]:
	 *   b7-b4 = Complement of b3 to b0     
	 *   b3= PIOB Output Latch State   
	 *   b2=PIOB Pin State     
	 *   b1=PIOA Output Latch State   
	 *   b0= PIOA Pin State
	 * 
	 *         </pre>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #writeDevice
	 */
	public byte[] readDevice() throws OneWireIOException, OneWireException {

		byte[] scratchData;
		byte[] switchData = new byte[2];
		byte[] resultBuff = new byte[11];

		// scratchpad read
		scratchData = recallE2();

		// switch read
		switchData[0] = (byte) PIO_ACCESS_READ; // PIO Access Read Command
		switchData[1] = (byte) 0xFF; // Used to read the PIO Status Bit Assignment

		// select the device
		if (adapter.select(address)) {
			adapter.dataBlock(switchData, 0, 2);
		} else
			throw new OneWireIOException("Device select failed");

		// copy data to the results buffer
		System.arraycopy(scratchData, 0, resultBuff, 0, 9);
		resultBuff[9] = switchData[0]; // Since this is a read, we don't care what this byte is
		resultBuff[10] = switchData[1];

		return resultBuff;
	}

	/**
	 * Writes to this <code>OneWireContainer42</code> <code>state</code> information
	 * that have been changed by '<code>set</code>' methods. Only the device's
	 * "changed" state information is written to the part. This is done by
	 * referencing a field information appended to the state data.
	 *
	 * @param state byte array with device state information from a previous
	 *              readDevice()
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #readDevice
	 */
	public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {
		byte[] temp = new byte[3];
		byte[] switchBuff = new byte[5];

		temp[0] = state[2];
		temp[1] = state[3];
		temp[2] = state[4];

		// Write it to the Scratchpad.
		writeScratchpad(temp);

		// Place in memory.
		copyScratchpad();

		// Write switch information
		switchBuff[0] = (byte) PIO_ACCESS_WRITE; // PIO Access Write Command
		switchBuff[1] = (byte) state[9]; // Channel write information
		switchBuff[2] = (byte) ~state[9]; // Inverted write byte
		switchBuff[3] = (byte) 0xFF; // Confirmation Byte
		switchBuff[4] = (byte) 0xFF; // PIO Pin Status

		// select the device
		if (adapter.select(address)) {
			adapter.dataBlock(switchBuff, 0, 5);
		} else
			throw new OneWireIOException("OneWireContainer42-Device select failed");

		if (switchBuff[3] != (byte) 0x00AA) {
			throw new OneWireIOException("OneWireContainer42-Failure to change latch state.");
		}
	}

	// --------
	// -------- Custom Methods for this iButton Type
	// --------

	/**
	 * Reads the Scratchpad of the DS28EA00.
	 *
	 * @return 9-byte buffer representing the scratchpad
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public byte[] readScratchpad() throws OneWireIOException, OneWireException {
		byte[] result_block;

		// select the device
		if (adapter.select(address)) {

			// create a block to send that reads the scratchpad
			byte[] send_block = new byte[10];

			// read scratchpad command
			send_block[0] = (byte) READ_SCRATCHPAD_COMMAND;

			// now add the read bytes for data bytes and crc8
			for (int i = 1; i < 10; i++)
				send_block[i] = (byte) 0xFF;

			// send the block
			adapter.dataBlock(send_block, 0, send_block.length);

			// now, send_block contains the 9-byte Scratchpad plus READ_SCRATCHPAD_COMMAND
			// byte
			// convert the block to a 9-byte array representing Scratchpad (get rid of first
			// byte)
			result_block = new byte[9];

			for (int i = 0; i < 9; i++) {
				result_block[i] = send_block[i + 1];
			}

			// see if CRC8 is correct
			if (CRC8.compute(send_block, 1, 9) == 0)
				return (result_block);
			else
				throw new OneWireIOException("OneWireContainer42-Error reading CRC8 from device.");
		}

		// device must not have been present
		throw new OneWireIOException("OneWireContainer42-Device not found on 1-Wire Network");
	}

	/**
	 * Writes to the Scratchpad of the DS28EA00.
	 *
	 * @param data data to be written to the scratchpad. First byte of data must be
	 *             the temperature High Trip Point, the second byte must be the
	 *             temperature Low Trip Point, and the third must be the Resolution
	 *             (configuration register).
	 *
	 * @throws OneWireIOException       on a 1-Wire communication error such as
	 *                                  reading an incorrect CRC from this
	 *                                  <code>OneWireContainer42</code>. This could
	 *                                  be caused by a physical interruption in the
	 *                                  1-Wire Network due to shorts or a newly
	 *                                  arriving 1-Wire device issuing a 'presence
	 *                                  pulse'.
	 * @throws OneWireException         on a communication or setup error with the
	 *                                  1-Wire adapter
	 * @throws IllegalArgumentException when data is of invalid length
	 */
	public void writeScratchpad(byte[] data) throws OneWireIOException, OneWireException {

		// setup buffer to write to scratchpad
		byte[] writeBuffer = new byte[4];

		writeBuffer[0] = WRITE_SCRATCHPAD_COMMAND;
		writeBuffer[1] = data[0];
		writeBuffer[2] = data[1];
		writeBuffer[3] = data[2];

		// send command block to device
		if (adapter.select(address)) {
			adapter.dataBlock(writeBuffer, 0, writeBuffer.length);
		} else {

			// device must not have been present
			throw new OneWireIOException("OneWireContainer42-Device not found on 1-Wire Network");
		}

		// double check by reading scratchpad
		byte[] readBuffer;

		readBuffer = readScratchpad();

		if ((readBuffer[2] != data[0]) || (readBuffer[3] != data[1]) || (readBuffer[4] != data[2])) {

			// writing to scratchpad failed
			throw new OneWireIOException("OneWireContainer42-Error writing to scratchpad");
		}

		return;
	}

	/**
	 * Copies the Scratchpad to the E-squared memory of the DS28EA00.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public void copyScratchpad() throws OneWireIOException, OneWireException {

		// first, let's read the scratchpad to compare later.
		byte[] readfirstbuffer;

		readfirstbuffer = readScratchpad();

		// second, let's copy the scratchpad.
		if (adapter.select(address)) {

			// apply the power delivery
			adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
			adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);

			// send the copy scratchpad command
			adapter.putByte(COPY_SCRATCHPAD_COMMAND);

			// sleep for 10 milliseconds to allow copy to take place.
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			;

			// Turn power back to normal.
			adapter.setPowerNormal();
		} else {

			// device must not have been present
			throw new OneWireIOException("OneWireContainer42-Device not found on 1-Wire Network");
		}

		// third, let's read the scratchpad again with the recallE2 command and compare.
		byte[] readlastbuffer;

		readlastbuffer = recallE2();

		if ((readfirstbuffer[2] != readlastbuffer[2]) || (readfirstbuffer[3] != readlastbuffer[3])
				|| (readfirstbuffer[4] != readlastbuffer[4])) {

			// copying to scratchpad failed
			throw new OneWireIOException("OneWireContainer42-Error copying scratchpad to E2 memory.");
		}
	}

	/**
	 * Recalls the DS28EA00 temperature trigger values (<code>ALARM_HIGH</code> and
	 * <code>ALARM_LOW</code>) and the configuration register to the scratchpad and
	 * reads the scratchpad.
	 *
	 * @return byte array representing data in the device's scratchpad.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public byte[] recallE2() throws OneWireIOException, OneWireException {
		byte[] ScratchBuff;

		// select the device
		if (adapter.select(address)) {

			// send the Recall E-squared memory command
			adapter.putByte(RECALL_E2MEMORY_COMMAND);

			// read scratchpad
			ScratchBuff = readScratchpad();

			return (ScratchBuff);
		}

		// device must not have been present
		throw new OneWireIOException("OneWireContainer42-Device not found on 1-Wire Network");
	}

	/**
	 * Reads the way power is supplied to the DS28EA00.
	 *
	 * @return <code>true</code> for external power, <BR>
	 *         <code>false</code> for parasite power
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer42</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public boolean isExternalPowerSupplied() throws OneWireIOException, OneWireException {
		int intresult = 0;
		boolean result = false;

		// select the device
		if (adapter.select(address)) {
			// send the "Read Power Supply" memory command
			adapter.putByte(READ_POWER_SUPPLY_COMMAND);

			// read results
			intresult = adapter.getByte();
		} else {

			// device must not have been present
			throw new OneWireIOException("OneWireContainer42-Device not found on 1-Wire Network");
		}
		if (intresult != 0x00)
			result = true; // reads 0xFF for true and 0x00 for false

		return result;
	}

	/**
	 * Converts a temperature reading from Celsius to Fahrenheit.
	 *
	 * @param celsiusTemperature temperature value in Celsius
	 *
	 * @return the Fahrenheit conversion of the supplied temperature
	 *
	 * @deprecated Replace with call to
	 *             com.dalsemi.onewire.utils.Convert.toFahrenheit()
	 *
	 * @see com.dalsemi.onewire.utils.Convert#toFahrenheit(double)
	 */
	public float convertToFahrenheit(float celsiusTemperature) {
		return (float) Convert.toFahrenheit(celsiusTemperature);
	}

	// --------
	// -------- Switch Feature methods
	// --------

	/**
	 * Gets the number of channels supported by this switch. Channel specific
	 * methods will use a channel number specified by an integer from [0 to
	 * (<code>getNumberChannels(byte[])</code> - 1)]. Note that all devices of the
	 * same family will not necessarily have the same number of channels.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return the number of channels for this device
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 */
	public int getNumberChannels(byte[] state) {
		return 2;
	}

	/**
	 * Checks if the channels of this switch are 'high side' switches. This
	 * indicates that when 'on' or <code>true</code>, the switch output is connect
	 * to the 1-Wire data. If this method returns <code>false</code> then when the
	 * switch is 'on' or <code>true</code>, the switch is connected to ground.
	 *
	 * @return <code>true</code> if the switch is a 'high side' switch,
	 *         <code>false</code> if the switch is a 'low side' switch
	 *
	 * @see #getLatchState(int,byte[])
	 */
	public boolean isHighSideSwitch() {
		return false;
	}

	/**
	 * Checks if the channels of this switch support activity sensing. If this
	 * method returns <code>true</code> then the method
	 * <code>getSensedActivity(int,byte[])</code> can be used.
	 *
	 * @return <code>true</code> if channels support activity sensing
	 *
	 * @see #getSensedActivity(int,byte[])
	 * @see #clearActivity()
	 */
	public boolean hasActivitySensing() {
		return false;
	}

	/**
	 * Checks if the channels of this switch support level sensing. If this method
	 * returns <code>true</code> then the method <code>getLevel(int,byte[])</code>
	 * can be used.
	 *
	 * @return <code>true</code> if channels support level sensing
	 *
	 * @see #getLevel(int,byte[])
	 */
	public boolean hasLevelSensing() {
		return true;
	}

	/**
	 * Checks if the channels of this switch support 'smart on'. Smart on is the
	 * ability to turn on a channel such that only 1-Wire device on this channel are
	 * awake and ready to do an operation. This greatly reduces the time to discover
	 * the device down a branch. If this method returns <code>true</code> then the
	 * method <code>setLatchState(int,boolean,boolean,byte[])</code> can be used
	 * with the <code>doSmart</code> parameter <code>true</code>.
	 *
	 * @return <code>true</code> if channels support 'smart on'
	 *
	 * @see #setLatchState(int,boolean,boolean,byte[])
	 */
	public boolean hasSmartOn() {
		return false;
	}

	/**
	 * Checks if the channels of this switch require that only one channel is on at
	 * any one time. If this method returns <code>true</code> then the method
	 * <code>setLatchState(int,boolean,boolean,byte[])</code> will not only affect
	 * the state of the given channel but may affect the state of the other channels
	 * as well to insure that only one channel is on at a time.
	 *
	 * @return <code>true</code> if only one channel can be on at a time.
	 *
	 * @see #setLatchState(int,boolean,boolean,byte[])
	 */
	public boolean onlySingleChannelOn() {
		return false;
	}

	// --------
	// -------- Switch 'get' Methods
	// --------

	/**
	 * Checks the sensed level on the indicated channel. To avoid an exception,
	 * verify that this switch has level sensing with the
	 * <code>hasLevelSensing()</code>. Level sensing means that the device can sense
	 * the logic level on its PIO pin.
	 *
	 * @param channel channel to execute this operation, in the range [0 to
	 *                (<code>getNumberChannels(byte[])</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return <code>true</code> if level sensed is 'high' and <code>false</code> if
	 *         level sensed is 'low'
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasLevelSensing()
	 */
	public boolean getLevel(int channel, byte[] state) {
		byte level = (byte) (0x01 << (channel * 2));
		return ((state[10] & level) == level);
	}

	/**
	 * Checks the latch state of the indicated channel.
	 *
	 * @param channel channel to execute this operation, in the range [0 to
	 *                (<code>getNumberChannels(byte[])</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return <code>true</code> if channel latch is 'on' or conducting and
	 *         <code>false</code> if channel latch is 'off' and not conducting. Note
	 *         that the actual output when the latch is 'on' is returned from the
	 *         <code>isHighSideSwitch()</code> method.
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #isHighSideSwitch()
	 * @see #setLatchState(int,boolean,boolean,byte[])
	 */
	public boolean getLatchState(int channel, byte[] state) {
		byte latch = (byte) (0x01 << ((channel * 2) + 1));
		return ((state[10] & latch) == latch);
	}

	/**
	 * This method always returns false for the DS28EA00 (no activity sensing).
	 * Checks if the indicated channel has experienced activity. This occurs when
	 * the level on the PIO pins changes. To clear the activity that is reported,
	 * call <code>clearActivity()</code>. To avoid an exception, verify that this
	 * device supports activity sensing by calling the method
	 * <code>hasActivitySensing()</code>.
	 *
	 * @param channel channel to execute this operation, in the range [0 to
	 *                (<code>getNumberChannels(byte[])</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return <code>true</code> if activity was detected and <code>false</code> if
	 *         no activity was detected
	 *
	 * @throws OneWireException if this device does not have activity sensing
	 *
	 * @see #hasActivitySensing()
	 * @see #clearActivity()
	 */
	public boolean getSensedActivity(int channel, byte[] state) throws OneWireException {
		return false;
	}

	/**
	 * This method does nothing for the DS28EA00 (not needed). Clears the activity
	 * latches the next time possible. For example, on a DS2406/07, this happens the
	 * next time the status is read with <code>readDevice()</code>.
	 *
	 * @throws OneWireException if this device does not support activity sensing
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #getSensedActivity(int,byte[])
	 */
	public void clearActivity() throws OneWireException {
	}

	// --------
	// -------- Switch 'set' Methods
	// --------

	/**
	 * Sets the latch state of the indicated channel. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>.
	 *
	 * @param channel    channel to execute this operation, in the range [0 to
	 *                   (<code>getNumberChannels(byte[])</code> - 1)]
	 * @param latchState <code>true</code> to set the channel latch 'on'
	 *                   (conducting) and <code>false</code> to set the channel
	 *                   latch 'off' (not conducting). Note that the actual output
	 *                   when the latch is 'on' is returned from the
	 *                   <code>isHighSideSwitch()</code> method.
	 * @param doSmart    If latchState is 'on'/<code>true</code> then doSmart
	 *                   indicates if a 'smart on' is to be done. To avoid an
	 *                   exception check the capabilities of this device using the
	 *                   <code>hasSmartOn()</code> method.
	 * @param state      current state of the device returned from
	 *                   <code>readDevice()</code>
	 *
	 * @see #hasSmartOn()
	 * @see #getLatchState(int,byte[])
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 */
	public void setLatchState(int channel, boolean latchState, boolean doSmart, byte[] state) {
		byte latch = (byte) (0x01 << channel);
		byte temp;

		state[9] = (byte) 0x00FC;

		if (getLatchState(0, state)) {
			temp = (byte) 0x01;
			state[9] = (byte) (((byte) state[9]) | temp);
		}

		if (getLatchState(1, state)) {
			temp = (byte) 0x02;
			state[9] = (byte) (((byte) state[9]) | temp);
		}

		if (latchState)
			state[9] = (byte) (state[9] | latch);
		else
			state[9] = (byte) (state[9] & ~latch);
	}

	/**
	 * Sets the latch state for all of the channels. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>.
	 *
	 * @param set   the state to set all of the channels, in the range [0 to
	 *              (<code>getNumberChannels(byte[])</code> - 1)]
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @see #getLatchState(int,byte[])
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 */
	public void setLatchState(byte set, byte[] state) {
		state[9] = (byte) set;
	}

	/**
	 * This method does nothing for the DS28EA00.
	 *
	 * @return 1-Wire device register mask
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public byte[] readRegister() throws OneWireIOException, OneWireException {
		byte[] register = new byte[3];

		return register;
	}

	/**
	 * This method does nothing for the DS28EA00.
	 *
	 * @param register 1-Wire device sensor state
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public void writeRegister(byte[] register) throws OneWireIOException, OneWireException {
	}
}
