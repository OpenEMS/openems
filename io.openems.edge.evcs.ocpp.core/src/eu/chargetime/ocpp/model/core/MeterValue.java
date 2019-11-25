package eu.chargetime.ocpp.model.core;

import eu.chargetime.ocpp.model.Validatable;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Arrays;
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

/**
 * Collection of one or more sampled values in {@link MeterValuesRequest}. All
 * {@link SampledValue} in a {@link MeterValue} are sampled at the same point in
 * time.
 */
@XmlRootElement
@XmlType(propOrder = { "timestamp", "sampledValue" })
public class MeterValue implements Validatable {

	private Calendar timestamp;
	private SampledValue[] sampledValue;

	@Override
	public boolean validate() {
		boolean valid = timestamp != null && sampledValue != null;

		if (valid) {
			for (SampledValue value : sampledValue) {
				valid &= value.validate();
			}
		}
		return valid;
	}

	/**
	 * Timestamp for measured value(s).
	 *
	 * @return original timestamp.
	 */
	public Calendar getTimestamp() {
		return timestamp;
	}

	/**
	 * Timestamp for measured value(s).
	 *
	 * @return original timestamp.
	 */
	@Deprecated
	public Calendar objTimestamp() {
		return timestamp;
	}

	/**
	 * Required. Timestamp for measured value(s).
	 *
	 * @param timestamp {@link Calendar} timestamp
	 */
	@XmlElement
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * One or more measured values.
	 *
	 * @return Array of {@link SampledValue}.
	 */
	public SampledValue[] getSampledValue() {
		return sampledValue;
	}

	/**
	 * Required. One or more measured values.
	 *
	 * @param sampledValue Array of {@link SampledValue}.
	 */
	@XmlElement
	public void setSampledValue(SampledValue[] sampledValue) {
		this.sampledValue = sampledValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MeterValue that = (MeterValue) o;
		return Objects.equals(timestamp, that.timestamp) && Arrays.equals(sampledValue, that.sampledValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, sampledValue);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("timestamp", timestamp).add("sampledValue", sampledValue)
				.add("isValid", validate()).toString();
	}
}
