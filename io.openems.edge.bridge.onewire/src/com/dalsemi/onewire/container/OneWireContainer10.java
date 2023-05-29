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

// imports
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.utils.CRC8;

/**
 * <P>
 * 1-Wire container for temperature iButton which measures temperatures from
 * -55@htmlonly &#176C @endhtmlonly to +100@htmlonly &#176C @endhtmlonly, DS1920
 * or DS18S20. This container encapsulates the functionality of the iButton
 * family type <B>10</B> (hex)
 * </P>
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Measures temperatures from -55@htmlonly &#176C @endhtmlonly to
 * +100@htmlonly &#176C @endhtmlonly in typically 0.2 seconds
 * <LI>Zero standby power
 * <LI>0.5@htmlonly &#176C @endhtmlonly resolution, digital temperature reading
 * in two's complement
 * <LI>Increased resolution through interpolation in internal counters
 * <LI>8-bit device-generated CRC for data integrity
 * <LI>Special command set allows user to skip ROM section and do temperature
 * measurements simultaneously for all devices on the bus
 * <LI>2 bytes of EEPROM to be used either as alarm triggers or user memory
 * <LI>Alarm search directly indicates which device senses alarming temperatures
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
 * <DD><A HREF="http://pdfserv.maxim-ic.com/arpdf/DS1920.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS1920.pdf</A>
 * <DD><A HREF="http://pdfserv.maxim-ic.com/arpdf/DS18S20.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS18S20.pdf</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.container.TemperatureContainer
 *
 * @version 1.00, 1 Sep 2000
 * @author DS,JK Converted to use TemperatureContainer interface 9-1-2000 KLA
 */
public class OneWireContainer10 extends OneWireContainer implements TemperatureContainer {
	private boolean normalResolution = true;

	// --------
	// -------- Static Final Variables
	// --------

	/**
	 * default temperature resolution for this <code>OneWireContainer10</code>
	 * device.
	 */
	public static final double RESOLUTION_NORMAL = 0.5;

	/**
	 * maximum temperature resolution for this <code>OneWireContainer10</code>
	 * device. Use <code>RESOLUTION_MAXIMUM</code> in <code>setResolution()</code>
	 * if higher resolution is desired.
	 */
	public static final double RESOLUTION_MAXIMUM = 0.1;

	/** DS1920 convert temperature command */
	private static final byte CONVERT_TEMPERATURE_COMMAND = 0x44;

	/** DS1920 read data from scratchpad command */
	private static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;

	/** DS1920 write data to scratchpad command */
	private static final byte WRITE_SCRATCHPAD_COMMAND = (byte) 0x4E;

	/** DS1920 copy data from scratchpad to EEPROM command */
	private static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x48;

	/**
	 * Creates an empty <code>OneWireContainer10</code>. Must call
	 * <code>setupContainer()</code> before using this new container.
	 * <p>
	 *
	 * This is one of the methods to construct a <code>OneWireContainer10</code>.
	 * The others are through creating a <code>OneWireContainer10</code> with
	 * parameters.
	 *
	 * @see #OneWireContainer10(DSPortAdapter,byte[])
	 * @see #OneWireContainer10(DSPortAdapter,long)
	 * @see #OneWireContainer10(DSPortAdapter,String)
	 */
	public OneWireContainer10() {
	}

	/**
	 * Creates a <code>OneWireContainer10</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer10</code>.
	 * The others are through creating a <code>OneWireContainer10</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer10()
	 * @see #OneWireContainer10(DSPortAdapter,long)
	 * @see #OneWireContainer10(DSPortAdapter,String)
	 */
	public OneWireContainer10(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer10</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer10</code>.
	 * The others are through creating a <code>OneWireContainer10</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer10()
	 * @see #OneWireContainer10(DSPortAdapter,byte[])
	 * @see #OneWireContainer10(DSPortAdapter,String)
	 */
	public OneWireContainer10(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);
	}

	/**
	 * Creates a <code>OneWireContainer10</code> with the provided adapter object
	 * and the address of this One-Wire device.
	 *
	 * This is one of the methods to construct a <code>OneWireContainer10</code>.
	 * The others are through creating a <code>OneWireContainer10</code> with
	 * different parameters types.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      One-Wire device
	 * @param newAddress    address of this One-Wire device
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #OneWireContainer10()
	 * @see #OneWireContainer10(DSPortAdapter,byte[])
	 * @see #OneWireContainer10(DSPortAdapter,long)
	 */
	public OneWireContainer10(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);
	}

	// --------
	// -------- Information methods
	// --------

	/**
	 * Retrieves the Maxim Integrated Products part number of this
	 * <code>OneWireContainer10</code> as a <code>String</code>. For example
	 * 'DS1920'.
	 *
	 * @return this <code>OneWireContainer10</code> name
	 */
	@Override
	public String getName() {
		return "DS1920";
	}

	/**
	 * Retrieves the alternate Maxim Integrated Products part numbers or names. A
	 * 'family' of 1-Wire Network devices may have more than one part number
	 * depending on packaging. There can also be nicknames such as 'Crypto iButton'.
	 *
	 * @return this <code>OneWireContainer10</code> alternate names
	 */
	@Override
	public String getAlternateNames() {
		return "DS18S20";
	}

	/**
	 * Retrieves a short description of the function of this
	 * <code>OneWireContainer10</code> type.
	 *
	 * @return <code>OneWireContainer10</code> functional description
	 */
	@Override
	public String getDescription() {
		return """
				Digital thermometer measures temperatures from \
				-55C to 100C in typically 0.2 seconds.  +/- 0.5C \
				Accuracy between 0C and 70C. 0.5C standard \
				resolution, higher resolution through interpolation. \
				Contains high and low temperature set points for \
				generation of alarm.""";
	}

	// --------
	// -------- Custom Methods for OneWireContainer10
	// --------
	// --------
	// -------- Temperature Feature methods
	// --------

	/**
	 * Checks to see if this temperature measuring device has high/low trip alarms.
	 *
	 * @return <code>true</code> if this <code>OneWireContainer10</code> has
	 *         high/low trip alarms
	 *
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 */
	@Override
	public boolean hasTemperatureAlarms() {
		return true;
	}

	/**
	 * Checks to see if this device has selectable temperature resolution.
	 *
	 * @return <code>true</code> if this <code>OneWireContainer10</code> has
	 *         selectable temperature resolution
	 *
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	@Override
	public boolean hasSelectableTemperatureResolution() {
		return true;
	}

	/**
	 * Gets an array of available temperature resolutions in Celsius.
	 *
	 * @return byte array of available temperature resolutions in Celsius for this
	 *         <code>OneWireContainer10</code>. The minimum resolution is returned
	 *         as the first element and maximum resolution as the last element.
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #setTemperatureResolution
	 */
	@Override
	public double[] getTemperatureResolutions() {
		var resolutions = new double[2];

		resolutions[0] = RESOLUTION_NORMAL;
		resolutions[1] = RESOLUTION_MAXIMUM;

		return resolutions;
	}

	/**
	 * Gets the temperature alarm resolution in Celsius.
	 *
	 * @return temperature alarm resolution in Celsius for this
	 *         <code>OneWireContainer10</code>
	 *
	 * @see #hasTemperatureAlarms
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 *
	 */
	@Override
	public double getTemperatureAlarmResolution() {
		return 1.0;
	}

	/**
	 * Gets the maximum temperature in Celsius.
	 *
	 * @return maximum temperature in Celsius for this
	 *         <code>OneWireContainer10</code>
	 *
	 * @see #getMinTemperature
	 */
	@Override
	public double getMaxTemperature() {
		return 100.0;
	}

	/**
	 * Gets the minimum temperature in Celsius.
	 *
	 * @return minimum temperature in Celsius for this
	 *         <code>OneWireContainer10</code>
	 *
	 * @see #getMaxTemperature
	 */
	@Override
	public double getMinTemperature() {
		return -55.0;
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
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #getTemperature
	 */
	@Override
	public void doTemperatureConvert(byte[] state) throws OneWireIOException, OneWireException {
		this.doSpeed();

		// select the device
		if (!this.adapter.select(this.address)) {
			// device must not have been present
			throw new OneWireIOException("OneWireContainer10-device not present");
		}
		// Setup Power Delivery
		this.adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
		this.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);

		// send the convert temperature command
		this.adapter.putByte(CONVERT_TEMPERATURE_COMMAND);

		// delay for 750 ms
		try {
			Thread.sleep(750);
		} catch (InterruptedException e) {
		}

		// Turn power back to normal.
		this.adapter.setPowerNormal();

		// check to see if the temperature conversion is over
		if (this.adapter.getByte() != 0x0FF) {
			throw new OneWireIOException("OneWireContainer10-temperature conversion not complete");
		}

		// read the result
		var mode = state[4]; // preserve the resolution in the state

		this.adapter.select(this.address);
		this.readScratch(state);

		state[4] = mode;
	}

	// --------
	// -------- Temperature 'get' Methods
	// --------

	/**
	 * Gets the temperature value in Celsius from the <code>state</code> data
	 * retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information for this
	 *              <code>OneWireContainer10</code>
	 *
	 * @return temperature in Celsius from the last
	 *         <code>doTemperatureConvert()</code>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 *
	 * @see #doTemperatureConvert
	 */
	@Override
	public double getTemperature(byte[] state) throws OneWireIOException {

		// on some parts, namely the 18S20, you can get invalid readings.
		// basically, the detection is that all the upper 8 bits should
		// be the same by sign extension. the error condition (DS18S20
		// returns 185.0+) violated that condition
		if ((state[1] & 0x0ff) != 0x00 && (state[1] & 0x0ff) != 0x0FF) {
			throw new OneWireIOException("Invalid temperature data!");
		}

		var temp = (short) (state[0] & 0x0ff | state[1] << 8);

		if (state[4] == 1) {
			temp = (short) (temp >> 1); // lop off the last bit

			// also takes care of the / 2.0
			double tmp = temp;
			double cr = state[6] & 0x0ff;
			double cpc = state[7] & 0x0ff;

			return tmp - 0.25 + (cpc - cr) / cpc;
		}
		// do normal resolution
		return temp / 2.0;
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
	 *         <code>OneWireContainer10</code>
	 *
	 * @see #hasTemperatureAlarms
	 * @see #setTemperatureAlarm
	 */
	@Override
	public double getTemperatureAlarm(int alarmType, byte[] state) {
		return state[alarmType == ALARM_LOW ? 3 : 2];
	}

	/**
	 * Gets the current temperature resolution in Celsius from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information
	 *
	 * @return temperature resolution in Celsius for this
	 *         <code>OneWireContainer10</code>
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	@Override
	public double getTemperatureResolution(byte[] state) {
		if (state[4] == 0) {
			return RESOLUTION_NORMAL;
		}

		return RESOLUTION_MAXIMUM;
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
	@Override
	public void setTemperatureAlarm(int alarmType, double alarmValue, byte[] state) {
		if (alarmType != ALARM_LOW && alarmType != ALARM_HIGH) {
			throw new IllegalArgumentException("Invalid alarm type.");
		}

		if (alarmValue > 100.0 || alarmValue < -55.0) {
			throw new IllegalArgumentException("Value for alarm not in accepted range.  Must be -55 C <-> +100 C.");
		}

		state[alarmType == ALARM_LOW ? 3 : 2] = (byte) alarmValue;
	}

	/**
	 * Sets the current temperature resolution in Celsius in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param resolution temperature resolution in Celsius. Valid values are
	 *                   <code>RESOLUTION_NORMAL</code> and
	 *                   <code>RESOLUTION_MAXIMUM</code>.
	 * @param state      byte array with device state information
	 *
	 * @see #RESOLUTION_NORMAL
	 * @see #RESOLUTION_MAXIMUM
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 */
	@Override
	public void setTemperatureResolution(double resolution, byte[] state) {
		synchronized (this) {
			if (resolution == RESOLUTION_NORMAL) {
				this.normalResolution = true;
			} else {
				this.normalResolution = false;
			}

			state[4] = (byte) (this.normalResolution ? 0 : 1);
		}
	}

	/**
	 * Retrieves this <code>OneWireContainer10</code> state information. The state
	 * information is returned as a byte array. Pass this byte array to the
	 * '<code>get</code>' and '<code>set</code>' methods. If the device state needs
	 * to be changed, then call the <code>writeDevice()</code> to finalize the
	 * changes.
	 *
	 * @return <code>OneWireContainer10</code> state information. Device state looks
	 *         like this:
	 *
	 *         <pre>
	 *   0 : temperature LSB
	 *   1 : temperature MSB
	 *   2 : trip high
	 *   3 : trip low
	 *   4 : reserved (put the resolution here, 0 for normal, 1 for max)
	 *   5 : reserved
	 *   6 : count remain
	 *   7 : count per degree Celsius
	 *   8 : an 8 bit CRC over the previous 8 bytes of data
	 *         </pre>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #writeDevice
	 */
	@Override
	public byte[] readDevice() throws OneWireIOException, OneWireException {

		var data = new byte[8];

		this.doSpeed();

		// select the device
		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("OneWireContainer10-Device not found on 1-Wire Network");
		}
		// construct a block to read the scratchpad
		var buffer = new byte[10];

		// read scratchpad command
		buffer[0] = READ_SCRATCHPAD_COMMAND;

		// now add the read bytes for data bytes and crc8
		for (var i = 1; i < 10; i++) {
			buffer[i] = (byte) 0x0FF;
		}

		// send the block
		this.adapter.dataBlock(buffer, 0, buffer.length);

		// see if crc is correct
		if (CRC8.compute(buffer, 1, 9) == 0) {
			System.arraycopy(buffer, 1, data, 0, 8);
		} else {
			throw new OneWireIOException("OneWireContainer10-Error reading CRC8 from device.");
		}

		// we are just reading normalResolution here, no need to synchronize
		data[4] = (byte) (this.normalResolution ? 0 : 1);

		return data;
	}

	/**
	 * Writes to this <code>OneWireContainer10</code> <code>state</code> information
	 * that have been changed by '<code>set</code>' methods. Only the state
	 * registers that changed are updated. This is done by referencing a field
	 * information appended to the state data.
	 *
	 * @param state byte array with device state information
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #readDevice
	 */
	@Override
	public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {
		this.doSpeed();

		var temp = new byte[2];

		temp[0] = state[2];
		temp[1] = state[3];

		// Write it to the Scratchpad.
		this.writeScratchpad(temp);

		// Place in memory.
		this.copyScratchpad();
	}

	// --------
	// -------- Private Methods
	// --------

	/**
	 * Reads the 8 bytes from the scratchpad and verify CRC8 returned.
	 *
	 * @param data buffer to store the scratchpad data
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	private void readScratch(byte[] data) throws OneWireIOException, OneWireException {

		// select the device
		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("OneWireContainer10-Device not found on 1-Wire Network");
		}
		// construct a block to read the scratchpad
		var buffer = new byte[10];

		// read scratchpad command
		buffer[0] = READ_SCRATCHPAD_COMMAND;

		// now add the read bytes for data bytes and crc8
		for (var i = 1; i < 10; i++) {
			buffer[i] = (byte) 0x0FF;
		}

		// send the block
		this.adapter.dataBlock(buffer, 0, buffer.length);

		// see if crc is correct
		if (CRC8.compute(buffer, 1, 9) == 0) {
			System.arraycopy(buffer, 1, data, 0, 8);
		} else {
			throw new OneWireIOException("OneWireContainer10-Error reading CRC8 from device.");
		}
	}

	/**
	 * Writes to the Scratchpad.
	 *
	 * @param data this is the data to be written to the scratchpad. Cannot be more
	 *             than two bytes in size. First byte of data must be the
	 *             temperature High Trip Point and second byte must be temperature
	 *             Low Trip Point.
	 *
	 * @throws OneWireIOException       on a 1-Wire communication error such as
	 *                                  reading an incorrect CRC from this
	 *                                  <code>OneWireContainer10</code>. This could
	 *                                  be caused by a physical interruption in the
	 *                                  1-Wire Network due to shorts or a newly
	 *                                  arriving 1-Wire device issuing a 'presence
	 *                                  pulse'.
	 * @throws OneWireException         on a communication or setup error with the
	 *                                  1-Wire adapter
	 * @throws IllegalArgumentException when data length is not equal to
	 *                                  <code>2</code>
	 */
	private void writeScratchpad(byte[] data) throws OneWireIOException, OneWireException, IllegalArgumentException {

		// Variables.
		var write_block = new byte[3];
		var buffer = new byte[8];

		// First do some error checking.
		if (data.length != 2) {
			throw new IllegalArgumentException("Bad data.  Data must consist of only TWO bytes.");
		}

		// Prepare the write_block to be sent.
		write_block[0] = WRITE_SCRATCHPAD_COMMAND;
		write_block[1] = data[0];
		write_block[2] = data[1];

		// Send the block of data to the DS1920.
		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("OneWireContainer10 - Device not found");
		}
		this.adapter.dataBlock(write_block, 0, 3);

		// Check data to ensure correctly received.
		buffer = new byte[8];

		this.readScratch(buffer);

		// verify data
		if (buffer[2] != data[0] || buffer[3] != data[1]) {
			throw new OneWireIOException("OneWireContainer10 - data read back incorrect");
		}
	}

	/**
	 * Copies the contents of the User bytes of the ScratchPad to the EEPROM.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from this
	 *                            <code>OneWireContainer10</code>. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	private void copyScratchpad() throws OneWireIOException, OneWireException {

		// select the device
		if (!this.adapter.select(this.address)) {
			throw new OneWireIOException("OneWireContainer10 - device not found");
		}
		// send the copy command
		this.adapter.putByte(COPY_SCRATCHPAD_COMMAND);

		// Setup Power Delivery
		this.adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
		this.adapter.startPowerDelivery(DSPortAdapter.CONDITION_NOW);

		// delay for 10 ms
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}

		// Turn power back to normal.
		this.adapter.setPowerNormal();
	}
}
// CHECKSTYLE:ON
