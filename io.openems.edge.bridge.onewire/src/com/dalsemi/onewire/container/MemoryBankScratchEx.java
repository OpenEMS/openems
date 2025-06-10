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
class MemoryBankScratchEx extends MemoryBankScratch {

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankScratchEx(OneWireContainer ibutton) {
		super(ibutton);

		// initialize attributes of this memory bank
		this.bankDescription = "Scratchpad Ex";

		// change copy scratchpad command
		this.COPY_SCRATCHPAD_COMMAND = (byte) 0x5A;
	}

	// --------
	// -------- ScratchPad methods
	// --------

	/**
	 * Write to the scratchpad page of memory a NVRAM device.
	 *
	 * @param startAddr starting address
	 * @param writeBuf  byte array containing data to write
	 * @param offset    offset into readBuf to place data
	 * @param len       length in bytes to write
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void writeScratchpad(int startAddr, byte[] writeBuf, int offset, int len)
			throws OneWireIOException, OneWireException {
		var calcCRC = false;

		if (len > this.pageLength) {
			throw new OneWireException("Write exceeds memory bank end");
		}

		// select the device
		if (!this.ib.adapter.select(this.ib.address)) {
			this.forceVerify();

			throw new OneWireIOException("Device select failed");
		}

		// build block to send
		var raw_buf = new byte[this.pageLength + 5]; // [37];

		raw_buf[0] = WRITE_SCRATCHPAD_COMMAND;
		raw_buf[1] = (byte) (startAddr & 0xFF);
		raw_buf[2] = (byte) ((startAddr & 0xFFFF) >>> 8 & 0xFF);

		System.arraycopy(writeBuf, offset, raw_buf, 3, len);

		// check if full page (can utilize CRC)
		if ((startAddr + len) % this.pageLength == 0) {
			System.arraycopy(this.ffBlock, 0, raw_buf, len + 3, 2);

			calcCRC = true;
		}

		// send block, return result
		this.ib.adapter.dataBlock(raw_buf, 0, len + 3 + (calcCRC ? 2 : 0));
		// System.out.println("WriteScratchpad: " +
		// com.dalsemi.onewire.utils.Convert.toHexString(raw_buf));

		// check crc
		if (calcCRC) {
			if (CRC16.compute(raw_buf, 0, len + 5, 0) != 0x0000B001) {
				this.forceVerify();

				throw new OneWireIOException("Invalid CRC16 read from device");
			}
		}
	}

	/**
	 * Copy the scratchpad page to memory.
	 *
	 * @param startAddr starting address
	 * @param len       length in bytes that was written already
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public void copyScratchpad(int startAddr, int len) throws OneWireIOException, OneWireException {

		// select the device
		if (!this.ib.adapter.select(this.ib.address)) {
			this.forceVerify();

			throw new OneWireIOException("Device select failed");
		}

		// build block to send
		var raw_buf = new byte[6];

		raw_buf[0] = this.COPY_SCRATCHPAD_COMMAND;
		raw_buf[1] = (byte) (startAddr & 0xFF);
		raw_buf[2] = (byte) ((startAddr & 0xFFFF) >>> 8 & 0xFF);
		raw_buf[3] = (byte) (startAddr + len - 1 & 0x1F);

		System.arraycopy(this.ffBlock, 0, raw_buf, 4, 2);

		// send block (check copy indication complete)
		this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length);

		if ((byte) (raw_buf[raw_buf.length - 1] & 0x0F0) != (byte) 0xA0
				&& (byte) (raw_buf[raw_buf.length - 1] & 0x0F0) != (byte) 0x50) {
			this.forceVerify();

			throw new OneWireIOException("Copy scratchpad complete not found");
		}
	}
}
// CHECKSTYLE:ON
