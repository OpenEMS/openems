package eu.chargetime.ocpp.model.core;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;

/** Sent from Charge Point to Central System. */
@XmlRootElement(name = "remoteStartTransactionResponse")
public class RemoteStartTransactionConfirmation implements Confirmation {
	private RemoteStartStopStatus status;

	public RemoteStartTransactionConfirmation() {
	}

	/**
	 * Set required fields.
	 *
	 * @param status status for request, see
	 *               {@link #setStatus(RemoteStartStopStatus)}.
	 */
	public RemoteStartTransactionConfirmation(RemoteStartStopStatus status) {
		setStatus(status);
	}

	/**
	 * Status indicating whether Charge Point accepts the request to start a
	 * transaction.
	 *
	 * @return the {@link RemoteStartStopStatus}.
	 */
	public RemoteStartStopStatus getStatus() {
		return status;
	}

	/**
	 * Status indicating whether Charge Point accepts the request to start a
	 * transaction.
	 *
	 * @return the {@link RemoteStartStopStatus}.
	 */
	@Deprecated
	public RemoteStartStopStatus objStatus() {
		return status;
	}

	/**
	 * Required. Status indicating whether Charge Point accepts the request to start
	 * a transaction.
	 *
	 * @param status the {@link RemoteStartStopStatus}.
	 */
	@XmlElement
	public void setStatus(RemoteStartStopStatus status) {
		this.status = status;
	}

	@Override
	public boolean validate() {
		return status != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RemoteStartTransactionConfirmation that = (RemoteStartTransactionConfirmation) o;
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
