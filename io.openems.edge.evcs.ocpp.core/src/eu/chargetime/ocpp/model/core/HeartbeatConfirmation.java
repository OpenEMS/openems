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
import java.util.Calendar;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Sent by the Central System to the Charge Point in response to a
 * {@link HeartbeatRequest}.
 */
@XmlRootElement(name = "heartbeatResponse")
public class HeartbeatConfirmation implements Confirmation {
	private Calendar currentTime;

	/**
	 * This contains the current time of the Central System.
	 *
	 * @return The current time.
	 */
	@Deprecated
	public Calendar objCurrentTime() {
		return currentTime;
	}

	/**
	 * This contains the current time of the Central System.
	 *
	 * @return The current time.
	 */
	public Calendar getCurrentTime() {
		return currentTime;
	}

	/**
	 * Required. This contains the current time of the Central System.
	 *
	 * @param currentTime Calendar, current time.
	 */
	@XmlElement
	public void setCurrentTime(Calendar currentTime) {
		this.currentTime = currentTime;
	}

	@Override
	public boolean validate() {
		return currentTime != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HeartbeatConfirmation that = (HeartbeatConfirmation) o;
		return Objects.equals(currentTime, that.currentTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(currentTime);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("currentTime", currentTime).add("isValid", validate()).toString();
	}
}
