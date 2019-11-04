package eu.chargetime.ocpp; /*
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

/** Exception returned to an outgoing request if an error is reported from the other end. */
public class CallErrorException extends Exception {
  private String errorCode;
  private String errorDescription;
  private Object payload;

  /**
   * Constructor.
   *
   * @param errorCode send from the other end.
   * @param errorDescription describing the error.
   * @param payload raw payload send from the other end.
   */
  public CallErrorException(String errorCode, String errorDescription, Object payload) {
    super(
        "errorCode='"
            + errorCode
            + '\''
            + ", errorDescription='"
            + errorDescription
            + '\''
            + ", payload="
            + payload);
    this.errorCode = errorCode;
    this.errorDescription = errorDescription;
    this.payload = payload;
  }

  /**
   * A String containing the error code, send by the other part.
   *
   * @return the error code.
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * A String containing the error code, send by the other part.
   *
   * @param errorCode the error code.
   */
  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Description of the error.
   *
   * @return the error description.
   */
  public String getErrorDescription() {
    return errorDescription;
  }

  /**
   * Description of the error.
   *
   * @param errorDescription the error description.
   */
  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }

  /**
   * The raw payload send from the other end.
   *
   * @return the raw payload.
   */
  public Object getPayload() {
    return payload;
  }

  /**
   * The raw payload send from the other end.
   *
   * @param payload the raw payload.
   */
  public void setPayload(Object payload) {
    this.payload = payload;
  }
}
