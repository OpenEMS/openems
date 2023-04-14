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

import java.util.Enumeration;
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;

/**
 * <P>
 * 1-Wire&reg; container that encapsulates the functionality of the 1-Wire
 * family type <B>30</B> (hex), Maxim Integrated Products part number:
 * <B>DS2760, High Precision Li-ion Battery Monitor</B>.
 * </P>
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Li-ion safety circuit
 * <UL>
 * <LI>Overvoltage protection
 * <LI>Overcurrent/short circuit protection
 * <LI>Undervoltage protection
 * </UL>
 * <LI>Two sense resistor configurations
 * <UL>
 * <LI>Internal 25 mOhm sense resistor
 * <LI>External user-selectable sense resistor
 * </UL>
 * <LI>12-bit bi-directional current measurement
 * <LI>Current accumulation
 * <LI>Voltage measurement
 * <LI>Direct-to-digital temperature measurement
 * <LI>32 bytes of lockable EEPROM
 * <LI>16 bytes of general purpose SRAM
 * <LI>Low power consumption
 * <UL>
 * <LI>Active current: 80 @htmlonly &#181A @endhtmlonly max
 * <LI>Sleep current: 2 @htmlonly &#181A @endhtmlonly max
 * </UL>
 * </UL>
 *
 * <H3>Data sheet</H3>
 *
 * <A HREF="http://pdfserv.maxim-ic.com/arpdf/DS2760.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS2760.pdf</A>
 *
 *
 *
 * @version 0.00, 28 Aug 2000
 * @author COlmstea
 *
 *         Converted to use interfaces, general rework KLA
 */
public class OneWireContainer30 extends OneWireContainer implements ADContainer, TemperatureContainer {

	/**
	 * Memory functions.
	 */
	private static final byte WRITE_DATA_COMMAND = (byte) 0x6C;
	private static final byte READ_DATA_COMMAND = (byte) 0x69;
	private static final byte COPY_DATA_COMMAND = (byte) 0x48;
	private static final byte RECALL_DATA_COMMAND = (byte) 0xB8;
	private static final byte LOCK_COMMAND = (byte) 0x6A;

	/**
	 * Address of the Protection Register. Used to set/check flags with
	 * <CODE>setFlag()/getFlag()</CODE>.
	 */
	public static final byte PROTECTION_REGISTER = 0;

	/**
	 * Address of the Status Register. Used to set/check flags with
	 * <CODE>setFlag()/getFlag()</CODE>.
	 */
	public static final byte STATUS_REGISTER = 1;

	/**
	 * Address of the EEPROM Register. Used to set/check flags with
	 * <CODE>setFlag()/getFlag()</CODE>.
	 */
	public static final byte EEPROM_REGISTER = 7;

	/**
	 * Address of the Special Feature Register (SFR). Used to check flags with
	 * <CODE>getFlag()</CODE>.
	 */
	public static final byte SPECIAL_FEATURE_REGISTER = 8;

	/**
	 * PROTECTION REGISTER FLAG: When this flag is <CODE>true</CODE>, it indicates
	 * that the battery pack has experienced an overvoltage condition. This flag
	 * must be reset! Accessed with <CODE>getFlag()</CODE>.
	 */
	public static final byte OVERVOLTAGE_FLAG = (byte) 128;

	/**
	 * PROTECTION REGISTER FLAG: When this flag is <CODE>true</CODE>, the battery
	 * pack has experienced an undervoltage. This flag must be reset! Accessed with
	 * <CODE>getFlag()</CODE>
	 */
	public static final byte UNDERVOLTAGE_FLAG = 64;

	/**
	 * PROTECTION REGISTER FLAG: When this flag is <CODE>true</CODE> the battery has
	 * experienced a charge-direction overcurrent condition. This flag must be
	 * reset! Accessed with <CODE>getFlag()</CODE>
	 */
	public static final byte CHARGE_OVERCURRENT_FLAG = 32;

	/**
	 * PROTECTION REGISTER FLAG: When this flag is <CODE>true</CODE> the battery has
	 * experienced a discharge-direction overcurrent condition. This flag must be
	 * <CODE>reset()</CODE>! Accessed with <CODE>getFlag()</CODE>
	 */
	public static final byte DISCHARGE_OVERCURRENT_FLAG = 16;

	/**
	 * PROTECTION REGISTER FLAG: Mirrors the !CC output pin. Accessed with
	 * <CODE>getFlag()</CODE>
	 */
	public static final byte CC_PIN_STATE_FLAG = 8;

	/**
	 * PROTECTION REGISTER FLAG: Mirrors the !DC output pin. Accessed with
	 * <CODE>getFlag()</CODE>
	 */
	public static final byte DC_PIN_STATE_FLAG = 4;

	/**
	 * PROTECTION REGISTER FLAG: Resetting this flag will disable charging
	 * regardless of cell or pack conditions. Accessed with
	 * <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte CHARGE_ENABLE_FLAG = 2;

	/**
	 * PROTECTION REGISTER FLAG: Resetting this flag will disable discharging.
	 * Accessed with <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte DISCHARGE_ENABLE_FLAG = 1;

	/**
	 * STATUS REGISTER FLAG: Enables/disables the DS2760 to enter sleep mode when
	 * the DQ line goes low for greater than 2 seconds. Accessed with
	 * <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte SLEEP_MODE_ENABLE_FLAG = 32;

	/**
	 * STATUS REGISTER FLAG: If set, the opcode for the Read Net Address command
	 * will be set to 33h. If it is not set the opcode is set to 39h. Accessed with
	 * <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte READ_NET_ADDRESS_OPCODE_FLAG = 16;

	/**
	 * EEPROM REGISTER FLAG: This flag will be <CODE>true</CODE> if the Copy Data
	 * Command is in progress. Data may be written to EEPROM when this reads
	 * <CODE>false</CODE>. Accessed with <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte EEPROM_COPY_FLAG = (byte) 128;

	/**
	 * EEPROM REGISTER FLAG: When this flag is <CODE>true</CODE>, the Lock Command
	 * is enabled. The lock command is used to make memory permanently read only.
	 * Accessed with <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte EEPROM_LOCK_ENABLE_FLAG = 64;

	/**
	 * EEPROM REGISTER FLAG: When this flag is <CODE>true</CODE>, Block 1 of the
	 * EEPROM (addresses 48-63) is read-only. Accessed with <CODE>getFlag()</CODE>.
	 */
	public static final byte EEPROM_BLOCK_1_LOCK_FLAG = 2;

	/**
	 * EEPROM REGISTER FLAG: When this flag is <CODE>true</CODE>, Block 0 of the
	 * EEPROM (addresses 32-47) is read-only. Accessed with <CODE>getFlag()</CODE>.
	 */
	public static final byte EEPROM_BLOCK_0_LOCK_FLAG = 1;

	/**
	 * SPECIAL FEATURE REGISTER FLAG: Mirrors the state of the !PS pin. Accessed
	 * with <CODE>getFlag()</CODE>.
	 */
	public static final byte PS_PIN_STATE_FLAG = (byte) 128;

	/**
	 * SPECIAL FEATURE REGISTER FLAG: Mirrors/sets the state of the PIO pin. The PIO
	 * pin can be used as an output; resetting this flag disables the PIO output
	 * driver. Accessed with <CODE>getFlag()/setFlag()</CODE>.
	 */
	public static final byte PIO_PIN_SENSE_AND_CONTROL_FLAG = 64;

	/**
	 * Holds the value of the sensor external resistance.
	 */
	private double Rsens = .05;

	/**
	 * When this is true, all calculations are assumed to be done in the part.
	 */
	private boolean internalResistor;

	/**
	 * Default constructor
	 */
	public OneWireContainer30() {
		this.internalResistor = true;
	}

	/**
	 * Creates a container with a provided adapter object and the address of this
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer30(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);

		this.internalResistor = true;
	}

	/**
	 * Creates a container with a provided adapter object and the address of this
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer30(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);

		this.internalResistor = true;
	}

	/**
	 * Creates a container with a provided adapter object and the address of this
	 * 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this 1-Wire
	 *                      device
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer30(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);

		this.internalResistor = true;
	}

	/**
	 * Returns the Maxim Integrated Products part number of this 1-Wire device as a
	 * string.
	 *
	 * @return representation of the 1-Wire device name
	 *
	 */
	@Override
	public String getName() {
		return "DS2760";
	}

	/**
	 * Returns the alternate Maxim Integrated Products part number or name. ie.
	 * Smart Battery Monitor
	 *
	 * @return representation of the alternate names for this device
	 */
	@Override
	public String getAlternateNames() {
		return "1-Cell Li-Ion Battery Monitor";
	}

	/**
	 * Returns a short description of the function of this 1-Wire device type.
	 *
	 * @return representation of the function description
	 */
	@Override
	public String getDescription() {
		return """
			The DS2760 is a data acquisition, information storage, and safety\
			 protection device tailored for cost-sensitive battery pack applications.\
			 This low-power device integrates precise temperature, voltage, and\
			 current measurement , nonvolatile data storage, and Li-Ion protection\
			 into the small footprint of either a TSSOP packet or flip-chip.""";
	}

	/**
	 * Get an enumeration of memory bank instances that implement one or more of the
	 * following interfaces: {@link com.dalsemi.onewire.container.MemoryBank
	 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
	 * PagedMemoryBank}, and {@link com.dalsemi.onewire.container.OTPMemoryBank
	 * OTPMemoryBank}.
	 *
	 * @return <CODE>Enumeration</CODE> of memory banks
	 */
	@Override
	public Enumeration<MemoryBank> getMemoryBanks() {
		var bank_vector = new Vector<MemoryBank>(1);

		// EEPROM main bank
		var mn = new MemoryBankEEPROMblock(this);

		bank_vector.addElement(mn);

		return bank_vector.elements();
	}

	/**
	 * Sets the DS2760 to use its internal .025 ohm resistor for measurements. This
	 * should only be enabled if there is NO external resistor physically attached
	 * to the device.
	 */
	public synchronized void setResistorInternal() {
		this.internalResistor = true;
	}

	/**
	 * Sets the DS2760 to use an external, user-selectable resistance. This
	 * Resistance should be wired directly to the VSS (negative terminal of the
	 * cell).
	 *
	 * @param Rsens resistance in ohms
	 */
	public synchronized void setResistorExternal(double Rsens) {
		this.internalResistor = false;
		this.Rsens = Rsens;
	}

	/**
	 * Reads a register byte from the memory of the DS2760. Note that there is no
	 * error checking as the DS2760 performs no CRC on the data.
	 * <p>
	 * Note: This function should only be used when reading the register memory of
	 * the DS2760. The EEPROM blocks (addresses 32-64) should be accessed with
	 * writeBlock/readBlock.
	 *
	 * @param memAddr the address to read (0-255)
	 *
	 * @return data read from memory
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public byte readByte(int memAddr) throws OneWireIOException, OneWireException {
		var buffer = new byte[3];

		this.doSpeed();
		this.adapter.reset();

		if (this.adapter.select(this.address)) {

			/* setup the read */
			buffer[0] = READ_DATA_COMMAND;
			buffer[1] = (byte) memAddr;
			buffer[2] = (byte) 0xFF;

			this.adapter.dataBlock(buffer, 0, 3);

			return buffer[2];
		}
		throw new OneWireException("OneWireContainer30-Device not found.");
	}

	/**
	 * Reads bytes from the DS2760. Note that there is no error-checking as the
	 * DS2760 does not perform a CRC on the data.
	 * <p>
	 * Note: This function should only be used when reading the register memory of
	 * the DS2760. The EEPROM blocks (addresses 32-64) should be accessed with
	 * writeBlock/readBlock.
	 *
	 * @param memAddr the address to read (0-255)
	 * @param buffer  buffer to receive data
	 * @param start   start position within buffer to place data
	 * @param len     length of buffer
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public void readBytes(int memAddr, byte[] buffer, int start, int len) throws OneWireIOException, OneWireException {
		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		for (var i = start; i < start + len; i++) {
			buffer[i] = (byte) 0x0ff;
		}

		this.adapter.putByte(READ_DATA_COMMAND);
		this.adapter.putByte(memAddr & 0x0ff);
		this.adapter.dataBlock(buffer, start, len);
	}

	/**
	 * Writes a register byte to the memory of the DS2760. Note that the DS2760 does
	 * not make any use of cyclic redundancy checks (error-checking). To ensure
	 * error free transmission, double check write operation.
	 * <p>
	 * Note: This method should only be used when writing to the register memory of
	 * the DS2760. The EEPROM blocks (addresses 32-64) require special treatment and
	 * thus the writeBlock/readBlock functions should be used for those.
	 *
	 * @param memAddr address to write (0-255)
	 * @param data    data byte to write to memory
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find part
	 */
	public void writeByte(int memAddr, byte data) throws OneWireIOException, OneWireException {
		var buffer = new byte[3];

		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* first perform the write */
		buffer[0] = WRITE_DATA_COMMAND;
		buffer[1] = (byte) memAddr;
		buffer[2] = data;

		this.adapter.dataBlock(buffer, 0, 3);
	}

	/**
	 * Reads a 16 byte data block from one of the user EEPROM blocks. Note that
	 * there is no error-checking as the DS2760 performs no CRCs.
	 *
	 * @param blockNumber the EEPROM block number to read, acceptable parameters are
	 *                    0 and 1
	 *
	 * @return 16 data bytes
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public byte[] readEEPROMBlock(int blockNumber) throws OneWireIOException, OneWireException {
		var buffer = new byte[18];
		var result = new byte[16];

		// calculate the address (32 and 48 are valid addresses)
		var memAddr = (byte) (32 + blockNumber * 16);

		/* check for valid parameters */
		if (blockNumber != 0 && blockNumber != 1) {
			throw new IllegalArgumentException(
					"OneWireContainer30-Block number " + blockNumber + " is not a valid EEPROM block.");
		}

		/* perform the recall/read and verification */
		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* first recall the memory to shadow ram */
		buffer[0] = RECALL_DATA_COMMAND;
		buffer[1] = memAddr;

		this.adapter.dataBlock(buffer, 0, 2);

		/* now read the shadow ram */
		this.adapter.reset();
		this.adapter.select(this.address);

		buffer[0] = READ_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		for (var i = 0; i < 16; i++) {
			buffer[i + 2] = (byte) 0xff;
		}

		this.adapter.dataBlock(buffer, 0, 18);

		// keep this result
		System.arraycopy(buffer, 2, result, 0, 16);

		// user can re-read for verification
		return result;
	}

	/**
	 * Writes a 16 byte data block to one of the user blocks. The block may be
	 * rewritten at any time, except after it is locked with
	 * <CODE>lockBlock()</CODE>. This method performs error checking by verifying
	 * data written.
	 *
	 * @param blockNumber block to write, acceptable parameters are 0 and 1
	 * @param data        16 bytes of data to write
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find part
	 */
	public void writeEEPROMBlock(int blockNumber, byte[] data) throws OneWireIOException, OneWireException {
		var buffer = new byte[18];

		// the first block is at address 32 and the second is at address 48
		var memAddr = (byte) (32 + blockNumber * 16);

		/* check for valid parameters */
		if (data.length < 16) {
			throw new IllegalArgumentException("OneWireContainer30-Data block must consist of 16 bytes.");
		}

		if (blockNumber != 0 && blockNumber != 1) {
			throw new IllegalArgumentException(
					"OneWireContainer30-Block number " + blockNumber + " is not a valid EEPROM block.");
		}

		// if the EEPROM block is locked throw a OneWireIOException
		if (blockNumber == 0 && this.getFlag(EEPROM_REGISTER, EEPROM_BLOCK_0_LOCK_FLAG)
				|| blockNumber == 1 && this.getFlag(EEPROM_REGISTER, EEPROM_BLOCK_1_LOCK_FLAG)) {
			throw new OneWireIOException("OneWireContainer30-Cant write data to locked EEPROM block.");
		}

		/* perform the write/verification and copy */
		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* first write to shadow rom */
		buffer[0] = WRITE_DATA_COMMAND;
		buffer[1] = memAddr;

		for (var i = 0; i < 16; i++) {
			buffer[i + 2] = data[i];
		}

		this.adapter.dataBlock(buffer, 0, 18);

		/* read the shadow ram back for verification */
		this.adapter.reset();
		this.adapter.select(this.address);

		buffer[0] = READ_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		for (var i = 0; i < 16; i++) {
			buffer[i + 2] = (byte) 0xff;
		}

		this.adapter.dataBlock(buffer, 0, 18);

		// verify data
		for (var i = 0; i < 16; i++) {
			if (buffer[i + 2] != data[i]) {
				throw new OneWireIOException("OneWireContainer30-Error writing EEPROM block" + blockNumber + ".");
			}
		}

		/* now perform the copy to EEPROM */
		this.adapter.reset();
		this.adapter.select(this.address);

		buffer[0] = COPY_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		this.adapter.dataBlock(buffer, 0, 2);
	}

	/**
	 * Permanently write-protects one of the user blocks of EEPROM.
	 *
	 * @param blockNumber block number to permanently write protect, acceptable
	 *                    parameters are 0 and 1.
	 *
	 * @throws OneWireIOException Error locking block
	 * @throws OneWireException   Could not find part
	 */
	public void lockBlock(int blockNumber) throws OneWireIOException, OneWireException {

		// compute the byte location
		var memAddr = (byte) (32 + blockNumber * 16);

		/* check if the block is valid */
		if (blockNumber != 0 && blockNumber != 1) {
			throw new IllegalArgumentException(
					"OneWireContainer30-Block " + blockNumber + " is not a valid EEPROM block.");
		}

		/* perform the lock */
		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		this.adapter.putByte(LOCK_COMMAND);
		this.adapter.putByte(memAddr);
	}

	/**
	 * Checks the specified flag in the specified register. <BR>
	 * Valid registers are:
	 * <A HREF="#PROTECTION_REGISTER"><CODE>PROTECTION_REGISTER</CODE></A>,
	 * <A HREF="#STATUS_REGISTER"><CODE>STATUS_REGISTER</CODE></A>,
	 * <A HREF="#EEPROM_REGISTER"><CODE>EEPROM_REGISTER</CODE></A> and <A HREF=
	 * "#SPECIAL_FEATURE_REGISTER"><CODE>SPECIAL_FEATURE_REGISTER</CODE></A>.
	 *
	 * @param memAddr   registers address. Predefined fields for each register are
	 *                  defined above.
	 * @param flagToGet bitmask of desired flag, the acceptable parameters
	 *                  pertaining to each register are defined as constant fields
	 *                  above
	 *
	 * @return value of the flag: <CODE>true</CODE> if flag is set (=1)
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 *
	 */
	public boolean getFlag(int memAddr, byte flagToGet) throws OneWireIOException, OneWireException {

		// read the byte and perform a simple mask to determine if that byte is on
		var data = this.readByte(memAddr);

		if ((data & flagToGet) != 0) {
			return true;
		}

		return false;
	}

	/**
	 * Sets one of the flags in one of the registers.<BR>
	 * Valid registers are:
	 * <A HREF="#PROTECTION_REGISTER"><CODE>PROTECTION_REGISTER</CODE></A>,
	 * <A HREF="#STATUS_REGISTER"><CODE>STATUS_REGISTER</CODE></A>,
	 * <A HREF="#EEPROM_REGISTER"><CODE>EEPROM_REGISTER</CODE></A> and <A HREF=
	 * "#SPECIAL_FEATURE_REGISTER"><CODE>SPECIAL_FEATURE_REGISTER</CODE></A>.
	 *
	 * @param memAddr   register address, these addresses are predefined above
	 * @param flagToSet bitmask of flag to set, valid parameters pertaining to each
	 *                  register are defined as constant fields above
	 * @param flagValue value to set the flag to
	 *
	 * @throws OneWireIOException Error setting flag
	 * @throws OneWireException   Could not find part
	 */
	public void setFlag(int memAddr, byte flagToSet, boolean flagValue) throws OneWireIOException, OneWireException {

		// the desired default value for the status register flags has to be
		// set in a separate register for some reason, so I treat it specially.
		if (memAddr == STATUS_REGISTER) {
			memAddr = 49;
		}

		var data = this.readByte(memAddr);

		if (flagValue) {
			data = (byte) (data | flagToSet);
		} else {
			data = (byte) (data & ~flagToSet);
		}

		this.writeByte(memAddr, data);
	}

	/**
	 * Gets the instantaneous current.
	 *
	 *
	 * @param state device state
	 * @return current in Amperes
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public double getCurrent(byte[] state) throws OneWireIOException, OneWireException {

		// grab the data
		var data = state[14] << 8 | state[15] & 0x00ff;

		data = data >> 3;

		double result;

		// when the internal resistor is used, the device calculates it for you
		// the resolution is .625 mA
		if (this.internalResistor) {
			result = data * .625 / 1000;
		} else {
			result = data * .000015625 / this.Rsens;
		}

		return result;
	}

	/**
	 * Allows user to set the remaining capacity. Good for accurate capacity
	 * measurements using temperature and battery life.
	 * <p>
	 * By measuring the battery's current and voltage when it is fully charged and
	 * when it is empty, the voltage corresponding to an empty battery and the
	 * current corresponding to a full one can be derived. These values can be
	 * detected in user program and the remaining capacity can be set to the
	 * empty/full levels accordingly for nice accuracy.
	 *
	 * @param remainingCapacity remaining capacity in mAH
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public void setRemainingCapacity(double remainingCapacity) throws OneWireIOException, OneWireException {
		int data;

		// if the internal resistor is used, it can be stored as is (in mAH)
		if (this.internalResistor) {
			data = (int) (remainingCapacity * 4);
		} else {
			data = (int) (remainingCapacity * this.Rsens / .00626);
		}

		// break into bytes and store
		this.writeByte(16, (byte) (data >> 8));
		this.writeByte(17, (byte) (data & 0xff));
	}

	/**
	 * Calculates the remaining capacity in mAHours from the current Accumulator.
	 * Accurate to +/- .25 mAH.
	 *
	 * @param state device state
	 * @return mAHours of battery capacity remaining
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public double getRemainingCapacity(byte[] state) throws OneWireIOException, OneWireException {
		var result = 0D;

		// grab the data
		var data = (state[16] & 0xff) << 8 | state[17] & 0xff;

		// if the internal resistor is being used the part calculates it for us
		if (this.internalResistor) {
			result = data / 4.0;
		} else {
			result = data * .00626 / this.Rsens;
		}

		return result;
	}

	/**
	 * Sets the state for the Programmable Input/Output pin. In order to operate as
	 * a switch, PIO must be tied to a pull-up resistor to VDD.
	 *
	 * @param on state of the PIO pin to set
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find part
	 */

	// must have a pull-up resistor from PIO to VDD 4.7KOhm
	public void setLatchState(boolean on) throws OneWireIOException, OneWireException {

		// since bit 0 is read-only and bits 2-7 are don't cares,
		// we don't need to read location 8 first, we can just write
		this.writeByte(8, (byte) (on ? 0x40 : 0x00));
	}

	/**
	 * Returns the latch state of the Programmable Input/Output pin on the DS2760.
	 *
	 * @return state of the Programmable Input/Output pin
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	public boolean getLatchState() throws OneWireIOException, OneWireException {
		return (this.readByte(8) & 0x40) == 0x40;
	}

	/**
	 * Clears the overvoltage, undervoltage, charge overcurrent, and discharge
	 * overcurrent flags. Each time a violation occurs, these flags stay set until
	 * reset. This method resets all 4 flags.
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find part
	 */
	public void clearConditions() throws OneWireIOException, OneWireException {
		var protect_reg = this.readByte(0);

		this.writeByte(0, (byte) (protect_reg & 0x0f));
	}

	/////////////////////////////////////////////////////////////////////
	//
	// BEGIN CONTAINER INTERFACE METHODS
	//
	////////////////////////////////////////////////////////////////////
	// --------
	// -------- A/D Feature methods
	// --------

	/**
	 * Queries to get the number of channels supported by this A/D device. Channel
	 * specific methods will use a channel number specified by an integer from
	 * <CODE>[0 to (getNumberChannels() - 1)]</CODE>.
	 *
	 * @return number of channels
	 */
	@Override
	public int getNumberADChannels() {
		return 2;
	}

	/**
	 * Queries to see if this A/D measuring device has high/low alarms.
	 *
	 * @return <CODE>true</CODE> if has high/low trips
	 */
	@Override
	public boolean hasADAlarms() {
		return false;
	}

	/**
	 * Queries to get an array of available ranges for the specified A/D channel.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 *
	 * @return available ranges
	 */
	@Override
	public double[] getADRanges(int channel) {
		var result = new double[1];

		result[0] = 5.0;

		return result;
	}

	/**
	 * Queries to get an array of available resolutions based on the specified range
	 * on the specified A/D channel.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param range   range of channel specified in channel parameter
	 *
	 * @return available resolutions
	 */
	@Override
	public double[] getADResolutions(int channel, double range) {
		var result = new double[1];

		result[0] = this.getADResolution(channel, null);

		return result;
	}

	/**
	 * Queries to see if this A/D device supports doing multiple voltage conversions
	 * at the same time.
	 *
	 * @return <CODE>true</CODE> if can do multi-channel voltage reads
	 */
	@Override
	public boolean canADMultiChannelRead() {
		return false;
	}

	// --------
	// -------- A/D IO Methods
	// --------

	/**
	 * Performs voltage conversion on the specified channel. The method
	 * <CODE>getADVoltage()</CODE> can be used to read the result of the conversion.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param state   current state of this device returned from
	 *                <CODE>readDevice()</CODE>
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public void doADConvert(int channel, byte[] state) throws OneWireIOException, OneWireException {

		// this actually should be an airball as well...
		// BECAUSE...
		// the voltage is constantly being read when the part is
		// in active mode, so we can just read it out from the state.
		// the part only leaves active mode if the battery runs out
		// (i.e. voltage goes below threshold) in which case we should
		// return the lower bound anyway...
	}

	/**
	 * Performs voltage conversion on all specified channels. The method
	 * <CODE>getADVoltage()</CODE> can be used to read the result of the conversion.
	 * This A/D must support multi-channel read <CODE>canMultiChannelRead()</CODE>
	 * if there are more than 1 channel is specified.
	 *
	 * @param doConvert channels to perform conversion on
	 * @param state     current state of the device returned from
	 *                  <CODE>readDevice()</CODE>
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Device does not support multi-channel reading
	 */
	@Override
	public void doADConvert(boolean[] doConvert, byte[] state) throws OneWireIOException, OneWireException {
		throw new OneWireException("This device does not support multi-channel reading");
	}

	/**
	 * Reads the voltage values. Must be used after a <CODE>doADConvert()</CODE>
	 * method call. Also must include the last valid state from the
	 * <CODE>readDevice()</CODE> method and this A/D must support multi-channel read
	 * <CODE>canMultiChannelRead()</CODE> if there are more than 1 channel.
	 *
	 * @param state current state of the device returned from
	 *              <CODE>readDevice()</CODE>
	 *
	 * @return voltage values for all channels
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Device does not support multi-channel reading
	 */
	@Override
	public double[] getADVoltage(byte[] state) throws OneWireIOException, OneWireException {
		throw new OneWireException("This device does not support multi-channel reading");
	}

	/**
	 * Reads a channel voltage value. Must be used after a
	 * <CODE>doADConvert()</CODE> method call. Also must include the last valid
	 * state from the <CODE>readDevice()</CODE> method. Note, if more than one
	 * channel is to be read then it is more efficient to use the
	 * <CODE>getADVoltage(byte[])</CODE> method that returns all channel values.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param state   current state of the device returned from
	 *                <CODE>readDevice()</CODE>
	 *
	 * @return voltage value for the specified channel
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public double getADVoltage(int channel, byte[] state) throws OneWireIOException, OneWireException {
		if (channel < 0 || channel > 1) {
			throw new OneWireException("Invalid channel");
		}

		int data;

		// the measurement is put in two bytes, (MSB) and (LSB).
		data = state[12 + channel * 2] << 8 | state[13 + channel * 2] & 0x00ff;

		if (channel == 0) {
			// the voltage measurement channel
			// Once the two bytes are ORed, a right shift of 5 must occur
			// (signed shift)
			data = data >> 5;
		} else {
			// the current sensing channel
			// Once the two bytes are ORed, a right shift of 3 must occur
			// (signed shift)
			data = data >> 3;
		}

		return data * this.getADResolution(channel, state);
	}

	// --------
	// -------- A/D 'get' Methods
	// --------

	/**
	 * Extracts the alarm voltage value of the specified channel from the provided
	 * state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method.
	 *
	 * @param channel   channel in the range
	 *                  <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param alarmType desired alarm, <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param state     current state of the device returned from
	 *                  <CODE>readDevice()</CODE>
	 *
	 * @return alarm_value in volts
	 *
	 * @throws OneWireException Device does not support A/D alarms
	 */
	@Override
	public double getADAlarm(int channel, int alarmType, byte[] state) throws OneWireException {
		throw new OneWireException("This device does not have AD alarms");
	}

	/**
	 * Extracts the alarm enable value of the specified channel from the provided
	 * state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method.
	 *
	 * @param channel   channel in the range
	 *                  <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param alarmType desired alarm, <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param state     current state of the device returned from
	 *                  <CODE>readDevice()</CODE>
	 *
	 * @return <CODE>true</CODE> if specified alarm is enabled
	 *
	 * @throws OneWireException Device does not support A/D alarms
	 */
	@Override
	public boolean getADAlarmEnable(int channel, int alarmType, byte[] state) throws OneWireException {
		throw new OneWireException("This device does not have AD alarms");
	}

	/**
	 * Checks the A/D alarm event value of the specified channel from the provided
	 * state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method.
	 *
	 * @param channel   channel in the range
	 *                  <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param alarmType desired alarm, <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param state     current state of the device returned from
	 *                  <CODE>readDevice()</CODE>
	 *
	 * @return <CODE>true</CODE> if specified alarm occurred
	 *
	 * @throws OneWireException Device does not support A/D alarms
	 */
	@Override
	public boolean hasADAlarmed(int channel, int alarmType, byte[] state) throws OneWireException {
		throw new OneWireException("This device does not have AD alarms");
	}

	/**
	 * Extracts the A/D conversion resolution of the specified channel from the
	 * provided state buffer expressed in volts. The state is retrieved from the
	 * <CODE>readDevice()</CODE> method.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param state   current state of the device returned from
	 *                <CODE>readDevice()</CODE>
	 *
	 * @return A/D resolution of channel in volts
	 */
	@Override
	public double getADResolution(int channel, byte[] state) {
		if (channel == 0) {
			return 0.00488; // its always the same!
		}
		if (this.internalResistor) {
			// 0.625 mV units
			return 0.000625d;
		} else {
			// external resistor is used
			// 15.625 uV units
			return .000015625d;
		}
	}

	/**
	 * Extracts the A/D input voltage range of the specified channel from the
	 * provided state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param state   current state of the device returned from
	 *                <CODE>readDevice()</CODE>
	 *
	 * @return A/D input voltage range
	 */
	@Override
	public double getADRange(int channel, byte[] state) {
		return 5.0; // so is this one!
	}

	// --------
	// -------- A/D 'set' Methods
	// --------

	/**
	 * Sets the A/D alarm voltage value of the specified channel in the provided
	 * state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method. The method <CODE>writeDevice()</CODE> must
	 * be called to finalize these changes to the device. Note that multiple 'set'
	 * methods can be called before one call to <CODE>writeDevice()</CODE>.
	 *
	 * @param channel   channel in the range
	 *                  <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param alarmType desired alarm, <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param alarm     A/D alarm value (will be reduced to 8 bit resolution)
	 * @param state     current state of the device returned from
	 *                  <CODE>readDevice()</CODE>
	 *
	 * @throws OneWireException Device does not support A/D alarms
	 */
	@Override
	public void setADAlarm(int channel, int alarmType, double alarm, byte[] state) throws OneWireException {
		throw new OneWireException("This device does not have AD alarms");
	}

	/**
	 * Sets the A/D alarm enable value of the specified channel in the provided
	 * state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method. The method <CODE>writeDevice()</CODE> must
	 * be called to finalize these changes to the device. Note that multiple 'set'
	 * methods can be called before one call to <CODE>writeDevice()</CODE>.
	 *
	 * @param channel     channel in the range
	 *                    <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param alarmType   desired alarm, <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param alarmEnable A/D alarm enable value
	 * @param state       current state of the device returned from
	 *                    <CODE>readDevice()</CODE>
	 *
	 * @throws OneWireException Device does not support A/D alarms
	 */
	@Override
	public void setADAlarmEnable(int channel, int alarmType, boolean alarmEnable, byte[] state)
			throws OneWireException {
		throw new OneWireException("This device does not have AD alarms");
	}

	/**
	 * Sets the A/D conversion resolution value for the specified channel in the
	 * provided state buffer. The state buffer is retrieved from the
	 * <CODE>readDevice()</CODE> method. The method <CODE>writeDevice()</CODE> must
	 * be called to finalize these changes to the device. Note that multiple 'set'
	 * methods can be called before one call to <CODE>writeDevice()</CODE>.
	 *
	 * @param channel    channel in the range
	 *                   <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param resolution A/D resolution in volts
	 * @param state      current state of the device returned from
	 *                   <CODE>readDevice()</CODE>
	 */
	@Override
	public void setADResolution(int channel, double resolution, byte[] state) {

		// airball! no resolutions to set!
	}

	/**
	 * Sets the A/D input range for the specified channel in the provided state
	 * buffer. The state buffer is retrieved from the <CODE>readDevice()</CODE>
	 * method. The method <CODE>writeDevice()</CODE> must be called to finalize
	 * these changes to the device. Note that multiple 'set' methods can be called
	 * before one call to <CODE>writeDevice()</CODE>.
	 *
	 * @param channel channel in the range
	 *                <CODE>[0 to (getNumberChannels() - 1)]</CODE>
	 * @param range   max A/D volt range, use <CODE>getRanges()</CODE> method to get
	 *                available ranges
	 * @param state   current state of the device returned from
	 *                <CODE>readDevice()</CODE>
	 */
	@Override
	public void setADRange(int channel, double range, byte[] state) {

		// yet another airball--YAAB...only one range on this part
	}

	// --------
	// -------- Temperature Feature methods
	// --------

	/**
	 * Queries to see if this temperature measuring device has high/low trip alarms.
	 *
	 * @return <CODE>true</CODE> if has high/low temperature trip alarms
	 */
	@Override
	public boolean hasTemperatureAlarms() {
		return false;
	}

	/**
	 * Queries to see if this device has selectable temperature resolution.
	 *
	 * @return <CODE>true</CODE> if has selectable temperature resolution
	 */
	@Override
	public boolean hasSelectableTemperatureResolution() {
		return false;
	}

	/**
	 * Queries to get an array of available temperature resolutions in degrees C.
	 *
	 * @return available temperature resolutions in degrees C
	 */
	@Override
	public double[] getTemperatureResolutions() {
		var result = new double[1];

		result[0] = 0.125;

		return result;
	}

	/**
	 * Queries to get the high/low temperature alarm resolution in degrees C.
	 *
	 * @return high/low temperature alarm resolution in degrees C
	 *
	 * @throws OneWireException Device does not support temperature alarms
	 */
	@Override
	public double getTemperatureAlarmResolution() throws OneWireException {
		throw new OneWireException("This device does not have temperature alarms");
	}

	/**
	 * Queries to get the maximum temperature in degrees C.
	 *
	 * @return maximum temperature in degrees C
	 */
	@Override
	public double getMaxTemperature() {
		return 85.0;
	}

	/**
	 * Queries to get the minimum temperature in degrees C.
	 *
	 * @return minimum temperature in degrees C
	 */
	@Override
	public double getMinTemperature() {
		return -40.0;
	}

	// --------
	// -------- Temperature I/O Methods
	// --------

	/**
	 * Performs a temperature conversion.
	 *
	 * @param state device state
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public void doTemperatureConvert(byte[] state) throws OneWireIOException, OneWireException {

		// for the same reason we don't have to do an AD conversion,
		// we don't have to do a temperature conversion--its done
		// continuously
	}

	// --------
	// -------- Temperature 'get' Methods
	// --------

	/**
	 * Extracts the temperature value in degrees C from the state data retrieved
	 * from the <CODE>readDevice()</CODE> method.
	 *
	 * @param state device state
	 *
	 * @return temperature in degrees C from the last
	 *         <CODE>doTemperatureConvert()</CODE>
	 */
	@Override
	public double getTemperature(byte[] state) {
		int data;

		// the MSB is at 24, the LSB at 25 and the format is so that when
		// attached, the whole thing must be shifted right 5 (Signed)
		data = state[24] << 8 | state[25] & 0x00ff;
		data = data >> 5;

		return data / 8.0;
	}

	/**
	 * Extracts the specified temperature alarm value in degrees C from the state
	 * data retrieved from the <CODE>readDevice()</CODE> method.
	 *
	 * @param alarmType trip type <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param state     device state
	 *
	 * @return alarm trip temperature in degrees C
	 *
	 * @throws OneWireException Device does not support temperature alarms
	 */
	@Override
	public double getTemperatureAlarm(int alarmType, byte[] state) throws OneWireException {
		throw new OneWireException("This device does not have temperature alarms");
	}

	/**
	 * Extracts the current temperature resolution in degrees C from the state data
	 * retrieved from the <CODE>readDevice()</CODE> method.
	 *
	 * @param state device state
	 *
	 * @return temperature resolution in degrees C
	 */
	@Override
	public double getTemperatureResolution(byte[] state) {
		return 0.125;
	}

	// --------
	// -------- Temperature 'set' Methods
	// --------

	/**
	 * Sets the temperature alarm value in degrees C in the provided state data. Use
	 * the method <CODE>writeDevice()</CODE> with this data to finalize the change
	 * to the device.
	 *
	 * @param alarmType  trip type <CODE>ALARM_HIGH (1)
	 *               or ALARM_LOW (0)</CODE>
	 * @param alarmValue high/low temperature trip value in degrees C
	 * @param state      device state
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Device does not support temperature alarms
	 */
	@Override
	public void setTemperatureAlarm(int alarmType, double alarmValue, byte[] state)
			throws OneWireException, OneWireIOException {
		throw new OneWireException("This device does not have temperature alarms");
	}

	/**
	 * Sets the current temperature resolution in degrees C in the provided state
	 * data. Use the method <CODE>writeDevice()</CODE> with this data to finalize
	 * the change to the device.
	 *
	 * @param resolution temperature resolution in degrees C
	 * @param state      device state
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public void setTemperatureResolution(double resolution, byte[] state) throws OneWireException, OneWireIOException {

		// airball, there can be only ONE resolution!
	}

	// --------
	// -------- Sensor I/O methods
	// --------

	/**
	 * Retrieves the 1-Wire device sensor state. This state is returned as a byte
	 * array. Pass this byte array to the static query and set methods. If the
	 * device state needs to be changed then call the <CODE>writeDevice()</CODE> to
	 * finalize the one or more change. With the DS2760, there are no CRC checks on
	 * data. To ensure the integrity of this data, call twice and make sure only
	 * fields expected to change have changed. This method returns bytes 0 to 31 of
	 * the memory. Many applications will not need to worry about the lack of CRC
	 * checks.
	 *
	 * @return 1-Wire device state
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public byte[] readDevice() throws OneWireIOException, OneWireException {
		var result = new byte[32];

		/* perform the read twice to ensure a good transmission */
		this.doSpeed();
		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* do the first read */
		this.adapter.putByte(READ_DATA_COMMAND);
		this.adapter.putByte(0);
		this.adapter.getBlock(result, 0, 32);

		return result;
	}

	/**
	 * Writes the 1-Wire device sensor state that have been changed by the 'set'
	 * methods. It knows which registers have changed by looking at the bitmap
	 * fields appended to the state data.
	 *
	 * @param state device state
	 *
	 * @throws OneWireIOException Error writing data
	 * @throws OneWireException   Could not find device
	 */
	@Override
	public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {

		/*
		 * need to write the following bytes: 0 Protection register 1 Status register 7
		 * EEPROM register 8 Special feature register 16 Accumulated current register
		 * MSB 17 Accumulated current register LSB
		 */

		// drain this....let's just make everything happen in real time
	}
}
// CHECKSTYLE:ON
