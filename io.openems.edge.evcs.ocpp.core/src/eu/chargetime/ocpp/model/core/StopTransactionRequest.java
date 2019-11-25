package eu.chargetime.ocpp.model.core;

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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** Sent by the Charge Point to the Central System. */
@XmlRootElement
@XmlType(propOrder = { "transactionId", "idTag", "timestamp", "meterStop", "reason", "transactionData" })
public class StopTransactionRequest implements Request {
	private String idTag;
	private Integer meterStop;
	private Calendar timestamp;
	private Integer transactionId;
	private Reason reason;
	private MeterValue[] transactionData;

	@Override
	public boolean validate() {
		boolean valid = meterStop != null;
		valid &= timestamp != null;
		valid &= transactionId != null;
		if (transactionData != null) {
			for (MeterValue meterValue : transactionData) {
				valid &= meterValue.validate();
			}
		}

		return valid;
	}

	/**
	 * This contains the identifier which requested to stop the charging.
	 *
	 * @return the IdToken.
	 */
	public String getIdTag() {
		return idTag;
	}

	/**
	 * Optional. This contains the identifier which requested to stop the charging.
	 * It is optional because a Charge Point may terminate charging without the
	 * presence of an idTag, e.g. in case of a reset. A Charge Point SHALL send the
	 * idTag if known.
	 *
	 * @param idTag a String with max length 20
	 */
	@XmlElement
	public void setIdTag(String idTag) {
		if (!ModelUtil.validate(idTag, 20)) {
			throw new PropertyConstraintException(idTag.length(), "Exceeded limit of 20 chars");
		}

		this.idTag = idTag;
	}

	/**
	 * This contains the meter value in Wh for the connector at end of the
	 * transaction.
	 *
	 * @return meter value in Wh.
	 */
	public Integer getMeterStop() {
		return meterStop;
	}

	/**
	 * Required. This contains the meter value in Wh for the connector at end of the
	 * transaction.
	 *
	 * @param meterStop integer, meter value in Wh.
	 */
	@XmlElement
	public void setMeterStop(Integer meterStop) {
		this.meterStop = meterStop;
	}

	/**
	 * This contains the date and time on which the transaction is stopped.
	 *
	 * @return stop time.
	 */
	public Calendar getTimestamp() {
		return timestamp;
	}

	/**
	 * This contains the date and time on which the transaction is stopped.
	 *
	 * @return stop time.
	 */
	@Deprecated
	public Calendar objTimestamp() {
		return timestamp;
	}

	/**
	 * Required. This contains the date and time on which the transaction is
	 * stopped.
	 *
	 * @param timestamp Calendar, stop time.
	 */
	@XmlElement
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * This contains the transaction-id as received by the
	 * {@link StartTransactionConfirmation}.
	 *
	 * @return transaction id.
	 */
	public Integer getTransactionId() {
		return transactionId;
	}

	/**
	 * Required. This contains the transaction-id as received by the
	 * {@link StartTransactionConfirmation}.
	 *
	 * @param transactionId integer, transaction id.
	 */
	@XmlElement
	public void setTransactionId(Integer transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * This contains the reason why the transaction was stopped.
	 *
	 * @return the {@link Reason}.
	 */
	public Reason getReason() {
		return reason;
	}

	/**
	 * This contains the reason why the transaction was stopped.
	 *
	 * @return the {@link Reason}.
	 */
	@Deprecated
	public Reason objReason() {
		return reason;
	}

	/**
	 * Optional. This contains the reason why the transaction was stopped. MAY only
	 * be omitted when the {@link Reason} is "Local".
	 *
	 * @param reason the {@link Reason}.
	 */
	@XmlElement
	public void setReason(Reason reason) {
		this.reason = reason;
	}

	/**
	 * This contains transaction usage details relevant for billing purposes.
	 *
	 * @return the {@link MeterValue}.
	 */
	public MeterValue[] getTransactionData() {
		return transactionData;
	}

	/**
	 * Optional. This contains transaction usage details relevant for billing
	 * purposes.
	 *
	 * @param transactionData the {@link MeterValue}.
	 */
	@XmlElement
	public void setTransactionData(MeterValue[] transactionData) {
		this.transactionData = transactionData;
	}

	@Override
	public boolean transactionRelated() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		StopTransactionRequest that = (StopTransactionRequest) o;
		return Objects.equals(idTag, that.idTag) && Objects.equals(meterStop, that.meterStop)
				&& Objects.equals(timestamp, that.timestamp) && Objects.equals(transactionId, that.transactionId)
				&& reason == that.reason && Arrays.equals(transactionData, that.transactionData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idTag, meterStop, timestamp, transactionId, reason, transactionData);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("idTag", idTag).add("meterStop", meterStop)
				.add("timestamp", timestamp).add("transactionId", transactionId).add("reason", reason)
				.add("transactionData", transactionData).add("isValid", validate()).toString();
	}
}
