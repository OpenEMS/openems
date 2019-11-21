package eu.chargetime.ocpp.model.smartcharging;

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChargingProfile;
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
 * Copyright (C) 2017 Emil Christopher Solli Melar <emil@iconsultable.no>
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
public class SetChargingProfileRequest implements Request {
	private Integer connectorId;
	private ChargingProfile csChargingProfiles;

	public SetChargingProfileRequest() {
	}

	public SetChargingProfileRequest(Integer connectorId, ChargingProfile csChargingProfiles) {
		this.connectorId = connectorId;
		this.csChargingProfiles = csChargingProfiles;
	}

	/**
	 * This identifies which connector of the Charge Point is used.
	 *
	 * @return connector.
	 */
	public Integer getConnectorId() {
		return connectorId;
	}

	/**
	 * Required. This identifies which connector of the Charge Point is used.
	 *
	 * @param connectorId integer. value &gt; 0
	 */
	@XmlElement
	public void setConnectorId(Integer connectorId) {
		if (connectorId == null || connectorId < 0) {
			throw new PropertyConstraintException(connectorId, "connectorId must be >= 0");
		}

		this.connectorId = connectorId;
	}

	/**
	 * Charging Profile to be used by the Charge Point for the requested
	 * transaction.
	 *
	 * @return the {@link ChargingProfile}.
	 */
	public ChargingProfile getCsChargingProfiles() {
		return csChargingProfiles;
	}

	/**
	 * Optional. Charging Profile to be used by the Charge Point for the requested
	 * transaction.
	 * {@link ChargingProfile#setChargingProfilePurpose(ChargingProfilePurposeType)}
	 * MUST be set to TxProfile.
	 *
	 * @param csChargingProfiles the {@link ChargingProfile}.
	 */
	@XmlElement(name = "csChargingProfiles")
	public void setCsChargingProfiles(ChargingProfile csChargingProfiles) {
		this.csChargingProfiles = csChargingProfiles;
	}

	@Override
	public boolean transactionRelated() {
		return false;
	}

	@Override
	public boolean validate() {
		boolean valid = connectorId != null && connectorId >= 0;

		if (csChargingProfiles != null) {
			valid &= csChargingProfiles.validate();
		}

		return valid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SetChargingProfileRequest that = (SetChargingProfileRequest) o;
		return Objects.equals(connectorId, that.connectorId)
				&& Objects.equals(csChargingProfiles, that.csChargingProfiles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectorId, csChargingProfiles);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("connectorId", connectorId)
				.add("csChargingProfiles", csChargingProfiles).add("isValid", validate()).toString();
	}
}
