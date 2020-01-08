package eu.chargetime.ocpp.model.core;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*
ChargeTime.eu - Java-OCA-OCPP
Copyright (C) 2015-2016 Thomas Volden <tv@chargetime.eu>

MIT License

Copyright (C) 2016-2018 Thomas Volden

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;

/** return by Charge Point to Central System. */
@XmlRootElement(name = "changeAvailabilityResponse")
public class ChangeAvailabilityConfirmation implements Confirmation {

	private AvailabilityStatus status;

	/**
	 * This indicates whether the Charge Point is able to perform the availability
	 * change.
	 *
	 * @return The {@link AvailabilityStatus} of the connector.
	 */
	public AvailabilityStatus getStatus() {
		return status;
	}

	/**
	 * This indicates whether the Charge Point is able to perform the availability
	 * change.
	 *
	 * @return The {@link AvailabilityStatus} of the connector.
	 */
	@Deprecated
	public AvailabilityStatus objStatus() {
		return status;
	}

	/**
	 * Required. This indicates whether the Charge Point is able to perform the
	 * availability change.
	 *
	 * @param status the {@link AvailabilityStatus} of connector.
	 */
	@XmlElement
	public void setStatus(AvailabilityStatus status) {
		this.status = status;
	}

	public ChangeAvailabilityConfirmation() {
	}

	/**
	 * Handle required fields.
	 *
	 * @param status the {@link AvailabilityStatus}, see
	 *               {@link #setStatus(AvailabilityStatus)}
	 */
	public ChangeAvailabilityConfirmation(AvailabilityStatus status) {
		this.status = status;
	}

	@Override
	public boolean validate() {
		return this.status != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChangeAvailabilityConfirmation that = (ChangeAvailabilityConfirmation) o;
		return status == that.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(status);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("status", status).add("isValid", validate()).toString();
	}
}
