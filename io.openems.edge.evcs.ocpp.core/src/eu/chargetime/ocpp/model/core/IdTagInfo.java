package eu.chargetime.ocpp.model.core;

import java.util.Calendar;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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

import eu.chargetime.ocpp.model.Validatable;
import eu.chargetime.ocpp.utilities.MoreObjects;

/**
 * Contains status information about an identifier. It is returned in
 * {@link AuthorizeConfirmation}, {@link StartTransactionConfirmation} and
 * {@link StopTransactionConfirmation} responses.
 *
 * <p>
 * If expiryDate is not given, the status has no end date.
 */
@XmlRootElement
@XmlType(propOrder = { "status", "expiryDate", "parentIdTag" })
public class IdTagInfo implements Validatable {
	private Calendar expiryDate;
	private String parentIdTag;
	private AuthorizationStatus status;

	/**
	 * This contains the date at which idTag should be removed from the
	 * Authorization Cache.
	 *
	 * @return Expiry date.
	 */
	public Calendar getExpiryDate() {
		return expiryDate;
	}

	/**
	 * This contains the date at which idTag should be removed from the
	 * Authorization Cache.
	 *
	 * @return Expiry date.
	 */
	@Deprecated
	public Calendar objExpiryDate() {
		return expiryDate;
	}

	/**
	 * Optional. This contains the date at which idTag should be removed from the
	 * Authorization Cache.
	 *
	 * @param expiryDate Calendar, expire date.
	 */
	@XmlElement
	public void setExpiryDate(Calendar expiryDate) {
		this.expiryDate = expiryDate;
	}

	/**
	 * This contains the parent-identifier.
	 *
	 * @return the IdToken of the parent.
	 */
	public String getParentIdTag() {
		return parentIdTag;
	}

	/**
	 * Optional. This contains the parent-identifier.
	 *
	 * @param parentIdTag an IdToken.
	 */
	@XmlElement
	public void setParentIdTag(String parentIdTag) {
		this.parentIdTag = parentIdTag;
	}

	/**
	 * This contains whether the idTag has been accepted or not by the Central
	 * System.
	 *
	 * @return the {@link AuthorizationStatus} for IdTag.
	 */
	public AuthorizationStatus getStatus() {
		return status;
	}

	/**
	 * This contains whether the idTag has been accepted or not by the Central
	 * System.
	 *
	 * @return the {@link AuthorizationStatus} for IdTag.
	 */
	@Deprecated
	public AuthorizationStatus objStatus() {
		return status;
	}

	/**
	 * Required. This contains whether the idTag has been accepted or not by the
	 * Central System.
	 *
	 * @param status the {@link AuthorizationStatus} for IdTag.
	 */
	@XmlElement
	public void setStatus(AuthorizationStatus status) {
		this.status = status;
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= this.status != null;
		return valid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IdTagInfo idTagInfo = (IdTagInfo) o;
		return Objects.equals(expiryDate, idTagInfo.expiryDate) && Objects.equals(parentIdTag, idTagInfo.parentIdTag)
				&& status == idTagInfo.status;
	}

	@Override
	public int hashCode() {

		return Objects.hash(expiryDate, parentIdTag, status);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("expiryDate", expiryDate).add("parentIdTag", parentIdTag)
				.add("status", status).toString();
	}
}
