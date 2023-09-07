import { compareVersions } from 'compare-versions';
import { BehaviorSubject, Subject } from 'rxjs';

import { JsonrpcRequest, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { EdgeConfigNotification } from '../jsonrpc/notification/edgeConfigNotification';
import { SystemLogNotification } from '../jsonrpc/notification/systemLogNotification';
import { CreateComponentConfigRequest } from '../jsonrpc/request/createComponentConfigRequest';
import { DeleteComponentConfigRequest } from '../jsonrpc/request/deleteComponentConfigRequest';
import { EdgeRpcRequest } from '../jsonrpc/request/edgeRpcRequest';
import { GetEdgeConfigRequest } from '../jsonrpc/request/getEdgeConfigRequest';
import { SubscribeChannelsRequest } from '../jsonrpc/request/subscribeChannelsRequest';
import { SubscribeSystemLogRequest } from '../jsonrpc/request/subscribeSystemLogRequest';
import { UpdateComponentConfigRequest } from '../jsonrpc/request/updateComponentConfigRequest';
import { GetEdgeConfigResponse } from '../jsonrpc/response/getEdgeConfigResponse';
import { ArrayUtils } from '../service/arrayUtils';
import { Websocket } from '../service/websocket';
import { ChannelAddress } from '../type/channeladdress';
import { Role } from '../type/role';
import { SystemLog } from '../type/systemlog';
import { CurrentData } from './currentdata';
import { EdgeConfig } from './edgeconfig';

export class Edge {

  constructor(
    public readonly id: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly version: string,
    public readonly role: Role,
    public isOnline: boolean,
    public readonly lastmessage: Date
  ) { }

  // holds currently subscribed channels, identified by source id
  private subscribedChannels: { [sourceId: string]: ChannelAddress[] } = {};

  // holds current data
  public currentData: BehaviorSubject<CurrentData> = new BehaviorSubject<CurrentData>(new CurrentData({}));

  // holds system log
  public systemLog: Subject<SystemLog> = new Subject<SystemLog>();

  // holds config
  private config: BehaviorSubject<EdgeConfig> = new BehaviorSubject<EdgeConfig>(null);

  // determine if subscribe on channels was successful
  // used in live component to hide elements while no channel data available
  public subscribeChannelsSuccessful: boolean = false;

  /**
   * Gets the Config. If not available yet, it requests it via Websocket.
   * 
   * @param websocket the Websocket connection
   */
  public getConfig(websocket: Websocket): BehaviorSubject<EdgeConfig> {
    if (this.config.value == null || !this.config.value.isValid()) {
      this.refreshConfig(websocket);
    }
    return this.config;
  }

  /**
   * Called by Service, when this Edge is set as currentEdge.
   */
  public markAsCurrentEdge(websocket: Websocket): void {
    this.getConfig(websocket);
  }

  /**
   * Refresh the config.
   */
  private refreshConfig(websocket: Websocket): void {
    // make sure to send not faster than every 1000 ms
    if (this.isRefreshConfigBlocked) {
      return;
    }
    // block refreshConfig()
    this.isRefreshConfigBlocked = true;
    setTimeout(() => {
      // unblock refreshConfig()
      this.isRefreshConfigBlocked = false;
    }, 1000);

    let request = new GetEdgeConfigRequest();
    this.sendRequest(websocket, request).then(response => {
      let edgeConfigResponse = response as GetEdgeConfigResponse;
      this.config.next(new EdgeConfig(this, edgeConfigResponse.result));
    }).catch(reason => {
      console.warn("Unable to refresh config", reason);
      this.config.next(new EdgeConfig(this));
    });
  }
  private isRefreshConfigBlocked: boolean = false;

  /**
   * Add Channels to subscription
   * 
   * @param websocket the Websocket
   * @param id        a unique ID for this subscription (e.g. the component selector)
   * @param channels  the subscribed Channel-Addresses
   */
  public subscribeChannels(websocket: Websocket, id: string, channels: ChannelAddress[]): void {
    this.subscribedChannels[id] = channels;
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Refreshes Channels subscriptions on websocket reconnect.
   * 
   * @param websocket the Websocket
   */
  public subscribeChannelsOnReconnect(websocket: Websocket): void {
    if (Object.keys(this.subscribedChannels).length > 0) {
      this.sendSubscribeChannels(websocket);
    }
  }

  /**
   * Removes Channels from subscription
   * 
   * @param websocket the Websocket
   * @param id        the unique ID for this subscription
   */
  public unsubscribeChannels(websocket: Websocket, id: string): void {
    delete this.subscribedChannels[id];
    this.sendSubscribeChannels(websocket);
  }

  public unsubscribeFromChannels(websocket: Websocket, channels: ChannelAddress[]) {
    this.subscribedChannels = Object.entries(this.subscribedChannels).reduce((arr, [id, subscribedChannels]) => {
      if (ArrayUtils.equalsCheck(channels.map(channel => channel.toString()), subscribedChannels.map(channel => channel.toString()))) {
        return arr;
      }

      arr[id] = subscribedChannels;

      return arr;
    }, {});

    this.sendSubscribeChannels(websocket);
  }

  public unsubscribeFromAllChannels(websocket: Websocket) {
    this.subscribedChannels = {};
    this.sendSubscribeChannels(websocket);
  }
  /**
   * Subscribe to System-Log
   * 
   * @param websocket the Websocket
   */
  public subscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest({ subscribe: true }));
  }

  /**
   * Unsubscribe from System-Log
   * 
   * @param websocket the Websocket
   */
  public unsubscribeSystemLog(websocket: Websocket): Promise<JsonrpcResponseSuccess> {
    return this.sendRequest(websocket, new SubscribeSystemLogRequest({ subscribe: false }));
  }

  /**
   * Sends a SubscribeChannelsRequest for all Channels in 'this.subscribedChannels'
   * 
   * @param websocket the Websocket
   */
  private sendSubscribeChannels(websocket: Websocket): void {
    // make sure to send not faster than every 100 ms
    if (this.subscribeChannelsTimeout == null) {
      this.subscribeChannelsTimeout = setTimeout(() => {
        // reset subscribeChannelsTimeout
        this.subscribeChannelsTimeout = null;

        // merge channels from currentDataSubscribes
        let channels: ChannelAddress[] = [];
        for (let componentId in this.subscribedChannels) {
          channels.push.apply(channels, this.subscribedChannels[componentId]);
        }
        let request = new SubscribeChannelsRequest(channels);
        this.sendRequest(websocket, request).then(() => {
          this.subscribeChannelsSuccessful = true;
        }).catch(reason => {
          this.subscribeChannelsSuccessful = false;
          console.warn(reason);
        });
      }, 100);
    }
  }
  private subscribeChannelsTimeout: any = null;

  /**
   * Handles a EdgeConfigNotification
   */
  public handleEdgeConfigNotification(message: EdgeConfigNotification): void {
    this.config.next(new EdgeConfig(this, message.params));
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
    let wrap = new EdgeRpcRequest({ edgeId: this.id, payload: request });
    return new Promise((resolve, reject) => {
      ws.sendRequest(wrap).then(response => {
        resolve(response['result']['payload']);
      }).catch(reason => {
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
   * Returns whether the given version is higher than the Edge' version
   * 
   * Example: {{ edge.isVersionAtLeast('2018.9') }}
   * 
   * @param version 
   */
  public isVersionAtLeast(version: string): boolean {
    return compareVersions(this.version, version) >= 0;
  }

  /**
   * Determines if the verion of the edge is a snapshot.
   * 
   * @returns true if the verion of the edge is a snapshot
   */
  public isSnapshot(): boolean {
    return this.version.includes("SNAPSHOT");
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