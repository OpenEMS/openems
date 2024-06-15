// @ts-strict-ignore
import { JsonrpcNotification, JsonrpcRequest, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { AuthenticateWithPasswordRequest } from '../jsonrpc/request/authenticateWithPasswordRequest';
import { AuthenticateWithTokenRequest } from '../jsonrpc/request/authenticateWithTokenRequest';

export interface WebsocketInterface {

  /**
   * Logs in by sending an authentication JSON-RPC Request and handles the AuthenticateResponse.
   *
   * @param request the JSON-RPC Request
   */
  login(request: AuthenticateWithPasswordRequest | AuthenticateWithTokenRequest);

  /**
   * Logs out by sending a logout JSON-RPC Request.
   */
  logout(): void;

  /**
   * Sends a JSON-RPC Request to a Websocket and promises a callback.
   *
   * @param request the JSON-RPC Request
   */
  sendRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess>;

  /**
   * Sends a JSON-RPC notification to a Websocket.
   *
   * @param notification the JSON-RPC Notification
   */
  sendNotification(notification: JsonrpcNotification): void;

}
