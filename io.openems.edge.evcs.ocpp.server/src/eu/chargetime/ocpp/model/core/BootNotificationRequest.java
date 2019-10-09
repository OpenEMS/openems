package eu.chargetime.ocpp.model.core;

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

import eu.chargetime.ocpp.PropertyConstraintException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;

/** Sent by the Charge Point to the Central System. */
@XmlRootElement
@XmlType(
    propOrder = {
      "chargePointVendor",
      "chargePointModel",
      "chargePointSerialNumber",
      "chargeBoxSerialNumber",
      "firmwareVersion",
      "iccid",
      "imsi",
      "meterType",
      "meterSerialNumber"
    })
public class BootNotificationRequest implements Request {

  private static final int STRING_20_CHAR_MAX_LENGTH = 20;
  private static final int STRING_25_CHAR_MAX_LENGTH = 25;
  private static final int STRING_50_CHAR_MAX_LENGTH = 50;
  private static final String ERROR_MESSAGE = "Exceeded limit of %s chars";

  private String chargePointVendor;
  private String chargePointModel;
  private String chargeBoxSerialNumber;
  private String chargePointSerialNumber;
  private String firmwareVersion;
  private String iccid;
  private String imsi;
  private String meterSerialNumber;
  private String meterType;

  public BootNotificationRequest() {}

  /**
   * Handle required fields.
   *
   * @param vendor Charge Point vendor, see {@link #setChargePointVendor(String)}.
   * @param model Charge Point model, see {@link #setChargePointModel(String)}.
   */
  public BootNotificationRequest(String vendor, String model) {
    chargePointVendor = vendor;
    chargePointModel = model;
  }

  /**
   * This contains a value that identifies the vendor of the ChargePoint.
   *
   * @return Vendor of the Charge Point.
   */
  public String getChargePointVendor() {
    return chargePointVendor;
  }

  /**
   * Required. This contains a value that identifies the vendor of the ChargePoint.
   *
   * @param chargePointVendor String, max 20 characters, case insensitive.
   */
  @XmlElement
  public void setChargePointVendor(String chargePointVendor) {
    if (!ModelUtil.validate(chargePointVendor, STRING_20_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          chargePointVendor.length(), validationErrorMessage(STRING_20_CHAR_MAX_LENGTH));
    }

    this.chargePointVendor = chargePointVendor;
  }

  /**
   * This contains a value that identifies the model of the ChargePoint.
   *
   * @return Model of the Charge Point.
   */
  public String getChargePointModel() {
    return chargePointModel;
  }

  /**
   * Required. This contains a value that identifies the model of the ChargePoint.
   *
   * @param chargePointModel String, max 20 characters, case insensitive.
   */
  @XmlElement
  public void setChargePointModel(String chargePointModel) {
    if (!ModelUtil.validate(chargePointModel, STRING_20_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          chargePointModel.length(), validationErrorMessage(STRING_20_CHAR_MAX_LENGTH));
    }

    this.chargePointModel = chargePointModel;
  }

  /**
   * This contains a value that identifies the serial number of the Charge Box inside the Charge
   * Point.
   *
   * @return Serial Number of the Charge Point.
   * @deprecated will be removed in future version. See {@link #getChargePointSerialNumber()}.
   */
  @Deprecated()
  public String getChargeBoxSerialNumber() {
    return chargeBoxSerialNumber;
  }

  /**
   * Optional. This contains a value that identifies the serial number of the Charge Box inside the
   * Charge Point.
   *
   * @param chargeBoxSerialNumber String, max 25 characters, case insensitive.
   * @deprecated will be removed in future version. See {@link #setChargePointSerialNumber(String)}.
   */
  @Deprecated()
  public void setChargeBoxSerialNumber(String chargeBoxSerialNumber) {
    if (!ModelUtil.validate(chargeBoxSerialNumber, STRING_25_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          chargeBoxSerialNumber.length(), validationErrorMessage(STRING_25_CHAR_MAX_LENGTH));
    }

    this.chargeBoxSerialNumber = chargeBoxSerialNumber;
  }

  /**
   * This contains a value that identifies the serial number of the Charge Point.
   *
   * @return Serial Number of the Charge Point.
   */
  public String getChargePointSerialNumber() {
    return chargePointSerialNumber;
  }

  /**
   * Optional. This contains a value that identifies the serial number of the Charge Point.
   *
   * @param chargePointSerialNumber String, max 25 characters, case insensitive.
   */
  @XmlElement
  public void setChargePointSerialNumber(String chargePointSerialNumber) {
    if (!ModelUtil.validate(chargePointSerialNumber, STRING_25_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          chargePointSerialNumber.length(), validationErrorMessage(STRING_25_CHAR_MAX_LENGTH));
    }

    this.chargePointSerialNumber = chargePointSerialNumber;
  }

  /**
   * This contains the firmware version of the Charge Point.
   *
   * @return Firmware version of Charge Point.
   */
  public String getFirmwareVersion() {
    return firmwareVersion;
  }

  /**
   * Optional. This contains the firmware version of the Charge Point.
   *
   * @param firmwareVersion String, max 50 characters, case insensitive.
   */
  @XmlElement
  public void setFirmwareVersion(String firmwareVersion) {
    if (!ModelUtil.validate(firmwareVersion, STRING_50_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          firmwareVersion.length(), validationErrorMessage(STRING_50_CHAR_MAX_LENGTH));
    }

    this.firmwareVersion = firmwareVersion;
  }

  /**
   * This contains the ICCID of the modem’s SIM card.
   *
   * @return ICCID of SIM card.
   */
  public String getIccid() {
    return iccid;
  }

  /**
   * Optional. This contains the ICCID of the modem’s SIM card.
   *
   * @param iccid String, max 20 characters, case insensitive.
   */
  @XmlElement
  public void setIccid(String iccid) {
    if (!ModelUtil.validate(iccid, STRING_20_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          iccid.length(), validationErrorMessage(STRING_20_CHAR_MAX_LENGTH));
    }

    this.iccid = iccid;
  }

  /**
   * This contains the IMSI of the modem’s SIM card.
   *
   * @return IMSI of SIM card.
   */
  public String getImsi() {
    return imsi;
  }

  /**
   * Optional. This contains the IMSI of the modem’s SIM card.
   *
   * @param imsi String, max 20 characters, case insensitive.
   */
  @XmlElement
  public void setImsi(String imsi) {
    if (!ModelUtil.validate(imsi, STRING_20_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          imsi.length(), validationErrorMessage(STRING_20_CHAR_MAX_LENGTH));
    }

    this.imsi = imsi;
  }

  /**
   * This contains the serial number of the main power meter of the Charge Point.
   *
   * @return Serial number of the meter.
   */
  public String getMeterSerialNumber() {
    return meterSerialNumber;
  }

  /**
   * Optional. This contains the serial number of the main power meter of the Charge Point.
   *
   * @param meterSerialNumber String, max 25 characters, case insensitive.
   */
  @XmlElement
  public void setMeterSerialNumber(String meterSerialNumber) {
    if (!ModelUtil.validate(meterSerialNumber, STRING_25_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          meterSerialNumber.length(), validationErrorMessage(STRING_25_CHAR_MAX_LENGTH));
    }

    this.meterSerialNumber = meterSerialNumber;
  }

  /**
   * This contains the type of the main power meter of the Charge Point.
   *
   * @return Type of main power meter.
   */
  public String getMeterType() {
    return meterType;
  }

  /**
   * Optional. This contains the type of the main power meter of the Charge Point.
   *
   * @param meterType String, max 25 characters, case insensitive.
   */
  @XmlElement
  public void setMeterType(String meterType) {
    if (!ModelUtil.validate(meterType, STRING_25_CHAR_MAX_LENGTH)) {
      throw new PropertyConstraintException(
          meterType.length(), validationErrorMessage(STRING_25_CHAR_MAX_LENGTH));
    }

    this.meterType = meterType;
  }

  @Override
  public boolean validate() {
    return ModelUtil.validate(chargePointModel, STRING_20_CHAR_MAX_LENGTH)
        && ModelUtil.validate(chargePointVendor, STRING_20_CHAR_MAX_LENGTH);
  }

  @Override
  public boolean transactionRelated() {
    return false;
  }

  private static String validationErrorMessage(int maxAllowedLength) {
    return String.format(ERROR_MESSAGE, maxAllowedLength);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BootNotificationRequest that = (BootNotificationRequest) o;
    return Objects.equals(chargePointVendor, that.chargePointVendor)
        && Objects.equals(chargePointModel, that.chargePointModel)
        && Objects.equals(chargeBoxSerialNumber, that.chargeBoxSerialNumber)
        && Objects.equals(chargePointSerialNumber, that.chargePointSerialNumber)
        && Objects.equals(firmwareVersion, that.firmwareVersion)
        && Objects.equals(iccid, that.iccid)
        && Objects.equals(imsi, that.imsi)
        && Objects.equals(meterSerialNumber, that.meterSerialNumber)
        && Objects.equals(meterType, that.meterType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        chargePointVendor,
        chargePointModel,
        chargeBoxSerialNumber,
        chargePointSerialNumber,
        firmwareVersion,
        iccid,
        imsi,
        meterSerialNumber,
        meterType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("chargePointVendor", chargePointVendor)
        .add("chargePointModel", chargePointModel)
        .add("chargeBoxSerialNumber", chargeBoxSerialNumber)
        .add("chargePointSerialNumber", chargePointSerialNumber)
        .add("firmwareVersion", firmwareVersion)
        .add("iccid", iccid)
        .add("imsi", imsi)
        .add("meterSerialNumber", meterSerialNumber)
        .add("meterType", meterType)
        .add("isValid", validate())
        .toString();
  }
}
