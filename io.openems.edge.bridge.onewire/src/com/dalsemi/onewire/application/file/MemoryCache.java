// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 2001 Maxim Integrated Products, All Rights Reserved.
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

package com.dalsemi.onewire.application.file;

// imports
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.MemoryBank;
import com.dalsemi.onewire.container.OTPMemoryBank;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.PagedMemoryBank;
import com.dalsemi.onewire.utils.Bit;
import com.dalsemi.onewire.utils.CRC16;

/**
 * Class to provide read/write cache services to a 1-Wire memory device. Writes
 * are only performed when this classes <code>sync()</code> method is called.
 * Provides page bitmap services for OTP devices.
 *
 * <p>
 * Objectives:
 * <ul>
 * <li>Cache read/written pages
 * <li>write only on sync()
 * <li>write order is oldest to newest.
 * <li>Collect redirection information when appropriate
 * </ul>
 *
 * @author DS
 * @version 0.01, 1 June 2001
 * @see com.dalsemi.onewire.application.file.OWFile
 * @see com.dalsemi.onewire.application.file.OWFileDescriptor
 * @see com.dalsemi.onewire.application.file.OWFileInputStream
 * @see com.dalsemi.onewire.application.file.OWFileOutputStream
 */
class MemoryCache {

	// --------
	// -------- Static Final Variables
	// --------

	/** cache pageState's */
	private static final int NOT_READ = 0;
	private static final int READ_CRC = 1;
	private static final int READ_NO_CRC = 2;
	private static final int VERIFY = 3;
	private static final int REDIRECT = 4;
	private static final int WRITE = 5;

	/** Flag to indicate the writeLog entry is empty */
	private static final int EMPTY = -1;

	/** Field NONE - flag to indicate last page read is not known */
	private static final int NONE = -100;

	/** Field USED - flag to indicate page bitmap file used */
	private static final int USED = 0;

	/** Field NOT_USED - flag to indicated page bitmap file un-used */
	private static final int NOT_USED = 1;

	/** Enable/disable debug messages */
	private static final boolean doDebugMessages = false;

	// --------
	// -------- Variables
	// --------

	/** Field owd - 1-Wire container that contains this memory to cache */
	private OneWireContainer[] owd;

	/** Field cache - 2 dimensional array to contain the cache */
	private byte[][] cache;

	/** Field len - array of lengths of packets found */
	private int[] len;

	/** Field pageState - array of flags to indicate the page has been changed */
	private int[] pageState;

	/** Field banks - vector of memory banks that contain the Filesystem */
	private Vector<MemoryBank> banks;

	/** Field totalPages - total pages in this Filesystem */
	private int totalPages;

	/** Field lastPageRead - last page read by this cache */
	private int lastPageRead;

	/** Field maxPacketDataLength - maximum data length on a page */
	private int maxPacketDataLength;

	/** Field bankPages - array of the number of pages in vector of memory banks */
	private int[] bankPages;

	/** Field startPages - array of the number of start pages for device list */
	private int[] startPages;

	/** Field writeLog - array to track the order of pages written to the cache */
	private int[] writeLog;

	/**
	 * Field tempExtra - temporary buffer used to to read the extra information from
	 * a page read
	 */
	private byte[] tempExtra;

	/** Field tempPage - temporary buffer the size of a page */
	private byte[] tempPage;

	/** Field redirect - array of redirection bytes */
	private int[] redirect;

	/** Field owners - vector of classes that are using this cache */
	private Vector<OWFileDescriptor> owners;

	/**
	 * Field openedToWrite - vector of files that have been opened to write on this
	 * filesystem
	 */
	private Vector<String> openedToWrite;

	/**
	 * Field canRedirect - flag to indicate page redirection information must be
	 * gathered
	 */
	private boolean canRedirect;

	/** Field pbmBank - memory bank used for the page bitmap */
	private OTPMemoryBank pbmBank;

	/** Field pbmByteOffset - byte offset into page bitmap buffer */
	private int pbmByteOffset;

	/** Field pbmBitOffset - bit offset into page bitmap buffer */
	private int pbmBitOffset;

	/** Field pbmCache - buffer to cache the page bitmap */
	private byte[] pbmCache;

	/** Field pbmCacheModified - modified version of the page bitmap */
	private byte[] pbmCacheModified;

	/** Field pbmRead - flag indicating that the page bitmap has been read */
	private boolean pbmRead;

	/** Field lastFreePage - last free page found in the page bitmap */
	private int lastFreePage;

	/** Field lastDevice - last device read/written */
	private int lastDevice;

	/** Field autoOverdrive - flag to indicate if we need to do auto-ovedrive */
	private boolean autoOverdrive;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Construct a new memory cache for provided 1-wire container device.
	 *
	 * @param device 1-Wire container
	 */
	public MemoryCache(OneWireContainer device) {
		var devices = new OneWireContainer[1];
		devices[0] = device;

		this.init(devices);
	}

	/**
	 * Construct a new memory cache for provided 1-wire container device.
	 *
	 * @param device 1-Wire container
	 */
	public MemoryCache(OneWireContainer[] devices) {
		this.init(devices);
	}

	/**
	 * Initializes this memory cache for provided 1-wire container device(s).
	 *
	 * @param devices 1-Wire container(s)
	 */
	private void init(OneWireContainer[] devices) {
		this.owd = devices;
		var mem_size = 0;

		PagedMemoryBank pmb = null;

		this.banks = new Vector<>(1);
		this.owners = new Vector<>(1);
		this.openedToWrite = new Vector<>(1);
		this.startPages = new int[this.owd.length];
		this.lastDevice = 0;

		// check to see if adapter supports overdrive
		try {
			this.autoOverdrive = devices[0].getAdapter().canOverdrive();
		} catch (OneWireException e) {
			this.autoOverdrive = false;
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println(
					"___Constructor MemoryCache: " + devices[0].getAddressAsString() + " num " + devices.length);
		}

		// loop through all of the devices in the array
		this.totalPages = 0;
		for (var dev = 0; dev < this.owd.length; dev++) {
			// check to make sure each device can do Overdrive
			if (this.owd[dev].getMaxSpeed() != DSPortAdapter.SPEED_OVERDRIVE) {
				this.autoOverdrive = false;
			}

			// record the start page offset for each device
			this.startPages[dev] = this.totalPages;

			// enumerate through the memory banks and collect the
			// general purpose banks in a vector
			for (var bank_enum = this.owd[dev].getMemoryBanks(); bank_enum.hasMoreElements();) {
				// get the next memory bank
				var mb = bank_enum.nextElement();

				// look for pbm memory bank (used in file structure)
				if (mb.isWriteOnce() && !mb.isGeneralPurposeMemory() && mb.isNonVolatile()
						&& mb instanceof OTPMemoryBank) {
					// if more then 1 device with a OTP then error
					if (this.owd.length > 1) {
						this.totalPages = 0;
						return;
					}

					// If only 128 bytes then have DS2502 or DS2406 which have bitmap included
					// in the only status page. All other EPROM devices have a special memory
					// bank that has 'Bitmap' in the title.
					if (mem_size == 128 || mb.getBankDescription().indexOf("Bitmap") != -1) {
						this.pbmBank = (OTPMemoryBank) mb;

						if (mem_size == 128) {
							this.pbmBitOffset = 4;
						}

						this.pbmByteOffset = 0;
						this.canRedirect = true;

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("_Paged BitMap MemoryBank: " + mb.getBankDescription()
									+ " with bit offset " + this.pbmBitOffset);
						}
					}
				}

				// check regular memory bank
				if (!mb.isGeneralPurposeMemory() || !mb.isNonVolatile() || !(mb instanceof PagedMemoryBank)) {
					continue;
				}

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("_Using MemoryBank: " + mb.getBankDescription());
				}

				this.banks.addElement(mb);
				mem_size += mb.getSize();
				this.totalPages += ((PagedMemoryBank) mb).getNumberPages();
			}
		}

		// count total bankPages
		this.bankPages = new int[this.banks.size()];
		this.totalPages = 0;

		for (var b = 0; b < this.banks.size(); b++) {
			pmb = (PagedMemoryBank) this.banks.elementAt(b);
			this.bankPages[b] = pmb.getNumberPages();
			this.totalPages += this.bankPages[b];
		}

		// create the cache
		this.len = new int[this.totalPages];
		this.pageState = new int[this.totalPages];
		this.writeLog = new int[this.totalPages];
		this.redirect = new int[this.totalPages];
		if (pmb != null) {
			this.maxPacketDataLength = pmb.getMaxPacketDataLength();
			this.cache = new byte[this.totalPages][pmb.getPageLength()];
			this.tempPage = new byte[pmb.getPageLength()];
		}

		// initialize some of the flag arrays
		for (var p = 0; p < this.totalPages; p++) {
			this.pageState[p] = NOT_READ;
			this.len[p] = 0;
			this.writeLog[p] = EMPTY;
		}

		// if getting redirection information, create necessarey arrays
		if (this.canRedirect) {
			this.tempExtra = new byte[pmb.getExtraInfoLength()];
			this.pbmCache = new byte[this.pbmBank.getSize()];
			this.pbmCacheModified = new byte[this.pbmBank.getSize()];
			this.pbmRead = false;
		} else {
			this.pbmRead = true;
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("_Total Pages: " + this.totalPages + ", get Redirection = " + this.canRedirect);
		}
	}

	/**
	 * Gets the number of pages in this cache
	 *
	 * @return number of pages in the cache
	 */
	public int getNumberPages() {
		return this.totalPages;
	}

	/**
	 * Gets the number of pages in the specified bank number
	 *
	 * @param bankNum bank number to retrieve number of pages
	 *
	 * @return number of pages in the bank
	 */
	public int getNumberPagesInBank(int bankNum) {
		if (this.totalPages > 0) {
			return this.bankPages[bankNum];
		}
		return 0;
	}

	/**
	 * Gets the page number of the first page on the specified device. If the device
	 * number is not valid then return 0.
	 *
	 * @param deviceNum device number to retrieve page offset
	 *
	 * @return page number of first page on device
	 */
	public int getPageOffsetForDevice(int deviceNum) {
		return this.startPages[deviceNum];
	}

	/**
	 * Gets the maximum number of bytes for data in each page.
	 *
	 * @return max number of data bytes per page
	 */
	public int getMaxPacketDataLength() {
		return this.maxPacketDataLength;
	}

	/**
	 * Check if this memory device is write-once. If this is true then the page
	 * bitmap facilities in this class will be used.
	 *
	 * @return true if this device is write-once
	 */
	public boolean isWriteOnce() {
		return this.canRedirect;
	}

	/**
	 * Read a page packet. If the page is available in the cache then return that
	 * data.
	 *
	 * @param page    page to read
	 * @param readBuf buffer to place the data in
	 * @param offset  offset into the read buffer
	 *
	 * @return the number byte in the packet
	 *
	 * @throws OneWireException   when the adapter is not setup properly
	 * @throws OneWireIOException when an 1-Wire IO error occurs
	 */
	public int readPagePacket(int page, byte[] readBuf, int offset) throws OneWireIOException, OneWireException {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___readPagePacket (" + page + ") ");
		}

		// check if have a cache (any memory banks)
		if (this.totalPages == 0) {
			throw new OneWireException("1-Wire Filesystem does not have memory");
		}

		// check if out of range
		if (page >= this.totalPages) {
			throw new OneWireException("Page requested is not in memory space");
		}

		// check if doing autoOverdrive (greatly improves multi-device cache speed)
		if (this.autoOverdrive) {
			this.autoOverdrive = false;
			var adapter = this.owd[0].getAdapter();
			adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);
			adapter.reset();
			adapter.putByte((byte) 0x3C);
			adapter.setSpeed(DSPortAdapter.SPEED_OVERDRIVE);
		}

		// check if need to read the page bitmap for the first time
		if (!this.pbmRead) {
			this.readPageBitMap();
		}

		// page NOT cached (maybe redirected)
		if (this.pageState[page] != NOT_READ && this.pageState[page] != READ_NO_CRC && this.redirect[page] == 0) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("_In cache (" + this.len[page] + "):");
				this.debugDump(this.cache[page], 1, this.len[page]);
			}

			// get from cache
			if (readBuf != null) {
				System.arraycopy(this.cache[page], 1, readBuf, offset, this.len[page]);
			}

			return this.len[page];
		}
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println(
					"_Not in cache or redirected, length=" + this.len[page] + " redirect=" + this.redirect[page]);
		}

		// page not cached, so read it
		var local_page = this.getLocalPage(page);
		var pmb = this.getMemoryBankForPage(page);
		var local_device_page = page - this.startPages[this.getDeviceIndex(page)];

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("_Look in MemoryBank " + pmb.getBankDescription());
		}

		if (this.canRedirect) {
			// don't use multi-bank page reference (would not work with redirect)

			// loop while page is redirected
			var loopcnt = 0;
			for (;;) {
				// check for redirection
				if (this.redirect[page] == 0) {
					// check if already in cache
					if (this.pageState[page] == READ_CRC || this.pageState[page] == VERIFY
							|| this.pageState[page] == WRITE) {
						break;
					}

					// read the page with device generated CRC
					if (pmb.hasExtraInfo()) {
						pmb.readPageCRC(page, this.lastPageRead == page - 1, this.cache[page], 0, this.tempExtra);

						// set the last page read
						this.lastPageRead = page;

						// get the redirection byte
						this.redirect[page] = ~this.tempExtra[0] & 0x00FF;
					}
					// OTP device that does not give redirect as extra info (DS1982/DS2502)
					else {
						pmb.readPageCRC(page, this.lastPageRead == page - 1, this.cache[page], 0);

						// get the redirection
						this.redirect[page] = (byte) ((OTPMemoryBank) pmb).getRedirectedPage(page);

						// last page can't be used due to redirect read
						this.lastPageRead = NONE;
					}

					// set the page state
					this.pageState[page] = READ_NO_CRC;

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("_Page: " + page + "->" + this.redirect[page] + " local " + local_page
								+ " with packet length byte " + (this.cache[page][0] & 0x00FF));
					}

					// not redirected so look at packet
					if (this.redirect[page] == 0) {
						// check if length is realistic
						if ((this.cache[page][0] & 0x00FF) > this.maxPacketDataLength) {
							throw new OneWireIOException("Invalid length in packet");
						}

						// verify the CRC is correct
						if (CRC16.compute(this.cache[page], 0, this.cache[page][0] + 3, page) == 0x0000B001) {
							// get the length
							this.len[page] = this.cache[page][0];

							// set the page state
							this.pageState[page] = READ_CRC;

							break;
						} else {
							throw new OneWireIOException("Invalid CRC16 in packet read " + page);
						}
					}
				} else {
					page = this.redirect[page];
				}

				// check for looping redirection
				if (loopcnt++ > this.totalPages) {
					throw new OneWireIOException("Circular redirection of pages");
				}
			}

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("_Data found (" + this.len[page] + "):");
				this.debugDump(this.cache[page], 1, this.len[page]);
			}

			// get copy of data for caller
			if (readBuf != null) {
				System.arraycopy(this.cache[page], 1, readBuf, offset, this.len[page]);
			}

			return this.len[page];
		}
		// not an EPROM
		else {
			// loop if get a crc error in packet data until get same data twice
			for (;;) {
				pmb.readPage(local_page, this.lastPageRead == page - 1, this.tempPage, 0);

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println(
							"_Page: " + page + " translates to " + local_page + " or device page " + local_device_page);
				}

				// set the last page read
				this.lastPageRead = page;

				// verify length is realistic
				if ((this.tempPage[0] & 0x00FF) <= this.maxPacketDataLength) {

					// verify the CRC is correct
					if (CRC16.compute(this.tempPage, 0, this.tempPage[0] + 3, local_device_page) == 0x0000B001) {

						// valid data so put into cache
						System.arraycopy(this.tempPage, 0, this.cache[page], 0, this.tempPage.length);

						// get the length
						this.len[page] = this.tempPage[0];

						// set the page state
						this.pageState[page] = READ_CRC;

						break;
					}
				}

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.print("_Invalid CRC, raw: ");
					this.debugDump(this.tempPage, 0, this.tempPage.length);
				}

				// must have been invalid packet
				// compare with data currently in the cache
				var same_data = true;

				for (var i = 0; i < this.tempPage.length; i++) {
					if ((this.tempPage[i] & 0x00FF) != (this.cache[page][i] & 0x00FF)) {

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("_Differenet at position=" + i);
						}

						same_data = false;

						break;
					}
				}

				// if the same then throw the exception, else loop again
				if (same_data) {
					// set the page state
					this.pageState[page] = READ_NO_CRC;

					throw new OneWireIOException("Invalid CRC16 in packet read");
				} else {
					System.arraycopy(this.tempPage, 0, this.cache[page], 0, this.tempPage.length);
				}
			}
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("_Data found (" + this.len[page] + "):");
			this.debugDump(this.cache[page], 1, this.len[page]);
		}

		// get copy of data for caller
		if (readBuf != null) {
			System.arraycopy(this.cache[page], 1, readBuf, offset, this.len[page]);
		}

		return this.len[page];
	}

	/**
	 * Write a page packet into the cache.
	 *
	 * @param page     page to write
	 * @param writeBuf buffer container the data to write
	 * @param offset   offset into write buffer
	 * @param buflen   length of data to write
	 */
	public void writePagePacket(int page, byte[] writeBuf, int offset, int buflen)
			throws OneWireIOException, OneWireException {
		int log;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("___writePagePacket on page " + page + " with data (" + buflen + "): ");
			this.debugDump(writeBuf, offset, buflen);
		}

		// check if have a cache (any memory banks)
		if (this.totalPages == 0) {
			throw new OneWireException("1-Wire Filesystem does not have memory");
		}

		// check if need to read the page bitmap for the first time
		if (!this.pbmRead) {
			this.readPageBitMap();
		}

		// OTP device
		if (this.canRedirect) {
			// get reference to memory bank
			var otp = (OTPMemoryBank) this.getMemoryBankForPage(page);

			// check redirectoin if writing to a page that has not been read
			if (this.redirect[page] == 0 && this.pageState[page] == NOT_READ) {
				this.redirect[page] = otp.getRedirectedPage(page);
			}

			// check if page to write to is already redirected
			if (this.redirect[page] != 0) {
				// loop to find the end of the redirect chain
				int last_page = page, cnt = 0;
				this.lastPageRead = NONE;
				do {
					last_page = this.redirect[last_page];

					this.redirect[last_page] = otp.getRedirectedPage(last_page);

					if (cnt++ > this.totalPages) {
						throw new OneWireException("Error in Filesystem, circular redirection of pages");
					}
				} while (this.redirect[last_page] != 0);

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.print("___redirection chain ended on page " + last_page);
				}

				// Use the last_page since it was not redirected
				System.arraycopy(writeBuf, offset, this.cache[last_page], 1, buflen);
				this.len[last_page] = buflen;
				this.cache[last_page][0] = (byte) buflen;
				var crc = CRC16.compute(this.cache[last_page], 0, buflen + 1, last_page);
				this.cache[last_page][buflen + 1] = (byte) (~crc & 0xFF);
				this.cache[last_page][buflen + 2] = (byte) ((~crc & 0xFFFF) >>> 8 & 0xFF);

				// set pageState flag
				this.pageState[last_page] = VERIFY;

				// change page to last_page to be used in writeLog
				page = last_page;
			} else {
				// Use the page since it is not redirected
				System.arraycopy(writeBuf, offset, this.cache[page], 1, buflen);
				this.len[page] = buflen;
				this.cache[page][0] = (byte) buflen;
				var crc = CRC16.compute(this.cache[page], 0, buflen + 1, page);
				this.cache[page][buflen + 1] = (byte) (~crc & 0xFF);
				this.cache[page][buflen + 2] = (byte) ((~crc & 0xFFFF) >>> 8 & 0xFF);

				// set pageState flag
				this.pageState[page] = VERIFY;
			}
		}
		// NON-OTP device
		else {
			// put in cache
			System.arraycopy(writeBuf, offset, this.cache[page], 1, buflen);

			this.len[page] = buflen;
			this.cache[page][0] = (byte) buflen;

			// set pageState flag
			this.pageState[page] = WRITE;
		}

		// record write in log
		// search the write log until find 'page' or EMPTY
		for (log = 0; log < this.totalPages; log++) {
			if (this.writeLog[log] == page || this.writeLog[log] == EMPTY) {
				break;
			}
		}

		// shift write log down 1 to 'log'
		for (; log > 0; log--) {
			this.writeLog[log] = this.writeLog[log - 1];
		}

		// add page at top
		this.writeLog[0] = page;
	}

	/**
	 * Flush the pages written back to the 1-Wire device.
	 *
	 * @throws OneWireException   when the adapter is not setup properly
	 * @throws OneWireIOException when an 1-Wire IO error occurs
	 */
	public void sync() throws OneWireIOException, OneWireException {
		int page, log;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___sync");
		}

		// check if have a cache (any memory banks)
		if (this.totalPages == 0) {
			return;
		}

		// loop until all jobs complete
		boolean jobs;
		do {
			jobs = false;

			// loop through write log and write the oldest pages first
			for (log = this.totalPages - 1; log >= 0; log--) {
				// check if this is a valid log entry
				if (this.writeLog[log] != EMPTY) {

					// this was not empty so there is a job
					jobs = true;

					// get page number to write
					page = this.writeLog[log];

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("_page " + page + " in log " + log + " is not empty, pageState: "
								+ this.pageState[page]);
					}

					// get the memory bank
					var pmb = this.getMemoryBankForPage(page);

					// get the local page number
					var local_page = this.getLocalPage(page);

					// Verify operation (only in EPROM operations)
					if (this.pageState[page] == VERIFY) {
						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("_verify page " + page);
						}

						// read the page with device generated CRC
						pmb.readPageCRC(page, this.lastPageRead == page - 1, this.tempPage, 0);

						// set the last page read
						this.lastPageRead = page;

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.print("_Desired data: ");
							this.debugDump(this.cache[page], 0, this.cache[page].length);
							System.out.print("_Current data: ");
							this.debugDump(this.tempPage, 0, this.tempPage.length);
							System.out.println("_len " + this.len[page]);
						}

						// check to see if the desired data can be written here
						var do_redirect = false;
						for (var i = 1; i < this.len[page] + 2; i++) {
							if (((this.tempPage[i] & 0x00FF ^ this.cache[page][i] & 0x00FF) & ~this.tempPage[i]
									& 0x00FF) > 0) {
								// error, data already on device, must redirect
								do_redirect = true;
								break;
							}
						}

						// need to redirect
						if (do_redirect) {
							// \\//\\//\\//\\//\\//\\//\\//
							if (doDebugMessages) {
								System.out.println("_page is occupied with conflicting data, must redirect");
							}

							// find a new page, set VERIFY job there
							// get the next available page
							var new_page = this.getFirstFreePage();
							while (new_page == page) {
								System.out.println("_can't use this page " + page);
								this.markPageUsed(new_page);
								new_page = this.getNextFreePage();
							}

							// verify got a free page
							if (new_page < 0) {
								throw new OneWireException("Redireciton required but out of space on 1-Wire device");
							}

							// mark page used
							this.markPageUsed(new_page);

							// put the data in the new page and setup the job
							System.arraycopy(this.cache[page], 0, this.cache[new_page], 0, this.tempPage.length);
							this.pageState[new_page] = VERIFY;
							this.len[new_page] = this.len[page];

							// add to write log
							for (var i = 0; i < this.totalPages; i++) {
								if (this.writeLog[i] == EMPTY) {
									this.writeLog[i] = new_page;
									break;
								}
							}

							// set old page for redirect
							this.pageState[page] = REDIRECT;
							this.cache[page][0] = (byte) (new_page & 0xFF);
						}
						// verify passed
						else {
							this.pageState[page] = WRITE;
						}
					}

					// Redirect operation
					if (this.pageState[page] == REDIRECT) {
						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("_redirecting page " + page + " to " + (this.cache[page][0] & 0x00FF));
						}

						// redirect the page (new page located in first byte of cache)
						((OTPMemoryBank) pmb).redirectPage(page, this.cache[page][0] & 0x00FF);

						// clear the redirect job
						this.pageState[page] = NOT_READ;
						this.lastPageRead = NONE;
						this.writeLog[log] = EMPTY;
					}

					// Write operation
					if (this.pageState[page] == WRITE) {
						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.print("_write page " + page + " with data (" + this.len[page] + "): ");
							this.debugDump(this.cache[page], 1, this.len[page]);
						}

						// check for new device, make sure it is at the correct speed
						var new_index = this.getDeviceIndex(page);
						if (new_index != this.lastDevice) {
							// \\//\\//\\//\\//\\//\\//\\//
							if (doDebugMessages) {
								System.out.print("(" + new_index + ")");
							}

							this.lastDevice = new_index;
							this.owd[this.lastDevice].doSpeed();
						}

						// write the page
						pmb.writePagePacket(local_page, this.cache[page], 1, this.len[page]);

						// clear pageState flag
						this.pageState[page] = READ_CRC;
						this.lastPageRead = NONE;
						this.writeLog[log] = EMPTY;
					}
				}
			}
		} while (jobs);

		// write the bitmap of used pages for OTP device
		if (this.canRedirect) {
			// make a buffer that contains only then new '0' bits in the bitmap
			// required to not overprogram any bits
			var numBytes = this.totalPages / 8;
			if (numBytes == 0) {
				numBytes = 1;
			}
			var changed = false;
			var temp_buf = new byte[numBytes];

			for (var i = 0; i < numBytes; i++) {
				temp_buf[i] = (byte) (~(this.pbmCache[i] ^ this.pbmCacheModified[i]) & 0x00FF);
				if (temp_buf[i] != (byte) 0xFF) {
					changed = true;
				}
			}

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("_device bitmap: ");
				this.debugDump(this.pbmCache, 0, this.pbmCache.length);
				System.out.print("_modified bitmap: ");
				this.debugDump(this.pbmCacheModified, 0, this.pbmCacheModified.length);
				System.out.print("_page bitmap to write, changed: " + changed + "   ");
				this.debugDump(temp_buf, 0, temp_buf.length);
			}

			// write if changed
			if (changed) {
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("_writing page bitmap");
				}

				// turn off read-back verification
				this.pbmBank.setWriteVerification(false);

				// write buffer
				this.pbmBank.write(0, temp_buf, 0, numBytes);

				// readback to make sure that it matches pbmCacheModified
				this.pbmBank.read(0, false, temp_buf, 0, numBytes);
				for (var i = 0; i < numBytes; i++) {
					if ((temp_buf[i] & 0x00FF) != (this.pbmCacheModified[i] & 0x00FF)) {
						throw new OneWireException("Readback verification of page bitmap was not correct");
					}
				}

				// put new value of bitmap pbmCache
				System.arraycopy(temp_buf, 0, this.pbmCache, 0, numBytes);
				System.arraycopy(temp_buf, 0, this.pbmCacheModified, 0, numBytes);
			}
		}
	}

	// --------
	// -------- Owner tracking methods
	// --------

	/**
	 * Add an owner to this memory cache. This will be tracked for later cleanup.
	 *
	 * @param tobj owner of instance
	 */
	public void addOwner(OWFileDescriptor tobj) {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___addOwner");
		}

		if (this.owners.indexOf(tobj) == -1) {
			this.owners.addElement(tobj);
		}
	}

	/**
	 * Remove the specified owner of this memory cache.
	 *
	 * @param tobj owner of instance
	 */
	public void removeOwner(Object tobj) {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___removeOwner");
		}

		this.owners.removeElement(tobj);
	}

	/**
	 * Check to see if there on no owners of this memory cache.
	 *
	 * @return true if not owners of this memory cache
	 */
	public boolean noOwners() {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___noOwners = " + this.owners.isEmpty());
		}

		return this.owners.isEmpty();
	}

	// --------
	// -------- Write file tracking methods
	// --------

	/**
	 * Remove the provided filePath from the list of files currently opened to
	 * write.
	 *
	 * @param filePath file to remove from write list
	 */
	public void removeWriteOpen(String filePath) {
		var index = this.openedToWrite.indexOf(filePath);

		if (index != -1) {
			this.openedToWrite.removeElementAt(index);
		}
	}

	/**
	 * Check to see if the provided filePath is currently opened to write.
	 * Optionally add it to the list if it not already there.
	 *
	 * @param filePath  file to check to see if opened to write
	 * @param addToList true to add file to list if not present
	 *
	 * @return true if file was not in the opened to write list
	 */
	public boolean isOpenedToWrite(String filePath, boolean addToList) {
		var index = this.openedToWrite.indexOf(filePath);

		if (index != -1) {
			return true;
		}
		if (addToList) {
			this.openedToWrite.addElement(filePath);
		}
		return false;
	}

	// --------
	// -------- Page-Bitmap methods
	// --------

	/**
	 * Check to see if this memory cache should handle the page bitmap.
	 *
	 * @return true if this memory cache should handle the page bitmap
	 */
	public boolean handlePageBitmap() {
		return !(this.pbmBank == null);
	}

	/**
	 * Mark the specified page as used in the page bitmap.
	 *
	 * @param page number to mark as used
	 */
	public void markPageUsed(int page) {
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___markPageUsed " + page);
		}

		// mark page used in cached bitmap of used pages
		Bit.arrayWriteBit(USED, this.pbmBitOffset + page, this.pbmByteOffset, this.pbmCacheModified);
	}

	/**
	 * free the specified page as being un-used in the page bitmap
	 *
	 * @param page number to mark as un-used
	 *
	 * @return true if the page as be been marked as un-used, false if the page is
	 *         on an OTP device and cannot be freed
	 */
	public boolean freePage(int page) {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("___freePage " + page);
		}

		// only free pages that have been written to cache
		// but not flushed to device
		if (Bit.arrayReadBit(this.pbmBitOffset + page, this.pbmByteOffset, this.pbmCache) == NOT_USED) {
			Bit.arrayWriteBit(NOT_USED, this.pbmBitOffset + page, this.pbmByteOffset, this.pbmCacheModified);

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("_ was cached so really free now ");
			}

			return true;
		}
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("_ not cached so not free");
		}

		return false;
	}

	/**
	 * Get the first free page from the page bitmap.
	 *
	 * @return first page number that is free to write
	 */
	public int getFirstFreePage() {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("___getFirstFreePage ");
		}

		this.lastFreePage = 0;

		return this.getNextFreePage();
	}

	/**
	 * Get the next free page from the page bitmap.
	 *
	 * @return next page number that is free to write
	 */
	public int getNextFreePage() {
		for (var pg = this.lastFreePage; pg < this.totalPages; pg++) {
			if (Bit.arrayReadBit(this.pbmBitOffset + pg, this.pbmByteOffset, this.pbmCacheModified) == NOT_USED) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("___getNextFreePage " + pg);
				}

				this.lastFreePage = pg + 1;

				return pg;
			}
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___getNextFreePage, no free pages ");
		}

		return -1;
	}

	/**
	 * Get the total number of free pages in this Filesystem.
	 *
	 * @return number of pages free
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	public int getNumberFreePages() throws OneWireException {
		// check if need to read the page bitmap for the first time
		if (!this.pbmRead) {
			// read the pbm
			this.pbmBank.read(0, false, this.pbmCache, 0, this.pbmCache.length);

			// make a copy of it
			System.arraycopy(this.pbmCache, 0, this.pbmCacheModified, 0, this.pbmCache.length);

			// mark as read
			this.pbmRead = true;

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("_Page bitmap read in getNumberFreePages: ");
				this.debugDump(this.pbmCache, 0, this.pbmCache.length);
			}
		}

		var free_pages = 0;
		for (var pg = 0; pg < this.totalPages; pg++) {
			if (Bit.arrayReadBit(this.pbmBitOffset + pg, this.pbmByteOffset, this.pbmCacheModified) == NOT_USED) {
				free_pages++;
			}
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("___getNumberFreePages = " + free_pages);
		}

		return free_pages;
	}

	/**
	 * Gets the page number used in the remote page bitmap in an OTP device.
	 *
	 * @return page number used in the directory for the remote page bitmap
	 */
	public int getBitMapPageNumber() {
		return this.pbmBank.getStartPhysicalAddress() / this.pbmBank.getPageLength();
	}

	/**
	 * Get the number of pages used for the remote page bitmap in an OTP device.
	 *
	 * @return number of pages used in page bitmap
	 */
	public int getBitMapNumberOfPages() {
		return this.totalPages / 8 / this.pbmBank.getPageLength();
	}

	/**
	 * Gets the memory bank object for the specified page. This is significant if
	 * the Filesystem spans memory banks on the same or different devices.
	 */
	public PagedMemoryBank getMemoryBankForPage(int page) {
		var cnt = 0;

		for (var bank_num = 0; bank_num < this.banks.size(); bank_num++) {
			// check if 'page' is in this memory bank
			if (cnt + this.bankPages[bank_num] > page) {
				return (PagedMemoryBank) this.banks.elementAt(bank_num);
			}

			cnt += this.bankPages[bank_num];
		}

		// page provided is not in this Filesystem
		return null;
	}

	/**
	 * Gets the index into the array of Devices where this page resides. This is
	 * significant if the Filesystem spans memory banks on the same or different
	 * devices.
	 */
	private int getDeviceIndex(int page) {
		for (var dev_num = this.startPages.length - 1; dev_num >= 0; dev_num--) {
			// check if 'page' is in this memory bank
			if (this.startPages[dev_num] < page) {
				return dev_num;
			}
		}

		// page provided is not in this Filesystem
		return 0;
	}

	/**
	 * Gets the local page number on the memory bank object for the specified page.
	 * This is significant if the Filesystem spans memory banks on the same or
	 * different devices.
	 */
	public int getLocalPage(int page) {
		var cnt = 0;

		for (var bank_num = 0; bank_num < this.banks.size(); bank_num++) {
			// check if 'page' is in this memory bank
			if (cnt + this.bankPages[bank_num] > page) {
				return page - cnt;
			}

			cnt += this.bankPages[bank_num];
		}

		// page provided is not in this Filesystem
		return 0;
	}

	/**
	 * Clears the lastPageRead global so that a readPage will not try to continue
	 * where the last page left off. This should be called anytime exclusive access
	 * to the 1-Wire cannot be guaranteed.
	 */
	public void clearLastPageRead() {
		// last page can't be used due to redirect read
		this.lastPageRead = NONE;
	}

	/**
	 * Read the page bitmap.
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	private void readPageBitMap() throws OneWireException {
		// read the pbm
		this.pbmBank.read(0, false, this.pbmCache, 0, this.pbmCache.length);

		// make a copy of it
		System.arraycopy(this.pbmCache, 0, this.pbmCacheModified, 0, this.pbmCache.length);

		// mark as read
		this.pbmRead = true;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("____Page bitmap read: ");
			this.debugDump(this.pbmCache, 0, this.pbmCache.length);
		}
	}

	// --------
	// -------- Misc Utility Methods
	// --------

	/**
	 * Debug dump utility method
	 *
	 * @param buf    buffer to dump
	 * @param offset offset to start in the buffer
	 * @param len    length to dump
	 */
	private void debugDump(byte[] buf, int offset, int len) {
		for (var i = offset; i < offset + len; i++) {
			System.out.print(Integer.toHexString(buf[i] & 0x00FF) + " ");
		}

		System.out.println();
	}
}
// CHECKSTYLE:ON
