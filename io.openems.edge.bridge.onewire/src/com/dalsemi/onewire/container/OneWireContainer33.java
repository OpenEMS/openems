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

import java.util.Enumeration;
// imports
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;

/**
 * <P>
 * 1-Wire&reg; container for the '1K-Bit protected 1-Wire EEPROM with SHA-1
 * Engine' family type <B>33</B> (hex), Maxim Integrated Products part number:
 * <B>DS1961S,DS2432</B>.
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>1128 bits of 5V EEPROM memory partitioned into four pages of 256 bits, a
 * 64-bit write-only secret and up to 5 general purpose read/write registers.
 * <LI>On-chip 512-bit SHA-1 engine to compute 160-bit Message Authentication
 * Codes (MAC) and to generate secrets.
 * <LI>Write access requires knowledge of the secret and the capability of
 * computing and transmitting a 160-bit MAC as authorization.
 * <LI>Secret and data memory can be write-protected (all or page 0 only) or put
 * in EPROM-emulation mode ("write to 0", page0)
 * <LI>unique, fatory-lasered and tested 64-bit registration number (8-bit
 * family code + 48-bit serial number + 8-bit CRC tester) assures absolute
 * traceability because no two parts are alike.
 * <LI>Built-in multidrop controller ensures compatibility with other 1-Wire net
 * products.
 * <LI>Reduces control, address, data and power to a single data pin.
 * <LI>Directly connects to a single port pin of a microprocessor and
 * communicates at up to 16.3k bits per second.
 * <LI>Overdrive mode boosts communication speed to 142k bits per second.
 * <LI>8-bit family code specifies DS2432 communication requirements to reader.
 * <LI>Presence detector acknowledges when reader first applies voltage.
 * <LI>Low cost 6-lead TSOC surface mount package, or solder-bumped chip scale
 * package.
 * <LI>Reads and writes over a wide voltage range of 2.8V to 5.25V from -40C to
 * +85C.
 * </UL>
 *
 * <P>
 * The memory can also be accessed through the objects that are returned from
 * the {@link #getMemoryBanks() getMemoryBanks} method.
 * </P>
 *
 * The following is a list of the MemoryBank instances that are returned:
 *
 * <UL>
 * <LI><B> Page Zero with write protection</B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 32 starting at physical address 0
 * <LI><I> Features</I> Read/Write general-purpose non-volatile
 * <LI><I> Page</I> 1 page of length 32 bytes giving 29 bytes Packet data
 * payload
 * <LI><I> Page Features </I> page-device-CRC and write protection.
 * </UL>
 * <LI><B> Page One with EPROM mode and write protection </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 32 starting at physical address 32
 * <LI><I> Features</I> Read/Write general-purpose non-volatile
 * <LI><I> Page</I> 1 page of length 32 bytes giving 29 bytes Packet data
 * payload
 * <LI><I> Page Features </I> page-device-CRC, EPROM mode and write protection.
 * </UL>
 * <LI><B> Page Two and Three with write protection </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 64 starting at physical address 64
 * <LI><I> Features</I> Read/Write general-purpose non-volatile
 * <LI><I> Pages</I> 2 pages of length 32 bytes giving 29 bytes Packet data
 * payload
 * <LI><I> Page Features </I> page-device-CRC and write protection.
 * </UL>
 * <LI><B> Status Page that contains the secret and the status. </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 24 starting at physical address 128
 * <LI><I> Page Features </I> Contains secret and status for the iButton.
 * </UL>
 * </UL>
 *
 * <DD>
 * <H4>Example 1</H4> Display some features of isMACValid where owd is an
 * instanceof OneWireContainer33 and bank is an instanceof PagedMemoryBank:
 *
 * <PRE>
 *  <CODE>
 *  byte[] read_buf  = new byte [bank.getPageLength()];
 *  byte[] extra_buf = new byte [bank.getExtraInfoLength()];
 *  byte[] challenge = new byte [8];
 *
 *  // read a page (use the most verbose and secure method)
 *  if (bank.hasPageAutoCRC())
 *  {
 *     System.out.println("Using device generated CRC");
 *
 *     if (bank.hasExtraInfo())
 *     {
 *        bank.readPageCRC(pg, false, read_buf, 0, extra_buf);
 *
 *        owd.getChallenge(challenge,0);
 *        owd.getContainerSecret(secret, 0);
 *        sernum = owd.getAddress();
 *        macvalid = owd.isMACValid(bank.getStartPhysicalAddress()+pg*bank.getPageLength(),
 *                                  sernum,read_buf,extra_buf,challenge,secret);
 *     }
 *     else
 *        bank.readPageCRC(pg, false, read_buf, 0);
 *  }
 *  else
 *  {
 *     if (bank.hasExtraInfo())
 *        bank.readPage(pg, false, read_buf, 0, extra_buf);
 *     else
 *        bank.readPage(pg, false, read_buf, 0);
 *  }
 * </CODE>
 * </PRE>
 *
 * <H3>DataSheet</H3>
 * <DL>
 * <DD><A HREF="http://pdfserv.maxim-ic.com/arpdf/DS2432.pdf">
 * http://pdfserv.maxim-ic.com/arpdf/DS2432.pdf</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.application.sha.SHAiButtonUser33
 * @version 0.00, 19 Dec 2000
 * @author JPE
 */
public class OneWireContainer33 extends OneWireContainer {

	// --------
	// -------- Static Final Variables
	// --------

	/** Private Secret */
	private final byte[] secret = new byte[8];

	/** Challenge to use for the Read Authenticate Methods */
	private final byte[] challenge = new byte[3];

	/** The different memory banks for the container. */
	private MemoryBankScratchSHAEE mbScratchpad;
	private MemoryBankSHAEE memstatus;
	private final MemoryBankSHAEE[] memoryPages = new MemoryBankSHAEE[4];

	/** Flag to indicate if the secret has been set. */
	protected boolean secretSet;

	/** Flag to indicate if the secret is write protected. */
	protected boolean secretProtected;

	/** Flag to indicate if the adapter has been specified. */
	protected boolean setAdapter;

	/** Flag to indicate if the status has been checked. */
	protected boolean container_check;

	/** block of 0xFF's used for faster read pre-fill of 1-Wire blocks */
	protected static final byte[] ffBlock = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	/** block of 0xFF's used for faster erase of blocks */
	protected static final byte[] zeroBlock = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/** This byte is used to set flags in the status register */
	private static final byte[] ACTIVATION_BYTE = { (byte) 0xAA };

	/**
	 * Default Constructor OneWireContainer33. Must call setupContainer before
	 * using.
	 */
	public OneWireContainer33() {
		System.arraycopy(ffBlock, 0, this.secret, 0, 8);
		System.arraycopy(ffBlock, 0, this.challenge, 0, 3);

		this.setAdapter = false;
		this.container_check = false;
	}

	/**
	 * Create a container with a provided adapter object and the address of the
	 * iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer33(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);

		System.arraycopy(ffBlock, 0, this.secret, 0, 8);
		System.arraycopy(ffBlock, 0, this.challenge, 0, 3);

		this.setAdapter = true;
		this.container_check = false;
		this.initmem();
	}

	/**
	 * Create a container with a provided adapter object and the address of the
	 * iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer33(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);

		System.arraycopy(ffBlock, 0, this.secret, 0, 8);
		System.arraycopy(ffBlock, 0, this.challenge, 0, 3);

		this.setAdapter = true;
		this.container_check = false;
		this.initmem();
	}

	/**
	 * Create a container with a provided adapter object and the address of the
	 * iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	public OneWireContainer33(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);

		System.arraycopy(ffBlock, 0, this.secret, 0, 8);
		System.arraycopy(ffBlock, 0, this.challenge, 0, 3);

		this.setAdapter = true;
		this.container_check = false;
		this.initmem();
	}

	// --------
	// -------- Methods
	// --------

	/**
	 * Tells whether an adapter has been set.
	 *
	 * @return boolean telling weather an adapter has been set.
	 */
	protected boolean adapterSet() {
		return this.setAdapter;
	}

	/**
	 * Provide this container the adapter object used to access this device and
	 * provide the address of this iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		if (!this.setAdapter) {
			this.initmem();
		}

		this.setAdapter = true;
	}

	/**
	 * Provide this container the adapter object used to access this device and
	 * provide the address of this iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, long newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		if (!this.setAdapter) {
			this.initmem();
		}

		this.setAdapter = true;
	}

	/**
	 * Provide this container the adapter object used to access this device and
	 * provide the address of this iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this
	 *                      iButton.
	 * @param newAddress    address of this 1-Wire device
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, String newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		if (!this.setAdapter) {
			this.initmem();
		}

		this.setAdapter = true;
	}

	/**
	 * Retrieve the Maxim Integrated Products part number of the iButton as a
	 * string. For example 'DS1992'.
	 *
	 * @return string representation of the iButton name.
	 */
	@Override
	public String getName() {
		return "DS1961S";
	}

	/**
	 * Retrieve the alternate Maxim Integrated Products part numbers or names. A
	 * 'family' of MicroLAN devices may have more than one part number depending on
	 * packaging.
	 *
	 * @return the alternate names for this iButton or 1-Wire device
	 */
	@Override
	public String getAlternateNames() {
		return "DS2432";
	}

	/**
	 * Retrieve a short description of the function of the iButton type.
	 *
	 * @return string representation of the function description.
	 */
	@Override
	public String getDescription() {
		return "1K-Bit protected 1-Wire EEPROM with SHA-1 Engine.";
	}

	/**
	 * Returns the maximum speed this iButton can communicate at.
	 *
	 * @return max. communication speed.
	 */
	@Override
	public int getMaxSpeed() {
		return DSPortAdapter.SPEED_OVERDRIVE;
	}

	/**
	 * Get an enumeration of memory bank instances that implement one or more of the
	 * following interfaces: {@link com.dalsemi.onewire.container.MemoryBank
	 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
	 * PagedMemoryBank}, and {@link com.dalsemi.onewire.container.OTPMemoryBank
	 * OTPMemoryBank}.
	 *
	 * @return <CODE>Enumeration</CODE> of memory banks
	 */
	@Override
	public Enumeration<MemoryBank> getMemoryBanks() {
		var bank_vector = new Vector<MemoryBank>(4);

		bank_vector.addElement(this.mbScratchpad);
		bank_vector.addElement(this.memoryPages[0]);
		bank_vector.addElement(this.memoryPages[1]);
		bank_vector.addElement(this.memoryPages[2]);
		bank_vector.addElement(this.memstatus);

		return bank_vector.elements();
	}

	/**
	 * Returns the instance of the Scratchpad memory bank. Contains methods for
	 * reading/writing the Scratchpad contents. Also, methods for Load First Secret,
	 * Compute Next Secret, and Refresh Scratchpad
	 *
	 * @return the instance of the Scratchpad memory bank
	 */
	public MemoryBankScratchSHAEE getScratchpadMemoryBank() {
		return this.mbScratchpad;
	}

	/**
	 * Returns the instance of the Status page memory bank.
	 *
	 * @return the instance of the Status page memory bank
	 */
	public MemoryBankSHAEE getStatusPageMemoryBank() {
		return this.memstatus;
	}

	/**
	 * Returns the instance of the memory bank for a particular page
	 *
	 * @param page the page for the requested memory bank;
	 *
	 * @return the instance of the memory bank for the specified page
	 */
	public MemoryBankSHAEE getMemoryBankForPage(int page) {
		if (page == 3) {
			page = 2;
		}
		return this.memoryPages[page];
	}

	/**
	 * Sets the bus master secret for this DS2432.
	 *
	 * @param newSecret Secret for this DS2432.
	 * @param offset    index into array to copy the secret from
	 */
	public void setContainerSecret(byte[] newSecret, int offset) {
		System.arraycopy(newSecret, offset, this.secret, 0, 8);
		this.secretSet = true;
	}

	/**
	 * Get the secret of this device as an array of bytes.
	 *
	 * @param secretBuf array of bytes for holding the container secret
	 * @param offset    index into array to copy the secret to
	 */
	public void getContainerSecret(byte[] secretBuf, int offset) {
		System.arraycopy(this.secret, 0, secretBuf, offset, 8);
	}

	/**
	 * Get the current status of the secret.
	 *
	 * @return boolean telling if the secret is set
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean isContainerSecretSet() throws OneWireIOException, OneWireException {
		// if(!container_check)
		// container_check = this.checkStatus();

		return this.secretSet;
	}

	/**
	 * Get the status of the secret, if it is write protected.
	 *
	 * @return boolean telling if the secret is write protected.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean isSecretWriteProtected() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		return this.secretProtected;
	}

	/**
	 * Sets the challenge for the Read Authenticate Page
	 *
	 * @param challengeset Challenge for all the memory banks.
	 */
	public void setChallenge(byte[] challengeset, int offset) {
		System.arraycopy(challengeset, offset, this.challenge, 0, 3);
	}

	/**
	 * Get the challenge of this device as an array of bytes.
	 *
	 * @param get array of bytes containing the iButton challenge
	 */
	public void getChallenge(byte[] get, int offset) {
		System.arraycopy(this.challenge, 0, get, offset, 3);
	}

	/**
	 * Get the status of all the pages, if they are write protected.
	 *
	 * @return boolean telling if all the pages are write protected.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean isWriteProtectAllSet() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		return this.memoryPages[2].readOnly;
	}

	/**
	 * Write protects the secret for the DS2432.
	 */
	public void writeProtectSecret() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		// write protect secret
		// - write status byte to address
		// ((8h) + physical) = 88h
		this.memstatus.write(8, ACTIVATION_BYTE, 0, 1);

		this.secretProtected = true;
	}

	/**
	 * Write protect pages 0 to 3
	 */
	public void writeProtectAll() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		// write protect all pages
		// - write status byte to address
		// ((9h) + physical) = 89h
		this.memstatus.write(9, ACTIVATION_BYTE, 0, 1);

		this.memoryPages[0].writeprotect();
		this.memoryPages[1].writeprotect();
		this.memoryPages[2].writeprotect();
	}

	/**
	 * Sets the EPROM mode for page 1. After setting, Page One can only be written
	 * to once.
	 */
	public void setEPROMModePageOne() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		// Enable EPROM mode for page 1.
		// - write status byte to address
		// ((12h) + physical) = 8Ch
		this.memstatus.write(12, ACTIVATION_BYTE, 0, 1);

		this.memoryPages[1].setEPROM();
	}

	/**
	 * Tells if page one is in EPROM mode.
	 *
	 * @return boolean telling if page one is in EPROM mode.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean isPageOneEPROMmode() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		return this.memoryPages[1].isWriteOnce();
	}

	/**
	 * Write protect page zero only.
	 */
	public void writeProtectPageZero() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		// Enable Write protection for page zero.
		// - write status byte to address
		// ((13h) + physical) = 8Dh
		this.memstatus.write(13, ACTIVATION_BYTE, 0, 1);

		this.memoryPages[0].writeprotect();
	}

	/**
	 * Get the status of page zero, if it is write protected.
	 *
	 * @return boolean telling if page zero is write protected.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean isWriteProtectPageZeroSet() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		return this.memoryPages[0].isReadOnly();
	}

	/**
	 * Compute Next Secret
	 *
	 * @param addr         address of page to use for the next secret computation.
	 * @param parialsecret the data to put into the scrathpad in computing next
	 *                     secret.
	 */
	public void computeNextSecret(int pageNum, byte[] partialsecret, int offset)
			throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		this.mbScratchpad.computeNextSecret(pageNum * 32, partialsecret, offset);
	}

	/**
	 * Compute Next Secret using the current contents of data page and scratchpad.
	 *
	 * @param addr address of page to use for the next secret computation.
	 */
	public void computeNextSecret(int pageNum) throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		this.mbScratchpad.computeNextSecret(pageNum * 32);
	}

	/**
	 * Load First Secret
	 *
	 * @return boolean saying if first secret was loaded
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean loadFirstSecret(byte[] data, int offset) throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		this.mbScratchpad.loadFirstSecret(0x080, data, offset);
		return true;
	}

	/**
	 * Refreshes a particular 8-byte set of data on a given page. This will correct
	 * any weakly-programmed EEPROM bits. This feature is only available on the
	 * DS1961S, but is safely ignored on the DS2432. The refresh consists of a
	 * Refresh Scratchpad command followed by a Load First Secret to the same
	 * offset.
	 *
	 * @param page   the page number that contains the 8-bytes to refresh.
	 * @param offset the offset into the page for the 8-bytes to refresh.
	 *
	 * @return <code>true</code> if refresh is successful.
	 */
	public boolean refreshPage(int page, int offset) throws OneWireException, OneWireIOException {
		if (!this.container_check) {
			this.container_check = this.checkStatus();
		}

		var addr = page * 32 + offset;
		try {
			this.mbScratchpad.refreshScratchpad(addr);
		} catch (OneWireException owe) {
			// only return false for the DS2432 devices
			// which do not support this command
			return false;
		}

		this.mbScratchpad.loadFirstSecret(addr);
		return true;
	}

	/**
	 * Refreshes all 32 bytes of data on a given page. This will correct any
	 * weakly-programmed EEPROM bits. This feature is only available on the DS1961S,
	 * but is safely ignored on the DS2432. The refresh consists of a Refresh
	 * Scratchpad command followed by a Load First Secret to the same offset, for
	 * all 8-byte offsets on the page.
	 *
	 * @param page the page number that will be refreshed.
	 *
	 * @return <code>true</code> if refresh is successful.
	 */
	public boolean refreshPage(int page) throws OneWireException, OneWireIOException {
		return this.refreshPage(page, 0) && this.refreshPage(page, 8) && this.refreshPage(page, 16)
				&& this.refreshPage(page, 24);
	}

	/**
	 * To check the status of the part.
	 *
	 * @return boolean saying the part has been checked or was checked.
	 */
	protected boolean checkStatus() throws OneWireIOException, OneWireException {
		if (!this.container_check) {
			var mem = new byte[8];

			this.memstatus.read(8, false, mem, 0, 8);

			if (mem[0] == (byte) 0xAA || mem[0] == (byte) 0x55) {
				this.secretProtected = true;
			} else {
				this.secretProtected = false;
			}

			if (mem[1] == (byte) 0xAA || mem[1] == (byte) 0x55 || mem[5] == (byte) 0xAA || mem[5] == (byte) 0x55) {
				this.memoryPages[0].readWrite = false;
				this.memoryPages[0].readOnly = true;
			} else {
				this.memoryPages[0].readWrite = true;
				this.memoryPages[0].readOnly = false;
			}

			if (mem[4] == (byte) 0xAA || mem[4] == (byte) 0x55) {
				this.memoryPages[1].writeOnce = true;
			} else {
				this.memoryPages[1].writeOnce = false;
			}

			if (mem[1] == (byte) 0xAA || mem[1] == (byte) 0x55) {
				this.memoryPages[1].readWrite = false;
				this.memoryPages[1].readOnly = true;
			} else {
				this.memoryPages[1].readWrite = true;
				this.memoryPages[1].readOnly = false;
			}

			if (mem[1] == (byte) 0xAA || mem[1] == (byte) 0x55) {
				this.memoryPages[2].readWrite = false;
				this.memoryPages[2].readOnly = true;
			} else {
				this.memoryPages[2].readWrite = true;
				this.memoryPages[2].readOnly = false;
			}

			this.memstatus.checked = true;
			this.memoryPages[0].checked = true;
			this.memoryPages[1].checked = true;
			this.memoryPages[2].checked = true;
			this.container_check = true;
		}

		return this.container_check;
	}

	/**
	 * Initialize the memory banks and data associated with each.
	 */
	private void initmem() {
		this.secretSet = false;

		this.mbScratchpad = new MemoryBankScratchSHAEE(this);

		// Set memory bank variables for the status and secret page
		this.memstatus = new MemoryBankSHAEE(this, this.mbScratchpad);
		this.memstatus.bankDescription = "Status Page that contains the secret and the status.";
		this.memstatus.generalPurposeMemory = true;
		this.memstatus.startPhysicalAddress = 128;
		this.memstatus.size = 32;
		this.memstatus.numberPages = 1;
		this.memstatus.pageLength = 32;
		this.memstatus.maxPacketDataLength = 32 - 3;
		this.memstatus.extraInfo = false;
		this.memstatus.extraInfoLength = 20;
		this.memstatus.readWrite = false;
		this.memstatus.writeOnce = false;
		this.memstatus.pageCRC = false;
		this.memstatus.readOnly = false;
		this.memstatus.checked = false;

		// Set memory bank variables
		this.memoryPages[0] = new MemoryBankSHAEE(this, this.mbScratchpad);
		this.memoryPages[0].bankDescription = "Page Zero with write protection.";
		this.memoryPages[0].generalPurposeMemory = true;
		this.memoryPages[0].startPhysicalAddress = 0;
		this.memoryPages[0].size = 32;
		this.memoryPages[0].numberPages = 1;
		this.memoryPages[0].pageLength = 32;
		this.memoryPages[0].maxPacketDataLength = 32 - 3;
		this.memoryPages[0].extraInfo = true;
		this.memoryPages[0].extraInfoLength = 20;
		this.memoryPages[0].writeOnce = false;
		this.memoryPages[0].pageCRC = true;
		this.memoryPages[0].checked = false;

		// Set memory bank variables
		this.memoryPages[1] = new MemoryBankSHAEE(this, this.mbScratchpad);
		this.memoryPages[1].bankDescription = "Page One with EPROM mode and write protection.";
		this.memoryPages[1].generalPurposeMemory = true;
		this.memoryPages[1].startPhysicalAddress = 32;
		this.memoryPages[1].size = 32;
		this.memoryPages[1].numberPages = 1;
		this.memoryPages[1].pageLength = 32;
		this.memoryPages[1].maxPacketDataLength = 32 - 3;
		this.memoryPages[1].extraInfo = true;
		this.memoryPages[1].extraInfoLength = 20;
		this.memoryPages[1].pageCRC = true;
		this.memoryPages[1].checked = false;

		// Set memory bank variables
		this.memoryPages[2] = new MemoryBankSHAEE(this, this.mbScratchpad);
		this.memoryPages[2].bankDescription = "Page Two and Three with write protection.";
		this.memoryPages[2].generalPurposeMemory = true;
		this.memoryPages[2].startPhysicalAddress = 64;
		this.memoryPages[2].size = 64;
		this.memoryPages[2].numberPages = 2;
		this.memoryPages[2].pageLength = 32;
		this.memoryPages[2].maxPacketDataLength = 32 - 3;
		this.memoryPages[2].extraInfo = true;
		this.memoryPages[2].extraInfoLength = 20;
		this.memoryPages[2].writeOnce = false;
		this.memoryPages[2].pageCRC = true;
		this.memoryPages[2].checked = false;

		this.memoryPages[3] = this.memoryPages[2];
	}

	/**
	 * Authenticates page data given a MAC.
	 *
	 * @param addr      address of the data to be read
	 * @param memory    the memory read from the page
	 * @param mac       the MAC calculated for this function given back as the extra
	 *                  info
	 * @param challenge the 3 bytes written to the scratch pad used in calculating
	 *                  the mac
	 *
	 */
	public static boolean isMACValid(int addr, byte[] SerNum, byte[] memory, byte[] mac, byte[] challenge,
			byte[] secret) {
		var MT = new byte[64];

		System.arraycopy(secret, 0, MT, 0, 4);
		System.arraycopy(memory, 0, MT, 4, 32);
		System.arraycopy(ffBlock, 0, MT, 36, 4);

		MT[40] = (byte) (0x40 | addr << 3 & 0x08 | addr >>> 5 & 0x07);

		System.arraycopy(SerNum, 0, MT, 41, 7);
		System.arraycopy(secret, 4, MT, 48, 4);
		System.arraycopy(challenge, 0, MT, 52, 3);

		// finish up with proper padding
		MT[55] = (byte) 0x80;
		for (var i = 56; i < 62; i++) {
			MT[i] = (byte) 0x00;
		}
		MT[62] = (byte) 0x01;
		MT[63] = (byte) 0xB8;

		var AtoE = new int[5];
		com.dalsemi.onewire.utils.SHA.ComputeSHA(MT, AtoE);

		var cnt = 0;
		for (var i = 0; i < 5; i++) {
			var temp = AtoE[4 - i];
			for (var j = 0; j < 4; j++) {
				if (mac[cnt++] != (byte) (temp & 0x0FF)) {
					return false;
				}
				temp >>>= 8;
			}
		}

		return true;
	}

	/**
	 * <p>
	 * Installs a secret on a DS1961S/DS2432. The secret is written in partial
	 * phrases of 47 bytes (32 bytes to a memory page, 8 bytes to the scratchpad, 7
	 * bytes are discarded (but included for compatibility with DS193S)) and is
	 * cumulative until the entire secret is processed.
	 * </p>
	 *
	 * <p>
	 * On TINI, this method will be slightly faster if the secret's length is
	 * divisible by 47. However, since secret key generation is a part of
	 * initialization, it is probably not necessary.
	 * </p>
	 *
	 * @param page   the page number used to write the partial secrets to
	 * @param secret the entire secret, in partial phrases, to be installed
	 *
	 * @return <code>true</code> if successful
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #bindSecretToiButton(int,byte[])
	 */
	public boolean installMasterSecret(int page, byte[] newSecret) throws OneWireIOException, OneWireException {
		for (var i = 0; i < this.secret.length; i++) {
			this.secret[i] = 0x00;
		}
		if (!this.loadFirstSecret(this.secret, 0)) {
			throw new OneWireException("Load first secret failed");
		}
		if (newSecret.length == 0) {
			return false;
		}

		byte[] input_secret = null;
		var sp_buffer = new byte[8];
		var secret_mod_length = newSecret.length % 47;

		if (secret_mod_length == 0) {
			input_secret = newSecret;
		} else {
			input_secret = new byte[newSecret.length + 47 - secret_mod_length];

			System.arraycopy(newSecret, 0, input_secret, 0, newSecret.length);
		}

		for (var i = 0; i < input_secret.length; i += 47) {
			this.writeDataPage(page, input_secret, i);
			System.arraycopy(input_secret, i + 36, sp_buffer, 0, 8);
			this.mbScratchpad.computeNextSecret(page * 32, sp_buffer, 0);
		}
		return true;
	}

	/**
	 * <p>
	 * Binds an installed secret to a DS1961S/DS2432 by using well-known binding
	 * data and the DS1961S/DS2432's unique address. This makes the secret unique
	 * for this iButton.
	 * </p>
	 *
	 * @param page      the page number that has the master secret already installed
	 * @param bind_data 32 bytes of binding data used to bind the iButton to the
	 *                  system
	 *
	 * @return <code>true</code> if successful
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #installMasterSecret(int,byte[])
	 */
	public boolean bindSecretToiButton(int pageNum, byte[] bindData) throws OneWireIOException, OneWireException {
		if (!this.writeDataPage(pageNum, bindData)) {
			return false;
		}

		var bind_code = new byte[8];
		bind_code[0] = (byte) pageNum;
		System.arraycopy(this.address, 0, bind_code, 1, 7);
		this.mbScratchpad.computeNextSecret(pageNum * 32, bind_code, 0);

		return true;
	}

	/**
	 * <p>
	 * Writes a data page to the DS1961S/DS2432.
	 * </p>
	 *
	 * @param page_number page number to write
	 * @param page_data   page data to write (must be at least 32 bytes long)
	 *
	 * @return <code>true</code> if successful, <code>false</code> if the operation
	 *         failed
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public boolean writeDataPage(int targetPage, byte[] pageData) throws OneWireIOException, OneWireException {

		return this.writeDataPage(targetPage, pageData, 0);
	}

	/**
	 * <p>
	 * Writes a data page to the DS1961S/DS2432.
	 * </p>
	 *
	 * @param page_number page number to write
	 * @param page_data   page data to write (must be at least 32 bytes long)
	 * @param offset      the offset to start copying the 32-bytes of page data.
	 *
	 * @return <code>true</code> if successful, <code>false</code> if the operation
	 *         failed
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public boolean writeDataPage(int targetPage, byte[] pageData, int offset)
			throws OneWireIOException, OneWireException {
		var addr = 0;
		if (targetPage == 3) {
			addr = 32;
		}
		this.memoryPages[targetPage].write(addr, pageData, offset, 32);
		return true;
	}

	/**
	 * <p>
	 * Writes data to the scratchpad. In order to write to a data page using this
	 * method, next call <code>readScratchPad()</code>, and then
	 * <code>copyScratchPad()</code>. Note that the addresses passed to this method
	 * will be the addresses the data is copied to if the
	 * <code>copyScratchPad()</code> method is called afterward.
	 * </p>
	 *
	 * <p>
	 * Also note that if too many bytes are written, this method will truncate the
	 * data so that only a valid number of bytes will be sent.
	 * </p>
	 *
	 * @param targetPage       the page number this data will eventually be copied
	 *                         to
	 * @param targetPageOffset the offset on the page to copy this data to
	 * @param inputbuffer      the data that will be copied into the scratchpad
	 * @param start            offset into the input buffer for the data to write
	 * @param length           number of bytes to write
	 *
	 * @return <code>true</code> if successful, <code>false</code> on a CRC error
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public boolean writeScratchpad(int targetPage, int targetPageOffset, byte[] inputbuffer, int start, int length)
			throws OneWireIOException, OneWireException {
		var addr = (targetPage << 5) + targetPageOffset;
		this.mbScratchpad.writeScratchpad(addr, inputbuffer, start, length);
		return true;
	}

	/**
	 * Read from the Scratch Pad, which is a max of 8 bytes.
	 *
	 * @param scratchpad byte array to place read data into length of array is
	 *                   always pageLength.
	 * @param offset     offset into readBuf to pug data
	 * @param extraInfo  byte array to put extra info read into (TA1, TA2, e/s byte)
	 *                   Can be 'null' if extra info is not needed.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public void readScratchpad(byte[] scratchpad, int offset, byte[] extraInfo)
			throws OneWireIOException, OneWireException {
		this.mbScratchpad.readScratchpad(scratchpad, offset, 8, extraInfo);
	}

	/**
	 * Copy all 8 bytes of the Scratch Pad to a certain page and offset in memory.
	 *
	 * @param targetPage       the page to copy the data to
	 * @param targetPageOffset the offset into the page to copy to
	 * @param copy_auth        byte[] containing write authorization
	 * @param authStart        the offset into the copy_auth array where the
	 *                         authorization begins.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean copyScratchpad(int targetPage, int targetPageOffset, byte[] copy_auth, int authStart)
			throws OneWireIOException, OneWireException {
		var addr = (targetPage << 5) + targetPageOffset;
		this.mbScratchpad.copyScratchpadWithMAC(addr, copy_auth, authStart);
		return true;
	}

	/**
	 * Copy all 8 bytes of the Scratch Pad to a certain page and offset in memory.
	 *
	 * The container secret must be set so that the container can produce the
	 * correct MAC.
	 *
	 * @param targetPage       the page to copy the data to
	 * @param targetPageOffset the offset into the page to copy to
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean copyScratchpad(int targetPage, int targetPageOffset) throws OneWireIOException, OneWireException {
		var addr = (targetPage << 5) + targetPageOffset;
		this.mbScratchpad.copyScratchpad(addr, 8);

		return true;
	}

	/**
	 * Reads a page of memory..
	 *
	 * @param page     page number to read packet from
	 * @param pageData byte array to place read data into
	 * @param offset   offset into readBuf to place data
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public boolean readMemoryPage(int page, byte[] pageData, int offset) throws OneWireIOException, OneWireException {
		var addr = 0;
		if (page == 3) {
			addr = 32;
		}
		this.memoryPages[page].read(addr, false, pageData, offset, 32);
		return true;
	}

	/**
	 * <p>
	 * Reads and authenticates a page. See <code>readMemoryPage()</code> for a
	 * description of page numbers and their contents. This method will also
	 * generate a signature for the selected page, used in the authentication of
	 * roving (User) iButtons.
	 * </p>
	 *
	 * @param pageNum      page number to read and authenticate
	 * @param pagedata     array for the page data.
	 * @param offset       offset to copy into the array
	 * @param computed_mac array for the MAC returned by the device.
	 * @param macStart     offset to copy into the mac array
	 *
	 * @return <code>true</code> if successful, <code>false</code> if the operation
	 *         failed while waiting for the DS1963S's output to alternate
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	public boolean readAuthenticatedPage(int page, byte[] pagedata, int offset, byte[] computed_mac, int macStart)
			throws OneWireIOException, OneWireException {
		var mbPage = 0;
		if (page == 3) {
			mbPage = 1;
			page = 2;
		}
		return this.memoryPages[page].readAuthenticatedPage(mbPage, pagedata, offset, computed_mac, macStart);
	}
}
// CHECKSTYLE:ON
