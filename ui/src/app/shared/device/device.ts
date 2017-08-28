import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { UUID } from 'angular2-uuid';
import * as moment from 'moment';

import { Notification, Websocket } from '../shared';
import { Config, ChannelAddresses } from './config';
import { Data, ChannelData, Summary } from './data';
import { DefaultMessages } from '../service/defaultmessages';
import { DefaultTypes } from '../service/defaulttypes';
import { Utils } from '../service/utils';
import { Role, ROLES } from '../type/role';

export { Data, ChannelData, Summary, Config, ChannelAddresses };

export class Log {
  timestamp: number;
  time: string = "";
  level: string;
  color: string = "black";
  source: string;
  message: string;
}

export class QueryReply {
  requestId: string;
  data: [{
    time: string
    channels: ChannelData
  }]
}

export class Device {
  public config: BehaviorSubject<DefaultTypes.Config> = new BehaviorSubject<DefaultTypes.Config>(null);

  public event = new Subject<Notification>();
  public address: string;
  public log = new Subject<Log>();

  //public historykWh = new BehaviorSubject<any[]>(null);
  private state: 'active' | 'inactive' | 'test' | 'installed-on-stock' | '' = '';
  private ngUnsubscribeQueryReply: Subject<void> = new Subject<void>();
  private currentData = new BehaviorSubject<Data>(null);
  private ngUnsubscribeCurrentData: Subject<void> = new Subject<void>();

  constructor(
    public readonly name: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly role: Role,
    public readonly online: boolean,
    private replyStream: Subject<DefaultMessages.Reply>,
    private websocket: Websocket
  ) { }

  /**
   * Returns a promise for a config. If a config is availabe, returns immediately. 
   * Otherwise queries backend. Returns anyway after a timeout.
   */
  public getConfig(): Promise<DefaultTypes.Config> {
    let configPromise: Promise<DefaultTypes.Config>;
    if (this.configQueryIsCurrentlyRunning) {
      // debounce calls to this method
      let ngUnsubscribeWaitForConfig = new Subject<any>();
      this.config.takeUntil(ngUnsubscribeWaitForConfig).subscribe(config => {
        configPromise = Promise.resolve(config);
      });

    } else if (this.config.getValue() != null) {
      // config is immediately available
      configPromise = Promise.resolve(this.config.getValue());

    } else {
      // debounce calls to this method
      this.configQueryIsCurrentlyRunning = true;
      // query new config with timeout
      configPromise = Utils.timeoutPromise(Websocket.TIMEOUT, new Promise<DefaultTypes.Config>((resolve, reject) => {
        let message = DefaultMessages.configQuery();
        this.send(message);
        // wait for answer
        let ngUnsubscribeQueryReply = new Subject<any>();
        this.replyStream.takeUntil(ngUnsubscribeQueryReply).subscribe(reply => {
          if (reply.id.pop() == message.id.pop()) {
            // stop waiting for an answer
            ngUnsubscribeQueryReply.next();
            ngUnsubscribeQueryReply.complete();
            // clean datatype
            delete reply.id;
            // set local config and  resolve Promise
            let config = (<DefaultMessages.ConfigQueryReply>reply).config;
            this.config.next(config);
            resolve(config);
            // debounce
            this.configQueryIsCurrentlyRunning = false;
          }
        })
      }));
      // empty config on error
      configPromise.catch(reason => {
        this.config.next(null);
      })
    }
    return configPromise;
  }
  private configQueryIsCurrentlyRunning: boolean = false;

  /**
   * Sends a message to websocket
   */
  public send(value: any): void {
    this.websocket.send(this, value);
  }

  /**
   * Subscribe to specified channels
   */
  public subscribeCurrentData(channels: ChannelAddresses): Subject<Data> {
    // send subscribe
    this.send({
      subscribe: {
        channels: channels
      }
    });
    // prepare result
    let result = new Subject<Data>();
    let gotData: boolean = false;
    // timeout after 10 seconds
    setTimeout(() => {
      if (!gotData) {
        result.error("CurrentData timeout");
        result.complete();
      }
    }, 10000);
    // wait for current data
    this.currentData.takeUntil(this.ngUnsubscribeCurrentData).subscribe(currentData => {
      gotData = true;
      result.next(currentData);
    });
    return result;
  }

  /**
   * Unsubscribe from channels
   */
  public unsubscribeCurrentData() {
    if (!this.ngUnsubscribeCurrentData.isStopped) {
      this.ngUnsubscribeCurrentData.next();
      this.ngUnsubscribeCurrentData.complete();
    }
    this.send({
      subscribe: {
        channels: {}
      }
    });
  }

  /**
   * Subscribe to log
   */
  public subscribeLog(key: "all" | "info" | "warning" | "error") {
    this.send({
      subscribe: {
        log: key
      }
    });
  }

  /**
   * Unsubscribe from channels
   */
  public unsubscribeLog() {
    this.send({
      subscribe: {
        log: ""
      }
    });
  }

  /**
   * Send "query" message to websocket
   */
  // TODO: kWh: this.getkWhResult(this.getImportantChannels())
  public query(fromDate: moment.Moment, toDate: moment.Moment, channels: ChannelAddresses): Subject<QueryReply> {
    // create query object
    let obj = {
      mode: "history",
      fromDate: fromDate.format("YYYY-MM-DD"),
      toDate: toDate.format("YYYY-MM-DD"),
      timezone: new Date().getTimezoneOffset() * 60,
      channels: channels
    };
    // send query and receive requestId
    let requestId = this.send({ query: obj });
    // prepare result
    let ngUnsubscribe: Subject<void> = new Subject<void>();
    let result = new Subject<QueryReply>();
    // timeout after 10 seconds
    setTimeout(() => {
      result.error("Query timeout");
      result.complete();
    }, 10000);
    // wait for queryreply with this requestId
    this.replyStream.takeUntil(ngUnsubscribe).subscribe(queryreply => {
      // if (queryreply.requestId == requestId) {
      //   ngUnsubscribe.next();
      //   ngUnsubscribe.complete();
      //   result.next(queryreply);
      //   result.complete();
      // }
    });
    return result;
  }

  /*
   * currentdata
   */
  // if ("currentdata" in message) {
  //   let data: Data = new Data(<ChannelData>message.currentdata, this.config.getValue());
  //   this.currentData.next(data);
  // }

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