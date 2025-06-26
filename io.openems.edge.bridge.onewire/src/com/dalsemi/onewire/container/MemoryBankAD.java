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
 * Memory bank class for the DS2450.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankAD implements PagedMemoryBank {

	// --------
	// --------Static Final Variables
	// --------

	/**
	 * Read Memory Command
	 */
	public static final byte READ_MEMORY_COMMAND = (byte) 0xAA;

	/**
	 * Write Memory Command
	 */
	public static final byte WRITE_MEMORY_COMMAND = (byte) 0x55;

	/**
	 * Page length
	 */
	public static final int PAGE_LENGTH = 8;

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
	// -------- Protected Variables for MemoryBank implementation
	// --------

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
	 * Starting physical address in memory bank. Needed for different types of
	 * memory in the same logical memory bank. This can be used to separate them
	 * into two virtual memory banks. Example: DS2406 status page has mixed EPROM
	 * and Volatile RAM.
	 */
	protected int startPhysicalAddress;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankAD(OneWireContainer ibutton) {

		// keep reference to ibutton where memory bank is
		this.ib = ibutton;

		// create the ffblock (used for faster 0xFF fills)
		this.ffBlock = new byte[50];

		for (var i = 0; i < 50; i++) {
			this.ffBlock[i] = (byte) 0xFF;
		}

		// defaults for Page0 of DS2450
		this.bankDescription = "A/D Conversion read-out";
		this.generalPurposeMemory = false;
		this.startPhysicalAddress = 0;
		this.readWrite = false;
		this.writeOnce = false;
		this.readOnly = true;
		this.nonVolatile = false;
		this.writeVerification = true;

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
		return PAGE_LENGTH;
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
		return 1;
	}

	/**
	 * Query to get page length in bytes in current memory bank.
	 *
	 * @return page length in bytes in current memory bank
	 */
	@Override
	public int getPageLength() {
		return PAGE_LENGTH;
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
		return PAGE_LENGTH - 3;
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
		return true;
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

		// check if read exceeds memory
		if (startAddr + len > PAGE_LENGTH) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// no regular read memory so must use readPageCRC
		var start_pg = startAddr / PAGE_LENGTH;
		var end_pg = (startAddr + len) / PAGE_LENGTH - 1;

		if ((startAddr + len) % PAGE_LENGTH > 0) {
			end_pg++;
		}

		var raw_buf = new byte[(end_pg - start_pg + 1) * PAGE_LENGTH];

		// loop to read the pages
		for (var pg = start_pg; pg <= end_pg; pg++) {
			this.readPageCRC(pg, !(pg == start_pg), raw_buf, (pg - start_pg) * PAGE_LENGTH);
		}

		// extract out the data
		System.arraycopy(raw_buf, startAddr % PAGE_LENGTH, readBuf, offset, len);
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
		var raw_buf = new byte[7];
		int cnt = 0, start_addr, addr, end_addr, lastcrc;

		// return if nothing to do
		if (len == 0) {
			return;
		}

		// attempt to put device at max desired speed
		this.checkSpeed();

		// select the device
		if (!this.ib.adapter.select(this.ib.address)) {
			this.forceVerify();

			throw new OneWireIOException("Device select failed");
		}

		// build packet
		raw_buf[cnt++] = WRITE_MEMORY_COMMAND;
		start_addr = startAddr + this.startPhysicalAddress;
		end_addr = start_addr + len;
		raw_buf[cnt++] = (byte) (start_addr & 0xFF);
		raw_buf[cnt++] = (byte) ((start_addr & 0xFFFF) >>> 8 & 0xFF);

		// loop for each byte to write
		for (addr = start_addr; addr < end_addr; addr++) {

			// add the byte to write to buffer
			raw_buf[cnt++] = writeBuf[offset + addr - start_addr];

			// initialize crc16
			lastcrc = CRC16.compute(raw_buf, 0, cnt, addr == start_addr ? 0 : addr);

			// add the read crc and echo byte to block
			System.arraycopy(this.ffBlock, 0, raw_buf, cnt, 3);

			// perform the block
			this.ib.adapter.dataBlock(raw_buf, 0, cnt + 3);

			// check the CRC
			if (CRC16.compute(raw_buf, cnt, 2, lastcrc) != 0x0000B001) {
				this.forceVerify();

				throw new OneWireIOException("Invalid CRC16 read from device");
			}

			// check echo
			if (raw_buf[cnt + 2] != writeBuf[offset + addr - start_addr]) {
				this.forceVerify();

				throw new OneWireIOException("Write byte echo was invalid");
			}

			// reset the buffer and loop
			cnt = 0;
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

		// read page with read page CRC
		this.readPageCRC(page, readContinue, readBuf, offset);
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

		// only needs to be implemented if supported by hardware
		throw new OneWireException("Read page with extra-info not supported by this memory bank");
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
		var raw_buf = new byte[PAGE_LENGTH];

		// read entire page with read page CRC
		this.readPageCRC(page, readContinue, raw_buf, 0);

		// check if length is realistic
		if (raw_buf[0] > PAGE_LENGTH - 3) {
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

		// only needs to be implemented if supported by hardware
		throw new OneWireException("Read page packet with extra-info not supported by this memory bank");
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
		if (len > PAGE_LENGTH - 3) {
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
		this.write(page * PAGE_LENGTH, raw_buf, 0, len + 3);
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
		var raw_buf = new byte[5 + PAGE_LENGTH];
		int len;

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.checkSpeed();
		}

		// see if need to access the device
		if (!readContinue) {

			// select the device
			if (!this.ib.adapter.select(this.ib.address)) {
				this.forceVerify();

				throw new OneWireIOException("Device select failed");
			}

			// build start reading memory block with: command, address
			len = raw_buf.length;

			System.arraycopy(this.ffBlock, 0, raw_buf, 0, len);

			raw_buf[0] = READ_MEMORY_COMMAND;

			var addr = page * PAGE_LENGTH + this.startPhysicalAddress;

			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);
		} else {
			len = PAGE_LENGTH + 2;

			System.arraycopy(this.ffBlock, 0, raw_buf, 0, len);
		}

		// do the block
		this.ib.adapter.dataBlock(raw_buf, 0, len);

		// check the CRC
		if (CRC16.compute(raw_buf, 0, len, 0) != 0x0000B001) {
			this.forceVerify();

			throw new OneWireIOException("Invalid CRC16 read from device");
		}

		// extract the data to return
		System.arraycopy(raw_buf, len - 2 - PAGE_LENGTH, readBuf, offset, PAGE_LENGTH);
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

		// only needs to be implemented if supported by hardware
		throw new OneWireException("Read page with CRC and extra-info not supported by this memory bank");
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
