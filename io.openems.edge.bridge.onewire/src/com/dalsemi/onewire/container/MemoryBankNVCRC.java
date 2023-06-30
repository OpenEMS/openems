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
 * Memory bank class for the NVRAM with build in CRC section of iButtons and
 * 1-Wire devices.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankNVCRC extends MemoryBankNV {

	// --------
	// -------- Static Final Variables
	// --------

	/**
	 * Write Scratchpad Command
	 */
	public static final byte READ_PAGE_WITH_CRC = (byte) 0xA5;

	// --------
	// -------- Variables
	// --------

	/**
	 * Flag to indicate if read-continue is possible
	 */
	protected boolean readContinuePossible;

	/**
	 * Number of extra verify byte in the read CRC page
	 */
	protected int numVerifyBytes;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankNVCRC(OneWireContainer ibutton, ScratchPad scratch) {
		super(ibutton, scratch);

		// initialize attributes of this memory bank
		this.pageAutoCRC = true;
		this.readContinuePossible = true;
		this.numVerifyBytes = 0;
	}

	// --------
	// -------- I/O methods
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

		// all other pages
		this.readPageCRC(page, readContinue, readBuf, offset, null, this.extraInfoLength);
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

		// read entire page with read page CRC
		this.readPageCRC(page, readContinue, raw_buf, 0, extraInfo, this.extraInfoLength);

		// check if length is realistic
		if (raw_buf[0] > this.maxPacketDataLength) {
			this.sp.forceVerify();

			throw new OneWireIOException("Invalid length in packet");
		}

		// verify the CRC is correct
		var abs_page = this.startPhysicalAddress / this.pageLength + page;
		if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, abs_page) == 0x0000B001) {

			// extract the data out of the packet
			System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);

			// return the length
			return raw_buf[0];
		}
		this.sp.forceVerify();

		throw new OneWireIOException("Invalid CRC16 in packet read");
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
		var last_crc = 0;
		byte[] raw_buf;

		// only needs to be implemented if supported by hardware
		if (!this.pageAutoCRC) {
			throw new OneWireException("Read page with CRC not supported in this memory bank");
		}

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.sp.checkSpeed();
		}

		// check if read exceeds memory
		if (page > this.numberPages) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// see if need to access the device
		if (!readContinue || !this.readContinuePossible) {

			// select the device
			if (!this.ib.adapter.select(this.ib.address)) {
				this.sp.forceVerify();

				throw new OneWireIOException("Device select failed");
			}

			// build start reading memory block
			raw_buf = new byte[3];
			raw_buf[0] = READ_PAGE_WITH_CRC;

			var addr = page * this.pageLength + this.startPhysicalAddress;

			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

			// perform CRC16 on first part
			last_crc = CRC16.compute(raw_buf, 0, raw_buf.length, last_crc);

			// do the first block for command, TA1, TA2
			this.ib.adapter.dataBlock(raw_buf, 0, 3);
		}

		// pre-fill with 0xFF
		raw_buf = new byte[this.pageLength + extraLength + 2 + this.numVerifyBytes];

		System.arraycopy(this.ffBlock, 0, raw_buf, 0, raw_buf.length);

		// send block to read data + extra info? + crc
		this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length);

		// check the CRC
		if (CRC16.compute(raw_buf, 0, raw_buf.length - this.numVerifyBytes, last_crc) != 0x0000B001) {
			this.sp.forceVerify();

			throw new OneWireIOException("Invalid CRC16 read from device");
		}

		// extract the page data
		System.arraycopy(raw_buf, 0, readBuf, offset, this.pageLength);

		// optional extract the extra info
		if (extraInfo != null) {
			System.arraycopy(raw_buf, this.pageLength, extraInfo, 0, extraLength);
		}
	}
}
// CHECKSTYLE:ON
