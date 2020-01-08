package eu.chargetime.ocpp.model.core;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;

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

/** Sent by the Charge Point to the Central System. */
@XmlRootElement
public class AuthorizeRequest implements Request {

	private static final int IDTAG_MAX_LENGTH = 20;
	private static final String ERROR_MESSAGE = "Exceeded limit of " + IDTAG_MAX_LENGTH + " chars";

	private String idTag;

	public AuthorizeRequest() {
	}

	/**
	 * Handle required fields.
	 *
	 * @param idToken authorize id.
	 */
	public AuthorizeRequest(String idToken) {
		setIdTag(idToken);
	}

	/**
	 * This contains the identifier that needs to be authorized.
	 *
	 * @return String, max 20 characters. Case insensitive.
	 */
	public String getIdTag() {
		return idTag;
	}

	/**
	 * Required. This contains the identifier that needs to be authorized.
	 *
	 * @param idTag String, max 20 characters. Case insensitive.
	 */
	@XmlElement
	public void setIdTag(String idTag) {
		if (!ModelUtil.validate(idTag, IDTAG_MAX_LENGTH)) {
			throw new PropertyConstraintException(idTag.length(), ERROR_MESSAGE);
		}

		this.idTag = idTag;
	}

	@Override
	public boolean validate() {
		return ModelUtil.validate(idTag, IDTAG_MAX_LENGTH);
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
		AuthorizeRequest request = (AuthorizeRequest) o;
		return Objects.equals(idTag, request.idTag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idTag);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("idTag", idTag).add("isValid", validate()).toString();
	}
}
