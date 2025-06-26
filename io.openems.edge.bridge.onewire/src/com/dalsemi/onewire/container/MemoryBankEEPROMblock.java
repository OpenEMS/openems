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
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.utils.CRC16;

/**
 * Memory bank class for the EEPROM section of iButtons and 1-Wire devices on
 * the DS2408.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankEEPROMblock implements OTPMemoryBank {
	/**
	 * Memory functions.
	 */
	private static final byte WRITE_DATA_COMMAND = (byte) 0x6C;
	private static final byte READ_DATA_COMMAND = (byte) 0x69;
	private static final byte COPY_DATA_COMMAND = (byte) 0x48;
	private static final byte RECALL_DATA_COMMAND = (byte) 0xB8;

	// --------
	// -------- Variables
	// --------

	/**
	 * Reference to the OneWireContainer this bank resides on.
	 */
	protected OneWireContainer30 ib;

	/**
	 * block of 0xFF's used for faster read pre-fill of 1-Wire blocks
	 */
	protected byte[] ffBlock = new byte[32];

	/**
	 * Flag to indicate that speed needs to be set
	 */
	protected boolean doSetSpeed;

	// --------
	// -------- Protected Variables for MemoryBank implementation
	// --------

	/**
	 * Size of memory bank in bytes
	 */
	protected int size;

	/**
	 * Starting physical address in memory bank. Needed for different types of
	 * memory in the same logical memory bank. This can be used to separate them
	 * into two virtual memory banks. Example: DS2406 status page has mixed EPROM
	 * and Volatile RAM.
	 */
	protected int startPhysicalAddress;

	/**
	 * Flag if read back verification is enabled in 'write()'.
	 */
	protected boolean writeVerification;

	// --------
	// -------- Protected Variables for PagedMemoryBank implementation
	// --------

	/**
	 * Number of pages in memory bank
	 */
	protected int numberPages;

	/**
	 * page length in memory bank
	 */
	protected int pageLength;

	/**
	 * Max data length in page packet in memory bank
	 */
	protected int maxPacketDataLength;

	// --------
	// -------- Protected Variables for OTPMemoryBank implementation
	// --------

	/**
	 * Flag if memory bank page 1 is locked.
	 */
	protected boolean lockPage0;

	/**
	 * Flag if memory bank page 2 is locked.
	 */
	protected boolean lockPage1;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on. Requires reference to memory banks used in OTP
	 * operations.
	 */
	public MemoryBankEEPROMblock(OneWireContainer30 ibutton) {

		// keep reference to ibutton where memory bank is
		this.ib = ibutton;

		// initialize attributes of this memory bank - DEFAULT: Main memory DS1985 w/o
		// lock stuff
		this.numberPages = 2;
		this.size = 32;
		this.pageLength = 16;
		this.maxPacketDataLength = 13;

		try {
			this.lockPage0 = this.ib.getFlag(OneWireContainer30.EEPROM_REGISTER,
					OneWireContainer30.EEPROM_BLOCK_0_LOCK_FLAG);
			this.lockPage1 = this.ib.getFlag(OneWireContainer30.EEPROM_REGISTER,
					OneWireContainer30.EEPROM_BLOCK_1_LOCK_FLAG);
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}

		this.writeVerification = false;
		this.startPhysicalAddress = 32;
		this.doSetSpeed = true;

		// create the ffblock (used for faster 0xFF fills)
		for (var i = 0; i < 32; i++) {
			this.ffBlock[i] = (byte) 0xFF;
		}
	}

	// --------
	// -------- MemoryBank query methods
	// --------

	/**
	 * Query to see get a string description of the current memory bank.
	 *
	 * @return String containing the memory bank description
	 */
	@Override
	public String getBankDescription() {
		return "EEPROM memory for DS2760";
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
		return true;
	}

	/**
	 * Query to see if current memory bank is read/write.
	 *
	 * @return 'true' if current memory bank is read/write
	 */
	@Override
	public boolean isReadWrite() {
		if (this.lockPage0 && this.lockPage1) {
			return false;
		}
		return true;
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
		if (this.lockPage0 && this.lockPage1) {
			return true;
		}
		return false;
	}

	/**
	 * Query to see if current memory bank non-volatile. Memory is non-volatile if
	 * it retains its contents even when removed from the 1-Wire network.
	 *
	 * @return 'true' if current memory bank non volatile.
	 */
	@Override
	public boolean isNonVolatile() {
		return true;
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
		return false;
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
	 * Query to get the memory bank size in bytes.
	 *
	 * @return memory bank size in bytes.
	 */
	@Override
	public int getSize() {
		return this.size;
	}

	// --------
	// -------- PagedMemoryBank query methods
	// --------

	/**
	 * Query to get the number of pages in current memory bank.
	 *
	 * @return number of pages in current memory bank
	 */
	@Override
	public int getNumberPages() {
		return this.numberPages;
	}

	/**
	 * Query to get page length in bytes in current memory bank.
	 *
	 * @return page length in bytes in current memory bank
	 */
	@Override
	public int getPageLength() {
		return this.pageLength;
	}

	/**
	 * Query to get Maximum data page length in bytes for a packet read or written
	 * in the current memory bank. See the 'ReadPagePacket()' and
	 * 'WritePagePacket()' methods. This method is only useful if the current memory
	 * bank is general purpose memory.
	 *
	 * @return max packet page length in bytes in current memory bank
	 */
	@Override
	public int getMaxPacketDataLength() {
		return this.maxPacketDataLength;
	}

	/**
	 * Query to see if current memory bank pages can be read with the contents being
	 * verified by a device generated CRC. This is used to see if the
	 * 'ReadPageCRC()' can be used.
	 *
	 * @return 'true' if current memory bank can be read with self generated CRC.
	 */
	@Override
	public boolean hasPageAutoCRC() {
		return false;
	}

	/**
	 * Checks to see if this memory bank's pages deliver extra information outside
	 * of the normal data space, when read. Examples of this may be a redirection
	 * byte, counter, tamper protection bytes, or SHA-1 result. If this method
	 * returns true then the methods with an 'extraInfo' parameter can be used:
	 * {@link #readPage(int,boolean,byte[],int,byte[]) readPage},
	 * {@link #readPageCRC(int,boolean,byte[],int,byte[]) readPageCRC}, and
	 * {@link #readPagePacket(int,boolean,byte[],int,byte[]) readPagePacket}.
	 *
	 * @return <CODE> true </CODE> if reading the this memory bank's pages provides
	 *         extra information
	 *
	 * @see #readPage(int,boolean,byte[],int,byte[]) readPage(extra)
	 * @see #readPageCRC(int,boolean,byte[],int,byte[]) readPageCRC(extra)
	 * @see #readPagePacket(int,boolean,byte[],int,byte[]) readPagePacket(extra)
	 * @since 1-Wire API 0.01
	 */
	@Override
	public boolean hasExtraInfo() {
		return false;
	}

	/**
	 * Query to get the length in bytes of extra information that is read when read
	 * a page in the current memory bank. See 'hasExtraInfo()'.
	 *
	 * @return number of bytes in Extra Information read when reading pages in the
	 *         current memory bank.
	 */
	@Override
	public int getExtraInfoLength() {
		return 0;
	}

	/**
	 * Query to get a string description of what is contained in the Extra
	 * Informationed return when reading pages in the current memory bank. See
	 * 'hasExtraInfo()'.
	 *
	 * @return string describing extra information.
	 */
	@Override
	public String getExtraInfoDescription() {
		return null;
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
	// -------- OTPMemoryBank query methods
	// --------

	/**
	 * Query to see if current memory bank pages can be redirected to another pages.
	 * This is mostly used in Write-Once memory to provide a means to update.
	 *
	 * @return 'true' if current memory bank pages can be redirected to a new page.
	 */
	@Override
	public boolean canRedirectPage() {
		return false;
	}

	/**
	 * Query to see if current memory bank pages can be locked. A locked page would
	 * prevent any changes to the memory.
	 *
	 * @return 'true' if current memory bank pages can be redirected to a new page.
	 */
	@Override
	public boolean canLockPage() {
		return true;
	}

	/**
	 * Query to see if current memory bank pages can be locked from being
	 * redirected. This would prevent a Write-Once memory from being updated.
	 *
	 * @return 'true' if current memory bank pages can be locked from being
	 *         redirected to a new page.
	 */
	@Override
	public boolean canLockRedirectPage() {
		return false;
	}

	// --------
	// -------- MemoryBank I/O methods
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
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new read()
	 *                     continuous where the last one led off and it is inside a
	 *                     'beginExclusive/endExclusive' block.
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
		int i;
		var buffer = new byte[18];

		if (startAddr + len > 32) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// calculate the address (32 and 48 are valid addresses)
		byte memAddr;
		if (startAddr < 16) {
			memAddr = (byte) this.startPhysicalAddress;
		} else {
			memAddr = (byte) (this.startPhysicalAddress + 16);
		}

		/* perform the recall/read and verification */
		this.ib.doSpeed();
		this.ib.adapter.reset();

		if (!this.ib.adapter.select(this.ib.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* first recall the memory to shadow ram */
		buffer[0] = RECALL_DATA_COMMAND;
		buffer[1] = memAddr;

		this.ib.adapter.dataBlock(buffer, 0, 2);

		/* now read the shadow ram */
		this.ib.adapter.reset();
		this.ib.adapter.select(this.ib.address);

		buffer[0] = READ_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		System.arraycopy(this.ffBlock, 0, buffer, 2, 16);

		this.ib.adapter.dataBlock(buffer, 0, 18);

		// user can re-read for verification
		if (startAddr < 16 && startAddr + len < 16) {
			for (i = startAddr; i < startAddr + len; i++) {
				readBuf[offset + i - startAddr] = buffer[i + 2];
			}
		} else if (startAddr >= 16) {
			for (i = startAddr; i < startAddr + len; i++) {
				readBuf[offset + i - startAddr] = buffer[i - startAddr + 2 + startAddr - 16];
			}
		} else {
			for (i = startAddr; i < 16; i++) {
				readBuf[offset + i - startAddr] = buffer[i + 2];
			}

			this.ib.adapter.reset();
			this.ib.adapter.select(this.ib.address);

			buffer[0] = RECALL_DATA_COMMAND;
			buffer[1] = (byte) (memAddr + 16);

			this.ib.adapter.dataBlock(buffer, 0, 2);

			/* now read the shadow ram */
			this.ib.adapter.reset();
			this.ib.adapter.select(this.ib.address);

			buffer[0] = READ_DATA_COMMAND;

			// buffer[1] should still hold memAddr
			System.arraycopy(this.ffBlock, 0, buffer, 2, 16);

			this.ib.adapter.dataBlock(buffer, 0, 18);

			// user can re-read for verification
			for (i = 16; i < startAddr + len; i++) {
				readBuf[i + offset - startAddr] = buffer[i - 14];
			}
		}
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
	 * @param offset    offset into writeBuf to get data
	 * @param len       length in bytes to write
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void write(int startAddr, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
		var buffer = new byte[18];
		var memory = new byte[32];
		int modify;
		int i;

		if (startAddr + len > 32) {
			throw new OneWireException("Write exceeds memory bank end");
		}

		// the first block is at address 32 and the second is at address 48
		byte memAddr;
		if (startAddr < 16) {
			memAddr = (byte) this.startPhysicalAddress;
		} else {
			memAddr = (byte) (this.startPhysicalAddress + 16);
		}

		// if the EEPROM block is locked throw a OneWireIOException
		if (this.lockPage0 && memAddr == 32 || this.lockPage1 && memAddr == 48) {
			throw new OneWireIOException("OneWireContainer30-Cant write data to locked EEPROM block.");
		}

		// read memory that is already there
		this.read(0, false, memory, 0, 32);

		System.arraycopy(writeBuf, offset, memory, startAddr, len);

		/* perform the write/verification and copy */
		this.ib.doSpeed();
		this.ib.adapter.reset();

		if (!this.ib.adapter.select(this.ib.address)) {
			throw new OneWireException("OneWireContainer30-Device not found.");
		}
		/* first write to shadow rom */
		buffer[0] = WRITE_DATA_COMMAND;
		buffer[1] = memAddr;

		if (memAddr == 32) {
			System.arraycopy(memory, 0, buffer, 2, 16);
			modify = 0;
		} else {
			System.arraycopy(memory, 16, buffer, 2, 16);
			modify = 16;
		}

		this.ib.adapter.dataBlock(buffer, 0, 18);

		/* read the shadow ram back for verification */
		this.ib.adapter.reset();
		this.ib.adapter.select(this.ib.address);

		buffer[0] = READ_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		System.arraycopy(this.ffBlock, 0, buffer, 2, 16);

		this.ib.adapter.dataBlock(buffer, 0, 18);

		// verify data
		for (i = 0; i < 16; i++) {
			if (buffer[i + 2] != memory[i + modify]) {
				throw new OneWireIOException("Error writing EEPROM memory bank");
			}
		}

		/* now perform the copy to EEPROM */
		this.ib.adapter.reset();
		this.ib.adapter.select(this.ib.address);

		buffer[0] = COPY_DATA_COMMAND;

		// buffer[1] should still hold memAddr
		this.ib.adapter.dataBlock(buffer, 0, 2);

		if (startAddr < 16 && startAddr + len >= 16) {
			memAddr = 48;
			this.ib.adapter.reset();

			if (this.ib.adapter.select(this.ib.address)) {
				/* first write to shadow rom */
				buffer[0] = WRITE_DATA_COMMAND;
				buffer[1] = memAddr;

				System.arraycopy(memory, 16, buffer, 2, 16);

				this.ib.adapter.dataBlock(buffer, 0, 18);

				/* read the shadow ram back for verification */
				this.ib.adapter.reset();
				this.ib.adapter.select(this.ib.address);

				buffer[0] = READ_DATA_COMMAND;

				// buffer[1] should still hold memAddr
				System.arraycopy(this.ffBlock, 0, buffer, 2, 16);

				this.ib.adapter.dataBlock(buffer, 0, 18);

				// verify data
				for (i = 0; i < 16; i++) {
					if (buffer[i + 2] != memory[i + 16]) {
						throw new OneWireIOException("Error writing EEPROM memory bank");
					}
				}

				/* now perform the copy to EEPROM */
				this.ib.adapter.reset();
				this.ib.adapter.select(this.ib.address);

				buffer[0] = COPY_DATA_COMMAND;

				this.ib.adapter.dataBlock(buffer, 0, 2);
			}
		}
	}

	// --------
	// -------- PagedMemoryBank I/O methods
	// --------

	/**
	 * Read page in the current bank with no CRC checking (device or data). The
	 * resulting data from this API may or may not be what is on the 1-Wire device.
	 * It is recommends that the data contain some kind of checking (CRC) like in
	 * the readPagePacket() method or have the 1-Wire device provide the CRC as in
	 * readPageCRC(). readPageCRC() however is not supported on all memory types,
	 * see 'hasPageAutoCRC()'. If neither is an option then this method could be
	 * called more then once to at least verify that the same thing is read
	 * consistently.
	 *
	 * @param page         page number to read packet from
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new readPage()
	 *                     continuous where the last one led off and it is inside a
	 *                     'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to place read data into
	 * @param offset       offset into readBuf to place data
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void readPage(int page, boolean readContinue, byte[] readBuf, int offset)
			throws OneWireIOException, OneWireException {
		this.read(page * this.pageLength, readContinue, readBuf, offset, this.pageLength);
	}

	/**
	 * Read page with extra information in the current bank with no CRC checking
	 * (device or data). The resulting data from this API may or may not be what is
	 * on the 1-Wire device. It is recommends that the data contain some kind of
	 * checking (CRC) like in the readPagePacket() method or have the 1-Wire device
	 * provide the CRC as in readPageCRC(). readPageCRC() however is not supported
	 * on all memory types, see 'hasPageAutoCRC()'. If neither is an option then
	 * this method could be called more then once to at least verify that the same
	 * thing is read consistently. See the method 'hasExtraInfo()' for a description
	 * of the optional extra information some devices have.
	 *
	 * @param page         page number to read packet from
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new readPage()
	 *                     continuous where the last one led off and it is inside a
	 *                     'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to place read data into
	 * @param offset       offset into readBuf to place data
	 * @param extraInfo    byte array to put extra info read into
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void readPage(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		throw new OneWireException("Read extra information not supported on this memory bank");
	}

	/**
	 * Read a Universal Data Packet and extra information. See the method
	 * 'readPagePacket()' for a description of the packet structure. See the method
	 * 'hasExtraInfo()' for a description of the optional extra information some
	 * devices have.
	 *
	 * @param page         page number to read packet from
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new
	 *                     readPagePacket() continuous where the last one stopped
	 *                     and it is inside a 'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to put data read. Must have at least
	 *                     'getMaxPacketDataLength()' elements.
	 * @param offset       offset into readBuf to place data
	 * @param extraInfo    byte array to put extra info read into
	 *
	 * @return number of data bytes written to readBuf at the offset.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		throw new OneWireException("Read extra information not supported on this memory bank");
	}

	/**
	 * Read a Universal Data Packet and extra information. See the method
	 * 'readPagePacket()' for a description of the packet structure. See the method
	 * 'hasExtraInfo()' for a description of the optional extra information some
	 * devices have.
	 *
	 * @param page         page number to read packet from
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new
	 *                     readPagePacket() continuous where the last one stopped
	 *                     and it is inside a 'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to put data read. Must have at least
	 *                     'getMaxPacketDataLength()' elements.
	 * @param offset       offset into readBuf to place data
	 *
	 * @return number of data bytes written to readBuf at the offset.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset)
			throws OneWireIOException, OneWireException {
		var raw_buf = new byte[this.pageLength];

		// read entire page with read page CRC
		this.read(page * this.pageLength, readContinue, raw_buf, 0, this.pageLength);

		// check if length is realistic
		if ((raw_buf[0] & 0x00FF) > this.maxPacketDataLength) {
			this.forceVerify();

			throw new OneWireIOException("Invalid length in packet");
		}

		// verify the CRC is correct
		if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {

			// extract the data out of the packet
			System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);

			// return the length
			return raw_buf[0];
		}
		this.forceVerify();

		throw new OneWireIOException("Invalid CRC16 in packet read");
	}

	/**
	 * Write a Universal Data Packet. See the method 'readPagePacket()' for a
	 * description of the packet structure.
	 *
	 * @param page     page number to write packet to
	 * @param writeBuf data byte array to write
	 * @param offset   offset into writeBuf where data to write is
	 * @param len      number of bytes to write
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void writePagePacket(int page, byte[] writeBuf, int offset, int len)
			throws OneWireIOException, OneWireException {

		// make sure length does not exceed max
		if (len > this.maxPacketDataLength) {
			throw new OneWireIOException("Length of packet requested exceeds page size");
		}

		// construct the packet to write
		var raw_buf = new byte[len + 3];

		raw_buf[0] = (byte) len;

		System.arraycopy(writeBuf, offset, raw_buf, 1, len);

		var crc = CRC16.compute(raw_buf, 0, len + 1, page);

		raw_buf[len + 1] = (byte) (~crc & 0xFF);
		raw_buf[len + 2] = (byte) ((~crc & 0xFFFF) >>> 8 & 0xFF);

		// write the packet, return result
		this.write(page * this.pageLength, raw_buf, 0, len + 3);
	}

	/**
	 * Read a complete memory page with CRC verification provided by the device. Not
	 * supported by all devices. See the method 'hasPageAutoCRC()'.
	 *
	 * @param page         page number to read
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new
	 *                     readPagePacket() continuous where the last one stopped
	 *                     and it is inside a 'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to put data read. Must have at least
	 *                     'getMaxPacketDataLength()' elements.
	 * @param offset       offset into readBuf to place data
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset)
			throws OneWireIOException, OneWireException {
		throw new OneWireException("Read page with CRC not supported in this memory bank");
	}

	/**
	 * Read a complete memory page with CRC verification provided by the device with
	 * extra information. Not supported by all devices. See the method
	 * 'hasPageAutoCRC()'. See the method 'hasExtraInfo()' for a description of the
	 * optional extra information.
	 *
	 * @param page         page number to read
	 * @param readContinue if 'true' then device read is continued without
	 *                     re-selecting. This can only be used if the new
	 *                     readPagePacket() continuous where the last one stopped
	 *                     and it is inside a 'beginExclusive/endExclusive' block.
	 * @param readBuf      byte array to put data read. Must have at least
	 *                     'getMaxPacketDataLength()' elements.
	 * @param offset       offset into readBuf to place data
	 * @param extraInfo    byte array to put extra info read into
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		throw new OneWireException("Read page with CRC not supported in this memory bank");
	}

	// --------
	// -------- OTPMemoryBank I/O methods
	// --------

	/**
	 * Lock the specified page in the current memory bank. Not supported by all
	 * devices. See the method 'canLockPage()'.
	 *
	 * @param page number of page to lock
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void lockPage(int page) throws OneWireIOException, OneWireException {
		if (page > 1) {
			throw new OneWireException("Page does not exist to lock");
		}

		this.ib.setFlag(OneWireContainer30.EEPROM_REGISTER, OneWireContainer30.EEPROM_LOCK_ENABLE_FLAG, true);

		if (page == 0) {
			this.ib.setFlag(OneWireContainer30.EEPROM_REGISTER, OneWireContainer30.EEPROM_BLOCK_0_LOCK_FLAG, true);
		} else if (page == 1) {
			this.ib.setFlag(OneWireContainer30.EEPROM_REGISTER, OneWireContainer30.EEPROM_BLOCK_1_LOCK_FLAG, true);
		}

		// read back to verify
		if (!this.isPageLocked(page)) {
			this.forceVerify();

			throw new OneWireIOException("Read back from write incorrect, could not lock page");
		}
		if (page == 0) {
			this.lockPage0 = true;
		} else if (page == 1) {
			this.lockPage1 = true;
		}
	}

	/**
	 * Query to see if the specified page is locked. See the method 'canLockPage()'.
	 *
	 * @param page number of page to see if locked
	 *
	 * @return 'true' if page locked.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public boolean isPageLocked(int page) throws OneWireIOException, OneWireException {
		var flag = false;

		if (page > 1) {
			throw new OneWireException("Page does not exist to be locked");
		}

		if (page == 0) {
			flag = this.ib.getFlag(OneWireContainer30.EEPROM_REGISTER, OneWireContainer30.EEPROM_BLOCK_0_LOCK_FLAG);
		} else if (page == 1) {
			flag = this.ib.getFlag(OneWireContainer30.EEPROM_REGISTER, OneWireContainer30.EEPROM_BLOCK_1_LOCK_FLAG);
		}

		return flag;

	}

	/**
	 * Redirect the specified page in the current memory bank to a new page. Not
	 * supported by all devices. See the method 'canRedirectPage()'.
	 *
	 * @param page    number of page to redirect
	 * @param newPage new page number to redirect to
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void redirectPage(int page, int newPage) throws OneWireIOException, OneWireException {
		throw new OneWireException("This memory bank does not support redirection.");
	}

	/**
	 * Gets the page redirection of the specified page. Not supported by all
	 * devices.
	 *
	 * @param page page to check for redirection
	 *
	 * @return the new page number or 0 if not redirected
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no device
	 *                            present or a CRC read from the device is
	 *                            incorrect. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter.
	 *
	 * @see #canRedirectPage() canRedirectPage
	 * @see #redirectPage(int,int) redirectPage
	 * @since 1-Wire API 0.01
	 */
	@Override
	public int getRedirectedPage(int page) throws OneWireIOException, OneWireException {
		throw new OneWireException("This memory bank does not support redirection.");
	}

	/**
	 * Lock the redirection option for the specified page in the current memory
	 * bank. Not supported by all devices. See the method 'canLockRedirectPage()'.
	 *
	 * @param page number of page to redirect
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void lockRedirectPage(int page) throws OneWireIOException, OneWireException {
		throw new OneWireException("This memory bank does not support redirection.");
	}

	/**
	 * Query to see if the specified page has redirection locked. Not supported by
	 * all devices. See the method 'canRedirectPage()'.
	 *
	 * @param page number of page check for locked redirection
	 *
	 * @return return 'true' if redirection is locked for this page
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public boolean isRedirectPageLocked(int page) throws OneWireIOException, OneWireException {
		throw new OneWireException("This memory bank does not support redirection.");
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
