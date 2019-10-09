package eu.chargetime.ocpp.model;

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

/** Wrapper class for a message */
public class Message {
  private String id;
  private Object payload;
  private String action;

  /**
   * Get unique id for message.
   *
   * @return message id.
   */
  public String getId() {
    return id;
  }

  /**
   * Set unique id for message.
   *
   * @param id String, message id.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get attached payload.
   *
   * @return payload.
   */
  public Object getPayload() {
    return payload;
  }

  /**
   * Attach payload to message.
   *
   * @param payload payload object.
   */
  public void setPayload(Object payload) {
    this.payload = payload;
  }

  /**
   * Get the action.
   *
   * @return String, action.
   */
  public String getAction() {
    return action;
  }

  /**
   * Set the action.
   *
   * @param action String, action.
   */
  public void setAction(String action) {
    this.action = action;
  }
}
