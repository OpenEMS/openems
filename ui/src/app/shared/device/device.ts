import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { ReplaySubject } from 'rxjs/ReplaySubject';
import { Observer } from 'rxjs/Observer';
import { Observable } from 'rxjs/Observable';
import { UUID } from 'angular2-uuid';
import 'rxjs/add/operator/combineLatest';

import { Websocket } from '../shared';
import { ConfigImpl } from './config';
import { CurrentDataAndSummary } from './currentdata';
import { DefaultMessages } from '../service/defaultmessages';
import { DefaultTypes } from '../service/defaulttypes';
import { Utils } from '../service/utils';
import { Role } from '../type/role';

export class Log {
  timestamp: number;
  time: string = "";
  level: string;
  color: string = "black";
  source: string;
  message: string;
}

export class Device {

  constructor(
    public readonly edgeId: number,
    public readonly name: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly role: Role,
    public online: boolean,
    private replyStreams: { [messageId: string]: Subject<DefaultMessages.Reply> },
    private websocket: Websocket
  ) {
    // prepare stream/obersable for log
    let logStream = replyStreams["log"] = new Subject<DefaultMessages.LogReply>();
    this.log = logStream
      .map(message => message.log);
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
   * Called by websocket, when this device is set as currentDevice
   */
  public markAsCurrentDevice() {
    if (this.config.getValue() == null) {
      this.refreshConfig();
    }
  }

  /*
   * Refresh the config
   */
  public refreshConfig(): BehaviorSubject<ConfigImpl> {
    let message = DefaultMessages.configQuery(this.edgeId);
    let messageId = message.messageId.ui;
    this.replyStreams[messageId] = new Subject<DefaultMessages.Reply>();
    this.send(message);
    // wait for reply
    this.replyStreams[messageId].first().subscribe(reply => {
      let config = (<DefaultMessages.ConfigQueryReply>reply).config;
      let configImpl = new ConfigImpl(config)
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
      .map(message => (message as DefaultMessages.CurrentDataReply).currentData)
      .combineLatest(this.config, (currentData, config) => new CurrentDataAndSummary(currentData, config));
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
      replyStream.first().subscribe(reply => {
        let historicData = (<DefaultMessages.HistoricDataReply>reply).historicData;
        this.removeReplyStream(reply);
        resolve(historicData);
      });
    })
  }

  /**
   * Mark this device as online or offline
   * @param online 
   */
  public setOnline(online: boolean) {
    this.online = online;
  }

  /**
   * Subscribe to log
   */
  public subscribeLog(): Observable<DefaultTypes.Log> {
    let message = DefaultMessages.logSubscribe();
    this.send(message);
    return this.log;
  }

  /**
   * Unsubscribe from log
   */
  public unsubscribeLog() {
    let message = DefaultMessages.logUnsubscribe();
    this.send(message);
  }

  /**
   * System Execute
   */
  public systemExecute(password: string, command: string, background: boolean, timeout: number): Promise<string> {
    let message = DefaultMessages.systemExecute(password, command, background, timeout);
    let messageId = message.id[0];
    this.replyStreams[messageId] = new Subject<DefaultMessages.Reply>();
    this.send(message);
    // wait for reply
    return new Promise((resolve, reject) => {
      this.replyStreams[messageId].first().subscribe(reply => {
        let output = (<DefaultMessages.SystemExecuteReply>reply).system.output;
        this.replyStreams[messageId].unsubscribe();
        delete this.replyStreams[messageId];
        resolve(output);
      });
    })
  }

  /*
   * log
   */
  // if ("log" in message) {
  //   let log = message.log;
  //   this.log.next(log);
  // }


  //let kWh = null;
  // history data
  // if (message.queryreply != null) {
  //   if ("data" in message.queryreply && message.queryreply.data != null) {
  //     data = message.queryreply.data;
  //     for (let datum of data) {
  //       let sum = this.calculateSummary(datum.channels);
  //       datum["summary"] = sum;
  //     }
  //   }
  //   // kWh data
  //   if ("kWh" in message.queryreply) {
  //     kWh = message.queryreply.kWh;
  //   }
  // }
  //this.historykWh.next(kWh);
  // }
}