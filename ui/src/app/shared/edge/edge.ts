import { Subject, BehaviorSubject, Observable } from 'rxjs';
import { cmp } from 'semver-compare-multi';

import { CurrentData } from './currentdata';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';
import { SubscribeChannelsRequest } from '../service/jsonrpc/request/subscribeChannelsRequest';
import { JsonrpcRequest, JsonrpcResponseSuccess } from '../service/jsonrpc/base';
import { EdgeRpcRequest } from '../service/jsonrpc/request/edgeRpcRequest';
import { ChannelAddress } from '../type/channeladdress';
import { Websocket } from '../service/websocket';
import { GetEdgeConfigRequest } from '../service/jsonrpc/request/getEdgeConfigRequest';
import { GetEdgeConfigResponse } from '../service/jsonrpc/response/getEdgeConfigResponse';
import { EdgeConfig } from './edgeconfig';
import { CurrentDataNotification } from '../service/jsonrpc/notification/currentDataNotification';

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

  // holds log
  public log: Observable<DefaultTypes.Log>;

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

  public event = new Subject<Notification>();
  public address: string;

  //public historykWh = new BehaviorSubject<any[]>(null);
  private state: 'active' | 'inactive' | 'test' | 'installed-on-stock' | '' = '';
  private subscribeCurrentDataChannels: string[] = [];

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
   * Sends a message to websocket
   * 
   * TODO deprecated
   */
  public send(value: any): void {
    console.warn("Edge.send()", value);
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
   * Sends a JSON-RPC Request, wrapped in a EdgeRpcRequest with the Edge-ID.
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
    })
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
   * Subscribe to log
   */
  public subscribeLog(): void {
    console.warn("Edge.subscribeLog()");
    // const message = DefaultMessages.logSubscribe(this.edgeId);
    // let replyStream = this.sendMessageWithReply(message);
    // return { messageId: message.messageId.ui, logs: replyStream.pipe(map(message => message.log as DefaultTypes.Log)) };
  }

  /**
   * Unsubscribe from log
   */
  public unsubscribeLog(messageId: string) {
    console.warn("Edge.unsubscribeLog()");
    // let message = DefaultMessages.logUnsubscribe(messageId, this.edgeId);
    // this.send(message);
  }

  /**
   * System Execute
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