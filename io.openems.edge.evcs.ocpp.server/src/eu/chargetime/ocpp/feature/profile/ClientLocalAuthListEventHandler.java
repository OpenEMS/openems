package eu.chargetime.ocpp.feature.profile;

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

import eu.chargetime.ocpp.model.localauthlist.GetLocalListVersionConfirmation;
import eu.chargetime.ocpp.model.localauthlist.GetLocalListVersionRequest;
import eu.chargetime.ocpp.model.localauthlist.SendLocalListConfirmation;
import eu.chargetime.ocpp.model.localauthlist.SendLocalListRequest;

public interface ClientLocalAuthListEventHandler {
  /**
   * Handle a {@link GetLocalListVersionRequest} and return a {@link
   * GetLocalListVersionConfirmation}.
   *
   * @param request incoming {@link GetLocalListVersionRequest} to handle.
   * @return outgoing {@link GetLocalListVersionConfirmation} to reply with.
   */
  GetLocalListVersionConfirmation handleGetLocalListVersionRequest(
      GetLocalListVersionRequest request);

  /**
   * Handle a {@link SendLocalListRequest} and return a {@link SendLocalListConfirmation}.
   *
   * @param request incoming {@link SendLocalListRequest} to handle.
   * @return outgoing {@link SendLocalListConfirmation} to reply with.
   */
  SendLocalListConfirmation handleSendLocalListRequest(SendLocalListRequest request);
}
