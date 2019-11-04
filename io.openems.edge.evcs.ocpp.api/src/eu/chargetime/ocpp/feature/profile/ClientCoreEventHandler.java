package eu.chargetime.ocpp.feature.profile;

import eu.chargetime.ocpp.model.core.*;

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

/** Call back handler for client events of the core feature profile. */
public interface ClientCoreEventHandler {
  /**
   * Handle a {@link ChangeAvailabilityRequest} and return a {@link ChangeAvailabilityConfirmation}.
   *
   * @param request incoming {@link ChangeAvailabilityRequest} to handle.
   * @return outgoing {@link ChangeAvailabilityConfirmation} to reply with.
   */
  ChangeAvailabilityConfirmation handleChangeAvailabilityRequest(ChangeAvailabilityRequest request);

  /**
   * Handle a {@link GetConfigurationRequest} and return a {@link GetConfigurationConfirmation}.
   *
   * @param request incoming {@link GetConfigurationRequest} to handle.
   * @return outgoing {@link GetConfigurationConfirmation} to reply with.
   */
  GetConfigurationConfirmation handleGetConfigurationRequest(GetConfigurationRequest request);

  /**
   * Handle a {@link ChangeConfigurationRequest} and return a {@link
   * ChangeConfigurationConfirmation}.
   *
   * @param request incoming {@link ChangeConfigurationRequest} to handle.
   * @return outgoing {@link ChangeConfigurationConfirmation} to reply with.
   */
  ChangeConfigurationConfirmation handleChangeConfigurationRequest(
      ChangeConfigurationRequest request);

  /**
   * Handle a {@link ClearCacheRequest} and return a {@link ClearCacheConfirmation}.
   *
   * @param request incoming {@link ClearCacheRequest} to handle.
   * @return outgoing {@link ClearCacheConfirmation} to reply with.
   */
  ClearCacheConfirmation handleClearCacheRequest(ClearCacheRequest request);

  /**
   * Handle a {@link DataTransferRequest} and return a {@link DataTransferConfirmation}.
   *
   * @param request incoming {@link DataTransferRequest} to handle.
   * @return outgoing {@link DataTransferConfirmation} to reply with.
   */
  DataTransferConfirmation handleDataTransferRequest(DataTransferRequest request);

  /**
   * Handle a {@link RemoteStartTransactionRequest} and return a {@link
   * RemoteStartTransactionConfirmation}.
   *
   * @param request incoming {@link RemoteStartTransactionRequest} to handle.
   * @return outgoing {@link RemoteStartTransactionConfirmation} to reply with.
   */
  RemoteStartTransactionConfirmation handleRemoteStartTransactionRequest(
      RemoteStartTransactionRequest request);

  /**
   * Handle a {@link RemoteStopTransactionRequest} and return a {@link
   * RemoteStopTransactionConfirmation}.
   *
   * @param request incoming {@link RemoteStopTransactionRequest} to handle.
   * @return outgoing {@link RemoteStopTransactionConfirmation} to reply with.
   */
  RemoteStopTransactionConfirmation handleRemoteStopTransactionRequest(
      RemoteStopTransactionRequest request);

  /**
   * Handle a {@link ResetRequest} and return a {@link ResetConfirmation}.
   *
   * @param request incoming {@link ResetRequest} to handle.
   * @return outgoing {@link ResetConfirmation} to reply with.
   */
  ResetConfirmation handleResetRequest(ResetRequest request);

  /**
   * Handle a {@link UnlockConnectorRequest} and return a {@link UnlockConnectorConfirmation}.
   *
   * @param request incoming {@link UnlockConnectorRequest} to handle.
   * @return outgoing {@link UnlockConnectorConfirmation} to reply with.
   */
  UnlockConnectorConfirmation handleUnlockConnectorRequest(UnlockConnectorRequest request);
}
