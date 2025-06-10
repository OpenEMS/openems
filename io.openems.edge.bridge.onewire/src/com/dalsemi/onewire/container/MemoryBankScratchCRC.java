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
 * Memory bank class for the Scratchpad section of NVRAM iButtons and 1-Wire
 * devices.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankScratchCRC extends MemoryBankScratchEx {

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankScratchCRC(OneWireContainer ibutton) {
		super(ibutton);

		// initialize attributes of this memory bank - DEFAULT: DS1963L scratchapd
		this.bankDescription = "Scratchpad with CRC";
		this.pageAutoCRC = true;

		// default copy scratchpad command
		this.COPY_SCRATCHPAD_COMMAND = (byte) 0x55;
	}

	// --------
	// -------- PagedMemoryBank I/O methods
	// --------

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
		var extraInfo = new byte[this.extraInfoLength];

		this.readPageCRC(page, readContinue, readBuf, offset, extraInfo);
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

		// read the scratchpad
		this.readScratchpad(readBuf, offset, this.pageLength, extraInfo);
	}

	// --------
	// -------- ScratchPad methods
	// --------

	/**
	 * Read the scratchpad page of memory from a NVRAM device This method reads and
	 * returns the entire scratchpad after the byte offset regardless of the actual
	 * ending offset
	 *
	 * @param readBuf   byte array to place read data into length of array is always
	 *                  pageLength.
	 * @param offset    offset into readBuf to pug data
	 * @param len       length in bytes to read
	 * @param extraInfo byte array to put extra info read into (TA1, TA2, e/s byte)
	 *                  length of array is always extraInfoLength. Can be 'null' if
	 *                  extra info is not needed.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void readScratchpad(byte[] readBuf, int offset, int len, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		// select the device
		if (!this.ib.adapter.select(this.ib.address)) {
			this.forceVerify();

			throw new OneWireIOException("Device select failed");
		}

		// build block
		var raw_buf = new byte[this.extraInfoLength + this.pageLength + 3];

		raw_buf[0] = READ_SCRATCHPAD_COMMAND;

		System.arraycopy(this.ffBlock, 0, raw_buf, 1, raw_buf.length - 1);

		// send block, command + (extra) + page data + CRC
		this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length);

		// get the starting offset to see when the crc will show up
		int addr = raw_buf[1];

		addr = (addr | raw_buf[2] << 8 & 0xFF00) & 0xFFFF;

		var num_crc = 35 - (addr & 0x001F) + this.extraInfoLength;

		// check crc of entire block
		if (CRC16.compute(raw_buf, 0, num_crc, 0) != 0x0000B001) {
			this.forceVerify();

			throw new OneWireIOException("Invalid CRC16 read from device");
		}

		// optionally extract the extra info
		if (extraInfo != null) {
			System.arraycopy(raw_buf, 1, extraInfo, 0, this.extraInfoLength);
		}

		// extract the page data
		System.arraycopy(raw_buf, this.extraInfoLength + 1, readBuf, 0, this.pageLength);
	}
}
// CHECKSTYLE:ON
