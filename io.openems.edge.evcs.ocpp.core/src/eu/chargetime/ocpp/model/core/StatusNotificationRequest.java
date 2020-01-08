package eu.chargetime.ocpp.model.core;

import java.util.Calendar;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;

/** Sent by the Charge Point to the Central System. */
@XmlRootElement
@XmlType(propOrder = { "connectorId", "status", "errorCode", "info", "timestamp", "vendorId", "vendorErrorCode" })
public class StatusNotificationRequest implements Request {

	private static final String ERROR_MESSAGE = "Exceeds limit of %s chars";

	private Integer connectorId;
	private ChargePointErrorCode errorCode;
	private String info;
	private ChargePointStatus status;
	private Calendar timestamp;
	private String vendorId;
	private String vendorErrorCode;

	@Override
	public boolean validate() {
		boolean valid = isValidConnectorId(connectorId);
		valid &= errorCode != null;
		valid &= status != null;
		return valid;
	}

	/**
	 * The id of the connector for which the status is reported. Id '0' (zero) is
	 * used if the status is for the Charge Point main controller.
	 *
	 * @return connector id. 0 = main controller.
	 */
	public Integer getConnectorId() {
		return connectorId;
	}

	/**
	 * Required. The id of the connector for which the status is reported. Id '0'
	 * (zero) is used if the status is for the Charge Point main controller.
	 *
	 * @param connectorId integer, connector id. 0 = main controller.
	 */
	@XmlElement
	public void setConnectorId(Integer connectorId) {
		if (!isValidConnectorId(connectorId)) {
			throw new PropertyConstraintException(connectorId, "connectorId >= 0");
		}

		this.connectorId = connectorId;
	}

	private boolean isValidConnectorId(Integer connectorId) {
		return connectorId != null && connectorId >= 0;
	}

	/**
	 * This contains the error code reported by the Charge Point.
	 *
	 * @return the {@link ChargePointErrorCode}.
	 */
	public ChargePointErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * This contains the error code reported by the Charge Point.
	 *
	 * @return the {@link ChargePointErrorCode}.
	 */
	@Deprecated
	public ChargePointErrorCode objErrorCode() {
		return errorCode;
	}

	/**
	 * Required. This contains the error code reported by the Charge Point.
	 *
	 * @param errorCode the {@link ChargePointErrorCode}.
	 */
	@XmlElement
	public void setErrorCode(ChargePointErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Additional free format information related to the error.
	 *
	 * @return Additional information.
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Optional. Additional free format information related to the error.
	 *
	 * @param info String, max 50 characters, case insensitive.
	 */
	@XmlElement
	public void setInfo(String info) {
		if (!ModelUtil.validate(info, 50)) {
			throw new PropertyConstraintException(info.length(), createErrorMessage(50));
		}

		this.info = info;
	}

	/**
	 * This contains the current status of the Charge Point.
	 *
	 * @return the {@link ChargePointStatus}.
	 */
	public ChargePointStatus getStatus() {
		return status;
	}

	/**
	 * This contains the current status of the Charge Point.
	 *
	 * @return the {@link ChargePointStatus}.
	 */
	@Deprecated
	public ChargePointStatus objStatus() {
		return status;
	}

	/**
	 * Required. This contains the current status of the Charge Point.
	 *
	 * @param status the {@link ChargePointStatus}.
	 */
	@XmlElement
	public void setStatus(ChargePointStatus status) {
		this.status = status;
	}

	/**
	 * The time for which the status is reported. If absent time of receipt of the
	 * message will be assumed.
	 *
	 * @return status time.
	 */
	public Calendar getTimestamp() {
		return timestamp;
	}

	/**
	 * The time for which the status is reported. If absent time of receipt of the
	 * message will be assumed.
	 *
	 * @return status time.
	 */
	@Deprecated
	public Calendar objTimestamp() {
		return timestamp;
	}

	/**
	 * Optional. The time for which the status is reported. If absent time of
	 * receipt of the message will be assumed.
	 *
	 * @param timestamp Calendar, status time.
	 */
	@XmlElement
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * This identifies the vendor-specific implementation.
	 *
	 * @return Identification of the vendor.
	 */
	public String getVendorId() {
		return vendorId;
	}

	/**
	 * Optional. This identifies the vendor-specific implementation.
	 *
	 * @param vendorId String, max 255 characters, case insensitive.
	 */
	@XmlElement
	public void setVendorId(String vendorId) {
		if (!ModelUtil.validate(vendorId, 255)) {
			throw new PropertyConstraintException(vendorId.length(), createErrorMessage(255));
		}

		this.vendorId = vendorId;
	}

	/**
	 * This contains the vendor-specific error code.
	 *
	 * @return Custom vendor error code.
	 */
	public String getVendorErrorCode() {
		return vendorErrorCode;
	}

	/**
	 * Optional. This contains the vendor-specific error code.
	 *
	 * @param vendorErrorCode String, max 50 characters, case insensitive.
	 */
	@XmlElement
	public void setVendorErrorCode(String vendorErrorCode) {
		if (!ModelUtil.validate(vendorErrorCode, 50)) {
			throw new PropertyConstraintException(vendorErrorCode.length(), createErrorMessage(50));
		}

		this.vendorErrorCode = vendorErrorCode;
	}

	@Override
	public boolean transactionRelated() {
		return false;
	}

	private static String createErrorMessage(int maxLength) {
		return String.format(ERROR_MESSAGE, maxLength);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		StatusNotificationRequest that = (StatusNotificationRequest) o;
		return Objects.equals(connectorId, that.connectorId) && errorCode == that.errorCode
				&& Objects.equals(info, that.info) && status == that.status && Objects.equals(timestamp, that.timestamp)
				&& Objects.equals(vendorId, that.vendorId) && Objects.equals(vendorErrorCode, that.vendorErrorCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectorId, errorCode, info, status, timestamp, vendorId, vendorErrorCode);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("connectorId", connectorId).add("errorCode", errorCode)
				.add("info", info).add("status", status).add("timestamp", timestamp).add("vendorId", vendorId)
				.add("vendorErrorCode", vendorErrorCode).add("isValid", validate()).toString();
	}
}
