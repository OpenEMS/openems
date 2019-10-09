package eu.chargetime.ocpp.model.localauthlist;

import java.util.Arrays;
import java.util.Objects;

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
import eu.chargetime.ocpp.utilities.MoreObjects;

public class SendLocalListRequest implements Request {

  private int listVersion = 0;
  private AuthorizationData[] localAuthorizationList = null;
  private UpdateType updateType = null;

  public SendLocalListRequest() {}

  public SendLocalListRequest(int listVersion, UpdateType updateType) {
    this.listVersion = listVersion;
    this.updateType = updateType;
  }

  public void setListVersion(int listVersion) {
    if (listVersion < 1) {
      throw new PropertyConstraintException(listVersion, "listVersion must be > 0");
    }
    this.listVersion = listVersion;
  }

  public int getListVersion() {
    return listVersion;
  }

  public void setLocalAuthorizationList(AuthorizationData[] localAuthorizationList) {
    this.localAuthorizationList = localAuthorizationList;
  }

  public AuthorizationData[] getLocalAuthorizationList() {
    return localAuthorizationList;
  }

  public void setUpdateType(UpdateType updateType) {
    this.updateType = updateType;
  }

  public UpdateType getUpdateType() {
    return updateType;
  }

  @Override
  public boolean validate() {
    boolean valid = (listVersion >= 1) && (updateType != null);

    if (localAuthorizationList != null) {
      for (AuthorizationData data : localAuthorizationList) {
        valid &= data.validate();
      }
    }

    return valid;
  }

  @Override
  public boolean transactionRelated() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SendLocalListRequest that = (SendLocalListRequest) o;
    return Objects.equals(listVersion, that.listVersion)
        && Arrays.equals(localAuthorizationList, that.localAuthorizationList)
        && updateType == that.updateType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(listVersion, localAuthorizationList, updateType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("listVersion", listVersion)
        .add("localAuthorizationList", localAuthorizationList)
        .add("updateType", updateType)
        .add("isValid", validate())
        .toString();
  }
}
