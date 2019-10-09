package eu.chargetime.ocpp.model.localauthlist;

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
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.MoreObjects;

public class SendLocalListConfirmation implements Confirmation {

  private UpdateStatus status;

  public SendLocalListConfirmation() {}

  public SendLocalListConfirmation(UpdateStatus status) {
    this.status = status;
  }

  /**
   * This indicates whether the Charge Point has successfully received and applied the update of the
   * local authorization list.
   *
   * @return UpdateStatus, status of localAuthList updating.
   */
  public UpdateStatus getStatus() {
    return status;
  }

  /**
   * Required. This indicates whether the Charge Point has successfully received and applied the
   * update of the local authorization list.
   *
   * @param status {@link UpdateStatus}, status of localAuthList updating.
   */
  public void setStatus(UpdateStatus status) {
    if (status == null) {
      throw new PropertyConstraintException(null, "updateStatus must be present");
    }

    this.status = status;
  }

  @Override
  public boolean validate() {
    return status != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SendLocalListConfirmation that = (SendLocalListConfirmation) o;
    return status == that.status;
  }

  @Override
  public int hashCode() {

    return Objects.hash(status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("isValid", validate())
        .toString();
  }
}
