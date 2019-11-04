package eu.chargetime.ocpp;
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

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.RegistrationStatus;
import eu.chargetime.ocpp.utilities.TimeoutTimer;
import java.util.UUID;

public class TimeoutSessionDecorator implements ISession {

  private TimeoutTimer timeoutTimer;
  private final ISession session;

  /** Handles required injections. */
  public TimeoutSessionDecorator(TimeoutTimer timeoutTimer, ISession session) {
    this.timeoutTimer = timeoutTimer;
    this.session = session;
  }

  private void resetTimer(int timeoutInSec) {
    if (timeoutTimer != null) timeoutTimer.setTimeout(timeoutInSec * 1000);
    resetTimer();
  }

  private void resetTimer() {
    if (timeoutTimer != null) timeoutTimer.reset();
  }

  private void stopTimer() {
    if (timeoutTimer != null) timeoutTimer.end();
  }

  private void startTimer() {
    if (timeoutTimer != null) timeoutTimer.begin();
  }

  @Override
  public UUID getSessionId() {
    return session.getSessionId();
  }

  @Override
  public void open(String uri, SessionEvents eventHandler) {
    SessionEvents events = createEventHandler(eventHandler);
    this.session.open(uri, events);
  }

  @Override
  public void accept(SessionEvents eventHandler) {
    SessionEvents events = createEventHandler(eventHandler);
    this.session.accept(events);
  }

  @Override
  public String storeRequest(Request payload) {
    return this.session.storeRequest(payload);
  }

  @Override
  public void sendRequest(String action, Request payload, String uuid) {
    this.session.sendRequest(action, payload, uuid);
  }

  @Override
  public void close() {
    this.session.close();
  }

  private SessionEvents createEventHandler(SessionEvents eventHandler) {
    return new SessionEvents() {
      @Override
      public void handleConfirmation(String uniqueId, Confirmation confirmation) {
        resetTimer();
        eventHandler.handleConfirmation(uniqueId, confirmation);
      }

      @Override
      public synchronized Confirmation handleRequest(Request request)
          throws UnsupportedFeatureException {
        resetTimer();
        Confirmation confirmation = eventHandler.handleRequest(request);

        if (confirmation instanceof BootNotificationConfirmation) {
          BootNotificationConfirmation bootNotification =
              (BootNotificationConfirmation) confirmation;
          if (bootNotification.getStatus() == RegistrationStatus.Accepted) {
            resetTimer(bootNotification.getInterval());
          }
        }
        return confirmation;
      }

      @Override
      public void handleError(
          String uniqueId, String errorCode, String errorDescription, Object payload) {
        eventHandler.handleError(uniqueId, errorCode, errorDescription, payload);
      }

      @Override
      public void handleConnectionClosed() {
        eventHandler.handleConnectionClosed();
        stopTimer();
      }

      @Override
      public void handleConnectionOpened() {
        eventHandler.handleConnectionOpened();
        startTimer();
      }
    };
  }
}
