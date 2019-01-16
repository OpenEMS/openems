import { BehaviorSubject, Subject } from 'rxjs';
import { cmp } from 'semver-compare-multi';
import { JsonrpcRequest, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { EdgeRpcRequest } from '../jsonrpc/request/edgeRpcRequest';
import { GetEdgeConfigRequest } from '../jsonrpc/request/getEdgeConfigRequest';
import { SubscribeChannelsRequest } from '../jsonrpc/request/subscribeChannelsRequest';
import { UpdateComponentConfigRequest } from '../jsonrpc/request/updateComponentConfigRequest';
import { GetEdgeConfigResponse } from '../jsonrpc/response/getEdgeConfigResponse';
import { Websocket } from '../service/websocket';
import { ChannelAddress } from '../type/channeladdress';
import { Role } from '../type/role';
import { CurrentData } from './currentdata';
import { EdgeConfig } from './edgeconfig';

export class Log {
  timestamp: number;
  time: string = "";
  level: string;
  color: string = "black";
  source: string;
  message: string;
}

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
  public markAsCurrentEdge(websocket: Websocket) {
    if (this.config.value == null) {
      this.refreshConfig(websocket);
    }
  }

  /**
   * Refresh the config.
   */
  public refreshConfig(websocket: Websocket) {
    let request = new GetEdgeConfigRequest();
    this.sendRequest(websocket, request).then(response => {
      this.config.next(new EdgeConfig(response as GetEdgeConfigResponse));
    }).catch(reason => {
      console.log("refreshConfig got error", reason)
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
  public subscribeChannels(websocket: Websocket, id: string, channels: ChannelAddress[]) {
    this.subscribedChannels[id] = channels;
    this.sendSubscribeChannels(websocket);
  }

  /**
   * Removes Channels from subscription
   * 
   * @param ws the Websocket
   * @param id the unique ID for this subscription
   */
  public unsubscribeChannels(ws: Websocket, id: string) {
    delete this.subscribedChannels[id];
    this.sendSubscribeChannels(ws);
  }

  /**
   * Sends a SubscribeChannelsRequest for all Channels in 'this.subscribedChannels'
   * 
   * @param ws the Websocket
   */
  private sendSubscribeChannels(ws: Websocket) {
    // merge channels from currentDataSubscribes
    let channels: ChannelAddress[] = [];
    for (let componentId in this.subscribedChannels) {
      channels.push.apply(channels, this.subscribedChannels[componentId]);
    }
    let request = new SubscribeChannelsRequest(channels);
    this.sendRequest(ws, request); // ignore Response
  }

  /**
   * Handles a CurrentDataNotification
   */
  public handleCurrentDataNotification(message: CurrentDataNotification): void {
    this.currentData.next(new CurrentData(message.params));
  }

  /**
   * Updates the configuration of a OpenEMS Edge Component.
   * 
   * @param ws          the Websocket
   * @param componentId the OpenEMS Edge Component-ID 
   * @param update      the attributes to be updated.
   */
  public updateComponentConfig(ws: Websocket, componentId: string, update: [{ property: string, value: any }]): Promise<JsonrpcResponseSuccess> {
    let request = new UpdateComponentConfigRequest(componentId, update);
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
  public setOnline(isOnline: boolean) {
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
}