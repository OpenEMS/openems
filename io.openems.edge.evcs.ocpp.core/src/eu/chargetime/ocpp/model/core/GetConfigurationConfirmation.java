package eu.chargetime.ocpp.model.core;

import java.util.Arrays;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Confirmation;
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

/**
 * Sent by Charge Point the to the Central System in response to a
 * {@link GetConfigurationRequest}.
 */
@XmlRootElement(name = "getConfigurationResponse")
@XmlType(propOrder = { "configurationKey", "unknownKey" })
public class GetConfigurationConfirmation implements Confirmation {

	private static final String ERROR_MESSAGE = "Exceeds limit of %s chars";

	private KeyValueType[] configurationKey;
	private String[] unknownKey;

	/**
	 * List of requested or known keys.
	 *
	 * @return Array of {@link KeyValueType}.
	 */
	public KeyValueType[] getConfigurationKey() {
		return configurationKey;
	}

	/**
	 * Optional. List of requested or known keys.
	 *
	 * @param configurationKey Array of {@link KeyValueType}.
	 */
	@XmlElement
	public void setConfigurationKey(KeyValueType[] configurationKey) {
		this.configurationKey = configurationKey;
	}

	/**
	 * Requested keys that are unknown.
	 *
	 * @return Array of key names.
	 */
	public String[] getUnknownKey() {
		return unknownKey;
	}

	/**
	 * Optional. Requested keys that are unknown.
	 *
	 * @param unknownKey Array of String, max 50 characters, case insensitive.
	 */
	@XmlElement
	public void setUnknownKey(String[] unknownKey) {
		isValidUnknownKey(unknownKey);

		this.unknownKey = unknownKey;
	}

	private void isValidUnknownKey(String[] unknownKeys) {

		for (String key : unknownKeys) {
			if (!ModelUtil.validate(key, 50)) {
				throw new PropertyConstraintException(key.length(), String.format(ERROR_MESSAGE, 50));
			}
		}
	}

	private boolean validateConfigurationKeys() {
		boolean output = true;

		if (configurationKey != null && configurationKey.length > 0) {
			for (KeyValueType key : configurationKey) {
				if (!key.validate()) {
					output = false;
					break;
				}
			}
		}

		return output;
	}

	@Override
	public boolean validate() {
		return validateConfigurationKeys();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GetConfigurationConfirmation that = (GetConfigurationConfirmation) o;
		return Arrays.equals(configurationKey, that.configurationKey) && Arrays.equals(unknownKey, that.unknownKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configurationKey, unknownKey);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("configurationKey", configurationKey).add("unknownKey", unknownKey)
				.add("isValid", validate()).toString();
	}
}
