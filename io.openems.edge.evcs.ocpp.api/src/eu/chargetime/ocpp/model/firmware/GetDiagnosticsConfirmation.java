package eu.chargetime.ocpp.model.firmware;
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
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.utilities.ModelUtil;
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getDiagnosticsResponse")
public class GetDiagnosticsConfirmation implements Confirmation {
  private String fileName;

  @Override
  public boolean validate() {
    return true;
  }

  /**
   * This contains the name of the file with diagnostic information that will be uploaded. This
   * field is not present when no diagnostic information is available.
   *
   * @return String, file name
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Optional. This contains the name of the file with diagnostic information that will be uploaded.
   * This field is not present when no diagnostic information is available.
   *
   * @param fileName String, file name
   */
  @XmlElement
  public void setFileName(String fileName) {
    if (!ModelUtil.validate(fileName, 255)) {
      throw new PropertyConstraintException(fileName.length(), "Exceeds limit of 255 chars");
    }

    this.fileName = fileName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetDiagnosticsConfirmation that = (GetDiagnosticsConfirmation) o;
    return Objects.equals(fileName, that.fileName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileName", fileName)
        .add("isValid", validate())
        .toString();
  }
}
