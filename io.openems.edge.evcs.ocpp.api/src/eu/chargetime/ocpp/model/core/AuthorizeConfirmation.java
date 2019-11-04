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

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** Sent by the Central System to the Charge Point in response to a {@link AuthorizeRequest}. */
@XmlRootElement(name = "authorizeResponse")
public class AuthorizeConfirmation implements Confirmation {
  private IdTagInfo idTagInfo;

  /**
   * This contains information about authorization status, expiry and parent id.
   *
   * @return an instance of {@link IdTagInfo}.
   */
  public IdTagInfo getIdTagInfo() {
    return idTagInfo;
  }

  /**
   * Required. This contains information about authorization status, expiry and parent id.
   *
   * @param idTagInfo an instance of {@link IdTagInfo}.
   */
  @XmlElement
  public void setIdTagInfo(IdTagInfo idTagInfo) {
    this.idTagInfo = idTagInfo;
  }

  @Override
  public boolean validate() {
    boolean valid = true;
    if (valid &= idTagInfo != null) valid &= idTagInfo.validate();
    return valid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuthorizeConfirmation response = (AuthorizeConfirmation) o;
    return Objects.equals(idTagInfo, response.idTagInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(idTagInfo);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("idTagInfo", idTagInfo)
        .add("isValid", validate())
        .toString();
  }
}
