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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.PagedMemoryBank;
import com.dalsemi.onewire.utils.Address;
import com.dalsemi.onewire.utils.Bit;
import com.dalsemi.onewire.utils.Convert;

/**
 * Instances of the 1-Wire file descriptor class serve as an opaque handle to
 * the underlying machine-specific structure representing an open file, an open
 * socket, or another source or sink of bytes. The main practical use for a file
 * descriptor is to create a <code>OWFileInputStream</code> or
 * <code>OWFileOutputStream</code> to contain it.
 * <p>
 * Applications should not create their own file descriptors.
 *
 * @author DS
 * @version 0.01, 1 June 2001
 * @see com.dalsemi.onewire.application.file.OWFile
 * @see com.dalsemi.onewire.application.file.OWFileInputStream
 * @see com.dalsemi.onewire.application.file.OWFileOutputStream
 */
@SuppressWarnings({ "unused" })
public class OWFileDescriptor {
	// --------
	// -------- Static Variables
	// --------

	/** Hashtable to contain MemoryCache instances (one per container) */
	private static Hashtable<Long, MemoryCache> memoryCacheHash = new Hashtable<>(4);

	/** Field EXT_DIRECTORY extension value */
	private static final byte EXT_DIRECTORY = 0x007F;

	/**
	 * Field EXT_UNKNOWN marker in path vector to indicate don't know if file or
	 * directory
	 */
	private static final byte EXT_UNKNOWN = 0x007E;

	/** Field BM_CACHE bitmap type MemoryCache */
	private static final int BM_CACHE = 0;

	/** Field BM_LOCAL bitmap type Local (in directory page 0) */
	private static final int BM_LOCAL = 1;

	/** Field BM_FILE bitmap type file, in an external file */
	private static final int BM_FILE = 2;

	/** Field PAGE_USED marker for a used page in the bitmap */
	private static final int PAGE_USED = 1;

	/** Field PAGE_NOT_USED marker for an unused page in the bitmap */
	private static final int PAGE_NOT_USED = 0;

	/** Field LEN_FILENAME */
	private static final int LEN_FILENAME = 5;

	/** Enable/disable debug messages */
	private static final boolean doDebugMessages = false;

	// --------
	// -------- Variables
	// --------

	/** Field address - 1-Wire device address */
	private Long address;

	/** Field cache - used to read/write 1-Wire device */
	private MemoryCache cache;

	/** Field owd - is the 1-Wire container */
	private OneWireContainer[] owd;

	/** Field rawPath - what was provided in constructor except for toUpper */
	private String rawPath;

	/** Field path - converted path to vector of 5 byte arrays */
	private Vector<byte[]> path;

	/** Field verbosePath - same as 'path' but includes '.' and '..' */
	private Vector<byte[]> verbosePath;

	// --------
	// file entry (fe) info on device
	// --------

	/** Field fePage - File Entry page number */
	private int fePage;

	/** Field feOffset - Offset into File Entry page */
	private int feOffset;

	/** Field feData - buffer containing the last File Entry Page */
	private byte[] feData;

	/** Field feLen - length of packet in the last File Entry Page */
	private int feLen;

	/** Field feNumPages - Number of Pages specified in File Entry */
	private int feNumPages;

	/** Field feStartPage - Start Page specified in the File Entry */
	private int feStartPage;

	// --------
	// file read/write info
	// --------

	/** Field lastPage - last page read */
	private int lastPage;

	/** Field lastOffset - offset into last page read */
	private int lastOffset;

	/** Field lastLen - length of last page read */
	private int lastLen;

	/** Field lastPageData - buffer for the last page read */
	private byte[] lastPageData;

	/** Field filePosition - overall file position when reading */
	private int filePosition;

	/** Field markPosition - mark position in read file */
	private int markPosition;

	/** Field markLimit - mark position limit */
	private int markLimit;

	// --------
	// total device info
	// --------

	/** Field totalPages - number of pages in filesystem */
	private int totalPages;

	/**
	 * Field rootTotalPages - number of pages on the ROOT device in the filesystem
	 */
	private int rootTotalPages;

	/** Field maxDataLen - max data per page including page pointer */
	private int maxDataLen;

	/** Field LEN_PAGE_PTR - length in bytes for the page pointer */
	private int LEN_PAGE_PTR;

	/** Field LEN_FILE_ENTRY - length in bytes of the directory file entry */
	private int LEN_FILE_ENTRY;

	/** Field LEN_FILE_ENTRY - length in bytes of the directory control Data */
	private int LEN_CONTROL_DATA;

	/** Field openedToWrite - flag to indicate file is opened for writing */
	private boolean openedToWrite;

	// --------
	// page used bitmap
	// --------

	/** Field lastFreePage - last free page */
	private int lastFreePage;

	/** Field bitmapType - type of page bitmap */
	private int bitmapType;

	/** Field pbm - buffer containering the current image for the page bitmap */
	private byte[] pbm;

	/** Field pbmByteOffset - byte offset into page bitmap */
	private int pbmByteOffset;

	/** Field pbmBitOffset - bit offset into page bitmap */
	private int pbmBitOffset;

	/** Field pbmStartPage - start page of page bitmap */
	private int pbmStartPage;

	/** Field pbmNumPages - number of pages in the page bitmap */
	private int pbmNumPages;

	// --------
	// Misc
	// --------

	/** Field tempPage - temporary page buffer */
	private byte[] tempPage;

	/** Field initName - image of blank directory entry, used in parsing */
	private final byte[] initName = { 0x20, 0x20, 0x20, 0x20, EXT_UNKNOWN };

	/** Field smallBuf - small buffer */
	private byte[] smallBuf;

	/** Field dmBuf - device map page buffer */
	private byte[] dmBuf;

	/** Field addrBuf - address buffer */
	private byte[] addrBuf;

	// --------
	// -------- Constructors
	// --------

	/**
	 * Construct an invalid 1-Wire FileDescriptor
	 *
	 */
	public OWFileDescriptor() {
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===Invalid Constructor OWFileDescriptor ");
		}
	}

	/**
	 * Construct a 1-Wire FileDescrioptor providing the Filesystem 1-Wire device and
	 * file path.
	 *
	 * @param owd     - 1-Wire container where the filesystem resides
	 * @param newPath - path containing the file/directory that represents this file
	 *                descriptor
	 */
	protected OWFileDescriptor(OneWireContainer owd, String newPath) {
		var devices = new OneWireContainer[1];
		devices[0] = owd;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===Constructor OWFileDescriptor with device: " + devices[0].getAddressAsString()
					+ " and path: " + newPath);
		}
		this.setupFD(devices, newPath);
	}

	/**
	 * Construct a 1-Wire FileDescrioptor providing the Filesystem 1-Wire device and
	 * file path.
	 *
	 * @param owd     - 1-Wire container where the filesystem resides
	 * @param newPath - path containing the file/directory that represents this file
	 *                descriptor
	 */
	protected OWFileDescriptor(OneWireContainer[] owd, String newPath) {
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===Constructor OWFileDescriptor with device: " + owd[0].getAddressAsString()
					+ " and path: " + newPath);
		}
		this.setupFD(owd, newPath);
	}

	/**
	 * Setups the 1-Wire FileDescrioptor providing the Filesystem 1-Wire device(s)
	 * and file path.
	 *
	 * @param owd     - 1-Wire container where the filesystem resides
	 * @param newPath - path containing the file/directory that represents this file
	 *                descriptor
	 */
	protected void setupFD(OneWireContainer[] owd, String newPath) {
		// synchronize with the static memoryCacheHash while initializing
		synchronized (memoryCacheHash) {
			// keep reference to container, adapter, and name
			this.owd = owd;

			if (newPath != null) {
				this.rawPath = newPath.toUpperCase();
			} else {
				this.rawPath = "";
			}

			// check the hash to see if already have a MemoryCache for this device
			this.address = Long.valueOf(owd[0].getAddressAsLong());
			this.cache = memoryCacheHash.get(this.address);

			if (this.cache == null) {
				// create a new cache
				this.cache = new MemoryCache(owd);

				// add to hash
				memoryCacheHash.put(this.address, this.cache);
			}

			// indicate this fd uses this cache, used later in cleanup
			this.cache.addOwner(this);

			// get info on device through cache
			this.totalPages = this.cache.getNumberPages();
			this.rootTotalPages = this.cache.getNumberPagesInBank(0);
			this.maxDataLen = this.cache.getMaxPacketDataLength();
			this.openedToWrite = false;

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("=cache has totalPages = " + this.totalPages + " with max data " + this.maxDataLen);
			}

			// construct the page bufs
			this.lastPageData = new byte[this.maxDataLen];
			this.tempPage = new byte[this.lastPageData.length];
			this.feData = new byte[this.lastPageData.length];
			this.dmBuf = new byte[this.lastPageData.length];
			this.smallBuf = new byte[10];
			this.addrBuf = new byte[8];

			// guese at the number of bytes to represent a page number
			// since have not read the root directory yet this may change
			this.LEN_PAGE_PTR = this.totalPages > 256 ? 2 : 1;
			this.LEN_FILE_ENTRY = LEN_FILENAME + this.LEN_PAGE_PTR * 2;
			this.LEN_CONTROL_DATA = 6 + this.LEN_PAGE_PTR;

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("=Number of page bytes = " + this.LEN_PAGE_PTR + " with directory entry size of "
						+ this.LEN_FILE_ENTRY);
			}

			// decide what type of bitmap we will have
			if (this.cache.handlePageBitmap()) {
				this.bitmapType = BM_CACHE;
			} else {
				if (this.totalPages <= 32) {
					this.bitmapType = BM_LOCAL;

					// make PageBitMap max size of first page of directory
					this.pbm = new byte[this.maxDataLen];
					this.pbmByteOffset = 3;
				} else {
					this.bitmapType = BM_FILE;

					// make PageBitMap correct size number of pages in fs
					this.pbm = new byte[this.totalPages / 8 + this.LEN_PAGE_PTR];
					this.pbmByteOffset = 0;
				}
				this.pbmBitOffset = 0;
			}
			this.pbmStartPage = -1;

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println(
						"=Page BitMap type is " + this.bitmapType + " with bit offset of " + this.pbmBitOffset);
			}

			// parse the path into a Vector
			this.verbosePath = new Vector<>(3);

			// done could not parse the skip compressing
			if (!this.parsePath(this.rawPath, this.verbosePath)) {
				return;
			}

			// create a compressed path (take out "." and "..")
			this.path = new Vector<>(this.verbosePath.size());

			byte[] element;

			for (var element_num = 0; element_num < this.verbosePath.size(); element_num++) {
				element = this.verbosePath.elementAt(element_num);

				// ".."
				if (element[0] == '.' && element[1] == '.') {

					// remove last entry in path
					if (this.path.size() <= 0) {
						this.path = null;
						break;
					}
					this.path.removeElementAt(this.path.size() - 1);
				}

				// not "." (so ignore entries ".")
				else if (element[0] != '.') {
					this.path.addElement(element);
				}
			}
		}
	}

	// --------
	// -------- Standard FileDescriptor methods
	// --------

	/**
	 * Tests if this file descriptor object is valid.
	 *
	 * @return <code>true</code> if the file descriptor object represents a valid,
	 *         open file, socket, or other active I/O connection; <code>false</code>
	 *         otherwise.
	 */
	public boolean valid() {
		synchronized (this.cache) {
			return this.cache != null;
		}
	}

	/**
	 * Force all system buffers to synchronize with the underlying device. This
	 * method returns after all modified data and attributes of this FileDescriptor
	 * have been written to the relevant device(s). In particular, if this
	 * FileDescriptor refers to a physical storage medium, such as a file in a file
	 * system, sync will not return until all in-memory modified copies of buffers
	 * associated with this FileDesecriptor have been written to the physical
	 * medium.
	 *
	 * sync is meant to be used by code that requires physical storage (such as a
	 * file) to be in a known state For example, a class that provided a simple
	 * transaction facility might use sync to ensure that all changes to a file
	 * caused by a given transaction were recorded on a storage medium.
	 *
	 * sync only affects buffers downstream of this FileDescriptor. If any in-memory
	 * buffering is being done by the application (for example, by a
	 * BufferedOutputStream object), those buffers must be flushed into the
	 * OWFileDescriptor (for example, by invoking OutputStream.flush) before that
	 * data will be affected by sync.
	 *
	 * <p>
	 * This method may be called multiple times if the source of
	 * OWSyncFailedException has been rectified (1-Wire device was reattached to the
	 * network).
	 *
	 * @exception OWSyncFailedException Thrown when the buffers cannot be flushed,
	 *                                  or because the system cannot guarantee that
	 *                                  all the buffers have been synchronized with
	 *                                  physical media.
	 */
	public void sync() throws OWSyncFailedException {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===sync");
		}

		try {
			synchronized (this.cache) {
				// clear last page read flag
				this.cache.clearLastPageRead();

				// flush the writes to the device
				this.cache.sync();
			}
		} catch (OneWireIOException e) {
			throw new OWSyncFailedException(e.toString());
		} catch (OneWireException e) {
			throw new OWSyncFailedException(e.toString());
		}
	}

	// --------
	// -------- General File methods
	// --------

	/**
	 * Opens the file for reading. If successful (no exceptions) then the following
	 * class member variables will be set:
	 * <ul>
	 * <li>fePage - File Entry page number
	 * <li>feOffset - Offset into File Entry page
	 * <li>feData - buffer containing the last File Entry Page
	 * <li>feLen - length of packet in the last File Entry Page
	 * <li>feNumPages - Number of Pages specified in File Entry
	 * <li>feStartPage - Start Page specified in the File Entry
	 * <li>feParentPage - Parent page of current File Entry Page
	 * <li>feParentOffset - Offset into Parent page
	 * <li>lastPage - (file only) last page read
	 * <li>lastOffset - (file only) offset into last page read
	 * <li>lastLen - (file only) length of last page read
	 * <li>lastPageData - (file only) buffer for the last page read
	 * <li>filePosition - (file only) overall file position when reading
	 * </ul>
	 *
	 * @throws OWFileNotFoundException when the file/directory path is invalid or
	 *                                 there was an IOException thrown when trying
	 *                                 to read the device.
	 */
	protected void open() throws OWFileNotFoundException {
		String last_error = null;
		var cnt = 0;

		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===open");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// reset the file position
			this.lastPage = -1;

			// check if had an invalid path
			if (this.path == null) {
				throw new OWFileNotFoundException("Invalid path");
			}

			// check if have an empty path
			if (this.path.size() == 0) {
				throw new OWFileNotFoundException("Invalid path, no elements");
			}

			// check to see if this file entry has been found
			if (this.feStartPage <= 0) {

				// loop up to 2 times if getting 1-Wire IO exceptions
				do {
					try {
						if (this.verifyPath(this.path.size())) {
							return;
						}
					} catch (OneWireException e) {
						last_error = e.toString();
					}
				} while (cnt++ < 2);

				// could not find file so pass along the last error
				throw new OWFileNotFoundException(last_error);
			}
		}
	}

	/**
	 * Closes this file descriptor and releases any system resources associated with
	 * this stream. Any cached writes are flushed into the filesystem. This file
	 * descriptor may no longer be used for writing bytes. If successful (no
	 * exceptions) then the following class member variables will be set:
	 * <ul>
	 * <li>fePage - File Entry page number
	 * <li>feOffset - Offset into File Entry page
	 * <li>feData - buffer containing the last File Entry Page
	 * <li>feLen - length of packet in the last File Entry Page
	 * <li>feNumPages - Number of Pages specified in File Entry
	 * <li>feStartPage - Start Page specified in the File Entry
	 * <li>feParentPage - Parent page of current File Entry Page
	 * <li>feParentOffset - Offset into Parent page
	 * <li>lastPage - (file only) last page read
	 * <li>lastOffset - (file only) offset into last page read
	 * <li>lastLen - (file only) length of last page read
	 * <li>lastPageData - (file only) buffer for the last page read
	 * <li>filePosition - (file only) overall file position when reading
	 * </ul>
	 *
	 * @throws IOException if an I/O error occurs
	 */
	protected void close() throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===close");
			}

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("thread " + Thread.currentThread().hashCode());
				Thread.dumpStack();
			}

			// sync the cache to the device
			try {
				this.sync();
			} catch (OWSyncFailedException e) {
				throw new IOException(e.toString());
			}

			// free the resources for this fd
			this.free();
		}
	}

	/**
	 * Creates a directory or file to write.
	 *
	 * @param append      for files only, true to append data to end of file, false
	 *                    to reset the file
	 * @param isDirectory true if creating a directory, false for a file
	 * @param makeParents true if creating all needed parent directories in order to
	 *                    create the file/directory
	 * @param startPage   starting page of file/directory, -1 if not renaming
	 * @param numberPages number of pages in file/directory, -1 if not renaming
	 *
	 * @throws OWFileNotFoundException if file already opened to write, if
	 *                                 makeParents=false and parent directories not
	 *                                 found, if file is read only, or if there is
	 *                                 an IO error reading filesystem
	 */
	protected void create(boolean append, boolean isDirectory, boolean makeParents, int startPage, int numberPages)
			throws OWFileNotFoundException {
		byte[] element;
		boolean element_found;

		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println(
						"===create, append=" + append + " isDirectory=" + isDirectory + " makeParents=" + makeParents);
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// reset the file position
			this.lastPage = -1;

			// check if had an invalid path
			if (this.path == null) {
				throw new OWFileNotFoundException("Invalid path");
			}

			// check if have an empty path
			if (this.path.size() == 0) {
				throw new OWFileNotFoundException("Invalid path, no elements");
			}

			// make sure last element in path is a directory (or unknown) if making
			// directory
			if (isDirectory || makeParents) {
				element = this.path.elementAt(this.path.size() - 1);

				if ((element[4] & 0x7F) != EXT_UNKNOWN && (element[4] & 0x7F) != EXT_DIRECTORY) {
					throw new OWFileNotFoundException("Invalid path, directory has an extension");
				}
			}

			// check if file is already opened to write
			if (!isDirectory) {
				if (this.cache.isOpenedToWrite(this.owd[0].getAddressAsString() + this.getPath(), true)) {
					throw new OWFileNotFoundException("File already opened to write");
				}

				this.openedToWrite = true;
			}

			// loop through the path elements, creating directories/file as needed
			this.feStartPage = 0;
			var file_exists = false;
			byte[] prev_element = { (byte) 0x52, (byte) 0x4F, (byte) 0x4F, (byte) 0x54 };
			var prev_element_start = 0;

			for (var element_num = 0; element_num < this.path.size(); element_num++) {
				element = this.path.elementAt(element_num);

				try {
					element_found = this.findElement(this.feStartPage, element, 0);
				} catch (OneWireException e) {
					throw new OWFileNotFoundException(e.toString());
				}

				if (!element_found) {
					if (isDirectory) {

						// convert unknown entry to directory
						if (element[4] == EXT_UNKNOWN) {
							element[4] = EXT_DIRECTORY;
						}

						if (element_num != this.path.size() - 1 && !makeParents) {
							throw new OWFileNotFoundException("Invalid path, parent not found");
						}
					} else {

						// convert unknown entry to file with 0 extension
						if (element[4] == EXT_UNKNOWN) {
							element[4] = 0;
						}

						if (element_num != this.path.size() - 1) {
							// remove the entry in the cache before throwing exception
							this.cache.removeWriteOpen(this.owd[0].getAddressAsString() + this.getPath());
							throw new OWFileNotFoundException("Path not found");
						}
					}
					this.createEntry(element, startPage, numberPages, prev_element, prev_element_start);
				} else if (element_num == this.path.size() - 1) {
					// last element
					if (isDirectory) {
						if (startPage != -1) {
							throw new OWFileNotFoundException("Destination File exists");
						}
					} else {
						// check if last element is a directory and should be a file
						if (element[4] == EXT_DIRECTORY && !isDirectory) {
							// remove the entry in the cache before throwing exception
							this.cache.removeWriteOpen(this.owd[0].getAddressAsString() + this.getPath());
							throw new OWFileNotFoundException("Filename provided is a directory!");
						}
						file_exists = true;
					}
				}

				// get pointers to the next element
				this.feStartPage = Convert.toInt(this.feData, this.feOffset + LEN_FILENAME, this.LEN_PAGE_PTR);
				this.feNumPages = Convert.toInt(this.feData, this.feOffset + LEN_FILENAME + this.LEN_PAGE_PTR,
						this.LEN_PAGE_PTR);
				prev_element = element;
				prev_element_start = this.feStartPage;

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=feStartPage " + this.feStartPage + " feNumPages " + this.feNumPages);
				}
			}

			// if is a file and it already exists, free all but the first data page
			if (file_exists) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=file exists");
				}

				// check for readonly
				if (!this.canWrite()) {
					throw new OWFileNotFoundException("File is read only (access is denied)");
				}

				try {

					// read the first file page
					this.lastLen = this.cache.readPagePacket(this.feStartPage, this.lastPageData, 0);

					// write over this with an 'empty' page
					Convert.toByteArray(0, this.smallBuf, 0, this.LEN_PAGE_PTR);
					this.cache.writePagePacket(this.feStartPage, this.smallBuf, 0, this.LEN_PAGE_PTR);

					// loop to read the rest of the pages and 'free' them
					var next_page = Convert.toInt(this.lastPageData, this.lastLen - this.LEN_PAGE_PTR,
							this.LEN_PAGE_PTR);

					while (next_page != 0) {

						// free the page
						this.readBitMap();
						this.freePage(next_page);
						this.writeBitMap();

						// read the file page
						this.lastLen = this.cache.readPagePacket(next_page, this.lastPageData, 0);

						// get the next page pointer
						next_page = Convert.toInt(this.lastPageData, this.lastLen - this.LEN_PAGE_PTR,
								this.LEN_PAGE_PTR);
					}

					// update the directory entry to free the pages
					this.feNumPages = 1;
					this.lastLen = this.cache.readPagePacket(this.fePage, this.lastPageData, 0);

					Convert.toByteArray(this.feNumPages, this.lastPageData,
							this.feOffset + LEN_FILENAME + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
					this.cache.writePagePacket(this.fePage, this.lastPageData, 0, this.lastLen);

					// set the lastPage pointer to the current page
					this.lastPage = this.feStartPage;
				} catch (OneWireException e) {
					throw new OWFileNotFoundException(e.toString());
				}
			}
		}
	}

	/**
	 * Format the Filesystem on the 1-Wire device.
	 * <p>
	 * WARNING: all files/directories will be deleted in the process.
	 *
	 * @throws OneWireException   when adapter is not setup properly
	 * @throws OneWireIOException when an IO error occurred reading the 1-Wire
	 *                            device
	 */
	protected void format() throws OneWireException, OneWireIOException {
		int i, j, len, next_page, cnt, cdcnt = 0, device_map_pages, dm_bytes = 0;

		synchronized (this.cache) {
			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// check for device with no memory
			if (this.totalPages == 0) {
				throw new OneWireException("1-Wire Filesystem does not have memory");
			}

			for (i = 0; i < this.feData.length; i++) {
				this.feData[i] = 0;
			}

			// create the directory page
			// Directory Marker 'DM'
			this.feData[cdcnt] = this.LEN_PAGE_PTR == 1 ? (byte) 0x0A : (byte) 0x0B;
			this.feData[cdcnt++] |= this.owd.length == 1 ? (byte) 0xA0 : 0xB0;

			// Map Address 'MA', skip for now
			cdcnt += this.LEN_PAGE_PTR;

			// decide what type of bitmap we will have
			if (this.cache.handlePageBitmap()) {
				this.bitmapType = BM_CACHE;
				this.feData[cdcnt++] = 0;

				Convert.toByteArray(this.cache.getBitMapPageNumber(), this.feData, cdcnt, this.LEN_PAGE_PTR);
				cdcnt += this.LEN_PAGE_PTR;
				Convert.toByteArray(this.cache.getBitMapNumberOfPages(), this.feData, cdcnt, this.LEN_PAGE_PTR);
			}
			// regular bitmap
			else {
				// check for Device Map file
				if (this.owd.length > 1) {
					// calculate the number of pages need so leave space
					dm_bytes = (this.owd.length - 1) * 8;
					device_map_pages = dm_bytes / (this.maxDataLen - this.LEN_PAGE_PTR);
					if (dm_bytes % (this.maxDataLen - this.LEN_PAGE_PTR) > 0) {
						device_map_pages++;
					}
				} else {
					device_map_pages = 0;
				}

				// local
				if (this.totalPages <= 32) {
					this.bitmapType = BM_LOCAL;

					// make PageBitMap max size of first page of directory
					this.pbm = new byte[this.maxDataLen];
					this.pbmByteOffset = 3;
					this.pbmBitOffset = 0;
					// 'BC'
					this.feData[cdcnt++] = this.owd.length > 1 ? (byte) 0x82 : (byte) 0x80;

					// check if this will fit on the ROOT device
					if (device_map_pages >= this.rootTotalPages) {
						throw new OneWireException(
								"ROOT 1-Wire device does not have memory to support this many SATELLITE devices");
					}

					// set local page bitmap
					for (i = 0; i <= device_map_pages; i++) {
						Bit.arrayWriteBit(PAGE_USED, i, cdcnt, this.feData);
					}

					// put dummy directory on each SATELLITE device
					if (this.owd.length > 1) {
						// create the dummy directory
						this.tempPage[0] = this.feData[0];
						this.tempPage[this.LEN_PAGE_PTR] = 0;
						this.tempPage[1] = (byte) 0x01;
						this.tempPage[this.LEN_PAGE_PTR + 1] = (byte) 0x80;
						for (j = 2; j <= 5; j++) {
							this.tempPage[this.LEN_PAGE_PTR + j] = (byte) 0xFF;
						}
						for (j = 6; j <= 7; j++) {
							this.tempPage[this.LEN_PAGE_PTR + j] = (byte) 0x00;
						}

						// create link back to the MASTER
						System.arraycopy(this.owd[0].getAddress(), 0, this.smallBuf, 0, 8);
						this.smallBuf[8] = 0;
						this.smallBuf[9] = 0;

						// write dummy directory on each SATELLITE device and mark in bitmap
						for (i = 1; i < this.owd.length; i++) {
							// dummy directory
							this.cache.writePagePacket(this.cache.getPageOffsetForDevice(i), this.tempPage, 0,
									this.LEN_PAGE_PTR * 2 + 6);
							Bit.arrayWriteBit(PAGE_USED, this.cache.getPageOffsetForDevice(i), cdcnt, this.feData);

							// MASTER device map link
							this.cache.writePagePacket(this.cache.getPageOffsetForDevice(i) + 1, this.smallBuf, 0,
									this.LEN_PAGE_PTR + 8);
							Bit.arrayWriteBit(PAGE_USED, this.cache.getPageOffsetForDevice(i) + 1, cdcnt, this.feData);
						}
					}
				}
				// file
				else {
					this.bitmapType = BM_FILE;
					this.pbmByteOffset = 0;
					this.pbmBitOffset = 0;

					// calculate the number of bitmap pages needed
					var pbm_bytes = this.totalPages / 8;
					var pgs = pbm_bytes / (this.maxDataLen - this.LEN_PAGE_PTR);

					if (pbm_bytes % (this.maxDataLen - this.LEN_PAGE_PTR) > 0) {
						pgs++;
					}

					// check if this will fit on the ROOT device
					if (device_map_pages + pgs >= this.rootTotalPages) {
						throw new OneWireException(
								"ROOT 1-Wire device does not have memory to support this many SATELLITE devices");
					}

					// 'BC' set the page number of the bitmap file
					this.feData[cdcnt++] = this.owd.length > 1 ? (byte) 0x02 : (byte) 0x00;

					// page address and number of pages for bitmap file
					if (this.LEN_PAGE_PTR == 1) {
						this.feData[cdcnt++] = 0;
						this.feData[cdcnt++] = 0;
					}
					Convert.toByteArray(device_map_pages + 1, this.feData, cdcnt, this.LEN_PAGE_PTR);
					cdcnt += this.LEN_PAGE_PTR;
					Convert.toByteArray(pgs, this.feData, cdcnt, this.LEN_PAGE_PTR);

					// clear the bitmap
					for (i = 0; i < this.pbm.length; i++) {
						this.pbm[i] = 0;
					}

					// set the pages used by the directory and bitmap file and device map
					for (i = 0; i <= pgs + device_map_pages; i++) {
						Bit.arrayWriteBit(PAGE_USED, this.pbmBitOffset + i, this.pbmByteOffset, this.pbm);
					}

					// put dummy directory on each SATELLITE device
					if (this.owd.length > 1) {
						// create the dummy directory
						this.tempPage[0] = this.feData[0];
						this.tempPage[this.LEN_PAGE_PTR] = 0;
						this.tempPage[1] = (byte) 0x01;
						this.tempPage[this.LEN_PAGE_PTR + 1] = (byte) 0x80;
						for (j = 2; j <= 5; j++) {
							this.tempPage[this.LEN_PAGE_PTR + j] = (byte) 0xFF;
						}
						for (j = 6; j <= 7; j++) {
							this.tempPage[this.LEN_PAGE_PTR + j] = (byte) 0x00;
						}

						// create link back to the MASTER
						System.arraycopy(this.owd[0].getAddress(), 0, this.smallBuf, 0, 8);
						this.smallBuf[8] = 0;
						this.smallBuf[9] = 0;

						// write dummy directory on each SATELLITE device and mark in bitmap
						for (i = 1; i < this.owd.length; i++) {
							// dummy directory
							this.cache.writePagePacket(this.cache.getPageOffsetForDevice(i), this.tempPage, 0,
									this.LEN_PAGE_PTR * 2 + 6);
							Bit.arrayWriteBit(PAGE_USED, this.pbmBitOffset + this.cache.getPageOffsetForDevice(i),
									this.pbmByteOffset, this.pbm);

							// MASTER device map link
							this.cache.writePagePacket(this.cache.getPageOffsetForDevice(i) + 1, this.smallBuf, 0,
									this.LEN_PAGE_PTR + 8);
							Bit.arrayWriteBit(PAGE_USED, this.pbmBitOffset + this.cache.getPageOffsetForDevice(i) + 1,
									this.pbmByteOffset, this.pbm);
						}
					}

					// write the bitmap file
					cnt = 0;
					for (i = device_map_pages + 1; i <= pgs + device_map_pages; i++) {
						// calculate length to write for this page
						if (pbm_bytes - cnt > this.maxDataLen - this.LEN_PAGE_PTR) {
							len = this.maxDataLen - this.LEN_PAGE_PTR;
						} else {
							len = pbm_bytes - cnt;
						}

						// copy bitmap data to temp
						System.arraycopy(this.pbm, this.pbmByteOffset + cnt, this.tempPage, 0, len);

						// set the next page marker
						next_page = i == pgs + device_map_pages ? 0 : i + 1;

						Convert.toByteArray(next_page, this.tempPage, len, this.LEN_PAGE_PTR);

						// write the page
						this.cache.writePagePacket(i, this.tempPage, 0, len + this.LEN_PAGE_PTR);

						cnt += len;
					}
				}

				// write Device Map file
				if (this.owd.length > 1) {
					// set the start page 'MA'
					Convert.toByteArray(1, this.feData, 1, this.LEN_PAGE_PTR);

					// bitmap already taken care of, just put right after directory
					// create the device map data to write
					var dmf = new byte[dm_bytes];
					for (i = 1; i < this.owd.length; i++) {
						System.arraycopy(this.owd[i].getAddress(), 0, dmf, (i - 1) * 8, 8);
					}

					// write the pages
					cnt = 0;
					for (i = 1; i <= device_map_pages; i++) {
						// calculate length to write for this page
						if (dm_bytes - cnt > this.maxDataLen - this.LEN_PAGE_PTR) {
							len = this.maxDataLen - this.LEN_PAGE_PTR;
						} else {
							len = dm_bytes - cnt;
						}

						// copy bitmap data to temp
						System.arraycopy(dmf, cnt, this.tempPage, 0, len);

						// set the next page marker
						next_page = i == device_map_pages ? 0 : i + 1;

						Convert.toByteArray(next_page, this.tempPage, len, this.LEN_PAGE_PTR);

						// write the page
						this.cache.writePagePacket(i, this.tempPage, 0, len + this.LEN_PAGE_PTR);

						cnt += len;
					}
				}
			}

			// write the directory page
			this.cache.writePagePacket(0, this.feData, 0, this.LEN_CONTROL_DATA + this.LEN_PAGE_PTR);

			// update bitmap if implemented in cache
			if (this.cache.handlePageBitmap()) {
				this.markPageUsed(0);
			}

			this.fePage = 0;
			this.feLen = this.LEN_CONTROL_DATA + this.LEN_PAGE_PTR;
		}
	}

	// --------
	// -------- Read methods
	// --------

	/**
	 * Reads up to <code>len</code> bytes of data from this input stream into an
	 * array of bytes. This method blocks until some input is available.
	 *
	 * @param b   the buffer into which the data is read.
	 * @param off the start offset of the data.
	 * @param len the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or <code>-1</code> if
	 *         there is no more data because the end of the file has been reached.
	 * @exception IOException if an I/O error occurs.
	 */
	protected int read(byte b[], int off, int len) throws IOException {
		int read_count = 0, page_data_read, next_page = 0;

		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===read(byte[],int,int)  with len " + len);
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// check for no pages read
			if (this.lastPage == -1) {
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=first read");
				}

				this.lastPage = this.feStartPage;
				this.lastOffset = 0;
				this.filePosition = 0;

				// read the lastPage into the lastPageData buffer
				this.fetchPage();
			}

			// loop to read pages needed or end of file found
			do {

				// check if need to fetch another page
				if (this.lastOffset + this.LEN_PAGE_PTR >= this.lastLen) {

					// any more pages?
					next_page = Convert.toInt(this.lastPageData, this.lastLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

					if (next_page == 0) {
						break;
					}

					// get the next page
					this.lastPage = next_page;

					this.fetchPage();
				}

				// calculate the data available/needed to read from this page
				if (len >= this.lastLen - this.lastOffset - this.LEN_PAGE_PTR) {
					page_data_read = this.lastLen - this.lastOffset - this.LEN_PAGE_PTR;
				} else {
					page_data_read = len;
				}

				// get the data from the page (if buffer not null)
				if (b != null) {
					System.arraycopy(this.lastPageData, this.lastOffset, b, off, page_data_read);
				}

				// adjust counters
				read_count += page_data_read;
				off += page_data_read;
				len -= page_data_read;
				this.lastOffset += page_data_read;
				this.filePosition += page_data_read;
				next_page = Convert.toInt(this.lastPageData, this.lastLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
			} while (len != 0 && next_page != 0);

			// check for end of file
			if (read_count == 0 && len != 0) {
				return -1;
			}

			// return number of bytes read
			return read_count;
		}
	}

	/**
	 * Reads a byte of data from this input stream. This method blocks if no input
	 * is yet available.
	 *
	 * @return the next byte of data, or <code>-1</code> if the end of the file is
	 *         reached.
	 * @exception IOException if an I/O error occurs.
	 */
	protected int read() throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===read()");
			}

			var len = this.read(this.smallBuf, 0, 1);

			if (len == 1) {
				return this.smallBuf[0] & 0x00FF;
			}
			return -1;
		}
	}

	/**
	 * Skips over and discards <code>n</code> bytes of data from the input stream.
	 * The <code>skip</code> method may, for a variety of reasons, end up skipping
	 * over some smaller number of bytes, possibly <code>0</code>. The actual number
	 * of bytes skipped is returned.
	 *
	 * @param n the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @exception IOException if an I/O error occurs.
	 */
	protected long skip(long n) throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===skip " + n);
			}

			return this.read(null, 0, (int) n);
		}
	}

	/**
	 * Returns the number of bytes that can be read from this file input stream
	 * without blocking.
	 *
	 * @return the number of bytes that can be read from this file input stream
	 *         without blocking.
	 * @exception IOException if an I/O error occurs.
	 */
	protected int available() throws IOException {
		synchronized (this.cache) {
			// check for no pages read
			if (this.lastPage == -1) {
				return 0;
			}
			return this.lastLen - this.lastOffset - 1;
		}
	}

	// --------
	// -------- Write methods
	// --------

	/**
	 * Writes the specified byte to this file output stream. Implements the
	 * <code>write</code> method of <code>OutputStream</code>.
	 *
	 * @param b the byte to be written.
	 * @exception IOException if an I/O error occurs.
	 */
	protected void write(int b) throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===write(int) " + b);
			}

			this.smallBuf[0] = (byte) b;

			this.write(this.smallBuf, 0, 1);
		}
	}

	/**
	 * Writes <code>len</code> bytes from the specified byte array starting at
	 * offset <code>off</code> to this file output stream.
	 *
	 * @param b   the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @exception IOException if an I/O error occurs.
	 */
	protected void write(byte b[], int off, int len) throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("===write(byte[],int,int) with data (" + len + ") :");
				this.debugDump(b, off, len);
			}

			// check for something to do
			if (len == 0) {
				return;
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// check for no pages read
			if (this.lastPage == -1) {
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=first write");
				}

				this.lastPage = this.feStartPage;
				this.lastOffset = 0;
				this.filePosition = 0;
			}

			try {
				// read the last page
				this.lastLen = this.cache.readPagePacket(this.lastPage, this.lastPageData, 0);

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("===write, readpagePacket " + this.lastPage + " got len " + this.lastLen);
				}

				int write_len;

				do {

					// check if room to write
					if (this.lastLen >= this.maxDataLen) {

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.print("=Need new page");
						}

						// get another page to write
						// get the pagebitmap
						this.readBitMap();

						// get the next available page
						var new_page = this.getFirstFreePage(false);

						// verify got a free page
						if (new_page < 0) {
							try {
								this.sync();
							} catch (OWSyncFailedException e) {
								// DRAIN
							}

							throw new IOException("Out of space on 1-Wire device");
						}

						// mark page used
						this.markPageUsed(new_page);

						// put blank data page in new page
						Convert.toByteArray(0, this.tempPage, 0, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(new_page, this.tempPage, 0, this.LEN_PAGE_PTR);

						// change next page pointer in last page
						Convert.toByteArray(new_page, this.lastPageData, this.lastLen - this.LEN_PAGE_PTR,
								this.LEN_PAGE_PTR);

						// put data page back in place with new next page pointer
						this.cache.writePagePacket(this.lastPage, this.lastPageData, 0, this.lastLen);

						// write the page bitmap
						this.writeBitMap();

						// update the directory entry to include this new page
						this.lastLen = this.cache.readPagePacket(this.fePage, this.lastPageData, 0);

						Convert.toByteArray(++this.feNumPages, this.lastPageData,
								this.feOffset + LEN_FILENAME + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(this.fePage, this.lastPageData, 0, this.lastLen);

						// make 'lastPage' the new empty page
						this.lastPageData[0] = 0;
						this.lastPage = new_page;
						this.lastLen = this.LEN_PAGE_PTR;
					}

					// calculate how much of the data can write to lastPage
					if (len > this.maxDataLen - this.lastLen) {
						write_len = this.maxDataLen - this.lastLen;
					} else {
						write_len = len;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("===write, len " + len + " maxDataLen " + this.maxDataLen + " lastLen "
								+ this.lastLen + " write_len " + write_len + " off " + off);
					}

					// copy the data
					System.arraycopy(b, off, this.lastPageData, this.lastLen - this.LEN_PAGE_PTR, write_len);

					// update the counters
					len -= write_len;
					off += write_len;
					this.lastLen += write_len;

					// set the next page pointer to end of file marker '0'
					Convert.toByteArray(0, this.lastPageData, this.lastLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

					// write the data
					this.cache.writePagePacket(this.lastPage, this.lastPageData, 0, this.lastLen);
				} while (len > 0);
			} catch (OneWireException e) {
				throw new IOException(e.toString());
			}
		}
	}

	// --------
	// -------- Info methods
	// --------

	/**
	 * Returns the name of the file or directory denoted by this abstract pathname.
	 * This is just the last name in the pathname's name sequence. If the pathname's
	 * name sequence is empty, then the empty string is returned.
	 *
	 * @return The name of the file or directory denoted by this abstract pathname,
	 *         or the empty string if this pathname's name sequence is empty
	 */
	protected String getName() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===getName()");
			}

			return this.pathToString(this.path, this.path.size() - 1, this.path.size(), true);
		}
	}

	/**
	 * Returns the pathname string of this abstract pathname's parent, or
	 * <code>null</code> if this pathname does not name a parent directory.
	 *
	 * <p>
	 * The <em>parent</em> of an abstract pathname consists of the pathname's
	 * prefix, if any, and each name in the pathname's name sequence except for the
	 * last. If the name sequence is empty then the pathname does not name a parent
	 * directory.
	 *
	 * @return The pathname string of the parent directory named by this abstract
	 *         pathname, or <code>null</code> if this pathname does not name a
	 *         parent
	 */
	protected String getParent() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===getParent(), path is null=" + (this.path == null));
			}

			if (this.path == null) {
				throw new NullPointerException("path is not valid");
			}

			if (this.path.size() >= 1) {
				return this.pathToString(this.path, 0, this.path.size() - 1, false);
			}
			return null;
		}
	}

	/**
	 * Converts this abstract pathname into a pathname string. The resulting string
	 * uses the {@link OWFile#separator default name-separator character} to
	 * separate the names in the name sequence.
	 *
	 * @return The string form of this abstract pathname
	 */
	protected String getPath() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===getPath(), path is null=" + (this.path == null));
			}

			if (this.path == null) {
				throw new NullPointerException("path is not valid");
			}

			return this.pathToString(this.verbosePath, 0, this.verbosePath.size(), false);
		}
	}

	/**
	 * Checks to see if the file exists
	 *
	 * @return true if the file exists and false otherwise
	 */
	protected boolean exists() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===exists()");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// check if this is the root
			if (this.path != null && this.path.size() == 0) {
				// force a check of the Filesystem if never been read
				if (this.pbmStartPage == -1) {
					try {
						this.readBitMap();
					} catch (OneWireException e) {
						return false;
					}
				}

				return true;
			}

			// attempt to open the file/directory
			try {
				this.open();

				return true;
			} catch (OWFileNotFoundException e) {
				return false;
			}
		}
	}

	/**
	 * Checks to see if can read the file associated with this descriptor.
	 *
	 * @return true if this file exists, false otherwise
	 */
	protected boolean canRead() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===canRead()");
			}

			return this.exists();
		}
	}

	/**
	 * Checks to see if the file represented by this descriptor is writable.
	 *
	 * @return true if this file exists and is not read only, false otherwise
	 */
	protected boolean canWrite() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===canWrite()");
			}

			if (!this.exists()) {
				return false;
			}
			if (this.isFile()) {
				return (this.feData[this.feOffset + LEN_FILENAME - 1] & 0x80) == 0;
			} else {
				return true;
			}
		}
	}

	/**
	 * Checks to see if this is a directory.
	 *
	 * @return true if this file exists and it is a directory, false otherwise
	 */
	protected boolean isDirectory() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===isDirectory()");
			}

			if (this.exists()) {
				return !this.isFile();
			}
			return false;
		}
	}

	/**
	 * Checks to see if this is a file
	 *
	 * @return true if this file exists and is a file, false otherwise
	 */
	protected boolean isFile() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===isFile()");
			}

			// check if this is the root
			if (this.path.size() == 0) {
				return false;
			}

			if (this.exists()) {
				return (this.feData[this.feOffset + LEN_FILENAME - 1] & 0x7F) != 0x7F;
			}
			return false;
		}
	}

	/**
	 * Checks to see if this directory is hidden.
	 *
	 * @return true if this is a directory and is marked as hidden, false otherwise
	 */
	protected boolean isHidden() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===isHidden()");
			}

			if (this.exists()) {

				// look at hidden flag in parent (if it has one)
				if (this.path.size() > 0) {
					if (this.isDirectory()) {
						var fl = this.path.elementAt(this.path.size() - 1);

						return (fl[LEN_FILENAME - 1] & 0x80) != 0;
					}
				}
			}

			return false;
		}
	}

	/**
	 * Get the estimated length of the file represented by this descriptor. This is
	 * calculated by looking at how may pages the file is using so is not a very
	 * accurate measure.
	 *
	 * @return estimated length of file in bytes
	 */
	protected long length() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===length()");
			}

			if (this.exists()) {
				return this.feNumPages * (this.maxDataLen - this.LEN_PAGE_PTR);
			}
			return 0;
		}
	}

	/**
	 * Delete this file or directory represented by this descriptor. Will fail if it
	 * is a read-only file or a non-empty directory.
	 *
	 * @return true if the file/directory was successfully deleted or false if not
	 */
	protected boolean delete() {
		synchronized (this.cache) {
			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			if (this.isFile()) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("===delete() is a file");
				}

				try {

					// remove the directory entry
					System.arraycopy(this.feData, this.feOffset + this.LEN_FILE_ENTRY, this.feData, this.feOffset,
							this.feLen - this.feOffset - this.LEN_FILE_ENTRY);

					this.feLen -= this.LEN_FILE_ENTRY;

					this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);

					// loop to remove all of the file pages 'free' only if not EPROM
					if (this.bitmapType != BM_CACHE) {

						// loop to read the rest of the pages and 'free' them
						var next_page = this.feStartPage;

						while (next_page != 0) {

							// free the page
							this.readBitMap();
							this.freePage(next_page);
							this.writeBitMap();

							// read the file page
							this.lastLen = this.cache.readPagePacket(next_page, this.lastPageData, 0);

							// get the next page pointer
							next_page = Convert.toInt(this.lastPageData, this.lastLen - this.LEN_PAGE_PTR,
									this.LEN_PAGE_PTR);
						}

						// update
						this.lastPage = -1;
						this.feStartPage = -1;
					}

					return true;
				} catch (OneWireException e) {
					return false;
				}
			}
			if (this.isDirectory()) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("===delete() is a directory");
				}

				try {

					// read the first page of the directory to see if empty
					var len = this.cache.readPagePacket(this.feStartPage, this.tempPage, 0);

					if (len != this.LEN_CONTROL_DATA + this.LEN_PAGE_PTR) {
						return false;
					}

					// remove the directory entry
					System.arraycopy(this.feData, this.feOffset + this.LEN_CONTROL_DATA, this.feData, this.feOffset,
							this.feLen - this.feOffset - this.LEN_CONTROL_DATA);

					this.feLen -= this.LEN_CONTROL_DATA;

					this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);

					// free the page
					this.readBitMap();
					this.freePage(this.feStartPage);
					this.writeBitMap();

					return true;
				} catch (OneWireException e) {
					return false;
				}
			} else {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("===delete() is neither file or directory so fail");
				}

				return false;
			}
		}
	}

	/**
	 * Returns an array of strings naming the files and directories in the directory
	 * denoted by this abstract pathname.
	 *
	 * <p>
	 * If this abstract pathname does not denote a directory, then this method
	 * returns <code>null</code>. Otherwise an array of strings is returned, one for
	 * each file or directory in the directory. Names denoting the directory itself
	 * and the directory's parent directory are not included in the result. Each
	 * string is a file name rather than a complete path.
	 *
	 * <p>
	 * There is no guarantee that the name strings in the resulting array will
	 * appear in any specific order; they are not, in particular, guaranteed to
	 * appear in alphabetical order.
	 *
	 * @return An array of strings naming the files and directories in the directory
	 *         denoted by this abstract pathname. The array will be empty if the
	 *         directory is empty. Returns <code>null</code> if this abstract
	 *         pathname does not denote a directory, or if an I/O error occurs.
	 */
	protected String[] list() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===list() string");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			if (!this.isDirectory()) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=not a directory so no list");
				}

				return null;
			}
			var entries = new Vector<String>(1);

			try {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=feStartPage " + this.feStartPage);
				}

				// loop though the entries and collect string reps
				var next_page = this.feStartPage;
				var build_buffer = new StringBuilder();
				int offset = this.LEN_CONTROL_DATA, len, i, page;

				do {
					page = next_page;

					// read the page
					len = this.cache.readPagePacket(page, this.tempPage, 0);

					// loop through the entries
					for (; offset < len - this.LEN_PAGE_PTR; offset += this.LEN_FILE_ENTRY) {
						build_buffer.setLength(0);

						for (i = 0; i < 4; i++) {
							if (this.tempPage[offset + i] != (byte) 0x20) {
								build_buffer.append((char) this.tempPage[offset + i]);
							} else {
								break;
							}
						}

						if ((byte) (this.tempPage[offset + 4] & 0x7F) != EXT_DIRECTORY) {
							build_buffer.append("." + Integer.toString(this.tempPage[offset + 4] & 0x7F));
						}

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("=entry= " + build_buffer.toString());
						}

						// only add if not hidden directory
						if (this.tempPage[offset + 4] != (byte) 0xFF) {
							// add to the vector of strings
							entries.addElement(build_buffer.toString());
						}
					}

					// get next page pointer to read
					next_page = Convert.toInt(this.tempPage, len - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
					offset = 0;

					// check for looping Filesystem
					if (entries.size() > this.totalPages) {
						return null;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("=next page = " + next_page);
					}
				} while (next_page != 0);
			} catch (OneWireException e) {

				// DRAIN
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("= " + e);
				}
			}

			// return the entries as an array of strings
			var strs = new String[entries.size()];
			for (var i = 0; i < strs.length; i++) {
				strs[i] = entries.elementAt(i);
			}
			return strs;
		}
	}

	/**
	 * Renames the file denoted by this abstract pathname.
	 *
	 * @param dest The new abstract pathname for the named file
	 *
	 * @return <code>true</code> if and only if the renaming succeeded;
	 *         <code>false</code> otherwise
	 *
	 * @throws NullPointerException If parameter <code>dest</code> is
	 *                              <code>null</code>
	 */
	protected boolean renameTo(OWFile dest) {
		if (dest == null) {
			throw new NullPointerException("Destination file is null");
		}

		synchronized (this.cache) {
			// make sure exists (also getting the file entry info)
			if (!this.exists()) {
				return false;
			}

			try {
				// get the file descriptor of the destination
				var dest_fd = dest.getFD();

				// create the new entry pointing to the old file
				dest_fd.create(false, this.isDirectory(), false, this.feStartPage, this.feNumPages);

				// delete the old entry
				this.feLen = this.cache.readPagePacket(this.fePage, this.feData, 0);
				System.arraycopy(this.feData, this.feOffset + this.LEN_FILE_ENTRY, this.feData, this.feOffset,
						this.feLen - this.feOffset - this.LEN_FILE_ENTRY);
				this.feLen -= this.LEN_FILE_ENTRY;
				this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);

				// open this file to make sure all file entry pointers get reset
				this.feStartPage = -1;
				try {
					this.open();
				} catch (OWFileNotFoundException e) {
					// DRAIN
				}

				return true;
			} catch (IOException e) {
				return false;
			} catch (OneWireException e) {
				return false;
			}
		}
	}

	/**
	 * Marks the file or directory named by this abstract pathname so that only read
	 * operations are allowed. After invoking this method the file or directory is
	 * guaranteed not to change until it is either deleted or marked to allow write
	 * access. Whether or not a read-only file or directory may be deleted depends
	 * upon the underlying system.
	 *
	 * @return <code>true</code> if and only if the operation succeeded;
	 *         <code>false</code> otherwise
	 */
	protected boolean setReadOnly() {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===setReadOnly()");
			}

			if (this.isFile()) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=is a file");
				}

				// mark the readonly bit in the file entry page
				this.feData[this.feOffset + LEN_FILENAME - 1] |= 0x80;

				try {
					// write new setting to cache
					this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);
				} catch (OneWireException e) {
					return false;
				}

				return true;
			}

			return false;
		}
	}

	/**
	 * Mark the current position in the file being read for later reference.
	 *
	 * @param readlimit limit to keep track of the current position
	 */
	protected void mark(int readlimit) {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===mark() with readlimit=" + readlimit + " current pos=" + this.filePosition);
			}

			this.markPosition = this.filePosition;
			this.markLimit = readlimit;
		}
	}

	/**
	 * Reset the the read of this file back to the marked position.
	 *
	 * @throws IOException when a read error occurs
	 */
	protected void reset() throws IOException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===reset() current pos=" + this.filePosition);
			}

			if (this.filePosition - this.markPosition > this.markLimit) {
				throw new IOException("File read beyond mark readlimit");
			}

			// reset the file
			this.lastPage = -1;

			// skip to the mark position
			this.skip(this.markPosition);
		}
	}

	// --------
	// -------- Page Bitmap Methods
	// --------

	/**
	 * Mark the specified page as used in the page bitmap.
	 *
	 * @param page number to mark as used
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	protected void markPageUsed(int page) throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===markPageUsed " + page);
			}

			if (this.bitmapType == BM_CACHE) {
				this.cache.markPageUsed(page);
			} else {
				// mark page used in cached bitmap of used pages
				Bit.arrayWriteBit(PAGE_USED, this.pbmBitOffset + page, this.pbmByteOffset, this.pbm);
			}
		}
	}

	/**
	 * free the specified page as being un-used in the page bitmap
	 *
	 * @param page number to mark as un-used
	 *
	 * @return true if the page as be been marked as un-used, false if the page is
	 *         on an OTP device and cannot be freed
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	protected boolean freePage(int page) throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("===freePage " + page);
			}

			if (this.bitmapType == BM_CACHE) {
				return this.cache.freePage(page);
			}
			// mark page used in cached bitmap of used pages
			Bit.arrayWriteBit(PAGE_NOT_USED, this.pbmBitOffset + page, this.pbmByteOffset, this.pbm);

			return true;
		}
	}

	/**
	 * Get the first free page from the page bitmap.
	 *
	 * @param counterPage <code> true </code> if page needed is a 'counter' page
	 *                    (used in for monetary files)
	 *
	 * @return first page number that is free to write
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	protected int getFirstFreePage(boolean counterPage) throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.print("===getFirstFreePage, counter " + counterPage);
			}

			if (this.bitmapType == BM_CACHE) {
				return this.cache.getFirstFreePage();
			}
			this.lastFreePage = 0;

			return this.getNextFreePage(counterPage);
		}
	}

	/**
	 * Get the next free page from the page bitmap.
	 *
	 * @param counterPage <code> true </code> if page needed is a 'counter' page
	 *                    (used in for monetary files)
	 *
	 * @return next page number that is free to write
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	protected int getNextFreePage(boolean counterPage) throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===getNextFreePage ");
			}

			if (this.bitmapType == BM_CACHE) {
				return this.cache.getNextFreePage();
			}
			for (var pg = this.lastFreePage; pg < this.totalPages; pg++) {
				if (Bit.arrayReadBit(this.pbmBitOffset + pg, this.pbmByteOffset, this.pbm) == PAGE_NOT_USED) {
					// check if need a counter page
					if (counterPage) {
						// counter page has extra info with COUNTER or MAC
						var pmb = this.getMemoryBankForPage(pg);
						if (pmb.hasExtraInfo() && pg != 8) {
							var ex_info = pmb.getExtraInfoDescription();
							if (ex_info.indexOf("counter") > -1 || ex_info.indexOf("MAC") > -1) {
								return pg;
							}
						}
						continue;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("=free page is " + pg);
					}

					this.lastFreePage = pg + 1;

					return pg;
				}
			}

			return -1;
		}
	}

	/**
	 * Gets the number of bytes available on this device for file and directory
	 * information.
	 *
	 * @return number of free bytes
	 *
	 * @throws OneWireException when an IO exception occurs
	 */
	protected int getFreeMemory() throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===getFreeMemory()");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			if (this.bitmapType == BM_CACHE) {
				return this.cache.getNumberFreePages() * (this.maxDataLen - this.LEN_PAGE_PTR);
			}
			// read the bitmap
			this.readBitMap();

			var free_pages = 0;
			for (var pg = 0; pg < this.totalPages; pg++) {
				if (Bit.arrayReadBit(this.pbmBitOffset + pg, this.pbmByteOffset, this.pbm) == PAGE_NOT_USED) {
					free_pages++;
				}
			}

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("=num free pages = " + free_pages);
			}

			return free_pages * (this.maxDataLen - this.LEN_PAGE_PTR);
		}
	}

	/**
	 * Write the page bitmap back to the device.
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	protected void writeBitMap() throws OneWireException {
		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===writeBitMap() ");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			if (this.bitmapType == BM_LOCAL) {
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=local ");
				}

				var pg_len = this.cache.readPagePacket(0, this.tempPage, 0);

				// check to see if the bitmap has changed
				for (var i = 3; i < 7; i++) {
					if ((this.tempPage[i] & 0x00FF) != (this.pbm[i] & 0x00FF)) {
						System.arraycopy(this.pbm, 3, this.tempPage, 3, 4);
						this.cache.writePagePacket(0, this.tempPage, 0, pg_len);
						break;
					}
				}
			} else if (this.bitmapType == BM_FILE) {
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=file ");
				}

				// FILE type page bitmap (got start page from validateFileSystem)
				int offset = 0, pg = this.pbmStartPage, len;

				// loop through all of the pages in the bitmap
				for (var pg_cnt = 0; pg_cnt < this.pbmNumPages; pg_cnt++) {
					// read the current bitmap just to get the next page pointer and length
					len = this.cache.readPagePacket(pg, this.tempPage, 0);

					// check to see if this bitmap segment has changed
					for (var i = 0; i < len - this.LEN_PAGE_PTR; i++) {
						if ((this.tempPage[i] & 0x00FF) != (this.pbm[i + offset] & 0x00FF)) {
							// copy new bitmap value to it
							System.arraycopy(this.pbm, offset, this.tempPage, 0, len - this.LEN_PAGE_PTR);

							// write back to device
							this.cache.writePagePacket(pg, this.tempPage, 0, len);

							break;
						}
					}

					// get next page number to read
					pg = Convert.toInt(this.tempPage, len - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
					offset += len - this.LEN_PAGE_PTR;
				}
			}
		}
	}

	/**
	 * Read the page bitmap.
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	protected void readBitMap() throws OneWireException {
		int len;

		synchronized (this.cache) {
			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===readBitMap() ");
			}

			// clear last page read flag in the cache
			this.cache.clearLastPageRead();

			// check to see if the directory has been read to know where the page bitmap is
			if (this.pbmStartPage == -1) {
				this.fePage = 0;
				this.feLen = this.cache.readPagePacket(this.fePage, this.feData, 0);
				this.validateFileSystem();
			}

			// depending on the type of the page bitmap, read it
			if (this.bitmapType == BM_LOCAL) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=local ");
				}

				this.cache.readPagePacket(0, this.pbm, 0);
			} else if (this.bitmapType == BM_FILE) {

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=file ");
				}

				// FILE type page bitmap (got start page from validateFileSystem)
				int offset = 0, pg = this.pbmStartPage;

				for (var pg_cnt = 0; pg_cnt < this.pbmNumPages; pg_cnt++) {
					len = this.cache.readPagePacket(pg, this.pbm, offset);
					pg = Convert.toInt(this.pbm, offset + len - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
					offset += len - this.LEN_PAGE_PTR;

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("=pg " + pg + " len " + len + " offset " + offset);
						// debugDump(pbm, offset + len - LEN_PAGE_PTR, LEN_PAGE_PTR);
					}
				}

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=pbm is ");
					this.debugDump(this.pbm, 0, this.pbm.length);
				}
			}
		}
	}

	/**
	 * Gets an array of integers that represents the page list of the file or
	 * directory represented by this OWFile.
	 *
	 * @exception OneWireException if an I/O error occurs.
	 */
	protected int[] getPageList() throws OneWireException {
		var page_list = new int[this.feNumPages + 10];
		int cnt = 0, len;
		var next_page = this.feStartPage;

		// clear last page read flag in the cache
		this.cache.clearLastPageRead();

		// loop to read all of the pages
		do {
			// check list for size limit
			if (cnt >= page_list.length) {
				// grow this list by 10
				var temp = new int[page_list.length + 10];
				System.arraycopy(page_list, 0, temp, 0, page_list.length);
				page_list = temp;
			}

			// add to the list
			page_list[cnt++] = next_page;

			// read the file page
			len = this.cache.readPagePacket(next_page, this.tempPage, 0);

			// get the next page pointer
			next_page = Convert.toInt(this.tempPage, len - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

			// check for looping pages
			if (cnt > this.totalPages) {
				throw new OneWireException("Error in Filesystem, looping pointers");
			}
		} while (next_page != 0);

		// create the return array
		var rt_array = new int[cnt];
		System.arraycopy(page_list, 0, rt_array, 0, cnt);

		return rt_array;
	}

	/**
	 * Returns an integer which represents the starting memory page of the file or
	 * directory represented by this OWFile.
	 *
	 * @return The starting page of the file or directory.
	 *
	 * @exception IOException if the file doesn't exist
	 */
	protected int getStartPage() throws IOException {
		return this.feStartPage;
	}

	/**
	 * Gets the memory bank object for the specified page. This is significant if
	 * the Filesystem spans memory banks on the same or different devices.
	 */
	protected PagedMemoryBank getMemoryBankForPage(int page) {
		return this.cache.getMemoryBankForPage(page);
	}

	/**
	 * Gets the local page number on the memory bank object for the specified page.
	 * This is significant if the Filesystem spans memory banks on the same or
	 * different devices.
	 */
	protected int getLocalPage(int page) {
		return this.cache.getLocalPage(page);
	}

	// --------
	// -------- Private methods
	// --------

	/**
	 * Convert the specified vector path into a string.
	 *
	 * @param tempPath   vector of byte arrays that represents the path
	 * @param beginIndex start index to convert
	 * @param endIndex   end iindex to convert
	 * @param single     true if only need a single field not a path
	 *
	 * @return string representation of the specified path
	 */
	private String pathToString(Vector<byte[]> tempPath, int beginIndex, int endIndex, boolean single) {
		if (beginIndex < 0) {
			return null;
		}

		byte[] name;
		var build_buffer = new StringBuilder(single ? "" : OWFile.separator);

		for (var element = beginIndex; element < endIndex; element++) {
			name = tempPath.elementAt(element);

			if (!single && element != beginIndex) {
				build_buffer.append(OWFile.separatorChar);
			}

			for (var i = 0; i < 4; i++) {
				if (name[i] == (byte) 0x20) {
					break;
				}
				build_buffer.append((char) name[i]);
			}

			if ((byte) (name[4] & 0x7F) != EXT_DIRECTORY && name[4] != EXT_UNKNOWN) {
				build_buffer.append("." + Integer.toString(name[4] & 0x7F));
			}
		}

		if (build_buffer.length() == 0) {
			return null;
		}
		return build_buffer.toString();
	}

	/**
	 * Verifies the path up to the specified depth. Sets feStartPage and feNumPages.
	 *
	 * @param depth of path to verify
	 *
	 * @return true if the path is valid, false if elements not found
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	private boolean verifyPath(int depth) throws OneWireException {
		byte[] element;

		this.feStartPage = 0;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===verifyPath() depth=" + depth);
		}

		for (var element_num = 0; element_num < depth; element_num++) {
			element = this.path.elementAt(element_num);

			// remember where the parent entry is
			var feParentPage = this.feStartPage;
			// attempt to find the element starting at the last entry
			if (!this.findElement(this.feStartPage, element, 0)) {
				return false;
			}

			// get the next entry start
			this.feStartPage = Convert.toInt(this.feData, this.feOffset + LEN_FILENAME, this.LEN_PAGE_PTR);
			this.feNumPages = Convert.toInt(this.feData, this.feOffset + LEN_FILENAME + this.LEN_PAGE_PTR,
					this.LEN_PAGE_PTR);
		}

		return true;
	}

	/**
	 * Search for the specified element staring at the current file entry page
	 * startPage. Set variables fePage,feOffset, and feData.
	 *
	 * @param startPage directory page to start looking for the element on
	 * @param element   element to search for
	 * @param offset    offset into element byte array where element is
	 *
	 * @return true if the element was found and the instance variables
	 *         fePage,feOffset, and feData have been set
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	private boolean findElement(int startPage, byte[] element, int offset) throws OneWireException {
		var next_page = startPage;

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("===findElement() start page=" + startPage + " element:");
			this.debugDump(element, offset, 5);
		}

		// clear last page read flag in the cache
		this.cache.clearLastPageRead();

		// read the 1-Wire device to find a file/directory reference
		this.feOffset = this.LEN_CONTROL_DATA;

		do {
			this.fePage = next_page;

			// read the page
			this.feLen = this.cache.readPagePacket(this.fePage, this.feData, 0);

			// if just read root directory, check filesystem
			if (this.fePage == 0) {
				this.readBitMap();
			}

			// loop through the entries
			for (; this.feOffset < this.feLen - this.LEN_PAGE_PTR; this.feOffset += this.LEN_FILE_ENTRY) {
				// compare with current element
				if (this.elementEquals(element, offset, this.feData, this.feOffset)) {

					// copy over any read-only or hidden flag in element name
					if ((this.feData[this.feOffset + LEN_FILENAME - 1] & 0x80) != 0) {
						element[offset + LEN_FILENAME - 1] |= 0x80;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.println("=found on page " + this.fePage + " at offset " + this.feOffset);
					}

					return true;
				}
			}

			// get next page pointer to read
			next_page = Convert.toInt(this.feData, this.feLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

			// reset loop start
			if (next_page != 0) {
				this.feOffset = 0;
			}
		} while (next_page != 0);

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("=NOT found");
		}

		// end of directory and no entry found
		return false;
	}

	/**
	 * Compare if two path elements are equal.
	 *
	 * @param file1   first file
	 * @param offset1 first file offset
	 * @param file2   second file
	 * @param offset2 second file offset
	 *
	 * @return
	 */
	private boolean elementEquals(byte[] file1, int offset1, byte[] file2, int offset2) {
		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("===elementEquals()  ");
			this.debugDump(file1, offset1, 5);
			System.out.print("=to                 ");
			this.debugDump(file2, offset2, 5);
		}

		for (var i = 0; i < 4; i++) {
			if (file1[offset1 + i] != file2[offset2 + i]) {
				return false;
			}
		}

		// check if type is inknown (either a file with 000 extension or directory)
		if (file1[offset1 + 4] == EXT_UNKNOWN) {
			if ((file2[offset2 + 4] & 0x7F) == 0) {
				file1[offset1 + 4] = 0;
			} else if ((file2[offset2 + 4] & 0x7F) == EXT_DIRECTORY) {
				file1[offset1 + 4] = EXT_DIRECTORY;
			}
		}

		return (byte) (file1[offset1 + 4] & 0x7F) == (byte) (file2[offset2 + 4] & 0x7F);
	}

	/**
	 * Read the current page <code>lastPage</code> and place it in the
	 * <code>lastPageData</code> buffer and set the <code>lastLen</code>.
	 *
	 * @throws IOException when an IO error occurs
	 */
	private void fetchPage() throws IOException {
		try {

			// \\//\\//\\//\\//\\//\\//\\//
			if (doDebugMessages) {
				System.out.println("===fetchPage() " + this.lastPage);
			}

			this.lastLen = this.cache.readPagePacket(this.lastPage, this.lastPageData, 0);
			this.lastOffset = 0;
		} catch (OneWireException e) {
			throw new IOException(e.toString());
		}
	}

	/**
	 * Create a file or directory entry in the current directory page specified with
	 * fePage, feOffset, and feData.
	 *
	 * @param newEntry       file or directory entry to create
	 * @param startPage      page that the elements data (-1 if has no data yet)
	 * @param numberPages    number of page of the elements data
	 * @param prevEntry      previous entry used for back reference in creating new
	 *                       directories
	 * @param prevEntryStart previous entry start page for directory back reference
	 *
	 * @throws FileNotFoundException if the filesystem runs out of space or if an IO
	 *                               error occurs
	 */
	private void createEntry(byte[] newEntry, int startPage, int numberPages, byte[] prevEntry, int prevEntryStart)
			throws OWFileNotFoundException {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.print("===createEntry() ");
			System.out.print("=prevEntryStart " + prevEntryStart + " ");
			this.debugDump(newEntry, 0, 5);
			this.debugDump(this.feData, 0, this.feLen);
		}

		// clear last page read flag in the cache
		this.cache.clearLastPageRead();

		int new_page;

		try {

			// check if room in current page
			if (this.feLen + this.LEN_FILE_ENTRY <= this.maxDataLen) {

				// add to current page
				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=add to current dir page " + this.fePage);
				}

				// get the pagebitmap
				this.readBitMap();

				// check if this is just a new entry pointing to an old file
				if (startPage != -1) {
					// instead of getting a new file, use the old file start
					new_page = startPage;
				} else {
					// get a new file to represent the new file/directory location

					// get the next available page
					new_page = this.getFirstFreePage(newEntry[4] == (byte) 102 || newEntry[4] == (byte) 101);

					// verify got a free page
					if (new_page < 0) {
						try {
							this.sync();
						} catch (OWSyncFailedException e) {
							// DRAIN
						}

						// if extension is 101 or 102, it could be there is not COUNTER pages
						if (newEntry[4] == (byte) 102 || newEntry[4] == (byte) 101) {
							throw new OWFileNotFoundException(
									"Out of space on 1-Wire device, or no secure pages available");
						}
						throw new OWFileNotFoundException("Out of space on 1-Wire device");
					}
				}

				// get next page pointer
				var npp = Convert.toInt(this.feData, this.feLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

				// copy the file name into
				System.arraycopy(newEntry, 0, this.feData, this.feLen - this.LEN_PAGE_PTR, LEN_FILENAME);
				Convert.toByteArray(new_page, this.feData, this.feLen - this.LEN_PAGE_PTR + LEN_FILENAME,
						this.LEN_PAGE_PTR);
				Convert.toByteArray(numberPages == -1 ? 1 : numberPages, this.feData,
						this.feLen - this.LEN_PAGE_PTR + LEN_FILENAME + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

				this.feOffset = this.feLen - this.LEN_PAGE_PTR;
				this.feLen = this.feLen + this.LEN_FILE_ENTRY;

				// restore next page pointer
				Convert.toByteArray(npp, this.feData, this.feLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

				// check if this is not a rename operation
				if (startPage == -1) {
					// no rename, so write the new file/directory starting data

					// mark page used
					this.markPageUsed(new_page);

					// put blank data page or directory entry in new page
					if ((newEntry[4] & 0x7F) == EXT_DIRECTORY) {
						// Directory Marker 'DM'
						this.tempPage[0] = this.LEN_PAGE_PTR == 1 ? (byte) 0x0A : (byte) 0x0B;
						this.tempPage[0] |= this.owd.length == 1 ? (byte) 0xA0 : 0xB0;

						// dummy byte
						this.tempPage[1] = 0;

						System.arraycopy(prevEntry, 0, this.tempPage, 2, 4);
						Convert.toByteArray(prevEntryStart, this.tempPage, 6, this.LEN_PAGE_PTR);
						// set next page pointer to end
						Convert.toByteArray(0, this.tempPage, 6 + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(new_page, this.tempPage, 0, 6 + this.LEN_PAGE_PTR * 2);
					} else {
						Convert.toByteArray(0, this.smallBuf, 0, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(new_page, this.smallBuf, 0, this.LEN_PAGE_PTR);
					}
				}

				// put new directory page in place
				this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);
			} else {
				// need a new directory page

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=need a new dir page ");
				}

				// get the pagebitmap
				this.readBitMap();

				// get new page for directory
				var new_dir_page = this.getFirstFreePage(false);

				// verify got a free page
				if (new_dir_page < 0) {
					try {
						this.sync();
					} catch (OWSyncFailedException e) {
						// DRAIN
					}

					throw new OWFileNotFoundException("Out of space on 1-Wire device");
				}

				// mark page used
				this.markPageUsed(new_dir_page);

				// check if this is just a new entry pointing to an old file
				if (startPage != -1) {
					// instead of getting a new file, use the old file start
					new_page = startPage;
				} else {
					// get a new file to represent the new file/directory location

					// get new page for the file
					new_page = this.getNextFreePage(newEntry[4] == (byte) 102 || newEntry[4] == (byte) 101);

					// verify got a free page
					if (new_page < 0) {
						try {
							this.sync();
						} catch (OWSyncFailedException e) {
							// DRAIN
						}

						// if extension is 101 or 102, it could be there is not COUNTER pages
						if (newEntry[4] == (byte) 102 || newEntry[4] == (byte) 101) {
							throw new OWFileNotFoundException(
									"Out of space on 1-Wire device, or no secure pages available");
						}
						throw new OWFileNotFoundException("Out of space on 1-Wire device");
					}

					// mark page used
					this.markPageUsed(new_page);
				}

				// create the new directory entry page
				System.arraycopy(newEntry, 0, this.lastPageData, 0, LEN_FILENAME);
				Convert.toByteArray(new_page, this.lastPageData, LEN_FILENAME, this.LEN_PAGE_PTR);
				Convert.toByteArray(numberPages == -1 ? 1 : numberPages, this.lastPageData,
						LEN_FILENAME + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
				this.feOffset = 0;

				// set next page pointer to end
				Convert.toByteArray(0, this.lastPageData, this.LEN_FILE_ENTRY, this.LEN_PAGE_PTR);

				// put new directory page in place
				this.cache.writePagePacket(new_dir_page, this.lastPageData, 0, this.LEN_FILE_ENTRY + this.LEN_PAGE_PTR);

				// check if this is not a rename operation
				if (startPage == -1) {
					// is not a rename operation so write new file/directory start data (stub)

					// write the new file/directory page
					if ((newEntry[4] & 0x7F) == EXT_DIRECTORY) {
						// Directory Marker 'DM'
						this.tempPage[0] = this.LEN_PAGE_PTR == 1 ? (byte) 0x0A : (byte) 0x0B;
						this.tempPage[0] |= this.owd.length == 1 ? (byte) 0xA0 : 0xB0;

						// dummy byte
						this.tempPage[1] = 0;

						System.arraycopy(prevEntry, 0, this.tempPage, 2, 4);
						Convert.toByteArray(prevEntryStart, this.tempPage, 6, this.LEN_PAGE_PTR);
						// set next page pointer to end
						Convert.toByteArray(0, this.tempPage, 6 + this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(new_page, this.tempPage, 0, 6 + this.LEN_PAGE_PTR * 2);
					} else {
						Convert.toByteArray(0, this.smallBuf, 0, this.LEN_PAGE_PTR);
						this.cache.writePagePacket(new_page, this.smallBuf, 0, this.LEN_PAGE_PTR);
					}
				}

				// update the page pointer in the old directory page
				Convert.toByteArray(new_dir_page, this.feData, this.feLen - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);

				// put new directory page in place
				this.cache.writePagePacket(this.fePage, this.feData, 0, this.feLen);

				// set file entry info to the new directory page
				this.fePage = new_dir_page;
				this.feOffset = 0;
				this.feLen = this.LEN_FILE_ENTRY + this.LEN_PAGE_PTR;
				System.arraycopy(this.lastPageData, 0, this.feData, 0, this.feLen);

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println(
							"=NEW fePage=" + this.fePage + " feOffset " + this.feOffset + " feLen " + this.feLen);
					this.debugDump(this.feData, 0, this.feLen);
				}
			}
			// set the page bitmap
			this.writeBitMap();

			// if the new entry is not a directory
			if ((newEntry[4] & 0x7F) != EXT_DIRECTORY) {

				// setup the pointers for writing
				this.filePosition = 0;
				this.lastPage = new_page;
				this.lastOffset = 0;
				this.lastLen = 1;
			}
		} catch (OneWireException e) {
			throw new OWFileNotFoundException(e.toString());
		}
	}

	/**
	 * Check to see if the Filesystem is valid based on the root directory provided
	 * in feData
	 *
	 * @throws OneWireException if the filesystem is invalid
	 */
	private void validateFileSystem() throws OneWireException {

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("===validateFileSystem()");
		}

		// clear last page read flag in the cache
		this.cache.clearLastPageRead();

		// check for SATELLITE
		this.LEN_PAGE_PTR = (this.feData[0] & 0x000F) == 0x0B ? 2 : 1;
		if ((this.feData[0] & 0x00F0) == 0xB0 && (this.feData[1 + this.LEN_PAGE_PTR] & 0x0002) == 0) {
			// read the DM page
			var len = this.cache.readPagePacket(Convert.toInt(this.feData, 1, this.LEN_PAGE_PTR), this.dmBuf, 0);

			if (len < 8 + this.LEN_PAGE_PTR) {
				throw new OneWireIOException(
						"Invalid filesystem, this is a satellite device with invalid MASTER reference");
			}
			// copy address to temp buff (or piece of address number)
			System.arraycopy(this.dmBuf, 0, this.addrBuf, 0, 8);

			// get ref to adapter to create the new MASTER container
			var adapter = this.owd[0].getAdapter();
			this.owd = new OneWireContainer[1];
			this.owd[0] = adapter.getDeviceContainer(this.addrBuf);

			// check for overdrive
			if (adapter.getSpeed() == DSPortAdapter.SPEED_OVERDRIVE
					&& this.owd[0].getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE) {
				this.owd[0].setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
			}

			// free the current cache
			this.cache.removeOwner(this);
			if (this.cache.noOwners()) {
				// remove the cache from the hash
				memoryCacheHash.remove(this.address);
				this.cache = null;
			}

			// recreate the cache and setup
			this.setupFD(this.owd, this.rawPath);

			// get the new root dir
			this.fePage = 0;
			this.feLen = this.cache.readPagePacket(0, this.feData, 0);

			// make sure this is not a SATELLITE also (could be recursively bad!)
			this.LEN_PAGE_PTR = (this.feData[0] & 0x000F) == 0x0B ? 2 : 1;
			if ((this.feData[0] & 0x000F) == 0x0B && (this.feData[1 + this.LEN_PAGE_PTR] & 0x0002) == 0) {
				throw new OneWireIOException(
						"Invalid filesystem, this is a satellite device, pointing to another satellite?");
			}

			// call this recursively
			this.validateFileSystem();
			return;
		}

		// check for MASTER
		if ((this.feData[0] & 0x00F0) == 0xB0 && (this.feData[1 + this.LEN_PAGE_PTR] & 0x0002) == 0x0002) {
			// read the DM file
			var page = Convert.toInt(this.feData, 1, this.LEN_PAGE_PTR);

			// make sure is valid page
			if (page == 0 || page >= this.totalPages) {
				throw new OneWireIOException("Invalid Filesystem, Device Map page number not valid.");
			}

			// check for incorrect or incomplete device list (member owd)
			var num_devices = this.verifyDeviceMap(page, 0, false);
			if (num_devices > 0) {
				// create a new list
				this.verifyDeviceMap(page, num_devices,
						this.owd[0].getAdapter().getSpeed() == DSPortAdapter.SPEED_OVERDRIVE);

				// free the current cache
				this.cache.removeOwner(this);
				if (this.cache.noOwners()) {
					// remove the cache from the hash
					memoryCacheHash.remove(this.address);
					this.cache = null;
				}

				// recreate the cache and setup
				this.setupFD(this.owd, this.rawPath);

				// continue on with the root dir
				this.fePage = 0;
				this.feLen = this.cache.readPagePacket(0, this.feData, 0);
			}
		}

		// check and verify that this is a valid directory
		if (this.totalPages <= 256 && (this.feData[0] & 0x000F) != 0x0A
				|| this.totalPages > 256 && (this.feData[0] & 0x000F) != 0x0B) {
			throw new OneWireIOException("Invalid Filesystem marker found, number of pages incorrect");
		}

		if (this.owd.length == 1 && (this.feData[0] & 0x00F0) != 0x00A0
				|| this.owd.length > 1 && (this.feData[0] & 0x00F0) != 0x00B0) {
			throw new OneWireIOException("Invalid Filesystem marker found, multi-device marker incorrect");
		}

		// check where page bitmap is
		if ((this.feData[1 + this.LEN_PAGE_PTR] & 0x0080) != 0) {
			this.bitmapType = BM_LOCAL;
			this.pbmByteOffset = 2 + this.LEN_PAGE_PTR;
			this.pbmBitOffset = 0;
			this.pbmStartPage = 0; // used as flag to see if filesystem has been validated
		} else if (this.bitmapType != BM_CACHE) {
			this.bitmapType = BM_FILE;
			this.pbmStartPage = Convert.toInt(this.feData, this.LEN_CONTROL_DATA - this.LEN_PAGE_PTR * 2,
					this.LEN_PAGE_PTR);
			this.pbmNumPages = Convert.toInt(this.feData, this.LEN_CONTROL_DATA - this.LEN_PAGE_PTR, this.LEN_PAGE_PTR);
			this.pbmByteOffset = 0;
			this.pbmBitOffset = 0;

			// make sure the number of pages in a FILE BM is correct
			var pbm_bytes = this.totalPages / 8;
			var pgs = pbm_bytes / (this.maxDataLen - this.LEN_PAGE_PTR);
			if (pbm_bytes % (this.maxDataLen - this.LEN_PAGE_PTR) > 0) {
				pgs++;
			}

			if (this.pbmNumPages != pgs) {
				throw new OneWireIOException("Invalid Filesystem, incorrect number of pages in remote bitmap file!");
			}
		} else {
			this.pbmStartPage = 0;
		}

		// \\//\\//\\//\\//\\//\\//\\//
		if (doDebugMessages) {
			System.out.println("= is valid, pbmStartPage=" + this.pbmStartPage);
		}
	}

	/**
	 * Verify the Device Map of a MASTER device is correct.
	 *
	 * @param startPage          starting page number of the device map file
	 * @param numberOfContainers to re-create the OneWireContainer array in the
	 *                           instance variable from the devices listed in the
	 *                           device map 'owd[]'. Zero indicates leave the list
	 *                           alone. &gt;0 means recreate the array keeping the
	 *                           same MASTER device.
	 * @param setOverdrive       <code> true </code> if set new containers to do a
	 *                           max speed of overdrive if possible
	 *
	 * @return the number of devices in the device map if the current device list is
	 *         INVALID and returns zero if the current device list is VALID.
	 *
	 * @throws OneWireException when an IO error occurs
	 */
	protected int verifyDeviceMap(int startPage, int numberOfContainers, boolean setOverdrive) throws OneWireException {
		int len, data_len;
		var ow_cnt = 1;
		var addr_offset = 0;
		var pg_offset = 0;
		int copy_len;
		DSPortAdapter adapter = null;

		// flag to indicate the device list 'owd' list is valid
		var list_valid = true;

		// first page to read
		var page = startPage;

		// clear last page read flag in the cache
		this.cache.clearLastPageRead();

		// check to see if need to create a new array for the new list of containers
		if (numberOfContainers > 0) {
			// get reference to the adapter for use in creating containers
			adapter = this.owd[0].getAdapter();

			var master_owc = this.owd[0];
			this.owd = new OneWireContainer[numberOfContainers + 1];
			this.owd[0] = master_owc;
		}

		// loop to read the Device Map file
		do {
			// read the first file page
			len = this.cache.readPagePacket(page, this.dmBuf, 0);
			data_len = len - this.LEN_PAGE_PTR;

			// loop through the device addresses in the device map file
			while (pg_offset < data_len) {
				if (data_len - pg_offset >= 8 - addr_offset) {
					copy_len = 8 - addr_offset;
				} else {
					copy_len = data_len - pg_offset;
				}

				// copy address to temp buff (or piece of address number)
				System.arraycopy(this.dmBuf, pg_offset, this.addrBuf, addr_offset, copy_len);

				// increment offsets
				addr_offset += copy_len;
				pg_offset += copy_len;

				// convert completed address to long and compare
				if (addr_offset >= 8) {
					// check if creating OneWireContainers
					if (numberOfContainers > 0) {
						this.owd[ow_cnt] = adapter.getDeviceContainer(this.addrBuf);

						// set new container to correct speed
						if (setOverdrive && this.owd[ow_cnt].getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE) {
							this.owd[ow_cnt].setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
						}
					} else // not creating containers so just check if correct
					if (this.owd.length <= ow_cnt) {
						list_valid = false;
					} else if (Address.toLong(this.addrBuf) != this.owd[ow_cnt].getAddressAsLong()) {
						list_valid = false;
					}

					ow_cnt++;
					addr_offset = 0;
				}
			}

			// get the next page
			page = Convert.toInt(this.dmBuf, data_len, this.LEN_PAGE_PTR);

			pg_offset = 0;
		} while (page != 0);

		// verify correct number of devices found
		return list_valid ? 0 : ow_cnt - 1;
	}

	/**
	 * Parse the provided raw path and set the provided vector.
	 *
	 * @param rawPath path to parse
	 */
	private boolean parsePath(String rawPath, Vector<byte[]> parsedPath) {
		// parse name into a vector of byte arrays using the file structure
		int index, last_index = 0, period_index, i, name_len;
		String field;
		byte[] name;

		do {
			index = rawPath.indexOf(OWFile.separator, last_index);
			name_len = 0;

			// check if this is the last field
			if (index == -1 && last_index < rawPath.length()) {
				index = rawPath.length();
			}

			// not done
			if (index > 0) {

				// get the field
				field = rawPath.substring(last_index, index);

				// check for bogus field
				if (field.length() == 0) {
					return false;
				}

				// create byte array for field
				name = new byte[LEN_FILENAME];

				System.arraycopy(this.initName, 0, name, 0, LEN_FILENAME);

				// check if this is: ".", "..", or "name.number"
				period_index = field.indexOf(".", 0);

				// period not in field
				if (period_index == -1) {

					// check for valid length
					if (field.length() > 4) {
						return false;
					}

					// is name only
					System.arraycopy(field.getBytes(), 0, name, 0, field.length());
					name_len = field.length();

					// check if last field
					if (index != rawPath.length()) {
						name[4] = EXT_DIRECTORY;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.print("=Parse, directory: ");
						this.debugDump(name, 0, 5);
					}
				} else // assume that if first char is '.' then must be '.' or '..' directory refs
				if (period_index == 0) {

					// check for valid length
					if (field.length() > 2) {

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("=Parse ERROR, '.' field length > 2 ");
						}

						return false;
					}

					System.arraycopy(field.getBytes(), 0, name, 0, field.length());
					name[4] = EXT_DIRECTORY;
					name_len = field.length();

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.print("=Parse, directory: ");
						this.debugDump(name, 0, 5);
					}
				} else {

					// is name.number file
					name_len = period_index;

					// get name part
					System.arraycopy(field.getBytes(), 0, name, 0, period_index);

					// get the name part
					try {
						name[4] = (byte) Integer.parseInt(field.substring(period_index + 1));
					} catch (NumberFormatException e) {
						return false;
					}

					// \\//\\//\\//\\//\\//\\//\\//
					if (doDebugMessages) {
						System.out.print("=Parse, file: ");
						this.debugDump(name, 0, 5);
					}

					// check if invalid field or extension
					if (index != rawPath.length() || (name[4] & 0x00FF) > 102) {

						// \\//\\//\\//\\//\\//\\//\\//
						if (doDebugMessages) {
							System.out.println("=Parse ERROR, extension > 102 or spaces detected");
						}

						return false;
					}
				}

				// make sure file name is exceptable
				for (i = 0; i < name_len; i++) {
					if ((name[i] & 0x00FF) < 0x21 || (name[i] & 0x00FF) > 0x7E) {
						return false;
					}
				}

				// add to path vector
				parsedPath.addElement(name);
			}

			last_index = index + 1;
		} while (index > -1);
		return true;
	}

	// --------
	// -------- Misc Utility Methods
	// --------

	/**
	 * Atomically creates a new, empty file named by this abstract pathname if and
	 * only if a file with this name does not yet exist. The check for the existence
	 * of the file and the creation of the file if it does not exist are a single
	 * operation that is atomic with respect to all other filesystem activities that
	 * might affect the file.
	 *
	 * @return <code>true</code> if the named file does not exist and was
	 *         successfully created; <code>false</code> if the named file already
	 *         exists
	 *
	 * @throws IOException If an I/O error occurred
	 *
	 */
	protected boolean createNewFile() throws IOException {
		if (this.exists()) {
			return false;
		}
		try {
			this.create(false, false, false, -1, -1);
		} catch (OWFileNotFoundException e) {
			throw new IOException(e.toString());
		}

		return true;
	}

	/**
	 * Computes a hash code for this abstract pathname. Because equality of abstract
	 * pathnames is inherently system-dependent, so is the computation of their hash
	 * codes. On UNIX systems, the hash code of an abstract pathname is equal to the
	 * exclusive <em>or</em> of its pathname string and the decimal value
	 * <code>1234321</code>. On Win32 systems, the hash code is equal to the
	 * exclusive <em>or</em> of its pathname string, converted to lower case, and
	 * the decimal value <code>1234321</code>.
	 *
	 * @return A hash code for this abstract pathname
	 */
	protected int getHashCode() {
		synchronized (this.cache) {
			int i, j, hash = 0;
			var this_path = this.owd[0].getAddressAsString() + this.getPath();
			var path_bytes = this_path.getBytes();

			for (i = 0; i < path_bytes.length / 4; i++) {
				hash ^= Convert.toInt(path_bytes, i * 4, 4);
			}

			for (j = 0; j < path_bytes.length % 4; j++) {
				hash ^= path_bytes[i * 4 + j] & 0x00FF;
			}

			return hash;
		}
	}

	/**
	 * Gets the OneWireContainers that represent this Filesystem.
	 *
	 *
	 * @return array of OneWireContainer's that represent this Filesystem.
	 *
	 */
	protected OneWireContainer[] getOneWireContainers() {
		return this.owd;
	}

	/**
	 * Free's this file descriptors system resources.
	 *
	 */
	protected void free() {
		synchronized (this.cache) {
			// if opened to write then remove the block
			if (this.openedToWrite) {
				this.cache.removeWriteOpen(this.owd[0].getAddressAsString() + this.getPath());
			}

			// remove this owner from the list
			this.cache.removeOwner(this);

			// if not more owners then free the cache
			if (this.cache.noOwners()) {
				// remove the cache from the hash
				memoryCacheHash.remove(this.address);

				this.cache = null;

				// \\//\\//\\//\\//\\//\\//\\//
				if (doDebugMessages) {
					System.out.println("=released cache for " + Address.toString(this.address.longValue()));
				}
			}
		}
	}

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
