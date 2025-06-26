// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 1999 Maxim Integrated Products, All Rights Reserved.
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
import com.dalsemi.onewire.debug.Debug;
import com.dalsemi.onewire.utils.CRC16;
import com.dalsemi.onewire.utils.Convert;
import com.dalsemi.onewire.utils.IOHelper;

/**
 * Memory bank class for the DS1961S/DS2432.
 *
 * @version 0.00, 19 Dec 2000
 * @author DS
 */
public class MemoryBankSHAEE implements PagedMemoryBank {

	// --------
	// --------Static Final Variables
	// --------

	/** turn on extra debugging output */
	private static final boolean DEBUG = false;

	/** Read Memory Command */
	public static final byte READ_MEMORY = (byte) 0xF0;

	/** Read Authenticate Page */
	public static final byte READ_AUTH_PAGE = (byte) 0xA5;

	// --------
	// -------- Protected Variables for MemoryBank implementation
	// --------

	/**
	 * Check the status of the memory page.
	 */
	protected boolean checked;

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

	/**
	 * Number of pages in memory bank
	 */
	protected int numberPages;

	/**
	 * page length in memory bank
	 */
	protected int pageLength;

	/**
	 * Flag if reading a page in memory bank provides optional extra information
	 * (counter, tamper protection, SHA-1...)
	 */
	protected boolean extraInfo;

	/**
	 * Extra information length in bytes
	 */
	protected int extraInfoLength;

	/**
	 * Max data length in page packet in memory bank
	 */
	protected int maxPacketDataLength;

	/**
	 * Flag if memory bank has page CRC.
	 */
	protected boolean pageCRC;

	// --------
	// -------- Variables
	// --------

	/**
	 * Reference to the OneWireContainer this bank resides on.
	 */
	protected OneWireContainer33 ib = null;

	/**
	 * Reference to the adapter the OneWireContainer resides on.
	 */
	protected DSPortAdapter adapter = null;

	/**
	 * Flag to indicate that speed needs to be set
	 */
	protected boolean doSetSpeed;

	/**
	 * block of 0xFF's used for faster read pre-fill of 1-Wire blocks Comes from
	 * OneWireContainer33 that this MemoryBank references.
	 */
	protected static final byte[] ffBlock = OneWireContainer33.ffBlock;

	/**
	 * block of 0x00's used for faster read pre-fill of 1-Wire blocks Comes from
	 * OneWireContainer33 that this MemoryBank references.
	 */
	protected static final byte[] zeroBlock = OneWireContainer33.zeroBlock;

	protected MemoryBankScratchSHAEE scratchpad;

	/**
	 * Memory bank constructor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankSHAEE(OneWireContainer33 ibutton, MemoryBankScratchSHAEE scratch) {
		// keep reference to ibutton where memory bank is
		this.ib = ibutton;

		this.scratchpad = scratch;

		// keep reference to adapter that button is on
		this.adapter = this.ib.getAdapter();

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
		return true;
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
	 * Query to see if current memory bank pages can be read with the contents being
	 * verified by a device generated CRC. This is used to see if the
	 * 'ReadPageCRC()' can be used.
	 *
	 * @return 'true' if current memory bank can be read with self generated CRC.
	 */
	@Override
	public boolean hasPageAutoCRC() {
		return this.pageCRC;
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
		return "The MAC for the SHA Engine";
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
	 * @param startAddr    starting address, relative to physical address for this
	 *                     memory bank.
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
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			Debug.debug("-----------------------------------------------------------");
			Debug.debug("MemoryBankSHAEE.read(int, boolean, byte[], int, int) called");
			Debug.debug("  startAddr=0x" + Convert.toHexString((byte) startAddr));
			Debug.debug("  readContinue=" + readContinue);
			Debug.debug("  offset=" + offset);
			Debug.debug("  len=" + len);
			Debug.debug("  this.startPhysicalAddress=0x" + Convert.toHexString((byte) this.startPhysicalAddress));
			Debug.debug("  this.pageLength=" + this.pageLength);
			Debug.debug("  this.numberPages=" + this.numberPages);
			Debug.stackTrace();
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		// attempt to put device at max desired speed
		if (!readContinue && this.ib.adapterSet()) {
			this.checkSpeed();
		}

		// check if read exceeds memory
		if (startAddr + len > this.pageLength * this.numberPages) {
			throw new OneWireException("Read exceeds memory bank end.");
		}

		// see if need to access the device
		if (!readContinue) {
			// select the device
			if (!this.adapter.select(this.ib.getAddress())) {
				throw new OneWireIOException("Device select failed.");
			}

			// build start reading memory block
			var addr = startAddr + this.startPhysicalAddress;
			var raw_buf = new byte[3];

			raw_buf[0] = READ_MEMORY;
			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

			// do the first block for command, address
			this.adapter.dataBlock(raw_buf, 0, 3);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				Debug.debug("  raw_buf", raw_buf, 0, 3);
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}
		}

		// pre-fill readBuf with 0xFF
		var pgs = len / this.pageLength;
		var extra = len % this.pageLength;

		for (var i = 0; i < pgs; i++) {
			System.arraycopy(ffBlock, 0, readBuf, offset + i * this.pageLength, this.pageLength);
		}
		System.arraycopy(ffBlock, 0, readBuf, offset + pgs * this.pageLength, extra);

		// send second block to read data, return result
		this.adapter.dataBlock(readBuf, offset, len);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			Debug.debug("  readBuf", readBuf, offset, len);
			Debug.debug("-----------------------------------------------------------");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
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
	 * @param startAddr starting address, relative to the starting physical address
	 *                  of this memory bank
	 * @param writeBuf  byte array containing data to write
	 * @param offset    offset into writeBuf to get data
	 * @param len       length in bytes to write
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void write(int startAddr, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("-----------------------------------------------------------");
			Debug.debug("MemoryBankSHAEE.write(int,byte[],int,int) called");
			Debug.debug("  startAddr=0x" + Convert.toHexString((byte) startAddr));
			Debug.debug("  writeBuf", writeBuf, offset, len);
			Debug.debug("  startPhysicalAddress=0x" + Convert.toHexString((byte) this.startPhysicalAddress));
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		int room_left;

		if (!this.checked) {
			this.checked = this.ib.checkStatus();
		}

		// return if nothing to do
		if (len == 0) {
			return;
		}

		// attempt to put device at speed
		this.checkSpeed();

		// check to see if secret is set
		if (!this.ib.isContainerSecretSet()) {
			throw new OneWireException("Secret is not set.");
		}

		// check if write exceeds memory
		if (startAddr + len > this.size) {
			throw new OneWireException("Write exceeds memory bank end");
		}

		// check if trying to write read only bank
		if (this.isReadOnly()) {
			throw new OneWireException("Trying to write read-only memory bank");
		}

		// loop while still have pages to write
		int startx = 0, nextx = 0; // (start and next index into writeBuf)
		var raw_buf = new byte[8];
		var memory = new byte[this.size - (startAddr & 0xE0)]; // till end of memory
		var abs_addr = this.startPhysicalAddress + startAddr;
		var pl = 8;

		this.read(startAddr & 0xE0, false, memory, 0, memory.length);

		if (abs_addr >= 128) {
			this.ib.getContainerSecret(memory, 0);
		}

		do {
			// calculate room left in current page
			room_left = pl - (abs_addr + startx) % pl;

			// check if block left will cross end of page
			if (len - startx > room_left) {
				nextx = startx + room_left;
			} else {
				nextx = len;
			}

			// bug fix, if updating pages two and three in the same write op
			// this used to fail, was (startAddr>=pageLength)
			if (startx + startAddr >= this.pageLength) {
				System.arraycopy(memory, (startx + startAddr) / 8 * 8 - 32, raw_buf, 0, 8);
			} else {
				System.arraycopy(memory, (startx + startAddr) / 8 * 8, raw_buf, 0, 8);
			}

			if (nextx - startx == 8) {
				System.arraycopy(writeBuf, offset + startx, raw_buf, 0, 8);
			} else if ((startAddr + nextx) % 8 == 0) {
				System.arraycopy(writeBuf, offset + startx, raw_buf, (startAddr + startx) % 8,
						8 - (startAddr + startx) % 8);
			} else {
				System.arraycopy(writeBuf, offset + startx, raw_buf, (startAddr + startx) % 8,
						(startAddr + nextx) % 8 - (startAddr + startx) % 8);
			}

			// write the page of data to scratchpad
			this.scratchpad.writeScratchpad(abs_addr + startx + room_left - 8, raw_buf, 0, 8);

			// Copy data from scratchpad into memory
			this.scratchpad.copyScratchpad(abs_addr + startx + room_left - 8, raw_buf, 0, memory, 0);

			// bug fix, if updating pages two and three in the same write op
			// this used to fail, was (startAddr>=pageLength)
			if (startx + startAddr >= this.pageLength) {
				System.arraycopy(raw_buf, 0, memory, (startx + startAddr) / 8 * 8 - 32, 8);
			} else {
				System.arraycopy(raw_buf, 0, memory, (startx + startAddr) / 8 * 8, 8);
			}

			// point to next index
			startx = nextx;
		} while (nextx < len);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("-----------------------------------------------------------");
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
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
		var pg = new byte[32];

		if (!this.checked) {
			this.checked = this.ib.checkStatus();
		}

		if (!this.hasPageAutoCRC()) {
			throw new OneWireException("This memory bank doesn't have crc capabilities.");
		}

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.checkSpeed();
		}

		if (!this.readAuthenticatedPage(page, pg, 0, extraInfo, 0)) {
			throw new OneWireException("Read didn't work.");
		}

		System.arraycopy(pg, 0, readBuf, offset, 32);
	}

	/**
	 * Read a Universal Data Packet.
	 *
	 * The Universal Data Packet always starts on page boundaries but can end
	 * anywhere in the page. The structure specifies the length of data bytes not
	 * including the length byte and the CRC16 bytes. There is one length byte. The
	 * CRC16 is first initialized to the page number. This provides a check to
	 * verify the page that was intended is being read. The CRC16 is then calculated
	 * over the length and data bytes. The CRC16 is then inverted and stored low
	 * byte first followed by the high byte. This is structure is used by this
	 * method to verify the data but is not returned, only the data payload is
	 * returned.
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
	 * @return number of data bytes read from the device and written to readBuf at
	 *         the offset.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset)
			throws OneWireIOException, OneWireException {
		var raw_buf = new byte[this.pageLength];

		// read the page
		this.readPage(page, readContinue, raw_buf, 0);

		// check if length is realistic
		if ((raw_buf[0] & 0x00FF) > this.maxPacketDataLength) {
			throw new OneWireIOException("Invalid length in packet.");
		}

		// verify the CRC is correct
		if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {

			// extract the data out of the packet
			System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);

			// return the length
			return raw_buf[0];
		}
		throw new OneWireIOException("Invalid CRC16 in packet read.");
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
	 * @return number of data bytes read from the device and written to readBuf at
	 *         the offset.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		var raw_buf = new byte[this.pageLength];

		if (!this.checked) {
			this.checked = this.ib.checkStatus();
		}

		if (!this.hasPageAutoCRC()) {
			throw new OneWireException("This memory bank page doesn't have CRC capabilities.");
		}

		// read the page
		this.readAuthenticatedPage(page, raw_buf, 0, extraInfo, 0);

		// check if length is realistic
		if ((raw_buf[0] & 0x00FF) > this.maxPacketDataLength) {
			throw new OneWireIOException("Invalid length in packet.");
		}

		// verify the CRC is correct
		if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {

			// extract the data out of the packet
			System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);

			// return the length
			return raw_buf[0];
		}
		throw new OneWireIOException("Invalid CRC16 in packet read.");
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
			throw new OneWireIOException("Length of packet requested exceeds page size.");
		}

		// see if this bank is general read/write
		if (!this.generalPurposeMemory) {
			throw new OneWireException("Current bank is not general purpose memory.");
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
		var extra = new byte[20];
		var pg = new byte[32];

		if (!this.checked) {
			this.checked = this.ib.checkStatus();
		}

		if (!this.hasPageAutoCRC()) {
			throw new OneWireException("This memory bank doesn't have CRC capabilities.");
		}

		if (!this.readAuthenticatedPage(page, pg, 0, extra, 0)) {
			throw new OneWireException("Read didn't work.");
		}

		System.arraycopy(pg, 0, readBuf, offset, this.pageLength);
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
		var pg = new byte[32];

		if (!this.checked) {
			this.checked = this.ib.checkStatus();
		}

		if (!this.hasPageAutoCRC()) {
			throw new OneWireException("This memory bank doesn't have CRC capabilities.");
		}

		if (!this.readAuthenticatedPage(page, pg, 0, extraInfo, 0)) {
			throw new OneWireException("Read didn't work.");
		}

		System.arraycopy(pg, 0, readBuf, offset, this.pageLength);
	}

	// ------------------------
	// Setting status
	// ------------------------

	/**
	 * Write protect the memory bank.
	 */
	public void writeprotect() {
		this.readOnly = true;
		this.readWrite = false;
	}

	/**
	 * Sets the EPROM mode for this page.
	 */
	public void setEPROM() {
		this.writeOnce = true;
	}

	// ------------------------
	// Extras
	// ------------------------

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
	 * Reads authenticated page.
	 *
	 * @param page       the page number in this bank to read from.
	 * @param data       the data read from the address
	 * @param extra_info the MAC calculated for this function
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean readAuthenticatedPage(int page, byte[] data, int dataStart, byte[] extra_info, int extraStart)
			throws OneWireException, OneWireIOException {
		var send_block = new byte[40];
		var challenge = new byte[8];

		var addr = page * this.pageLength + this.startPhysicalAddress;

		this.ib.getChallenge(challenge, 4);
		this.scratchpad.writeScratchpad(addr, challenge, 0, 8);

		// access the device
		if (!this.adapter.select(this.ib.getAddress())) {
			throw new OneWireIOException("Device select failed.");
		}

		// Read Authenticated Command
		send_block[0] = READ_AUTH_PAGE;
		// address 1
		send_block[1] = (byte) (addr & 0xFF);
		// address 2
		send_block[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

		// data + FF byte
		System.arraycopy(ffBlock, 0, send_block, 3, 35);

		// now send the block
		this.adapter.dataBlock(send_block, 0, 38);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			IOHelper.writeLine("-------------------------------------------------------------");
			IOHelper.writeLine("ReadAuthPage - send_block:");
			IOHelper.writeBytesHex(send_block, 0, 38);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

		// verify CRC16 is correct
		if (CRC16.compute(send_block, 0, 38, 0) != 0x0000B001) {
			throw new OneWireException("First CRC didn't pass.");
		}

		System.arraycopy(send_block, 3, data, dataStart, 32);

		System.arraycopy(ffBlock, 0, send_block, 0, 22);

		// adapter.startPowerDelivery(DSPortAdapter.CONDITION_NOW);
		try {
			Thread.sleep(2);
		} catch (InterruptedException ie) {

		}

		this.adapter.dataBlock(send_block, 0, 22);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			IOHelper.writeLine("ReadAuthPage - MAC:");
			IOHelper.writeBytesHex(send_block, 0, 20);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

		// verify CRC16 is correct
		if (CRC16.compute(send_block, 0, 22, 0) != 0x0000B001) {
			throw new OneWireException("Second CRC didn't pass.");
		}

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			IOHelper.writeLine("next read:");
			IOHelper.writeBytesHex(send_block, 0, 22);
			IOHelper.writeLine("-------------------------------------------------------------");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

		System.arraycopy(send_block, 0, extra_info, extraStart, 20);
		return true;
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
