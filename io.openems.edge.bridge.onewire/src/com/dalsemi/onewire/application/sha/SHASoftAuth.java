// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 1999-2001 Maxim Integrated Products, All Rights Reserved.
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

package com.dalsemi.onewire.application.sha;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.utils.CRC16;
import com.dalsemi.onewire.utils.Convert;
import com.dalsemi.onewire.utils.IOHelper;

/**
 * <P>
 * This class implements an software authrization account application for SHA
 * Transactions. The account data is stored on the user iButtons after being
 * digitally signed by a virtual machine coprocessor. Account data consists of
 * the following:
 * <UL>
 * <LI>1 byte: Length of the account file</LI>
 * <LI>2 bytes: Transaction ID</LI>
 * <LI>2 bytes: Account Money Conversion Factor</LI>
 * <LI>3 bytes: Account Balance</LI>
 * <LI>20 bytes: Account Data Signature</LI>
 * <LI>1 byte: Account data type (dynamic or static)</LI>
 * <LI>1 byte: File continuation pointer</LI>
 * <LI>2 bytes: CRC16 of entire 30 bytes seeded with the page number</LI>
 * <LI><B>32 bytes Total</B></LI>
 * </UL>
 * </P>
 *
 * <P>
 * A typical use case for this class might be as follows:
 *
 * <pre>
 * OneWireContainer18 coprOWC18 = new OneWireContainer18(adapter, address);
 *
 * // COPR.0 is the filename for coprocessor service data
 * SHAiButtonCopr copr = new SHAiButtonCopr(coprOWC18, "COPR.0");
 *
 * // Initial amount for new users is $100, and debit amount is 50 cents
 * byte[] ver_data = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
 * SATransaction trans = new SHAsoftauth(copr, ver_data, 7);
 *
 * OneWireContainer18 owc18 = new OneWireContainer18(adapter, userAddress);
 *
 * // The following constructor erases all transaction data from the user and
 * // installs the system authentication secret on the user iButton.
 * SHAiButtonUser user = new SHAiButtonUser18(copr, owc18, true, authSecret);
 *
 * // creates account data on iButton
 * if (trans.setupTransactionData(user))
 * 	System.out.println("Account data installed successfully");
 * else
 * 	System.out.println("Account data installation failed");
 *
 * // ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ...
 *
 * // "challenges" user iButton
 * if (trans.verifyUser(user)) {
 * 	System.out.println("User Verified Successfully");
 *
 * 	// checks data signature
 * 	if (trans.verifyTransactionData(user)) {
 * 		System.out.println("Account Data Verified Successfully");
 *
 * 		// writes verification data to user iButton
 * 		System.out.println("User's verification data: ");
 * 		softauth.getParameter(SHASoftAuth.VERIFICATION_DATA, ver_data, 0, 7);
 * 		IOHelper.writeBytes(ver_data);
 * 	}
 * }
 *
 * // ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ...
 *
 * if (trans.getLastError() != 0) {
 * 	System.err.println("Error code: " + trans.getLastError());
 * }
 * </pre>
 * </P>
 *
 * <p>
 * This class makes use of several performance enhancements for TINI. For
 * instance, most methods are <code>synchronized</code> to access instance
 * variable byte arrays rather than creating new byte arrays every time a
 * transaction is performed. This could hurt performance in multi-threaded
 * applications, but the usefulness of having several threads contending to talk
 * to a single iButton is questionable since the methods in
 * <code>com.dalsemi.onewire.adapter.DSPortAdapter</code>
 * <code>beginExclusive(boolean)</code> and <code>endExclusive()</code> should
 * be used.
 * </p>
 *
 * @see SHATransaction
 * @see SHAiButtonCopr
 * @see SHAiButtonUser
 *
 * @version 1.00
 * @author SKH
 */
public class SHASoftAuth extends SHATransaction {
	/** Used for fast FF copy */
	private static final byte[] ffBlock = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	// ************************************************************** //
	// Type constants
	// used to update/retrieve parameters of transaction
	// ************************************************************** //
	/** Update the verification data */
	public static final int VERIFICATION_DATA = 2;

	/** indices for fields in user account file */
	public static final int I_FILE_LENGTH = 0;
	public static final int I_DATA_TYPE_CODE = 1;
	public static final int I_SIGNATURE = 2;
	public static final int I_VERDATA = 22;
	public static final int I_CONTINUATION_PTR = 29;
	public static final int I_FILE_CRC16 = 30;

	// ************************************************************** //
	// Member variables
	// ************************************************************** //
	/** Data to be stored on the iButton for Verification */
	private final byte[] ver_data = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/** Data that is to be written to the iButton for Verification */
	private final byte[] master_ver_data = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	/** User apps should never call this */
	protected SHASoftAuth() {

	}

	/**
	 * SHADebit constructor. <code>copr</code> is the SHAiButtonCopr that is used to
	 * perform this transaction. After saving a reference to the SHA coprocessor,
	 * this constructor resets all parameters for this type of transaction to their
	 * default values.
	 *
	 * @param copr The coprocessor used for authentication and data signing in this
	 *             transaction.
	 */
	public SHASoftAuth(SHAiButtonCopr copr) {
		super(copr);

		this.resetParameters();
	}

	/**
	 * SHADebit constructor. <code>copr</code> is the SHAiButtonCopr that is used to
	 * perform this transaction. After saving a reference to the SHA coprocessor,
	 * this constructor resets all parameters for this type of transaction to their
	 * default values.
	 *
	 * @param copr       The coprocessor used for authentication and data signing in
	 *                   this transaction.
	 * @param extra_data The 7 bytes of extra data to be used instead of the
	 *                   balance.
	 * @param len        The len, 7 or less of the data. It is 0 padded.
	 */
	public SHASoftAuth(SHAiButtonCopr copr, byte[] extra_data, int len) {
		super(copr);

		System.arraycopy(extra_data, 0, this.master_ver_data, 0, len);
	}

	/**
	 * <P>
	 * Setup account data on a fresh user iButton. Prior to calling setup
	 * transaction data, the authentication secret for the iButton should already be
	 * setup and a directory entry (as well as at least an empty placeholder file)
	 * should exist for the account data. If you constructed the SHAiButtonUser
	 * using
	 * <code>SHAiButtonUser(SHAiButtonCopr,OneWireContainer18,boolean,byte[])</code>
	 * the secret has been setup for you and you should know call this function. If
	 * you try to install the authentication secret after creating the account data,
	 * you will destroy all account data on the iButton.
	 * </P>
	 *
	 * <P>
	 * You can set the value of the initial account balance by calling
	 * <code>transaction.setParameter(SHADebit.INITIAL_AMOUNT,10000)</code> where
	 * the value of the units is in cents (i.e. 10000 = $100).
	 * </P>
	 *
	 * <P>
	 * <B>Flow of action: </B>
	 * <ul>
	 * <li>Generate generic account page</li>
	 * <li>Insert the initial balance into account data</li>
	 * <li>Create a signature using coprocessor</li>
	 * <li>Insert the digital signature</li>
	 * <li>Write account data page to iButton</li>
	 * </ul>
	 * </P>
	 *
	 * @param user SHAiButtonUser upon which the transaction occurs.
	 *
	 * @return <code>true</code>if and only if the signature is successfully created
	 *         by the coprocessor AND the data is successfully written to the user
	 *         iButton.
	 *
	 * @see SHAiButtonCopr#createDataSignature(byte[],byte[],byte[],int)
	 * @see SHAiButtonUser#writeAccountData(byte[],int)
	 * @see #getLastError()
	 */
	@Override
	public boolean setupTransactionData(SHAiButtonUser user) throws OneWireException, OneWireIOException {
		// clear any error
		this.lastError = NO_ERROR;

		// not in critical path, so malloc'ing is okay
		var accountData = new byte[32];

		return this.writeTransactionData(user, this.master_ver_data, accountData);
	}

	// prevent malloc'ing in the critical path
	private final byte[] verifyUser_fullBindCode = new byte[15];
	private final byte[] verifyUser_scratchpad = new byte[32];
	private final byte[] verifyUser_accountData = new byte[32];
	private final byte[] verifyUser_mac = new byte[20];
	private final byte[] verifyUser_chlg = new byte[3];

	/**
	 * <P>
	 * Verifies user's authentication response. User is "authenticated" if and only
	 * if the digital signature generated the user iButton matches the digital
	 * signature generated by the coprocessor after the user's unique secret has
	 * been recreated on the coprocessor.
	 * </P>
	 *
	 * <P>
	 * <B>Flow of action: </B>
	 * <ul>
	 * <li>Generate 3-byte "challenge" on coprocessor</li>
	 * <li>Write challenge to scratchpad of user</li>
	 * <li>Read account data page with signature</li>
	 * <li>Attempt to match user's signature with the coprocessor</li>
	 * </ul>
	 * </P>
	 *
	 * @param user SHAiButtonUser upon which the transaction occurs.
	 *
	 * @see SHAiButtonCopr#generateChallenge(int,byte[],int)
	 * @see SHAiButtonCopr#verifyAuthentication(byte[],byte[],byte[],byte[],byte)
	 * @see SHAiButtonUser#readAccountData(byte[],int,byte[],int,byte[],int)
	 * @see #getLastError()
	 */
	@Override
	public synchronized boolean verifyUser(SHAiButtonUser user) throws OneWireException, OneWireIOException {
		// clear any error
		this.lastError = SHATransaction.NO_ERROR;

		// local vars
		var fullBindCode = this.verifyUser_fullBindCode;
		var scratchpad = this.verifyUser_scratchpad;
		var accountData = this.verifyUser_accountData;
		var mac = this.verifyUser_mac;
		var chlg = this.verifyUser_chlg;

		// Generate random challenge. This must be done on the
		// coprocessor, otherwise flags aren't setup for VALIDATE_PAGE.
		if (!this.copr.generateChallenge(0, chlg, 0)) {
			this.lastError = COPR_COMPUTE_CHALLENGE_FAILED;
			return false;
		}

		// have user answer the challenge
		var wcc = user.readAccountData(chlg, 0, accountData, 0, mac, 0);

		if (wcc < 0) {
			if (user.hasWriteCycleCounter()) {
				// failed to read account data
				this.lastError = SHATransaction.USER_READ_AUTH_FAILED;
				return false;
			}
			System.arraycopy(ffBlock, 0, scratchpad, 8, 4);
		} else {
			// copy the write cycle counter into scratchpad
			Convert.toByteArray(wcc, scratchpad, 8, 4);
		}

		// get the user's fullBindCode, formatted for user device
		user.getFullBindCode(fullBindCode, 0);

		// get the user address and page num from fullBindCode
		System.arraycopy(fullBindCode, 4, scratchpad, 12, 8);

		// set the same challenge bytes
		System.arraycopy(chlg, 0, scratchpad, 20, 3);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			IOHelper.writeLine("------------------------------------");
			IOHelper.writeLine("Verifying user");
			IOHelper.writeLine("chlg");
			IOHelper.writeBytesHex(chlg);
			IOHelper.writeLine("accountData");
			IOHelper.writeBytesHex(accountData);
			IOHelper.writeLine("mac");
			IOHelper.writeBytesHex(mac);
			IOHelper.writeLine("wcc: " + user.getWriteCycleCounter());
			IOHelper.writeLine("fullBindCode");
			IOHelper.writeBytesHex(fullBindCode);
			IOHelper.writeLine("scratchpad");
			IOHelper.writeBytesHex(scratchpad);
			IOHelper.writeLine("------------------------------------");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		if (!this.copr.verifyAuthentication(fullBindCode, accountData, scratchpad, mac,
				user.getAuthorizationCommand())) {
			this.lastError = SHATransaction.COPROCESSOR_FAILURE;
			return false;
		}

		return true;
	}

	private final byte[] verifyData_scratchpad = new byte[32];
	private final byte[] verifyData_accountData = new byte[32];
	private final byte[] verifyData_mac = new byte[20];

	/**
	 * <P>
	 * Verifies user's account data. Account data is "verified" if the digital
	 * signature matches the signature recreated by the coprocessor.
	 * </P>
	 *
	 * <P>
	 * <B>Flow of action: </B>
	 * <ul>
	 * <li>Read the account data from user</li>
	 * <li>Get verification data</li>
	 * <li>Reset the digital signature</li>
	 * <li>Use coprocessor to sign account data</li>
	 * <li>Insert the new digital signature</li>
	 * <li>Write the account data to the user</li>
	 * </ul>
	 * </P>
	 *
	 * <P>
	 * If previous steps have been executed, all "Read" commands on the user are
	 * reading from cached data.
	 * </P>
	 *
	 * @param user SHAiButtonUser upon which the transaction occurs.
	 *
	 * @return <code>true</code> if and only if the account balance is greater than
	 *         zero and digital signature matches the signature recreated by the
	 *         coprocessor.
	 *
	 * @see SHAiButtonUser#readAccountData(byte[],int)
	 * @see SHAiButtonCopr#verifySignature(byte[],byte[],byte[])
	 * @see #getLastError()
	 */
	@Override
	public synchronized boolean verifyTransactionData(SHAiButtonUser user) throws OneWireException, OneWireIOException {
		// clear any error
		this.lastError = NO_ERROR;

		// init local vars
		var scratchpad = this.verifyData_scratchpad;
		var accountData = this.verifyData_accountData;
		var verify_mac = this.verifyData_mac;

		// if verifyUser was called, this is a read of cached data
		var wcc = user.getWriteCycleCounter();

		// if verifyUser was called, this is a read of cached data
		user.readAccountData(accountData, 0);

		// get the user's verification data
		System.arraycopy(accountData, I_VERDATA, this.ver_data, 0, 7);

		// get the mac from the account data page
		System.arraycopy(accountData, I_SIGNATURE, verify_mac, 0, 20);

		// now lets reset the mac
		this.copr.getInitialSignature(accountData, I_SIGNATURE);
		// and reset the CRC
		accountData[I_FILE_CRC16 + 0] = (byte) 0;
		accountData[I_FILE_CRC16 + 1] = (byte) 0;

		// now we also need to get things like wcc, user_page_number, user ID
		if (wcc < 0) {
			if (user.hasWriteCycleCounter()) {
				// failed to read account data
				this.lastError = USER_READ_AUTH_FAILED;
				return false;
			}
			// has no write cycle counter
			System.arraycopy(ffBlock, 0, scratchpad, 8, 4);
		} else {
			// copy the write cycle counter into scratchpad
			Convert.toByteArray(wcc, scratchpad, 8, 4);
		}
		scratchpad[12] = (byte) user.getAccountPageNumber();
		user.getAddress(scratchpad, 13, 7);

		this.copr.getSigningChallenge(scratchpad, 20);

		if (!this.copr.verifySignature(accountData, scratchpad, verify_mac)) {
			this.lastError = COPROCESSOR_FAILURE;
			return false;
		}

		return true;
	}

	// prevent malloc'ing in critical path
	private final byte[] executeTransaction_accountData = new byte[32];
	private final byte[] executeTransaction_oldAcctData = new byte[32];
	private final byte[] executeTransaction_newAcctData = new byte[32];

	// private byte[] executeTransaction_scratchpad = new byte[32];
	/**
	 * <P>
	 * Performs the signed debit, subtracting the debit amount from the user's
	 * balance and storing the new, signed account data on the user's iButton. The
	 * debit amount can be set using
	 * <code>transaction.setParameter(SHADebit.DEBIT_AMOUNT, 50)</code>, where the
	 * value is in units of cents (i.e. for 1 dollar, use 100).
	 * </P>
	 *
	 * <P>
	 * <B>Flow of action: </B>
	 * <ul>
	 * <li>Read the account data from user</li>
	 * <li>Extract account balance</li>
	 * <li>Reset the digital signature</li>
	 * <li>Use coprocessor to sign account data</li>
	 * <li>Insert the new digital signature</li>
	 * <li>Write the account data to the user</li>
	 * </ul>
	 * </P>
	 *
	 * <P>
	 * If previous steps have been executed, all "Read" commands on the user are
	 * reading from cached data.
	 * </P>
	 *
	 * @param user          SHAiButtonUser upon which the transaction occurs.
	 * @param verifySuccess A boolean to let this method know if verification was
	 *                      successful.
	 *
	 * @return <code>true</code> if and only if the user has enough in the account
	 *         balance to perform the requested debit AND a new digital signature is
	 *         successfully created AND the account data has been written to the
	 *         button.
	 *
	 * @see SHAiButtonUser#readAccountData(byte[],int)
	 * @see SHAiButtonUser#writeAccountData(byte[],int)
	 * @see SHAiButtonCopr#createDataSignature(byte[],byte[],byte[],int)
	 * @see #getLastError()
	 */
	@Override
	public synchronized boolean executeTransaction(SHAiButtonUser user, boolean verifySuccess)
			throws OneWireException, OneWireIOException {
		// clear any error
		this.lastError = NO_ERROR;

		// init local vars
		// holds the working copy of account data
		var accountData = this.executeTransaction_accountData;
		// holds the backup copy of account data before writing
		var oldAcctData = this.executeTransaction_oldAcctData;
		// holds the account data read back for checking
		var newAcctData = this.executeTransaction_newAcctData;

		// if verifyUser was called, this is a read of cached data
		user.readAccountData(accountData, 0);

		// before we update the account data array at all, let's make a backup copy
		System.arraycopy(accountData, 0, oldAcctData, 0, 32);

		// get the user's verification data
		System.arraycopy(accountData, I_VERDATA, this.ver_data, 0, 7);

		var success = this.writeTransactionData(user, this.master_ver_data, accountData);

		// if write didn't succeeded or if we need to perform
		// a verification step anyways, let's double-check what
		// the user has on the button.
		if (verifySuccess || !success) {
			var dataOK = false;
			var cnt = MAX_RETRY_CNT;
			do {
				// calling verify user re-issues a challenge-response
				// and reloads the cached account data in the user object.
				if (this.verifyUser(user)) {
					// compare the user's account data against the working
					// copy and the backup copy.
					if (user.readAccountData(newAcctData, 0)) {
						var isOld = true;
						var isCur = true;
						for (var i = 0; i < 32 && (isOld || isCur); i++) {
							// match the backup
							isOld = isOld && newAcctData[i] == oldAcctData[i];
							// match the working copy
							isCur = isCur && newAcctData[i] == accountData[i];
						}
						if (isOld) {
							// if it matches the backup copy, we didn't write anything
							// and the data is still okay, but we didn't do a debit
							dataOK = true;
							success = false;
						} else if (isCur) {
							dataOK = true;
						} else {
							// iBUTTON DATA IS TOTALLY HOSED
							// keep trying to get account data on the button
							success = this.writeTransactionData(user, this.ver_data, accountData);
						}
					}
				}
			} while (!dataOK && cnt-- > 0);

			if (!dataOK) {
				// couldn't fix the data after 255 retries
				IOHelper.writeLine("Catastrophic Failure!");
				success = false;
			}
		}

		return success;
	}

	private final byte[] writeTransactionData_scratchpad = new byte[32];

	/**
	 * Does the writing of transaction data to the user button as well as actually
	 * signing the data with the coprocessor.
	 */
	private final boolean writeTransactionData(SHAiButtonUser user, byte[] ver_data, byte[] accountData)
			throws OneWireException, OneWireIOException {
		// init local vars
		var copr = this.copr;
		var acctPageNum = user.getAccountPageNumber();
		var scratchpad = this.writeTransactionData_scratchpad;

		// length of the TMEX file - 28 data, 1 cont. ptr
		accountData[I_FILE_LENGTH] = (byte) 29;

		System.arraycopy(ver_data, 0, accountData, I_VERDATA, 7);

		// initial signature - 20 data bytes
		copr.getInitialSignature(accountData, I_SIGNATURE);

		// data type code - dynamic: 0x00, static: 0x01
		accountData[I_DATA_TYPE_CODE] = 0x01;

		// continuation pointer for TMEX file
		accountData[I_CONTINUATION_PTR] = 0x00;

		// clear out the crc16 - 2 data bytes
		accountData[I_FILE_CRC16 + 0] = 0x00;
		accountData[I_FILE_CRC16 + 1] = 0x00;

		// we need to increment the writeCycleCounter since we will be writing to the
		// part
		var wcc = user.getWriteCycleCounter();
		if (wcc > 0) {
			// copy the write cycle counter into scratchpad
			Convert.toByteArray(wcc + 1, scratchpad, 8, 4);
		} else {
			if (user.hasWriteCycleCounter()) {
				// failed to read account data
				this.lastError = SHATransaction.USER_READ_AUTH_FAILED;
				return false;
			}
			System.arraycopy(ffBlock, 0, scratchpad, 8, 4);
		}

		// svcPageNumber, followed by address of device
		scratchpad[12] = (byte) acctPageNum;
		user.getAddress(scratchpad, 13, 7);

		// copy in the signing challenge
		copr.getSigningChallenge(scratchpad, 20);

		// sign the data, return the mac right into accountData
		copr.createDataSignature(accountData, scratchpad, accountData, I_SIGNATURE);

		// after signature make sure to dump in the inverted CRC
		var crc = ~CRC16.compute(accountData, 0, accountData[I_FILE_LENGTH] + 1, acctPageNum);

		// set the the crc16 bytes
		accountData[I_FILE_CRC16 + 0] = (byte) crc;
		accountData[I_FILE_CRC16 + 1] = (byte) (crc >> 8);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			IOHelper.writeLine("------------------------------------");
			IOHelper.writeLine("writing transaction data");
			IOHelper.writeLine("acctPageNum: " + acctPageNum);
			IOHelper.writeLine("accountData");
			IOHelper.writeBytesHex(accountData);
			IOHelper.writeLine("------------------------------------");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		// write it to the button
		try {
			if (user.writeAccountData(accountData, 0)) {
				return true;
			}

		} catch (OneWireException owe) {
			if (DEBUG) {
				IOHelper.writeLine(owe);
			}
		}

		this.lastError = SHATransaction.USER_WRITE_DATA_FAILED;
		return false;
	}

	/**
	 * <P>
	 * Retrieves the value of a particular parameter for this debit transaction.
	 * </P>
	 *
	 * <P>
	 * <B>Valid Parameters</B>
	 * <UL>
	 * <LI><code>SHADebit.DEBIT_AMOUNT</code></LI>
	 * <LI><code>SHADebit.INITIAL_AMOUNT</code></LI>
	 * <LI><code>SHADebit.USER_BALANCE</code></LI>
	 * </UL>
	 * </P>
	 *
	 * <P>
	 * Note that the value of <code>SHADebit.USER_BALANCE</code> will be set after
	 * calling <code>verifyTransactionData(SHAiButtonUser)</code> and after calling
	 * <code>executeTransaction(SHAiButtonUser)</code>.
	 * </P>
	 *
	 * @return The value of the requested parameter.
	 *
	 * @throws IllegalArgumentException if an invalid parameter type is requested.
	 */
	@Override
	public synchronized int getParameter(int type) {
		return 0;
	}

	/**
	 * <P>
	 * Retrieves the value of a particular parameter for this debit transaction.
	 * </P>
	 *
	 * <P>
	 * <B>Valid Parameters</B>
	 * <UL>
	 * <LI><code>SHADebit.DEBIT_AMOUNT</code></LI>
	 * <LI><code>SHADebit.INITIAL_AMOUNT</code></LI>
	 * <LI><code>SHADebit.USER_BALANCE</code></LI>
	 * </UL>
	 * </P>
	 *
	 * <P>
	 * Note that the value of <code>SHADebit.USER_BALANCE</code> will be set after
	 * calling <code>verifyTransactionData(SHAiButtonUser)</code> and after calling
	 * <code>executeTransaction(SHAiButtonUser)</code>.
	 * </P>
	 *
	 * @return The value of the requested parameter.
	 *
	 * @throws IllegalArgumentException if an invalid parameter type is requested.
	 */
	public synchronized int getParameter(int type, byte[] data, int offset, int len) {
		if (type == VERIFICATION_DATA) {
			System.arraycopy(this.ver_data, 0, data, offset, len);
			return 0;
		}

		throw new IllegalArgumentException("Invalid Parameter type");
	}

	/**
	 * <P>
	 * Sets the value of a particular parameter for this debit transaction.
	 * </P>
	 *
	 * <P>
	 * <B>Valid Parameters</B>
	 * <UL>
	 * <LI><code>SHADebit.DEBIT_AMOUNT</code></LI>
	 * <LI><code>SHADebit.INITIAL_AMOUNT</code></LI>
	 * </UL>
	 * </P>
	 *
	 * @param type Specifies the parameter type (<code>SHADebit.DEBIT_AMOUNT</code>
	 *             or <code>SHADebit.INITIAL_AMOUNT</code>)
	 * @return </code>true</code> if a valid parameter type was specified and the
	 *         value of the parameter is positive.
	 *
	 * @throws IllegalArgumentException if an invalid parameter type is requested.
	 */
	public synchronized boolean setParameter(int type, byte[] data, int offset, int len) {
		if (type != VERIFICATION_DATA) {
			return false;
		}
		System.arraycopy(data, offset, this.master_ver_data, 0, len);

		return true;
	}

	/**
	 * <P>
	 * Sets the value of a particular parameter for this debit transaction.
	 * </P>
	 *
	 * <P>
	 * <B>Valid Parameters</B>
	 * <UL>
	 * <LI><code>SHADebit.DEBIT_AMOUNT</code></LI>
	 * <LI><code>SHADebit.INITIAL_AMOUNT</code></LI>
	 * </UL>
	 * </P>
	 *
	 * @param type Specifies the parameter type (<code>SHADebit.DEBIT_AMOUNT</code>
	 *             or <code>SHADebit.INITIAL_AMOUNT</code>)
	 * @return </code>true</code> if a valid parameter type was specified and the
	 *         value of the parameter is positive.
	 *
	 * @throws IllegalArgumentException if an invalid parameter type is requested.
	 */
	@Override
	public synchronized boolean setParameter(int type, int param) {
		return false;
	}

	/**
	 * <p>
	 * Resets all transaction parameters to default values
	 * </p>
	 */
	@Override
	public synchronized void resetParameters() {
		for (var i = 0; i < 7; i++) {
			this.ver_data[i] = 0x00;
			this.master_ver_data[i] = 0x00;
		}

	}

}
// CHECKSTYLE:ON
