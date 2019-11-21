package eu.chargetime.ocpp.model.localauthlist;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;

public class GetLocalListVersionConfirmation implements Confirmation {

	private int listVersion = -2;

	public GetLocalListVersionConfirmation() {
	}

	public GetLocalListVersionConfirmation(int listVersion) {
		this.listVersion = listVersion;
	}

	/**
	 * This contains the current version number of the local authorization list in
	 * the Charge Point.
	 *
	 * @return String, version of localAuthList.
	 */
	public int getListVersion() {
		return listVersion;
	}

	/**
	 * Required. This contains the current version number of the local authorization
	 * list in the Charge Point.
	 *
	 * <p>
	 * A version number of 0 (zero) SHALL be used to indicate that the local
	 * authorization list is empty, and a version number of -1 SHALL be used to
	 * indicate that the Charge Point does not support Local Authorization Lists.
	 *
	 * @param listVersion int, version of localAuthList.
	 */
	public void setListVersion(int listVersion) {
		if (listVersion < -1) {
			throw new PropertyConstraintException(listVersion, "listVersion must be >= -1");
		}
		this.listVersion = listVersion;
	}

	@Override
	public boolean validate() {
		return listVersion >= -1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		GetLocalListVersionConfirmation that = (GetLocalListVersionConfirmation) o;
		return Objects.equals(listVersion, that.listVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(listVersion);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("listVersion", listVersion).add("isValid", validate()).toString();
	}
}
