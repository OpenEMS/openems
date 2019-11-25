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

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Sent by the Charge Point to the Central System or vice versa in response to a
 * {@link DataTransferRequest}.
 */
@XmlRootElement(name = "dataTransferResponse")
@XmlType(propOrder = { "status", "data" })
public class DataTransferConfirmation implements Confirmation {

	private DataTransferStatus status;
	private String data;

	/**
	 * This indicates the success or failure of the data transfer.
	 *
	 * @return the {@link DataTransferStatus}.
	 */
	public DataTransferStatus getStatus() {
		return status;
	}

	/**
	 * This indicates the success or failure of the data transfer.
	 *
	 * @return the {@link DataTransferStatus}.
	 */
	@Deprecated
	public DataTransferStatus objStatus() {
		return status;
	}

	/**
	 * Required. This indicates the success or failure of the data transfer.
	 *
	 * @param status the {@link DataTransferStatus}.
	 */
	@XmlElement
	public void setStatus(DataTransferStatus status) {
		this.status = status;
	}

	/**
	 * Optional. Data in response to request.
	 *
	 * @return data.
	 */
	public String getData() {
		return data;
	}

	/**
	 * Optional. Data in response to request.
	 *
	 * @param data String, data
	 */
	@XmlElement
	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean validate() {
		return this.status != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataTransferConfirmation that = (DataTransferConfirmation) o;
		return status == that.status && Objects.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, data);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("status", status).add("data", data).add("isValid", validate())
				.toString();
	}
}
