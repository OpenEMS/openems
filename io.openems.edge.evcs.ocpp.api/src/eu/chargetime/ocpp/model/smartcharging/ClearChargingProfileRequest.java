package eu.chargetime.ocpp.model.smartcharging;

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2018 Fabian Röhr <fabian.roehr@netlight.com>
 * Copyright (C) 2018 Robin Roscher <r.roscher@ee-mobility.com>
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

@XmlRootElement
public class ClearChargingProfileRequest implements Request {
  private Integer id;
  private Integer connectorId;
  private ChargingProfilePurposeType chargingProfilePurpose;
  private Integer stackLevel;

  /**
   * The ID of the charging profile to clear.
   *
   * @return id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Optional. The ID of the charging profile to clear.
   *
   * @param id integer.
   */
  @XmlElement
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Specifies the ID of the connector for which to clear charging profiles.
   *
   * @return connectorId.
   */
  public Integer getConnectorId() {
    return connectorId;
  }

  /**
   * Optional. Specifies the ID of the connector for which to clear charging profiles.
   *
   * @param connectorId integer. value &ge; 0
   */
  @XmlElement
  public void setConnectorId(Integer connectorId) {
    if (connectorId != null && connectorId < 0) {
      throw new PropertyConstraintException(connectorId, "connectorId must be >= 0");
    }

    this.connectorId = connectorId;
  }

  /**
   * Specifies the purpose of the charging profiles that will be cleared, if they meet the other
   * criteria in the request.
   *
   * @return the {@link ChargingProfilePurposeType}
   */
  public ChargingProfilePurposeType getChargingProfilePurpose() {
    return chargingProfilePurpose;
  }

  /**
   * Optional. Specifies the purpose of the charging profiles that will be cleared, if they meet the
   * other criteria in the request.
   *
   * @param chargingProfilePurpose the {@link ChargingProfilePurposeType}
   */
  @XmlElement
  public void setChargingProfilePurpose(ChargingProfilePurposeType chargingProfilePurpose) {
    this.chargingProfilePurpose = chargingProfilePurpose;
  }

  /**
   * Specifies the stackLevel for which charging profiles will be cleared, if they meet the other
   * criteria in the request.
   *
   * @return stackLevel.
   */
  public Integer getStackLevel() {
    return stackLevel;
  }

  /**
   * Optional. Specifies the stackLevel for which charging profiles will be cleared, if they meet
   * the other criteria in the request.
   *
   * @param stackLevel integer. value &ge; 0
   */
  @XmlElement
  public void setStackLevel(Integer stackLevel) {
    if (stackLevel != null && stackLevel < 0) {
      throw new PropertyConstraintException(stackLevel, "stackLevel must be >= 0");
    }

    this.stackLevel = stackLevel;
  }

  @Override
  public boolean transactionRelated() {
    return false;
  }

  @Override
  public boolean validate() {
    boolean valid = connectorId == null || connectorId >= 0;
    valid &= stackLevel == null || stackLevel >= 0;

    return valid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClearChargingProfileRequest that = (ClearChargingProfileRequest) o;
    return Objects.equals(id, that.id)
        && Objects.equals(connectorId, that.connectorId)
        && Objects.equals(chargingProfilePurpose, that.chargingProfilePurpose)
        && Objects.equals(stackLevel, that.stackLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, connectorId, chargingProfilePurpose, stackLevel);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("connectorId", connectorId)
        .add("chargingProfilePurpose", chargingProfilePurpose)
        .add("stackLevel", stackLevel)
        .add("isValid", validate())
        .toString();
  }
}
