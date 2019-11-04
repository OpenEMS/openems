package eu.chargetime.ocpp;

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

/** Call back handler for communicator events. */
public interface CommunicatorEvents {
  /**
   * Handle call result.
   *
   * <p>Hint: Use the id to identify the confirmation type, you can then choose to use the {@link
   * Communicator}s unpackPayload method.
   *
   * @param id unique id used to identify the original request.
   * @param action Optional. The action.
   * @param payload raw payload.
   */
  void onCallResult(String id, String action, Object payload);

  /**
   * Handle call.
   *
   * <p>Hint: Use the action name to identify the request, you can then choose to use {@link
   * Communicator}s unpackPayload method.
   *
   * @param id unique id used to reply to server.
   * @param action action name used to identify the feature.
   * @param payload raw payload.
   */
  void onCall(String id, String action, Object payload);

  /**
   * Handle call error.
   *
   * <p>Hint: Use the id to identify the original call. You can use {@link Communicator}s
   * unpackPayload method.
   *
   * @param id unique id used to identify the original request.
   * @param errorCode short text to categorize the error.
   * @param errorDescription a longer text to describe the error.
   * @param payload Object payload attached to the error.
   */
  void onError(String id, String errorCode, String errorDescription, Object payload);

  /** The connection was disconnected. */
  void onDisconnected();

  /** A connection was established. */
  void onConnected();
}
