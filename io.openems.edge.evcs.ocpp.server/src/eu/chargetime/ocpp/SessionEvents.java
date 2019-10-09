package eu.chargetime.ocpp;

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;

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

/** Call back handler for {@link Session} events. */
public interface SessionEvents {

  /**
   * Handle a {@link Confirmation} to a {@link Request}.
   *
   * @param uniqueId the unique id used for the {@link Request}.
   * @param confirmation the {@link Confirmation} to the {@link Request}.
   */
  void handleConfirmation(String uniqueId, Confirmation confirmation);

  /**
   * Handle a incoming {@link Request}.
   *
   * @param request the {@link Request}.
   * @return a {@link Confirmation} to send as a response.
   */
  Confirmation handleRequest(Request request) throws UnsupportedFeatureException;

  /**
   * Handle a error to a {@link Request}.
   *
   * @param uniqueId the unique identifier for the {@link Request}.
   * @param errorCode string to indicate the error.
   * @param errorDescription description of the error.
   * @param payload a raw payload.
   */
  void handleError(String uniqueId, String errorCode, String errorDescription, Object payload);

  /** Handle a closed connection. */
  void handleConnectionClosed();

  /** Handle a opened connection. */
  void handleConnectionOpened();
}
