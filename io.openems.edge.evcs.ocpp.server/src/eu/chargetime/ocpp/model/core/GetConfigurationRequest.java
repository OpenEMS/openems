package eu.chargetime.ocpp.model.core;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;

/** Sent by the the Central System to the Charge Point. */
@XmlRootElement
public class GetConfigurationRequest implements Request {

  private String[] key;

  /**
   * List of keys for which the configuration value is requested.
   *
   * @return Array of key names.
   */
  public String[] getKey() {
    return key;
  }

  /**
   * Optional. List of keys for which the configuration value is requested.
   *
   * @param key Array of Strings, max 50 characters each, case insensitive.
   */
  @XmlElement
  public void setKey(String[] key) {
    validateKeys(key);

    this.key = key;
  }

  private void validateKeys(String[] keys) {
    for (String k : keys) {
      if (!ModelUtil.validate(k, 50)) {
        throw new PropertyConstraintException(k.length(), "Exceeds limit of 50 chars");
      }
    }
  }

  @Override
  public boolean validate() {
    return true;
  }

  @Override
  public boolean transactionRelated() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetConfigurationRequest that = (GetConfigurationRequest) o;
    return Arrays.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(key);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("key", key).add("isValid", validate()).toString();
  }
}
