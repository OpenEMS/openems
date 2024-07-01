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
import com.dalsemi.onewire.utils.CRC16;
import com.dalsemi.onewire.utils.CRC8;

/**
 * Memory bank class for the EPROM section of iButtons and 1-Wire devices.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankEPROM implements OTPMemoryBank {

	// --------
	// -------- Static Final Variables
	// --------

	/**
	 * Read Memory Command
	 */
	public static final byte READ_MEMORY_COMMAND = (byte) 0xF0;

	/**
	 * Main memory read command
	 */
	public static final byte MAIN_READ_PAGE_COMMAND = (byte) 0xA5;

	/**
	 * Status memory read command
	 */
	public static final byte STATUS_READ_PAGE_COMMAND = (byte) 0xAA;

	/**
	 * Main memory write command
	 */
	public static final byte MAIN_WRITE_COMMAND = (byte) 0x0F;

	/**
	 * Status memory write command
	 */
	public static final byte STATUS_WRITE_COMMAND = (byte) 0x55;

	// --------
	// -------- Variables
	// --------

	/**
	 * Reference to the OneWireContainer this bank resides on.
	 */
	protected OneWireContainer ib;

	/**
	 * Read page with CRC command
	 */
	protected byte READ_PAGE_WITH_CRC;

	/**
	 * Number of CRC bytes (1-2)
	 */
	protected int numCRCBytes;

	/**
	 * Get crc after sending command,address
	 */
	protected boolean crcAfterAddress;

	/**
	 * Get crc during a normal read
	 */
	protected boolean normalReadCRC;

	/**
	 * Program Memory Command
	 */
	protected byte WRITE_MEMORY_COMMAND;

	/**
	 * block of 0xFF's used for faster read pre-fill of 1-Wire blocks
	 */
	protected byte[] ffBlock;

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
	 * Flag if memory bank needs program Pulse to write
	 */
	protected boolean programPulse;

	/**
	 * Flag if memory bank needs power delivery to write
	 */
	protected boolean powerDelivery;

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

	/**
	 * Flag if memory bank has page auto-CRC generation
	 */
	protected boolean pageAutoCRC;

	/**
	 * Flag if reading a page in memory bank provides optional extra information
	 * (counter, tamper protection, SHA-1...)
	 */
	protected boolean extraInfo;

	/**
	 * Length of extra information when reading a page in memory bank
	 */
	protected int extraInfoLength;

	/**
	 * Extra information description when reading page in memory bank
	 */
	protected String extraInfoDescription;

	// --------
	// -------- Protected Variables for OTPMemoryBank implementation
	// --------

	/**
	 * Flag if memory bank can have pages redirected
	 */
	protected boolean redirectPage;

	/**
	 * Flag if memory bank can have pages locked
	 */
	protected boolean lockPage;

	/**
	 * Flag if memory bank can have pages locked from redirected
	 */
	protected boolean lockRedirectPage;

	/**
	 * Memory bank to lock pages in 'this' memory bank
	 */
	protected PagedMemoryBank mbLock;

	/**
	 * Memory bank to redirect pages in 'this' memory bank
	 */
	protected PagedMemoryBank mbRedirect;

	/**
	 * Memory bank to lock redirect bytes in 'this' memory bank
	 */
	protected PagedMemoryBank mbLockRedirect;

	/**
	 * Byte offset into memory bank 'mbLock' to indicate where page 0 can be locked
	 */
	protected int lockOffset;

	/**
	 * Byte offset into memory bank 'mbRedirect' to indicate where page 0 can be
	 * redirected
	 */
	protected int redirectOffset;

	/**
	 * Byte offset into memory bank 'mbLockRedirect' to indicate where page 0 can
	 * have its redirection byte locked
	 */
	protected int lockRedirectOffset;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on. Requires reference to memory banks used in OTP
	 * operations.
	 */
	public MemoryBankEPROM(OneWireContainer ibutton) {

		// keep reference to ibutton where memory bank is
		this.ib = ibutton;

		// get references to MemoryBanks used in OTP operations, assume no
		// locking/redirection
		this.mbLock = null;
		this.mbRedirect = null;
		this.mbLockRedirect = null;
		this.lockOffset = 0;
		this.redirectOffset = 0;
		this.lockRedirectOffset = 0;

		// initialize attributes of this memory bank - DEFAULT: Main memory DS1985 w/o
		// lock stuff
		this.generalPurposeMemory = true;
		this.bankDescription = "Main Memory";
		this.numberPages = 64;
		this.size = 2048;
		this.pageLength = 32;
		this.maxPacketDataLength = 29;
		this.readWrite = false;
		this.writeOnce = true;
		this.readOnly = false;
		this.nonVolatile = true;
		this.pageAutoCRC = true;
		this.redirectPage = false;
		this.lockPage = false;
		this.lockRedirectPage = false;
		this.programPulse = true;
		this.powerDelivery = false;
		this.extraInfo = true;
		this.extraInfoLength = 1;
		this.extraInfoDescription = "Inverted redirection page";
		this.writeVerification = true;
		this.startPhysicalAddress = 0;
		this.READ_PAGE_WITH_CRC = MAIN_READ_PAGE_COMMAND;
		this.WRITE_MEMORY_COMMAND = MAIN_WRITE_COMMAND;
		this.numCRCBytes = 2;
		this.crcAfterAddress = true;
		this.normalReadCRC = false;
		this.doSetSpeed = true;

		// create the ffblock (used for faster 0xFF fills)
		this.ffBlock = new byte[50];

		for (var i = 0; i < 50; i++) {
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
		return this.writeOnce;
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
		return this.programPulse;
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
		return this.pageAutoCRC;
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
		return this.extraInfo;
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
		return this.extraInfoLength;
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
		return this.extraInfoDescription;
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
		return this.redirectPage;
	}

	/**
	 * Query to see if current memory bank pages can be locked. A locked page would
	 * prevent any changes to the memory.
	 *
	 * @return 'true' if current memory bank pages can be redirected to a new page.
	 */
	@Override
	public boolean canLockPage() {
		return this.lockPage;
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
		return this.lockRedirectPage;
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

		// check if read exceeds memory
		if (startAddr + len > this.pageLength * this.numberPages) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.checkSpeed();
		}

		// check if status memory
		if (this.READ_PAGE_WITH_CRC == STATUS_READ_PAGE_COMMAND) {

			// no regular read memory so must use readPageCRC
			var start_pg = startAddr / this.pageLength;
			var end_pg = (startAddr + len) / this.pageLength - 1;

			if ((startAddr + len) % this.pageLength > 0) {
				end_pg++;
			}

			var raw_buf = new byte[(end_pg - start_pg + 1) * this.pageLength];

			// loop to read the pages
			for (var pg = start_pg; pg <= end_pg; pg++) {
				this.readPageCRC(pg, !(pg == start_pg), raw_buf, (pg - start_pg) * this.pageLength, null, 0);
			}

			// extract out the data
			System.arraycopy(raw_buf, startAddr % this.pageLength, readBuf, offset, len);
		}

		// regular memory so use standard read memory command
		else {

			// see if need to access the device
			if (!readContinue) {

				// select the device
				if (!this.ib.adapter.select(this.ib.address)) {
					this.forceVerify();

					throw new OneWireIOException("Device select failed");
				}

				// build start reading memory block
				var raw_buf = new byte[4];

				raw_buf[0] = READ_MEMORY_COMMAND;
				raw_buf[1] = (byte) (startAddr + this.startPhysicalAddress & 0xFF);
				raw_buf[2] = (byte) ((startAddr + this.startPhysicalAddress & 0xFFFF) >>> 8 & 0xFF);
				raw_buf[3] = (byte) 0xFF;

				// check if get a 1 byte crc in a normal read.
				var num_bytes = this.normalReadCRC ? 4 : 3;

				// do the first block for command, address
				this.ib.adapter.dataBlock(raw_buf, 0, num_bytes);
			}

			// pre-fill readBuf with 0xFF
			var pgs = len / this.pageLength;
			var extra = len % this.pageLength;

			for (i = 0; i < pgs; i++) {
				System.arraycopy(this.ffBlock, 0, readBuf, offset + i * this.pageLength, this.pageLength);
			}
			System.arraycopy(this.ffBlock, 0, readBuf, offset + pgs * this.pageLength, extra);

			// send second block to read data, return result
			this.ib.adapter.dataBlock(readBuf, offset, len);
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
		int i;
		byte result;

		// return if nothing to do
		if (len == 0) {
			return;
		}

		// check if power delivery is available
		if (!this.ib.adapter.canProgram()) {
			throw new OneWireException("Program voltage required but not available");
		}

		// check if trying to write read only bank
		if (this.isReadOnly()) {
			throw new OneWireException("Trying to write read-only memory bank");
		}

		// check if write exceeds memory
		if (startAddr + len > this.pageLength * this.numberPages) {
			throw new OneWireException("Write exceeds memory bank end");
		}

		// set the program pulse duration
		this.ib.adapter.setProgramPulseDuration(DSPortAdapter.DELIVERY_EPROM);

		// attempt to put device at max desired speed
		this.checkSpeed();

		// loop while still have bytes to write
		var write_continue = false;

		for (i = 0; i < len; i++) {
			result = this.programByte(startAddr + i + this.startPhysicalAddress, writeBuf[offset + i], write_continue);

			if (this.writeVerification) {
				if (result != writeBuf[offset + i]) {
					this.forceVerify();

					throw new OneWireIOException("Read back byte on EPROM programming did not match");
				}
				write_continue = true;
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
		if (this.pageAutoCRC) {
			this.readPageCRC(page, readContinue, readBuf, offset, null, this.extraInfoLength);
		} else {
			this.read(page * this.pageLength, readContinue, readBuf, offset, this.pageLength);
		}
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

		// check if current bank is not scratchpad bank, or not page 0
		if (!this.extraInfo) {
			throw new OneWireException("Read extra information not supported on this memory bank");
		}

		this.readPageCRC(page, readContinue, readBuf, offset, extraInfo, this.extraInfoLength);
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
		var raw_buf = new byte[this.pageLength];

		// check if current bank is not scratchpad bank, or not page 0
		if (!this.extraInfo) {
			throw new OneWireException("Read extra information not supported on this memory bank");
		}

		// read entire page with read page CRC
		this.readPageCRC(page, readContinue, raw_buf, 0, extraInfo, this.extraInfoLength);

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
		this.readPageCRC(page, readContinue, raw_buf, 0, null, this.extraInfoLength);

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

		// see if this bank is general read/write
		if (!this.generalPurposeMemory) {
			throw new OneWireException("Current bank is not general purpose memory");
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
		this.readPageCRC(page, readContinue, readBuf, offset, null, this.extraInfoLength);
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
		this.readPageCRC(page, readContinue, readBuf, offset, extraInfo, this.extraInfoLength);
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

		// create byte to write to mlLock to lock page
		var nbyt = page >>> 3;
		var nbit = page - (nbyt << 3);
		var wr_byte = new byte[1];

		wr_byte[0] = (byte) ~(0x01 << nbit);

		// bit field so turn off write verification
		this.mbLock.setWriteVerification(false);

		// write the lock bit
		this.mbLock.write(nbyt + this.lockOffset, wr_byte, 0, 1);

		// read back to verify
		if (!this.isPageLocked(page)) {
			this.forceVerify();

			throw new OneWireIOException("Read back from write incorrect, could not lock page");
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

		// read page that locked bit is on
		var pg_len = this.mbLock.getPageLength();
		var read_pg = (page + this.lockOffset) / (pg_len * 8);

		// read page with bit
		var read_buf = new byte[pg_len];

		this.mbLock.readPageCRC(read_pg, false, read_buf, 0);

		// return boolean on locked bit
		var index = page + this.lockOffset - read_pg * 8 * pg_len;
		var nbyt = index >>> 3;
		var nbit = index - (nbyt << 3);

		return !((read_buf[nbyt] >>> nbit & 0x01) == 0x01);
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

		// create byte to redirect page
		var wr_byte = new byte[1];

		wr_byte[0] = (byte) ~newPage;

		// writing byte so turn on write verification
		this.mbRedirect.setWriteVerification(true);

		// write the redirection byte
		this.mbRedirect.write(page + this.redirectOffset, wr_byte, 0, 1);
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

		// read page that redirect byte is on
		var pg_len = this.mbRedirect.getPageLength();
		var read_pg = (page + this.redirectOffset) / pg_len;

		// read page with byte
		var read_buf = new byte[pg_len];

		this.mbRedirect.readPageCRC(read_pg, false, read_buf, 0);

		// return page
		return ~read_buf[(page + this.redirectOffset) % pg_len] & 0x000000FF;
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

		// create byte to write to mlLock to lock page
		var nbyt = page >>> 3;
		var nbit = page - (nbyt << 3);
		var wr_byte = new byte[1];

		wr_byte[0] = (byte) ~(0x01 << nbit);

		// bit field so turn off write verification
		this.mbLockRedirect.setWriteVerification(false);

		// write the lock bit
		this.mbLockRedirect.write(nbyt + this.lockRedirectOffset, wr_byte, 0, 1);

		// read back to verify
		if (!this.isRedirectPageLocked(page)) {
			this.forceVerify();

			throw new OneWireIOException("Read back from write incorrect, could not lock redirect byte");
		}
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

		// read page that lock redirect bit is on
		var pg_len = this.mbLockRedirect.getPageLength();
		var read_pg = (page + this.lockRedirectOffset) / (pg_len * 8);

		// read page with bit
		var read_buf = new byte[pg_len];

		this.mbLockRedirect.readPageCRC(read_pg, false, read_buf, 0);

		// return boolean on lock redirect bit
		var index = page + this.lockRedirectOffset - read_pg * 8 * pg_len;
		var nbyt = index >>> 3;
		var nbit = index - (nbyt << 3);

		return !((read_buf[nbyt] >>> nbit & 0x01) == 0x01);
	}

	// --------
	// -------- Bank specific methods
	// --------

	/**
	 * Read a complete memory page with CRC verification provided by the device with
	 * extra information. Not supported by all devices. If not extra information
	 * available then just call with extraLength=0.
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
	 * @param extraLength  length of extra information
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	protected void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo,
			int extraLength) throws OneWireIOException, OneWireException {
		int len = 0, lastcrc = 0;
		var raw_buf = new byte[this.pageLength + this.numCRCBytes];

		// only needs to be implemented if supported by hardware
		if (!this.pageAutoCRC) {
			throw new OneWireException("Read page with CRC not supported in this memory bank");
		}

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.checkSpeed();
		}

		// check if read exceeds memory
		if (page > this.numberPages) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// see if need to access the device
		if (!readContinue) {

			// select the device
			if (!this.ib.adapter.select(this.ib.address)) {
				this.forceVerify();

				throw new OneWireIOException("Device select failed");
			}

			// build start reading memory block with: command, address, (extraInfo?), (crc?)
			len = 3 + this.extraInfoLength;

			if (this.crcAfterAddress) {
				len += this.numCRCBytes;
			}

			System.arraycopy(this.ffBlock, 0, raw_buf, 0, len);

			raw_buf[0] = this.READ_PAGE_WITH_CRC;

			var addr = page * this.pageLength + this.startPhysicalAddress;

			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

			// do the first block
			this.ib.adapter.dataBlock(raw_buf, 0, len);
		} else if (this.extraInfoLength > 0) {

			// build first block with: extraInfo, crc
			len = this.extraInfoLength + this.numCRCBytes;

			System.arraycopy(this.ffBlock, 0, raw_buf, 0, len);

			// do the first block
			this.ib.adapter.dataBlock(raw_buf, 0, len);
		}

		// check CRC
		if (this.numCRCBytes == 2) {
			lastcrc = CRC16.compute(raw_buf, 0, len, 0);
		} else {
			lastcrc = CRC8.compute(raw_buf, 0, len, 0);
		}

		if (this.extraInfoLength > 0 || this.crcAfterAddress) {

			// check CRC
			if (this.numCRCBytes == 2) {
				if (lastcrc != 0x0000B001) {
					this.forceVerify();

					throw new OneWireIOException("Invalid CRC16 read from device");
				}
			} else if (lastcrc != 0) {
				this.forceVerify();

				throw new OneWireIOException("Invalid CRC8 read from device");
			}

			lastcrc = 0;

			// extract the extra information
			if (this.extraInfoLength > 0 && extraInfo != null) {
				System.arraycopy(raw_buf, len - this.extraInfoLength - this.numCRCBytes, extraInfo, 0, extraLength);
			}
		}

		// pre-fill with 0xFF
		System.arraycopy(this.ffBlock, 0, raw_buf, 0, raw_buf.length);

		// send block to read data + crc
		this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length);

		// check the CRC
		if (this.numCRCBytes == 2) {
			if (CRC16.compute(raw_buf, 0, raw_buf.length, lastcrc) != 0x0000B001) {
				this.forceVerify();

				throw new OneWireIOException("Invalid CRC16 read from device");
			}
		} else if (CRC8.compute(raw_buf, 0, raw_buf.length, lastcrc) != 0) {
			this.forceVerify();

			throw new OneWireIOException("Invalid CRC8 read from device");
		}

		// extract the page data
		System.arraycopy(raw_buf, 0, readBuf, offset, this.pageLength);
	}

	/**
	 * Program an EPROM byte at the specified address.
	 *
	 * @param addr          address
	 * @param data          data byte to program
	 * @param writeContinue if 'true' then device programming is continued without
	 *                      re-selecting. This can only be used if the new
	 *                      programByte() continuous where the last one stopped and
	 *                      it is inside a 'beginExclusive/endExclusive' block.
	 *
	 * @return the echo byte after programming. This should be the desired byte to
	 *         program if the location was previously unprogrammed.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	protected byte programByte(int addr, byte data, boolean writeContinue) throws OneWireIOException, OneWireException {
		int lastcrc = 0, len;

		if (!writeContinue) {

			// select the device
			if (!this.ib.adapter.select(this.ib.address)) {
				this.forceVerify();

				throw new OneWireIOException("device not present");
			}

			// pre-fill with 0xFF
			var raw_buf = new byte[6];

			System.arraycopy(this.ffBlock, 0, raw_buf, 0, raw_buf.length);

			// construct packet
			raw_buf[0] = this.WRITE_MEMORY_COMMAND;
			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);
			raw_buf[3] = data;

			if (this.numCRCBytes == 2) {
				lastcrc = CRC16.compute(raw_buf, 0, 4, 0);
				len = 6;
			} else {
				lastcrc = CRC8.compute(raw_buf, 0, 4, 0);
				len = 5;
			}

			// send block to read data + crc
			this.ib.adapter.dataBlock(raw_buf, 0, len);

			// check CRC
			if (this.numCRCBytes == 2) {
				if (CRC16.compute(raw_buf, 4, 2, lastcrc) != 0x0000B001) {
					this.forceVerify();

					throw new OneWireIOException("Invalid CRC16 read from device");
				}
			} else if (CRC8.compute(raw_buf, 4, 1, lastcrc) != 0) {
				this.forceVerify();

				throw new OneWireIOException("Invalid CRC8 read from device");
			}
		} else {

			// send the data
			this.ib.adapter.putByte(data);

			// check CRC from device
			if (this.numCRCBytes == 2) {
				lastcrc = CRC16.compute(data, addr);
				lastcrc = CRC16.compute(this.ib.adapter.getByte(), lastcrc);

				if (CRC16.compute(this.ib.adapter.getByte(), lastcrc) != 0x0000B001) {
					this.forceVerify();

					throw new OneWireIOException("Invalid CRC16 read from device");
				}
			} else {
				lastcrc = CRC8.compute(data, addr);

				if (CRC8.compute(this.ib.adapter.getByte(), lastcrc) != 0) {
					this.forceVerify();

					throw new OneWireIOException("Invalid CRC8 read from device");
				}
			}
		}

		// send the pulse
		this.ib.adapter.startProgramPulse(DSPortAdapter.CONDITION_NOW);

		// return the result
		return (byte) this.ib.adapter.getByte();
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
