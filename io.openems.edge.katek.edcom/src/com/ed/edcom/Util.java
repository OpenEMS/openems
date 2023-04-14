// CHECKSTYLE:OFF
/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.edcom;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;

/**
 * Library initialization.
 */
public final class Util {

	private static final String INFO = "Katek Memmingen GmbH";
	private static Util ref;
	private static final String sPub = """
		MIIBITANBgkqhkiG9w0BAQEFAAOCAQ4AMIIBCQKCAQAws44+vRZyAF\
		tDocnqtHjMQwytJqrjq34t7LuPv+6aC1vuMvvQHTxjeQetsvg1Q3WoK49YfnC\
		rJTRNTX0SCD2SIFVqZqDkJVqCheeZiuk+aCQ3GFpdZdmHkRswaO2s8BqJ0CVT\
		cWCExMbxWDFK/0NIsBdIoykIixv/bwmYRX3WxCG+I3J1Lp9geYu+EPdBy09x0\
		Mbh6rziLfcark9YNUp2Tvj+O2nO1fkSiFOA3czaS042ORXnxRYcl2Zu5DXDb9\
		4Uh28JEXWLr02gEqvMCBkx+yYNAT8TRfO2pw8T+CrT+R0tfufL3ELIPGokKQV\
		NvlsYkjEvHQ0M9dYCQqpwAmC7AgMBAAE=""";

	/**
	 * COM version flag
	 */
	public static boolean communication_ver8x = true;

	/**
	 * Demo mode flag
	 */
	public boolean demoMode = false;

	static int userId = -1;
	static int readPermission = 0;
	static int writePerission = 0;
	static int g = 0;
	private int a;
	private int libInitCnt = 0;

	private Util() {
	}

	/**
	 * Factory method
	 *
	 * @return a instance of class utils
	 */
	public static Util getInstance() {
		if (ref == null) {
			ref = new Util();
		}
		return ref;
	}

	/**
	 * Set library user name
	 *
	 * @deprecated Only use this method with inverter versions > 8.0.
	 *
	 * @param name registered library user name (please contact Katek Memmingen
	 *             GmbH)
	 */
	@Deprecated
	public void setUserName(String name) {
		communication_ver8x = false;
		try {
			// check key
			int bs = 0;
			byte[] kb = sPub.getBytes("UTF-8");
			for (byte bt : kb) {
				bs += bt;
			}
			if (bs != 33098) {
				throw new RuntimeException();
			}
			// check initialisierung counter
			if (++libInitCnt > 3) {
				throw new RuntimeException();
			}
			if (name.equals("DEMO_MODE")) {
				userId = 1;
				readPermission = 1;
				writePerission = 0;
				demoMode = true;
				return;
			}
			String sId = "_LIB_USER_ID:";
			KeyFactory kf = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec spec2 = new X509EncodedKeySpec(Base64.getDecoder().decode(sPub.getBytes("UTF-8")));
			Cipher chd = Cipher.getInstance("RSA");
			chd.init(Cipher.DECRYPT_MODE, kf.generatePublic(spec2));
			String plainText = new String(chd.doFinal(Base64.getDecoder().decode(name.getBytes("UTF-8"))));
			int fix = plainText.lastIndexOf(sId);
			fix = fix + sId.length();
			int lix = fix + 3;
			if (lix >= plainText.length()) {
				lix = plainText.length();
			}
			userId = Integer.parseInt(plainText.substring(fix, lix));
			if (g != 1980 && userId == 2) {
				throw new RuntimeException();
			}
			if (userId == 2 && g == 1980) {
				readPermission = writePerission = 5;
			} else {
				readPermission = writePerission = 2;
			}
		} catch (Exception e) {
			userId = -1; // bad user
			readPermission = writePerission = 0;
		}
	}

	/**
	 * Initialize (for package 8 and above)
	 *
	 */
	public void init() {
		communication_ver8x = true;
		userId = 3;
		readPermission = writePerission = 5;
	}

	/**
	 * Get library version
	 *
	 * @return version as string
	 */
	public String getEdcomVersion() {
		return Version.pomversion + " - " + Version.build_time;
	}

	@Deprecated
	public String getEDCOMVersion() {
		return getEdcomVersion();
	}

	/**
	 * Get general Vendor info
	 *
	 * @return vendor info text as string
	 */
	public static String getVendorInfo() {
		return INFO;
	}

	@Deprecated
	public int getIndex() {
		if (++libInitCnt > 3) {
			return 0;
		}
		float f = (new Random(System.currentTimeMillis())).nextFloat();
		a = Float.floatToIntBits(f);
		return a;
	}

	@Deprecated
	public void setIndex(int a) {
		if (++libInitCnt > 3) {
			return;
		}
		if (this.a == 0) {
			return;
		}
		if (Float.floatToIntBits((float) Math.cos(Float.intBitsToFloat(this.a + 1937) * Math.PI * 0.3795f)) == a) {
			g = 1980;
		}
		this.a = 0;
	}

	static ClientListener feedback;

	/**
	 * Set listener
	 *
	 * @param cl listener
	 */
	public void setListener(ClientListener cl) {
		feedback = cl;
	}

}
//CHECKSTYLE:ON
