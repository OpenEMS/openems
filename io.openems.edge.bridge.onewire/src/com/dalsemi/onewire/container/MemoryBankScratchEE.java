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

/**
 * Memory bank class for the Scratchpad section of EEPROM iButtons and 1-Wire
 * devices.
 *
 * @version 0.00, 28 Aug 2000
 * @author DS
 */
class MemoryBankScratchEE extends MemoryBankScratch {
	/**
	 * Copy Scratchpad Delay length
	 */
	protected byte COPY_DELAY_LEN;

	/**
	 * Mask for ES byte during copy scratchpad
	 */
	protected byte ES_MASK;

	/**
	 * Number of bytes to read for verification (only last one will be checked).
	 */
	protected int numVerificationBytes = 1;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankScratchEE(OneWireContainer ibutton) {
		super(ibutton);

		// default copy scratchpad delay
		this.COPY_DELAY_LEN = (byte) 5;

		// default ES mask for copy scratchpad
		this.ES_MASK = 0;
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

		// select the device
		if (!this.ib.adapter.select(this.ib.address)) {
			this.forceVerify();

			throw new OneWireIOException("Device select failed");
		}

		// build block to send
		var raw_buf = new byte[37];

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
		var raw_buf = new byte[3];

		raw_buf[0] = this.COPY_SCRATCHPAD_COMMAND;
		raw_buf[1] = (byte) (startAddr & 0xFF);
		raw_buf[2] = (byte) ((startAddr & 0xFFFF) >>> 8 & 0xFF);

		// send block (command, address)
		this.ib.adapter.dataBlock(raw_buf, 0, 3);

		try {

			// setup strong pullup
			this.ib.adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
			this.ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);

			// send the offset and start power delivery
			this.ib.adapter.putByte((byte) (startAddr + len - 1 & this.pageLength - 1) | this.ES_MASK);

			// delay for ms
			Thread.sleep(this.COPY_DELAY_LEN);

			// disable power
			this.ib.adapter.setPowerNormal();

			// check if complete
			byte rslt = 0;
			if (this.numVerificationBytes == 1) {
				rslt = (byte) this.ib.adapter.getByte();
			} else {
				raw_buf = new byte[this.numVerificationBytes];
				this.ib.adapter.getBlock(raw_buf, 0, this.numVerificationBytes);
				rslt = raw_buf[this.numVerificationBytes - 1];
			}

			if ((byte) (rslt & 0x0F0) != (byte) 0xA0 && (byte) (rslt & 0x0F0) != (byte) 0x50) {
				this.forceVerify();

				throw new OneWireIOException("Copy scratchpad complete not found");
			}
		} catch (InterruptedException e) {
		}
	}
}
// CHECKSTYLE:ON
