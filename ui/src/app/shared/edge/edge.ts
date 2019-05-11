import { BehaviorSubject, Subject } from 'rxjs';
import { cmp } from 'semver-compare-multi';
import { environment as env } from '../../../environments';
import { JsonrpcRequest, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { SystemLogNotification } from '../jsonrpc/notification/systemLogNotification';
import { CreateComponentConfigRequest } from '../jsonrpc/request/createComponentConfigRequest';
import { DeleteComponentConfigRequest } from '../jsonrpc/request/deleteComponentConfigRequest';
import { EdgeRpcRequest } from '../jsonrpc/request/edgeRpcRequest';
import { GetEdgeConfigRequest } from '../jsonrpc/request/getEdgeConfigRequest';
import { SubscribeChannelsRequest } from '../jsonrpc/request/subscribeChannelsRequest';
import { SubscribeSystemLogRequest } from '../jsonrpc/request/subscribeSystemLogRequest';
import { UpdateComponentConfigRequest } from '../jsonrpc/request/updateComponentConfigRequest';
import { GetEdgeConfigResponse } from '../jsonrpc/response/getEdgeConfigResponse';
import { Websocket } from '../service/websocket';
import { ChannelAddress } from '../type/channeladdress';
import { Role } from '../type/role';
import { SystemLog } from '../type/systemlog';
import { CurrentData } from './currentdata';
import { EdgeConfig } from './edgeconfig';
import { EdgeConfigNotification } from '../jsonrpc/notification/edgeConfigNotification';

export class Edge {

  constructor(
    public readonly id: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly version: string,
    public readonly role: Role,
    public isOnline: boolean
  ) { }

  // holds currently subscribed channels, identified by source id
  private subscribedChannels: { [sourceId: string]: ChannelAddress[] } = {};

  // holds current data
  public currentData: BehaviorSubject<CurrentData> = new BehaviorSubject<CurrentData>(new CurrentData({}));

  // holds system log
  public systemLog: Subject<SystemLog> = new Subject<SystemLog>();

  // holds config
  public config: BehaviorSubject<EdgeConfig> = new BehaviorSubject<EdgeConfig>(new EdgeConfig());

  /**
   * Gets the Config. If not available yet, it requests it via Websocket.
   * 
   * Alternatively use Service.getEdgeConfig() which gives you a Promise.
   * 
   * @param websocket the Websocket connection
   */
  public getConfig(websocket: Websocket): BehaviorSubject<EdgeConfig> {
    if (!this.config.value.isValid()) {
      this.refreshConfig(websocket);
    }
    return this.config;
  }

  /**
   * Called by Service, when this Edge is set as currentEdge.
   */
  public markAsCurrentEdge(websocket: Websocket): void {
    if (this.config.value == null) {
      this.refreshConfig(websocket);
    }
  }

  /**
   * Refresh the config.
   */
  public refreshConfig(websocket: Websocket): void {
    let request = new GetEdgeConfigRequest();
    this.sendRequest(websocket, request).then(response => {
      let edgeConfigResponse = response as GetEdgeConfigResponse;
      this.config.next(new EdgeConfig(edgeConfigResponse.result));
    }).catch(reason => {
      console.warn("refreshConfig got error", reason)
      // TODO error
      this.config.next(new EdgeConfig());
    });
  }

  /**
   * Add Channels to subscription
   * 
   * @param websocket the Websocket
   * @param id        a unique ID for this subscription (e.g. the component selector)
   * @param channels  the subscribed Channel-Adresses
   */
  public subscribeChannels(websocket: Websocket, id: string, channels: ChannelAddress[]): Promise<JsonrpcResponseSuccess> {
    this.subscribedChannels[id] = channels;
    return this.sendSubscribeChannels(websocket);
  }

  /**
   * Removes Channels from subscription
   * 
   * @param websocket the Websocket
   * @param id        the unique ID for this subscription
   */
  public unsubscribeChannels(websocket: Websocket, id: string): Promise<JsonrpcResponseSuccess> {
    delete this.subscribedChannels[id];
    return this.sendSubscribeChannels(websocket);
  }

  /**
   * Subscribe to System-Log
   * 
   * @param websocket the Websocket
   */
  public subscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest(true));
  }

  /**
   * Unsubscribe from System-Log
   * 
   * @param websocket the Websocket
   */
  public unsubscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest(false));
  }

  /**
   * Sends a SubscribeChannelsRequest for all Channels in 'this.subscribedChannels'
   * 
   * @param websocket the Websocket
   */
  private sendSubscribeChannels(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    // merge channels from currentDataSubscribes
    let channels: ChannelAddress[] = [];
    for (let componentId in this.subscribedChannels) {
      channels.push.apply(channels, this.subscribedChannels[componentId]);
    }
    let request = new SubscribeChannelsRequest(channels);
    return this.sendRequest(websocket, request);
  }

  /**
   * Handles a EdgeConfigNotification
   */
  public handleEdgeConfigNotification(message: EdgeConfigNotification): void {
    this.config.next(new EdgeConfig(message.params));
  }

  /**
   * Handles a CurrentDataNotification
   */
  public handleCurrentDataNotification(message: CurrentDataNotification): void {
    this.currentData.next(new CurrentData(message.params));
  }

  /**
   * Handles a SystemLogNotification
   */
  public handleSystemLogNotification(message: SystemLogNotification): void {
    this.systemLog.next(message.params.line);
  }

  /**
   * Creates a configuration for a OpenEMS Edge Component.
   * 
   * @param ws          the Websocket
   * @param factoryPId  the OpenEMS Edge Factory-PID 
   * @param properties  the properties to be updated.
   */
  public createComponentConfig(ws: Websocket, factoryPid: string, properties: { name: string, value: any }[]): Promise<JsonrpcResponseSuccess> {
    let request = new CreateComponentConfigRequest({ factoryPid: factoryPid, properties: properties });
    return this.sendRequest(ws, request);
  }

  /**
   * Updates the configuration of a OpenEMS Edge Component.
   * 
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID 
   * @param properties  the properties to be updated.
   */
  public updateComponentConfig(ws: Websocket, componentId: string, properties: { name: string, value: any }[]): Promise<JsonrpcResponseSuccess> {
    let request = new UpdateComponentConfigRequest({ componentId: componentId, properties: properties });
    return this.sendRequest(ws, request);
  }

  /**
   * Deletes the configuration of a OpenEMS Edge Component.
   * 
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID 
   */
  public deleteComponentConfig(ws: Websocket, componentId: string): Promise<JsonrpcResponseSuccess> {
    let request = new DeleteComponentConfigRequest({ componentId: componentId });
    return this.sendRequest(ws, request);
  }

  /**
   * Sends a JSON-RPC Request. The Request is wrapped in a EdgeRpcRequest.
   * 
   * @param ws               the Websocket
   * @param request          the JSON-RPC Request
   * @param responseCallback the JSON-RPC Response callback
   */
  public sendRequest(ws: Websocket, request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    let wrap = new EdgeRpcRequest(this.id, request);
    return new Promise((resolve, reject) => {
      ws.sendRequest(wrap).then(response => {
        if (env.debugMode) {
          console.info("Response     [" + request.method + "]", response);
        }
        resolve(response['result']['payload']);
      }).catch(reason => {
        if (env.debugMode) {
          console.warn("Request fail [" + request.method + "]", reason);
        }
        reject(reason);
      });
    });
  }

  /**
   * Mark this edge as online or offline
   * 
   * @param isOnline 
   */
  public setOnline(isOnline: boolean): void {
    this.isOnline = isOnline;
  }

  /**
   * System Execute
   * 
   * TODO deprecated
   */
  public systemExecute(password: string, command: string, background: boolean, timeout: number): void {
    console.warn("Edge.systemExecute()", password, command);
    // let replyStream = this.sendMessageWithReply(DefaultMessages.systemExecute(this.edgeId, password, command, background, timeout));
    // // wait for reply
    // return new Promise((resolve, reject) => {
    //   replyStream.pipe(first()).subscribe(reply => {
    //     let output = (reply as DefaultMessages.SystemExecuteReply).system.output;
    //     this.removeReplyStream(reply);
    //     resolve(output);
    //   });
    // })
  }

  /**
   * Returns whether the given version is higher than the Edge' version
   * 
   * Example: {{ edge.isVersionAtLeast('2018.9') }}
   * 
   * @param version 
   */
  public isVersionAtLeast(version: string): boolean {
    return cmp(this.version, version) >= 0;
  }

  /**
	 * Evaluates whether the current Role is equal or more privileged than the
	 * given Role.
	 * 
	 * @param role     the compared Role
	 * @return true if the current Role is equal or more privileged than the given Role
	 */
  public roleIsAtLeast(role: Role | string): boolean {
    return Role.isAtLeast(this.role, role);
  }

  /**
   * Gets the Role of the Edge as a human-readable string.
   * 
   * @returns the name of the role
   */
  public getRoleString(): string {
    return Role[this.role].toLowerCase();
  }
}