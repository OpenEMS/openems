// @ts-strict-ignore
import { compareVersions } from 'compare-versions';
import { BehaviorSubject, Subject } from 'rxjs';
import { SumState } from 'src/app/index/shared/sumState';

import { CurrentData } from './currentdata';
import { EdgeConfig } from './edgeconfig';
import { filter, first } from 'rxjs/operators';
import { JsonrpcResponseSuccess, JsonrpcRequest } from '../../jsonrpc/base';
import { CurrentDataNotification } from '../../jsonrpc/notification/currentDataNotification';
import { EdgeConfigNotification } from '../../jsonrpc/notification/edgeConfigNotification';
import { SystemLogNotification } from '../../jsonrpc/notification/systemLogNotification';
import { ComponentJsonApiRequest } from '../../jsonrpc/request/componentJsonApiRequest';
import { CreateComponentConfigRequest } from '../../jsonrpc/request/createComponentConfigRequest';
import { DeleteComponentConfigRequest } from '../../jsonrpc/request/deleteComponentConfigRequest';
import { EdgeRpcRequest } from '../../jsonrpc/request/edgeRpcRequest';
import { GetChannelRequest } from '../../jsonrpc/request/getChannelRequest';
import { GetChannelsOfComponentRequest } from '../../jsonrpc/request/getChannelsOfComponentRequest';
import { GetEdgeConfigRequest } from '../../jsonrpc/request/getEdgeConfigRequest';
import { GetPropertiesOfFactoryRequest } from '../../jsonrpc/request/getPropertiesOfFactoryRequest';
import { SubscribeChannelsRequest } from '../../jsonrpc/request/subscribeChannelsRequest';
import { SubscribeSystemLogRequest } from '../../jsonrpc/request/subscribeSystemLogRequest';
import { UpdateComponentConfigRequest } from '../../jsonrpc/request/updateComponentConfigRequest';
import { GetChannelResponse } from '../../jsonrpc/response/getChannelResponse';
import { Channel, GetChannelsOfComponentResponse } from '../../jsonrpc/response/getChannelsOfComponentResponse';
import { GetEdgeConfigResponse } from '../../jsonrpc/response/getEdgeConfigResponse';
import { GetPropertiesOfFactoryResponse } from '../../jsonrpc/response/getPropertiesOfFactoryResponse';
import { ArrayUtils } from '../../service/arrayutils';
import { ChannelAddress, SystemLog, Websocket, EdgePermission } from '../../shared';
import { Role } from '../../type/role';

export class Edge {

  constructor(
    public readonly id: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly version: string,
    public readonly role: Role,
    public isOnline: boolean,
    public readonly lastmessage: Date,
    public readonly sumState: SumState,
    public readonly firstSetupProtocol: Date,
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
   * Gets the first valid Config. If not available yet, it requests it via Websocket.
   *
   * @param websocket the Websocket connection
   */
  public getFirstValidConfig(websocket: Websocket): Promise<EdgeConfig> {
    return this.getConfig(websocket)
      .pipe(filter(config => config != null && config.isValid()),
        first())
      .toPromise();
  }

  /**
   * Gets a channel either from {@link EdgeConfig edgeconfig} or requests it from the edge.
   *
   * @param websocket the websocket to send a request if the
   *                  channel is not included in the edgeconfig
   * @param channel   the address of the channel to get
   * @returns a promise of the found channel
   */
  public async getChannel(websocket: Websocket, channel: ChannelAddress): Promise<Channel> {
    if (EdgePermission.hasChannelsInEdgeConfig(this)) {
      const config = await this.getFirstValidConfig(websocket);
      const foundChannel = config.getChannel(channel);
      if (!foundChannel) {
        throw new Error("Channel not found: " + channel);
      }
      return { id: channel.channelId, ...foundChannel };
    }

    const response = await this.sendRequest<GetChannelResponse>(websocket, new ComponentJsonApiRequest({
      componentId: '_componentManager',
      payload: new GetChannelRequest({
        componentId: channel.componentId,
        channelId: channel.channelId,
      }),
    }));

    return response.result.channel;
  }

  /**
   * Gets all channels of the component with the provided component id.
   *
   * @param websocket   the websocket to send a request if the
   *                    channels are not included in the edgeconfig
   * @param componentId the id of the component
   * @returns a promise with the reuslt channels
   */
  public async getChannels(websocket: Websocket, componentId: string): Promise<Channel[]> {
    if (EdgePermission.hasChannelsInEdgeConfig(this)) {
      const config = await this.getFirstValidConfig(websocket);
      const component = config.components[componentId];
      if (!component) {
        throw new Error('Component not found');
      }
      return Object.entries(component.channels).reduce((p, c) => {
        return [...p, { id: c[0], ...c[1] }];
      }, []);
    }

    const response = await this.sendRequest<GetChannelsOfComponentResponse>(websocket, new ComponentJsonApiRequest({
      componentId: '_componentManager',
      payload: new GetChannelsOfComponentRequest({ componentId: componentId }),
    }));

    return response.result.channels;
  }

  public async getFactoryProperties(websocket: Websocket, factoryId: string): Promise<[EdgeConfig.Factory, EdgeConfig.FactoryProperty[]]> {
    if (EdgePermission.hasReducedFactories(this)) {
      const response = await this.sendRequest<GetPropertiesOfFactoryResponse>(websocket, new ComponentJsonApiRequest({
        componentId: '_componentManager',
        payload: new GetPropertiesOfFactoryRequest({ factoryId }),
      }));
      return [response.result.factory, response.result.properties];
    }

    const factory = (await this.getFirstValidConfig(websocket)).factories[factoryId];
    return [factory, factory.properties];
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

    const request = new GetEdgeConfigRequest();
    this.sendRequest(websocket, request).then(response => {
      const edgeConfigResponse = response as GetEdgeConfigResponse;
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
   * @deprecated Use `unsubscribeFromChannels` instead.
   *
   * @todo should be removed
   */
  public unsubscribeChannels(websocket: Websocket, id: string): void {
    delete this.subscribedChannels[id];
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Removes all Channels from subscription
   *
   * @param websocket the Websocket
   */
  public unsubscribeAllChannels(websocket: Websocket) {
    this.subscribedChannels = {};
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Unsubscribes from passed channels
   *
   * @param websocket the Websocket
   * @param channels the channels
   *
   * @todo should be renamed to `unsubscribeChannels` after unsubscribeChannels is removed
   */
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
        const channels: ChannelAddress[] = [];
        for (const componentId in this.subscribedChannels) {
          channels.push(...this.subscribedChannels[componentId]);
        }
        const request = new SubscribeChannelsRequest(channels);
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
    const request = new CreateComponentConfigRequest({ factoryPid: factoryPid, properties: properties });
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
    const request = new UpdateComponentConfigRequest({ componentId: componentId, properties: properties });
    return this.sendRequest(ws, request);
  }

  /**
   * Deletes the configuration of a OpenEMS Edge Component.
   *
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID
   */
  public deleteComponentConfig(ws: Websocket, componentId: string): Promise<JsonrpcResponseSuccess> {
    const request = new DeleteComponentConfigRequest({ componentId: componentId });
    return this.sendRequest(ws, request);
  }

  /**
   * Sends a JSON-RPC Request. The Request is wrapped in a EdgeRpcRequest.
   *
   * @param ws               the Websocket
   * @param request          the JSON-RPC Request
   * @param responseCallback the JSON-RPC Response callback
   */
  public sendRequest<T = JsonrpcResponseSuccess>(ws: Websocket, request: JsonrpcRequest): Promise<T> {
    const wrap = new EdgeRpcRequest({ edgeId: this.id, payload: request });
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
   * Determines if the version of the edge is a SNAPSHOT.
   *
   * <p>
   * Version strings are built like `major.minor.patch-branch.date.hash`. So any version string that contains a hyphen is a SNAPSHOT.
   *
   * @returns true if the version of the edge is a SNAPSHOT
   */
  public isSnapshot(): boolean {
    return this.version.includes("-");
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
