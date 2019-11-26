package eu.chargetime.ocpp.model.reservation;

import java.util.Calendar;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;

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
@XmlType(propOrder = { "connectorId", "expiryDate", "idTag", "parentIdTag", "reservationId" })
public class ReserveNowRequest implements Request {

	private static final int ID_TAG_MAX_LENGTH = 20;
	private static final String ERROR_MESSAGE = "Exceeded limit of " + ID_TAG_MAX_LENGTH + " chars";

	private Integer connectorId;
	private Calendar expiryDate;
	private String idTag;
	private String parentIdTag;
	private Integer reservationId;

	public ReserveNowRequest() {
	}

	public ReserveNowRequest(Integer connectorId, Calendar expiryDate, String idTag, Integer reservationId) {
		this.connectorId = connectorId;
		this.expiryDate = expiryDate;
		this.idTag = idTag;
		this.reservationId = reservationId;
	}

	@Override
	public boolean validate() {
		boolean valid = (connectorId != null && connectorId >= 0);
		valid &= expiryDate != null;
		valid &= ModelUtil.validate(idTag, ID_TAG_MAX_LENGTH);
		valid &= reservationId != null;
		return valid;
	}

	/**
	 * This contains the id of the connector to be reserved. A value of 0 means that
	 * the reservation is not for a specific connector.
	 *
	 * @return Integer, the destination connectorId.
	 */
	public Integer getConnectorId() {
		return connectorId;
	}

	/**
	 * Required. This contains the id of the connector to be reserved. A value of 0
	 * means that the reservation is not for a specific connector.
	 *
	 * @param connectorId Integer, the destination connectorId.
	 */
	@XmlElement
	public void setConnectorId(Integer connectorId) {
		if (connectorId < 0) {
			throw new PropertyConstraintException(connectorId, "connectorId must be >= 0");
		}

		this.connectorId = connectorId;
	}

	/**
	 * This contains the date and time when the reservation ends.
	 *
	 * @return Calendar, end of reservation.
	 */
	public Calendar getExpiryDate() {
		return expiryDate;
	}

	/**
	 * Required. This contains the date and time when the reservation ends.
	 *
	 * @param expiryDate Calendar, end of reservation.
	 */
	@XmlElement
	public void setExpiryDate(Calendar expiryDate) {
		this.expiryDate = expiryDate;
	}

	/**
	 * The identifier for which the Charge Point has to reserve a connector.
	 *
	 * @return String, the identifier.
	 */
	public String getIdTag() {
		return idTag;
	}

	/**
	 * Required. The identifier for which the Charge Point has to reserve a
	 * connector.
	 *
	 * @param idTag String, the identifier.
	 */
	@XmlElement
	public void setIdTag(String idTag) {
		if (!ModelUtil.validate(idTag, ID_TAG_MAX_LENGTH)) {
			throw new PropertyConstraintException(idTag.length(), ERROR_MESSAGE);
		}

		this.idTag = idTag;
	}

	/**
	 * The parent idTag.
	 *
	 * @return String, the parent identifier.
	 */
	public String getParentIdTag() {
		return parentIdTag;
	}

	/**
	 * Optional. The parent idTag.
	 *
	 * @param parentIdTag String, the parent identifier.
	 */
	@XmlElement
	public void setParentIdTag(String parentIdTag) {
		if (!ModelUtil.validate(parentIdTag, ID_TAG_MAX_LENGTH)) {
			throw new PropertyConstraintException(parentIdTag.length(), ERROR_MESSAGE);
		}
		this.parentIdTag = parentIdTag;
	}

	/**
	 * Unique id for this reservation.
	 *
	 * @return Integer, id of reservation.
	 */
	public Integer getReservationId() {
		return reservationId;
	}

	/**
	 * Required. Unique id for this reservation.
	 *
	 * @param reservationId Integer, id of reservation.
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
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ReserveNowRequest that = (ReserveNowRequest) o;
		return Objects.equals(connectorId, that.connectorId) && Objects.equals(expiryDate, that.expiryDate)
				&& Objects.equals(idTag, that.idTag) && Objects.equals(parentIdTag, that.parentIdTag)
				&& Objects.equals(reservationId, that.reservationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectorId, expiryDate, idTag, parentIdTag, reservationId);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("connectorId", connectorId).add("expiryDate", expiryDate)
				.add("idTag", idTag).add("parentIdTag", parentIdTag).add("reservationId", reservationId)
				.add("isValid", validate()).toString();
	}
}
