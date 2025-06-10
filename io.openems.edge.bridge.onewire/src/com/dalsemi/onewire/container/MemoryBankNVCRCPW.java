// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Maxim Integrated Products, All Rights Reserved.
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
 * Memory bank class for the NVRAM with built-in CRC generation and Password
 * protected memory read/write iButtons and 1-Wire Devices. An example of such a
 * devices is the DS1922 Thermochron with 8k or password protected log memory.
 *
 * @version 1.00, 11 Feb 2002
 * @author SH
 */
@SuppressWarnings({ "unused" })
public class MemoryBankNVCRCPW extends MemoryBankNVCRC {
	/**
	 * Read Memory (with CRC and Password) Command
	 */
	public static final byte READ_MEMORY_CRC_PW_COMMAND = (byte) 0x69;

	/**
	 * Scratchpad with Password. Used as container for password.
	 */
	protected MemoryBankScratchCRCPW scratchpadPW = null;

	/**
	 * Password Container to access the passwords for the memory bank.
	 */
	protected PasswordContainer ibPass = null;

	/**
	 * Enable Provided Power for some Password checking.
	 */
	public boolean enablePower = false;

	/**
	 * Memory bank contstuctor. Requires reference to the OneWireContainer this
	 * memory bank resides on.
	 */
	public MemoryBankNVCRCPW(PasswordContainer ibutton, MemoryBankScratchCRCPW scratch) {
		super((OneWireContainer) ibutton, scratch);

		this.ibPass = ibutton;

		// initialize attributes of this memory bank
		this.pageAutoCRC = true;
		this.readContinuePossible = true;
		this.numVerifyBytes = 0;

		this.scratchpadPW = scratch;
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
	@Override
	protected void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo,
			int extraLength) throws OneWireIOException, OneWireException {
		var last_crc = 0;
		byte[] raw_buf;
		byte temp;

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
			raw_buf = new byte[11];
			raw_buf[0] = READ_MEMORY_CRC_PW_COMMAND;

			var addr = page * this.pageLength + this.startPhysicalAddress;

			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

			if (this.ibPass.isContainerReadWritePasswordSet()) {
				this.ibPass.getContainerReadWritePassword(raw_buf, 3);
			} else {
				this.ibPass.getContainerReadOnlyPassword(raw_buf, 3);
			}

			// perform CRC16 on first part (without the password)
			last_crc = CRC16.compute(raw_buf, 0, 3, last_crc);

			// do the first block for command, TA1, TA2, and password

			if (this.enablePower) {
				this.ib.adapter.dataBlock(raw_buf, 0, 10);

				this.ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);

				this.ib.adapter.putByte(raw_buf[10]);

			} else {
				this.ib.adapter.dataBlock(raw_buf, 0, 11);
			}
		} else if (this.enablePower) {
			this.ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);
			temp = (byte) this.ib.adapter.getByte();
		}

		if (this.enablePower) {
			msWait(3);

			this.ib.adapter.setPowerNormal();
		}

		// pre-fill with 0xFF
		raw_buf = new byte[this.pageLength + extraLength + 2 + this.numVerifyBytes];

		System.arraycopy(this.ffBlock, 0, raw_buf, 0, raw_buf.length);

		// send block to read data + extra info? + crc
		if (this.enablePower) {
			this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length - 1);
			last_crc = CRC16.compute(raw_buf, 0, raw_buf.length - this.numVerifyBytes - 2, last_crc);

			if ((last_crc & 0x0FF) != (~raw_buf[raw_buf.length - 2] & 0x0FF)) {
				this.sp.forceVerify();
				throw new OneWireIOException("Invalid CRC16 read from device.  Password may be incorrect.");
			}
		} else {
			this.ib.adapter.dataBlock(raw_buf, 0, raw_buf.length);

			// check the CRC
			if (CRC16.compute(raw_buf, 0, raw_buf.length - this.numVerifyBytes, last_crc) != 0x0000B001) {
				this.sp.forceVerify();
				throw new OneWireIOException("Invalid CRC16 read from device.  Password may be incorrect.");
			}
		}

		// extract the page data
		System.arraycopy(raw_buf, 0, readBuf, offset, this.pageLength);

		// optional extract the extra info
		if (extraInfo != null) {
			System.arraycopy(raw_buf, this.pageLength, extraInfo, 0, extraLength);
		}
	}

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
		var raw_buf = new byte[64];

		// attempt to put device at max desired speed
		if (!readContinue) {
			this.sp.checkSpeed();
		}

		// check if read exceeds memory
		if (startAddr + len > this.size) {
			throw new OneWireException("Read exceeds memory bank end");
		}

		// see if need to access the device
		if (!readContinue) {

			// select the device
			if (!this.ib.adapter.select(this.ib.address)) {
				this.sp.forceVerify();

				throw new OneWireIOException("Device select failed");
			}

			// build start reading memory block
			var addr = startAddr + this.startPhysicalAddress;

			raw_buf[0] = READ_MEMORY_CRC_PW_COMMAND;

			raw_buf[1] = (byte) (addr & 0xFF);
			raw_buf[2] = (byte) ((addr & 0xFFFF) >>> 8 & 0xFF);

			if (this.ibPass.isContainerReadWritePasswordSet()) {
				this.ibPass.getContainerReadWritePassword(raw_buf, 3);
			} else {
				this.ibPass.getContainerReadOnlyPassword(raw_buf, 3);
			}

			// do the first block for command, address. password
			if (this.enablePower) {
				this.ib.adapter.dataBlock(raw_buf, 0, 10);
				this.ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);
				this.ib.adapter.putByte(raw_buf[10]);

				msWait(10);

				this.ib.adapter.setPowerNormal();
			} else {
				this.ib.adapter.dataBlock(raw_buf, 0, 11);
			}
		}

		// pre-fill readBuf with 0xFF
		var startOffset = startAddr % this.pageLength;

		// First, count how many bytes are leftover from the
		// whole page reads.
		var bytesLeftover = len % this.pageLength;

		// Second, account for the whole number of pages
		var pgs = len / this.pageLength;

		// Third, if read starts in middle, go ahead and count
		// that whole page as 1 more page.
		if (startOffset > 0) {
			pgs += 1;
			// subtract the bytes from this extra page from our
			// leftover bytes, since we just accounted for each
			// byte to the end of this first page.
			bytesLeftover -= this.pageLength - startOffset;
		}

		// Finally, account for 1 more page read if there are
		// any more left over bytes
		if (bytesLeftover > 0) {
			pgs += 1;
		}

		var startPage = startAddr / this.pageLength;

		for (i = 0; i < pgs; i++) {
			this.readPageCRC(startPage + i, false, raw_buf, 0, null, 0);

			if (i == 0) {
				System.arraycopy(raw_buf, startOffset, readBuf, offset, this.pageLength - startOffset);
			} else if (i == pgs - 1 && bytesLeftover != 0) {
				System.arraycopy(raw_buf, 0, readBuf,
						offset + this.pageLength - startOffset + (i - 1) * this.pageLength, bytesLeftover);
			} else {
				System.arraycopy(raw_buf, 0, readBuf,
						offset + this.pageLength - startOffset + (i - 1) * this.pageLength, this.pageLength);
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
		// find the last (non-inclusive) address for this write
		var endingOffset = startAddr + len;
		if ((endingOffset & 0x1F) > 0 && !this.enablePower) {
			// find the number of bytes left until the end of the page
			var numBytes = this.pageLength - (endingOffset & 0x1F);
			if ( // endingOffset == 0x250 ???? why??
			this.ibPass.hasReadWritePassword()
					&& (0xFFE0 & endingOffset) == (0xFFE0 & this.ibPass.getReadWritePasswordAddress())
					&& endingOffset < this.ibPass.getReadWritePasswordAddress()
							+ this.ibPass.getReadWritePasswordLength()
					|| this.ibPass.hasReadOnlyPassword()
							&& (0xFFE0 & endingOffset) == (0xFFE0 & this.ibPass.getReadOnlyPasswordAddress())
							&& endingOffset < this.ibPass.getReadOnlyPasswordAddress()
									+ this.ibPass.getReadOnlyPasswordLength()
					|| this.ibPass.hasWriteOnlyPassword()
							&& (0xFFE0 & endingOffset) == (0xFFE0 & this.ibPass.getWriteOnlyPasswordAddress())
							&& endingOffset < this.ibPass.getWriteOnlyPasswordAddress()
									+ this.ibPass.getWriteOnlyPasswordLength()) {

				// password block would be written to with potentially bad data
				throw new OneWireException("""
						Executing write would overwrite password control registers with \
						potentially invalid data.  Please ensure write does not occur over \
						password control register page, or the password control data is \
						specified exactly in the write buffer.""");
			}

			var tempBuf = new byte[len + numBytes];
			System.arraycopy(writeBuf, offset, tempBuf, 0, len);
			this.read(endingOffset, false, tempBuf, len, numBytes);

			super.write(startAddr, tempBuf, 0, tempBuf.length);
		} else {
			// write does extend to end of page
			super.write(startAddr, writeBuf, offset, len);
		}

	}

	/**
	 * helper method to pause for specified milliseconds
	 */
	private static final void msWait(final long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {

		}
	}
}
// CHECKSTYLE:ON
