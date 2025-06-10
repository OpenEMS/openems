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

// ResponseAPDU.java
package com.dalsemi.onewire.container;

/**
 * A <code>ResponseAPDU</code> represents an Application Protocol Data Unit
 * (APDU) received from the smart card in response to a previous
 * <code>CommandAPDU</code>. A <code>ResponseAPDU</code> consists of an optional
 * body and a mandatory Status Word (SW). <BR>
 * <BR>
 *
 * According to ISO 7816-4, a <code>ResponseAPDU</code> has the following
 * format:
 *
 * <pre>
 *          DATA   |  STATUS WORD
 *         [data]  |  SW1     SW2
 * </pre>
 *
 * where
 * <ul>
 * <li><code>data</code> is an optional byte array of data received from the
 * smart card.
 * <li><code>SW1</code> is the status byte 1 containing command processing
 * status.
 * <li><code>SW2</code> is the status byte 2 containing command processing
 * qualifier.
 * </ul>
 *
 *
 * <H3>Usage</H3>
 * <OL>
 * <LI><code><pre>
 *   byte[] buffer = {(byte)0x01, (byte)0x02, (byte)0x90, (byte)0x00};
 *   ResponseAPDU rapdu = new ResponseAPDU(buffer); </pre></code>
 * <LI><code><pre>
 *   OneWireContainer16 owc16 = new OneWireContainer16(adapter, address);
 *   byte[] buffer = {(byte)0x90, (byte)0x00, (byte)0x00, (byte)0x00,
 *                    (byte)0x01, (byte)0x02, (byte)0x03};
 *   CommandAPDU capdu = new CommandAPDU(buffer);
 *   ResponseAPDU rapdu = owc16.sendAPDU(capdu, runTime); </pre></code>
 * </OL>
 *
 * <H3>Additional information</H3>
 * <DL>
 * <DD><A HREF="http://www.opencard.org"> http://www.opencard.org</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.container.CommandAPDU
 * @see com.dalsemi.onewire.container.OneWireContainer16
 *
 * @version 0.00, 28 Aug 2000
 * @author YL
 *
 */
public class ResponseAPDU {

	/** byte array containing the entire <code>ResponseAPDU</code> */
	protected byte[] apduBuffer = null;

	/**
	 * length of this <code>ResponseAPDU</code> currently in the
	 * <code>apduBuffer</code>
	 */
	protected int apduLength;

	/**
	 * Constructs a new <code>ResponseAPDU</code> with the given buffer byte array.
	 * The internal <code>apduLength</code> is set to the length of the buffer
	 * passed.
	 *
	 * @param buffer the byte array with data for the internal
	 *               <code>apduBuffer</code>
	 *
	 * @throws RuntimeException thrown when <code>buffer</code> length <
	 *                          <code>2</code>.
	 *
	 * @see CommandAPDU
	 */
	public ResponseAPDU(byte[] buffer) {
		if (buffer.length < 2) {
			throw new RuntimeException("invalid ResponseAPDU, " + "length must be at least 2 bytes");
		}

		this.apduLength = buffer.length;
		this.apduBuffer = new byte[this.apduLength];

		System.arraycopy(buffer, 0, this.apduBuffer, 0, this.apduLength);
	} // ResponseAPDU

	/**
	 * Gets the data field of this <code>ResponseAPDU</code>.
	 *
	 * @return a byte array containing this <code>ResponseAPDU</code> data field
	 */
	public byte[] getData() {
		if (this.apduLength > 2) {
			var data = new byte[this.apduLength - 2];

			System.arraycopy(this.apduBuffer, 0, data, 0, this.apduLength - 2);

			return data;
		}
		return null;
	} // data

	/**
	 * Gets the value of SW1 and SW2 as an integer. It is computed as:<BR>
	 * <BR>
	 * <code>(((SW1 << 8) & 0xFF00) | (SW2 & 0xFF))</code><BR>
	 *
	 * @return <code>(((SW1 << 8) & 0xFF00) | (SW2 & 0xFF))</code> as an integer
	 */
	public final int getSW() {
		return this.getSW1() << 8 & 0xFF00 | this.getSW2() & 0xFF;
	} // getSW

	/**
	 * Gets the value of SW1.
	 *
	 * @return value of SW1 as a byte
	 */
	public final byte getSW1() {
		return this.apduBuffer[this.apduLength - 2];
	} // getSW1

	/**
	 * Gets the value of SW2.
	 *
	 * @return value of SW2 as a byte
	 */
	public final byte getSW2() {
		return this.apduBuffer[this.apduLength - 1];
	} // getSW2

	/**
	 * Gets the byte value at the specified offset in <code>apduBuffer</code>.
	 *
	 * @param index the offset in the <code>apduBuffer</code>
	 * @return the value at the given offset, or <code>-1</code> if the offset is
	 *         invalid
	 *
	 * @see #getBytes
	 * @see #getLength
	 */
	final public byte getByte(int index) {
		if (index >= this.apduLength) {
			return (byte) -1; // read beyond end of ResponseAPDU
		}

		return this.apduBuffer[index];
	} // getByte

	/**
	 * Gets a byte array holding this <code>ResponseAPDU</code>
	 * <code>apduBuffer</code>.
	 *
	 * @return <code>apduBuffer</code> copied into a new array
	 *
	 * @see #getByte
	 * @see #getLength
	 */
	final public byte[] getBytes() {
		var apdu = new byte[this.apduLength];

		System.arraycopy(this.apduBuffer, 0, apdu, 0, this.apduLength);

		return apdu;
	} // getBytes

	/**
	 * Gets the length of <code>apduBuffer</code>.
	 *
	 * @return <code>apduLength</code> the length of the <code>apduBuffer</code>
	 *         currently stored
	 */
	final public int getLength() {
		return this.apduLength;
	} // getLength

	/**
	 * Gets a string representation of this <code>ResponseAPDU</code>.
	 *
	 * @return a string describing this <code>ResponseAPDU</code>
	 */
	@Override
	public String toString() {
		var apduString = new StringBuilder();

		if (this.apduLength > 2) {
			var dataBuffer = new byte[this.apduLength - 2];

			dataBuffer = this.getData();
			apduString.append("DATA = ");

			for (byte element : dataBuffer) {

				// make hex String representation of byte array
				if ((element & 0xFF) < 0x10) {
					apduString.append('0');
				}

				apduString.append(Integer.toHexString(element & 0xFF)).append(" ");
			}

			apduString.append(" | ");
		}

		apduString.append("SW1 = ");

		if ((this.getSW1() & 0xFF) < 0x10) {
			apduString.append('0');
		}

		apduString.append(Integer.toHexString(this.getSW1() & 0xFF));
		apduString.append(", SW2 = ");

		if ((this.getSW2() & 0xFF) < 0x10) {
			apduString.append('0');
		}

		apduString.append(Integer.toHexString(this.getSW2() & 0xFF));

		return apduString.toString().toUpperCase();
	} // toString
}
// CHECKSTYLE:ON
