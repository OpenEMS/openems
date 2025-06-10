// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 2006 Maxim Integrated Products, All Rights Reserved.
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
 * Memory bank class for the DS28EA00.
 *
 * @version 0.00, 15 October 2006
 * @author
 */
class MemoryBankScratchTemp implements MemoryBank {
	// --------
	// --------Static Final Variables
	// --------

	/**
	 * Read power supply command. This command is used to determine if external
	 * power is supplied.
	 */
	public static final byte READ_POWER_SUPPLY_COMMAND = (byte) 0xB4;

	/**
	 * Read scratchpad command
	 */
	private static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;

	/**
	 * Recall memory command
	 */
	private static final byte RECALL_MEMORY_COMMAND = (byte) 0xB8;

	/**
	 * Copy scratchpad command
	 */
	private static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x48;

	/**
	 * Starting physical address in memory bank. Needed for different types of
	 * memory in the same logical memory bank. This can be used to separate them
	 * into two virtual memory banks. Example: DS2406 status page has mixed EPROM
	 * and Volatile RAM.
	 */
	protected int startPhysicalAddress;

	/**
	 * Size of memory bank in bytes
	 */
	protected int size;

	/**
	 * Memory bank descriptions
	 */
	protected String bankDescription;

	/**
	 * Memory bank usage flags
	 */
	protected boolean generalPurposeMemory;

	/**
	 * Flag if memory bank is read/write
	 */
	protected boolean readWrite;

	/**
	 * Flag if memory bank is write once (EPROM)
	 */
	protected boolean writeOnce;

	/**
	 * Flag if memory bank is read only
	 */
	protected boolean readOnly;

	/**
	 * Flag if memory bank is non volatile (will not erase when power removed)
	 */
	protected boolean nonVolatile;

	/**
	 * Flag if memory bank needs power delivery to write
	 */
	protected boolean powerDelivery;

	// --------
	// -------- Variables
	// --------

	/**
	 * Reference to the OneWireContainer this bank resides on.
	 */
	protected OneWireContainer ib;

	/**
	 * block of 0xFF's used for faster read pre-fill of 1-Wire blocks
	 */
	protected byte[] ffBlock;

	/**
	 * Flag if read back verification is enabled in 'write()'.
	 */
	protected boolean writeVerification;

	/**
	 * Flag to indicate that speed needs to be set
	 */
	protected boolean doSetSpeed;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankScratchTemp(OneWireContainer ibutton) {
		// keep reference to ibutton where memory bank is
		this.ib = ibutton;

		// initialize attributes of this memory bank

		this.bankDescription = "Temperature";
		this.generalPurposeMemory = false;
		this.startPhysicalAddress = 0;
		this.size = 2;
		this.readWrite = false;
		this.readOnly = true;
		this.nonVolatile = false;
		this.powerDelivery = true;
		this.writeVerification = false; // not needed here but required for MemoryBank implementation

		// create the ffblock (used for faster 0xFF fills)
		this.ffBlock = new byte[15];

		for (var i = 0; i < 15; i++) {
			this.ffBlock[i] = (byte) 0xFF;
		}

		// indicate speed has not been set
		this.doSetSpeed = true;
	}

	// --------
	// -------- Memory Bank methods
	// --------

	/**
	 * Query to see get a string description of the current memory bank.
	 *
	 * @return String containing the memory bank description
	 */
	@Override
	public String getBankDescription() {
		return this.bankDescription;
	}

	/**
	 * Query to see if the current memory bank is general purpose user memory. If it
	 * is NOT then it is Memory-Mapped and writing values to this memory will affect
	 * the behavior of the 1-Wire device.
	 *
	 * @return 'true' if current memory bank is general purpose
	 */
	@Override
	public boolean isGeneralPurposeMemory() {
		return this.generalPurposeMemory;
	}

	/**
	 * Query to get the memory bank size in bytes.
	 *
	 * @return memory bank size in bytes.
	 */
	@Override
	public int getSize() {
		return this.size;
	}

	/**
	 * Query to see if current memory bank is read/write.
	 *
	 * @return 'true' if current memory bank is read/write
	 */
	@Override
	public boolean isReadWrite() {
		return this.readWrite;
	}

	/**
	 * Query to see if current memory bank is write write once such as with EPROM
	 * technology.
	 *
	 * @return 'true' if current memory bank can only be written once
	 */
	@Override
	public boolean isWriteOnce() {
		return false;
	}

	/**
	 * Query to see if current memory bank is read only.
	 *
	 * @return 'true' if current memory bank can only be read
	 */
	@Override
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Query to see if current memory bank non-volatile. Memory is non-volatile if
	 * it retains its contents even when removed from the 1-Wire network.
	 *
	 * @return 'true' if current memory bank non volatile.
	 */
	@Override
	public boolean isNonVolatile() {
		return this.nonVolatile;
	}

	/**
	 * Query to see if current memory bank pages need the adapter to have a
	 * 'ProgramPulse' in order to write to the memory.
	 *
	 * @return 'true' if writing to the current memory bank pages requires a
	 *         'ProgramPulse'.
	 */
	@Override
	public boolean needsProgramPulse() {
		return false;
	}

	/**
	 * Query to see if current memory bank pages need the adapter to have a
	 * 'PowerDelivery' feature in order to write to the memory.
	 *
	 * @return 'true' if writing to the current memory bank pages requires
	 *         'PowerDelivery'.
	 */
	@Override
	public boolean needsPowerDelivery() {
		return this.powerDelivery;
	}

	/**
	 * Query to get the starting physical address of this bank. Physical banks are
	 * sometimes sub-divided into logical banks due to changes in attributes.
	 *
	 * @return physical starting address of this logical bank.
	 */
	@Override
	public int getStartPhysicalAddress() {
		return this.startPhysicalAddress;
	}

	/**
	 * Set the write verification for the 'write()' method.
	 *
	 * @param doReadVerf true (default) verify write in 'write' false, don't verify
	 *                   write (used on Write-Once bit manipulation)
	 */
	@Override
	public void setWriteVerification(boolean doReadVerf) {
		this.writeVerification = doReadVerf;
	}

	// --------
	// -------- I/O methods
	// --------

	/**
	 * Read memory in the current bank with no CRC checking (device or data). The
	 * resulting data from this API may or may not be what is on the 1-Wire device.
	 * It is recommends that the data contain some kind of checking (CRC) like in
	 * the readPagePacket() method or have the 1-Wire device provide the CRC as in
	 * readPageCRC(). readPageCRC() however is not supported on all memory types,
	 * see 'hasPageAutoCRC()'. If neither is an option then this method could be
	 * called more then once to at least verify that the same thing is read
	 * consistently.
	 *
	 * @param startAddr    starting physical address
	 * @param readContinue ignored by method
	 * @param readBuf      byte array to place read data into
	 * @param offset       offset into readBuf to place data
	 * @param len          length in bytes to read
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void read(int startAddr, boolean readContinue, byte[] readBuf, int offset, int len)
			throws OneWireIOException, OneWireException {
		byte[] temp_buf;

		// check for valid address
		if (startAddr < 0 || startAddr + len > this.size) {
			throw new OneWireException("Read exceeds memory bank");
		}

		// check for zero length read (saves time)
		if (len == 0) {
			return;
		}

		// attempt to put device at max desired speed
		this.checkSpeed();

		// translate the address into a page_offset and offset
		var page_offset = startAddr + this.startPhysicalAddress;
		var data_len = 8 - page_offset;
		if (data_len > len) {
			data_len = len;
		}

		// read scratchpad
		temp_buf = this.readScratchpad();

		// copy contents to the readBuf
		System.arraycopy(temp_buf, page_offset, readBuf, offset, data_len);
	}

	/**
	 * Write memory in the current bank. It is recommended that when writing data
	 * that some structure in the data is created to provide error free reading back
	 * with read(). Or the method 'writePagePacket()' could be used which
	 * automatically wraps the data in a length and CRC.
	 *
	 * When using on Write-Once devices care must be taken to write into into empty
	 * space. If write() is used to write over an unlocked page on a Write-Once
	 * device it will fail. If write verification is turned off with the method
	 * 'setWriteVerification(false)' then the result will be an 'AND' of the
	 * existing data and the new data.
	 *
	 * @param startAddr starting address
	 * @param writeBuf  byte array containing data to write
	 * @param offset    offset into writeBuf to start writing data
	 * @param len       length in bytes to write
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void write(int startAddr, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
		byte[] temp_buf;

		// startAddr = starting address of the memory bank
		// writeBuf = array of bytes or unknown size given to method
		// offset = offset into writeBuf to start writing from
		// len = how many bytes to write

		// return if nothing to do
		if (len == 0) {
			return;
		}

		// check if power delivery is available
		if (!this.ib.adapter.canDeliverPower()) {
			throw new OneWireException("Power delivery required but not available");
		}

		// attempt to put device at max desired speed
		this.checkSpeed();

		// since writing is a bit odd in that the startPhysicalAddress
		// is off by 2 due to being able to read 9 bytes but only writing 3,
		// let's correct...

		// translate the address into a page offset
		var page_offset = startAddr + this.startPhysicalAddress;
		var page_buf = new byte[3];

		// pre-fill buff with current page contents
		temp_buf = this.readRecallScratchpad();

		// we have temp_buf, so now write the data we want changed to it.
		System.arraycopy(writeBuf, offset, temp_buf, page_offset, len);

		// since only 3 bytes can be written to scratchpad (TH/TL/Config) then
		// prefill this array with the correct three bytes
		System.arraycopy(temp_buf, 2, page_buf, 0, 3);

		// write the page
		this.writeScratchpad(page_buf);
	}

	// --------
	// -------- Bank specific methods
	// --------

	/**
	 * Reads the 8 byte scratchpad and returns the data in an array.
	 *
	 * @param none
	 *
	 * @return eight byte array that make up the scratchpad
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	protected byte[] readScratchpad() throws OneWireIOException, OneWireException {
		var send_block = new byte[10];
		var result_block = new byte[8];
		int crc8; // this device uses a crc 8

		if (this.ib.adapter.select(this.ib.address)) {
			/* recall memory to the scratchpad */
			// ib.adapter.putByte(RECALL_MEMORY_COMMAND);

			/* perform the read scratchpad */
			// ib.adapter.select(ib.address);

			/* read scratchpad command */
			send_block[0] = READ_SCRATCHPAD_COMMAND;

			/* now add the read bytes for data bytes and crc8 */
			for (var i = 1; i < 10; i++) {
				send_block[i] = (byte) 0xFF;
			}

			/* send the block */
			this.ib.adapter.dataBlock(send_block, 0, send_block.length);

			/*
			 * Now, send_block contains the 8-byte Scratchpad plus READ_SCRATCHPAD_COMMAND
			 * byte, and CRC8. So, convert the block to a 8-byte array representing
			 * Scratchpad (get rid of first byte and CRC8)
			 */

			// see if CRC8 is correct
			crc8 = CRC8.compute(send_block, 1, 9);
			if (crc8 != 0) {
				throw new OneWireIOException("Bad CRC during page read " + crc8);
			}

			// copy the data into the result
			System.arraycopy(send_block, 1, result_block, 0, 8);

			return result_block;
		}

		// device must not have been present
		throw new OneWireIOException("Device not found during scratchpad read");
	}

	/**
	 * Reads the 8 byte scratchpad with Esquared recalled and returns the data in an
	 * array.
	 *
	 * @param none
	 *
	 * @return eight byte array that make up the scratchpad
	 *
	 * @throws OneWireIOException Error reading data
	 * @throws OneWireException   Could not find part
	 */
	protected byte[] readRecallScratchpad() throws OneWireIOException, OneWireException {
		byte[] recallData;
		if (this.ib.adapter.select(this.ib.address)) {
			// recall memory to the scratchpad
			this.ib.adapter.putByte(RECALL_MEMORY_COMMAND);
			// read scratchpad
			recallData = this.readScratchpad();
			return recallData;
		}

		// device must not have been present
		throw new OneWireIOException("Device not found during E-squared recall scratchpad read");
	}

	/**
	 * Writes to the Scratchpad of the DS28EA00 and similar devices.
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
		/*
		 * OneWireContainer42 ib42;
		 *
		 * ib42 = (OneWireContainer42) ib; ib42.writeScratchpad(data);
		 */

		// setup buffer to write to scratchpad
		var writeBuffer = new byte[4];
		// byte[] writeBuffer = new byte [6];
		byte[] readBuffer;
		// boolean vddSupplied = false;

		/*
		 * writeBuffer [0] = 0x4E; writeBuffer [1] = 0x00; writeBuffer [2] = 0x00;
		 * writeBuffer [3] = data [0]; writeBuffer [4] = data [1]; writeBuffer [5] =
		 * data [2];
		 */
		writeBuffer[0] = 0x4E;
		writeBuffer[1] = data[0];
		writeBuffer[2] = data[1];
		writeBuffer[3] = data[2];

		// send command block to device
		if (!this.ib.adapter.select(this.ib.getAddressAsString())) {

			// device must not have been present
			throw new OneWireIOException("Device not found on 1-Wire Network");
		}
		this.ib.adapter.dataBlock(writeBuffer, 0, writeBuffer.length);

		// double check by reading scratchpad without recallEsquared
		readBuffer = this.readScratchpad();

		if (readBuffer[2] != data[0] || readBuffer[3] != data[1] || readBuffer[4] != data[2]) {

			// writing to scratchpad failed
			throw new OneWireIOException("Error writing to scratchpad " + data[0] + " " + readBuffer[2] + " " + data[1]
					+ " " + readBuffer[3] + " " + data[2] + " " + readBuffer[4] + " ");
		}

		// second, let's copy the scratchpad.
		if (!this.ib.adapter.select(this.ib.getAddressAsString())) {
			// device must not have been present
			throw new OneWireIOException("Device not found on 1-Wire Network");
		}
		// apply the power delivery
		this.ib.adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
		this.ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);

		// send the copy scratchpad command
		this.ib.adapter.putByte(COPY_SCRATCHPAD_COMMAND);

		// sleep for 10 milliseconds to allow copy to take place.
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}

		// Turn power back to normal.
		this.ib.adapter.setPowerNormal();

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
		var intresult = 0;
		var result = false;

		// select the device
		if (!this.ib.adapter.select(this.ib.getAddress())) {

			// device must not have been present
			throw new OneWireIOException("Device not found on 1-Wire Network");
		}
		// send the "Read Power Supply" memory command
		this.ib.adapter.putByte(READ_POWER_SUPPLY_COMMAND);

		// read results
		intresult = this.ib.adapter.getByte();
		if (intresult != 0x00) {
			result = true; // reads 0xFF for true and 0x00 for false
		}

		return result;
	}

	// --------
	// -------- checkSpeed methods
	// --------

	/**
	 * Check the device speed if has not been done before or if an error was
	 * detected.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public void checkSpeed() throws OneWireIOException, OneWireException {
		synchronized (this) {

			// only check the speed
			if (this.doSetSpeed) {

				// attempt to set the correct speed and verify device present
				this.ib.doSpeed();

				// no exceptions so clear flag
				this.doSetSpeed = false;
			}
		}
	}

	/**
	 * Set the flag to indicate the next 'checkSpeed()' will force a speed set and
	 * verify 'doSpeed()'.
	 */
	public void forceVerify() {
		synchronized (this) {
			this.doSetSpeed = true;
		}
	}

}
// CHECKSTYLE:ON
