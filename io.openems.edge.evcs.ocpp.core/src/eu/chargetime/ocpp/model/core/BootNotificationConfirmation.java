package eu.chargetime.ocpp.model.core;

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

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Calendar;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Sent by the Central System to the Charge Point in response to a
 * {@link BootNotificationRequest}.
 *
 * @see BootNotificationRequest
 */
@XmlRootElement(name = "bootNotificationResponse")
@XmlType(propOrder = { "status", "currentTime", "interval" })
public class BootNotificationConfirmation implements Confirmation {

	private Calendar currentTime;
	private int interval;
	private RegistrationStatus status;

	/**
	 * Central System's current time.
	 *
	 * @return an instance of Calendar.
	 */
	public Calendar getCurrentTime() {
		return currentTime;
	}

	/**
	 * Central System's current time.
	 *
	 * @return an instance of Calendar.
	 */
	@Deprecated
	public Calendar objCurrentTime() {
		return currentTime;
	}

	/**
	 * Required. This contains the Central System’s current time.
	 *
	 * @param currentTime Central System’s current time.
	 */
	@XmlElement
	public void setCurrentTime(Calendar currentTime) {
		this.currentTime = currentTime;
	}

	/**
	 * When RegistrationStatus is Accepted, this contains the heartbeat interval in
	 * seconds. If the Central System returns something other than Accepted, the
	 * value of the interval field indicates the minimum wait time before sending a
	 * next BootNotification request.
	 *
	 * @return Heartbeat/delay interval in seconds.
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Required. When RegistrationStatus is Accepted, this contains the heartbeat
	 * interval in seconds. If the Central System returns something other than
	 * Accepted, the value of the interval field indicates the minimum wait time
	 * before sending a next BootNotification request.
	 *
	 * @param interval heartbeat/delay interval in seconds. Min value 0.
	 */
	@XmlElement
	public void setInterval(int interval) {
		if (interval <= 0) {
			throw new PropertyConstraintException(interval, "interval be a positive value");
		}

		this.interval = interval;
	}

	/**
	 * This contains whether the Charge Point has been registered within the System
	 * Central.
	 *
	 * @return Charge Points registration status as {@link RegistrationStatus}.
	 */
	@Deprecated
	public RegistrationStatus objStatus() {
		return status;
	}

	/**
	 * This contains whether the Charge Point has been registered within the System
	 * Central.
	 *
	 * @return Charge Points registration status as {@link RegistrationStatus}.
	 */
	public RegistrationStatus getStatus() {
		return status;
	}

	/**
	 * Required. This contains whether the Charge Point has been registered within
	 * the System Central.
	 *
	 * @param status Charge Points registration status.
	 */
	@XmlElement
	public void setStatus(RegistrationStatus status) {
		this.status = status;
	}

	@Override
	public boolean validate() {
		boolean valid = status != null;
		valid &= currentTime != null;
		valid &= interval > 0;

		return valid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BootNotificationConfirmation that = (BootNotificationConfirmation) o;
		return interval == that.interval && Objects.equals(currentTime, that.currentTime) && status == that.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(currentTime, interval, status);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("currentTime", currentTime).add("interval", interval)
				.add("status", status).add("isValid", validate()).toString();
	}
}
