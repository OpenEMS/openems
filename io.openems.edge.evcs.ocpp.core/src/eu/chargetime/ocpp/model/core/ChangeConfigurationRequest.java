package eu.chargetime.ocpp.model.core;

/*
ChargeTime.eu - Java-OCA-OCPP

MIT License

Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>

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
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Sent by Central System to Charge Point.
 *
 * <p>
 * It is RECOMMENDED that the content and meaning of the 'key' and 'value'
 * fields is agreed upon between Charge Point and Central System.
 */
@XmlRootElement
@XmlType(propOrder = { "key", "value" })
public class ChangeConfigurationRequest implements Request {

	private static final String ERROR_MESSAGE = "Exceeded limit of %s chars";
	private static final int KEY_MAX_LENGTH = 50;
	private static final int VALUE_MAX_LENGTH = 500;

	private String key;
	private String value;

	/**
	 * The name of the configuration setting to change.
	 *
	 * @return Name of the configuration setting.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Required. The name of the configuration setting to change.
	 *
	 * @param key String, max 50 characters, case insensitive.
	 */
	@XmlElement
	public void setKey(String key) {
		if (!isValidKey(key)) {
			throw new PropertyConstraintException(key.length(), createErrorMessage(KEY_MAX_LENGTH));
		}

		this.key = key;
	}

	private boolean isValidKey(String key) {
		return ModelUtil.validate(key, KEY_MAX_LENGTH);
	}

	/**
	 * The new value as string for the setting.
	 *
	 * @return Value of the configuration setting.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Required. The new value as string for the setting.
	 *
	 * @param value String, max 500 characters, case insensitive.
	 */
	@XmlElement
	public void setValue(String value) {
		if (!isValidValue(value)) {
			throw new PropertyConstraintException(value.length(), createErrorMessage(VALUE_MAX_LENGTH));
		}

		this.value = value;
	}

	private boolean isValidValue(String value) {
		return ModelUtil.validate(value, VALUE_MAX_LENGTH);
	}

	@Override
	public boolean validate() {
		return isValidKey(this.key) && isValidValue(this.value);
	}

	@Override
	public boolean transactionRelated() {
		return false;
	}

	private static String createErrorMessage(int valueMaxLength) {
		return String.format(ERROR_MESSAGE, valueMaxLength);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ChangeConfigurationRequest that = (ChangeConfigurationRequest) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("key", key).add("value", value).add("isValid", validate())
				.toString();
	}
}
