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

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** Sent to Charge Point by Central System. */
@XmlRootElement
@XmlType(propOrder = {"connectorId", "idTag", "chargingProfile"})
public class RemoteStartTransactionRequest implements Request {

  private Integer connectorId;
  private String idTag;
  private ChargingProfile chargingProfile;

  @Override
  public boolean validate() {
    boolean valid = ModelUtil.validate(idTag, 20);

    if (chargingProfile != null) {
      valid &= chargingProfile.validate();
    }

    if (connectorId != null) {
      valid &= connectorId > 0;
    }

    return valid;
  }

  /**
   * Number of the connector on which to start the transaction. connectorId SHALL be &gt; 0.
   *
   * @return Connector.
   */
  public Integer getConnectorId() {
    return connectorId;
  }

  /**
   * Optional. Number of the connector on which to start the transaction. connectorId SHALL be &gt;
   * 0.
   *
   * @param connectorId integer, connector
   */
  @XmlElement
  public void setConnectorId(Integer connectorId) {
    if (connectorId <= 0) {
      throw new PropertyConstraintException(connectorId, "connectorId must be > 0");
    }

    this.connectorId = connectorId;
  }

  /**
   * The identifier that Charge Point must use to start a transaction.
   *
   * @return an IdToken.
   */
  public String getIdTag() {
    return idTag;
  }

  /**
   * Required. The identifier that Charge Point must use to start a transaction.
   *
   * @param idTag a String with max length 20
   */
  @XmlElement
  public void setIdTag(String idTag) {
    if (!ModelUtil.validate(idTag, 20)) {
      throw new PropertyConstraintException(idTag.length(), "Exceeded limit of 20 chars");
    }

    this.idTag = idTag;
  }

  /**
   * Charging Profile to be used by the Charge Point for the requested transaction.
   *
   * @return the {@link ChargingProfile}.
   */
  public ChargingProfile getChargingProfile() {
    return chargingProfile;
  }

  /**
   * Optional. Charging Profile to be used by the Charge Point for the requested transaction. {@link
   * ChargingProfile#setChargingProfilePurpose(ChargingProfilePurposeType)} MUST be set to
   * TxProfile.
   *
   * @param chargingProfile the {@link ChargingProfile}.
   */
  @XmlElement
  public void setChargingProfile(ChargingProfile chargingProfile) {
    this.chargingProfile = chargingProfile;
  }

  @Override
  public boolean transactionRelated() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RemoteStartTransactionRequest that = (RemoteStartTransactionRequest) o;
    return Objects.equals(connectorId, that.connectorId)
        && Objects.equals(idTag, that.idTag)
        && Objects.equals(chargingProfile, that.chargingProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectorId, idTag, chargingProfile);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("connectorId", connectorId)
        .add("idTag", idTag)
        .add("chargingProfile", chargingProfile)
        .add("isValid", validate())
        .toString();
  }
}
