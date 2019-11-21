package eu.chargetime.ocpp.model.reservation;

import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2016 Thomas Volden <tv@chargetime.eu>
 * Copyright (C) 2018 Mikhail Kladkevich <kladmv@ecp-share.com>
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

/** Sent by the Central System to the Charge Point. */
@XmlRootElement
public class CancelReservationRequest implements Request {
	private Integer reservationId;

	public CancelReservationRequest() {
	}

	public CancelReservationRequest(Integer reservationId) {
		this.reservationId = reservationId;
	}

	@Override
	public boolean validate() {
		return reservationId != null;
	}

	/**
	 * Id of the reservation to cancel.
	 *
	 * @return Integer, id of the reservation.
	 */
	public Integer getReservationId() {
		return reservationId;
	}

	/**
	 * Required. Id of the reservation to cancel.
	 *
	 * @param reservationId Integer, id of the reservation.
	 */
	@XmlElement
	public void setReservationId(Integer reservationId) {
		this.reservationId = reservationId;
	}

	@Override
	public boolean transactionRelated() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CancelReservationRequest that = (CancelReservationRequest) o;
		return Objects.equals(reservationId, that.reservationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(reservationId);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("reservationId", reservationId).add("isValid", validate())
				.toString();
	}
}
