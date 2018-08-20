import { Subject, BehaviorSubject, ReplaySubject, Observer, Observable } from 'rxjs';
import { first, map, combineLatest } from 'rxjs/operators';
import { cmp } from 'semver-compare-multi';

import { Websocket } from '../shared';
import { ConfigImpl } from './config';
import { CurrentDataAndSummary } from './currentdata';
import { CurrentDataAndSummary_2018_7 } from './currentdata.2018.7';
import { DefaultMessages } from '../service/defaultmessages';
import { DefaultTypes } from '../service/defaulttypes';
import { Role } from '../type/role';
import { ConfigImpl_2018_8 } from './config.2018.8';
import { ConfigImpl_2018_7 } from './config.2018.7';
import { CurrentDataAndSummary_2018_8 } from './currentdata.2018.8';

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
    public readonly edgeId: number,
    public readonly name: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly version: string,
    public readonly role: Role,
    public online: boolean,
    private replyStreams: { [messageId: string]: Subject<DefaultMessages.Reply> },
    private websocket: Websocket
  ) {
    // prepare stream/obersable for log
    let logStream = replyStreams["log"] = new Subject<DefaultMessages.LogReply>();
    this.log = logStream
      .pipe(map(message => message.log));
  }

  // holds current data
  public currentData: Observable<CurrentDataAndSummary>;

  // holds log
  public log: Observable<DefaultTypes.Log>;

  // holds config
  public config: BehaviorSubject<ConfigImpl> = new BehaviorSubject<ConfigImpl>(null);

  public event = new Subject<Notification>();
  public address: string;

  //public historykWh = new BehaviorSubject<any[]>(null);
  private state: 'active' | 'inactive' | 'test' | 'installed-on-stock' | '' = '';
  private subscribeCurrentDataChannels: DefaultTypes.ChannelAddresses = {};

  /*
   * Called by websocket, when this edge is set as currentEdge
   */
  public markAsCurrentEdge() {
    if (this.config.getValue() == null) {
      this.refreshConfig();
    }
  }

  /*
   * Refresh the config
   */
  public refreshConfig(): BehaviorSubject<ConfigImpl> {
    // TODO use sendMessageWithReply()
    let message = DefaultMessages.configQuery(this.edgeId);
    let messageId = message.messageId.ui;
    this.replyStreams[messageId] = new Subject<DefaultMessages.Reply>();
    this.send(message);
    // wait for reply
    this.replyStreams[messageId].pipe(first()).subscribe(reply => {
      let config = (<DefaultMessages.ConfigQueryReply>reply).config;
      let configImpl
      if (this.isVersionAtLeast('2018.8')) {
        configImpl = new ConfigImpl_2018_8(this, config);
      } else {
        configImpl = new ConfigImpl_2018_7(this, config);
      }
      this.config.next(configImpl);
      this.replyStreams[messageId].unsubscribe();
      delete this.replyStreams[messageId];
    });
    // TODO add timeout
    return this.config;
  }

  /**
   * Sends a message to websocket
   */
  public send(value: any): void {
    this.websocket.send(value);
  }

  private sendMessageWithReply(message: DefaultTypes.IdentifiedMessage): Subject<DefaultMessages.Reply> {
    let messageId: string = message.messageId.ui;
    this.replyStreams[messageId] = new Subject<DefaultMessages.Reply>();
    this.send(message);
    return this.replyStreams[messageId];
  }

  private removeReplyStream(reply: DefaultMessages.Reply) {
    let messageId: string = reply.messageId.ui;
    this.replyStreams[messageId].unsubscribe();
    delete this.replyStreams[messageId];
  }

  /**
   * Subscribe to current data of specified channels
   */
  public subscribeCurrentData(channels: DefaultTypes.ChannelAddresses): Observable<CurrentDataAndSummary> {
    this.subscribeCurrentDataChannels = channels;
    let replyStream = this.sendMessageWithReply(DefaultMessages.currentDataSubscribe(this.edgeId, channels));
    let obs = replyStream
      .pipe(map(message => (message as DefaultMessages.CurrentDataReply).currentData),
        combineLatest(this.config, (currentData, config) => {
          if (this.isVersionAtLeast('2018.8')) {
            return new CurrentDataAndSummary_2018_8(this, currentData, <ConfigImpl_2018_8>config);
          } else {
            return new CurrentDataAndSummary_2018_7(this, currentData, <ConfigImpl_2018_7>config);
          }
        }));
    // TODO send "unsubscribe" to websocket when nobody is subscribed on this observable anymore
    return obs;
  }

  /**
   * Unsubscribe from current data
   */
  public unsubscribeCurrentData() {
    this.subscribeCurrentData({});
  }

  /**
   * Query data
   */
  // TODO: kWh: this.getkWhResult(this.getImportantChannels())
  public historicDataQuery(fromDate: Date, toDate: Date, channels: DefaultTypes.ChannelAddresses): Promise<DefaultTypes.HistoricData> {
    let timezone = new Date().getTimezoneOffset() * 60;
    let replyStream = this.sendMessageWithReply(DefaultMessages.historicDataQuery(this.edgeId, fromDate, toDate, timezone, channels));
    // wait for reply
    return new Promise((resolve, reject) => {
      replyStream.pipe(first()).subscribe(reply => {
        let historicData = (reply as DefaultMessages.HistoricDataReply).historicData;
        this.removeReplyStream(reply);
        resolve(historicData);
      });
    })
  }

  /**
   * Mark this edge as online or offline
   * @param online 
   */
  public setOnline(online: boolean) {
    this.online = online;
  }

  /**
   * Subscribe to log
   */
  public subscribeLog(): { messageId: string, logs: Observable<DefaultTypes.Log> } {
    const message = DefaultMessages.logSubscribe(this.edgeId);
    let replyStream = this.sendMessageWithReply(message);
    return { messageId: message.messageId.ui, logs: replyStream.pipe(map(message => message.log as DefaultTypes.Log)) };
  }

  /**
   * Unsubscribe from log
   */
  public unsubscribeLog(messageId: string) {
    let message = DefaultMessages.logUnsubscribe(messageId, this.edgeId);
    this.send(message);
  }

  /**
   * System Execute
   */
  public systemExecute(password: string, command: string, background: boolean, timeout: number): Promise<string> {
    let replyStream = this.sendMessageWithReply(DefaultMessages.systemExecute(this.edgeId, password, command, background, timeout));
    // wait for reply
    return new Promise((resolve, reject) => {
      replyStream.pipe(first()).subscribe(reply => {
        let output = (reply as DefaultMessages.SystemExecuteReply).system.output;
        this.removeReplyStream(reply);
        resolve(output);
      });
    })
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