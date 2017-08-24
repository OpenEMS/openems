import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { UUID } from 'angular2-uuid';
import * as moment from 'moment';

import { Notification, Websocket } from '../shared';
import { Config, ChannelAddresses } from './config';
import { Data, ChannelData, Summary } from './data';
import { DefaultMessages } from '../service/defaultmessages';
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
  public event = new Subject<Notification>();
  public address: string;
  public log = new Subject<Log>();

  //public historykWh = new BehaviorSubject<any[]>(null);
  private config = new BehaviorSubject<Config>(null);
  private state: 'active' | 'inactive' | 'test' | 'installed-on-stock' | '' = '';
  private queryreply = new Subject<QueryReply>();
  private ngUnsubscribeQueryReply: Subject<void> = new Subject<void>();
  private currentData = new BehaviorSubject<Data>(null);
  private ngUnsubscribeCurrentData: Subject<void> = new Subject<void>();

  private influxdb: {
    ip: string,
    username: string,
    password: string,
    fems: string
  }

  constructor(
    public readonly name: string,
    public readonly comment: string,
    public readonly producttype: string,
    public readonly role: Role,
    public readonly online: boolean,
    private websocket: Websocket
  ) { }

  /**
   * Returns a promise for a config. If a config is availabe, returns immediately. Otherwise queries backend.
   */
  public getConfig(): Promise<Config> {
    let currentConfig = this.config.getValue();
    if (currentConfig != null) {
      // config is available
      return Promise.resolve(currentConfig);
    } else {
      // query new config
      return this.timeoutPromise(1000, new Promise((resolve, reject) => {
        let message = DefaultMessages.refreshConfig();
        this.send(message);
        this.queryreply.takeUntil(this.ngUnsubscribeQueryReply).subscribe(queryreply => {
          console.log(queryreply);
          if (queryreply.requestId == message.id.pop()) {
            console.log(queryreply);
            resolve();
          }
        })
      }));
    }
  }

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
    this.queryreply.takeUntil(ngUnsubscribe).subscribe(queryreply => {
      // if (queryreply.requestId == requestId) {
      //   ngUnsubscribe.next();
      //   ngUnsubscribe.complete();
      //   result.next(queryreply);
      //   result.complete();
      // }
    });
    return result;
  }

  /**
   * Receive new data from websocket
   */
  public receive(message: any) {
    if ("metadata" in message) {
      let metadata = message.metadata;
      /*
       * config
       */
      if ("config" in metadata) {
        let config: Config = new Config(metadata.config);

        // parse influxdb connection
        if ("persistence" in config) {
          for (let persistence of config.persistence) {
            if (persistence.class == "io.openems.impl.persistence.influxdb.InfluxdbPersistence" &&
              "ip" in persistence && "username" in persistence && "password" in persistence && "fems" in persistence) {
              let ip = persistence["ip"];
              if (ip == "127.0.0.1" || ip == "localhost") { // rewrite localhost to remote ip
                ip = location.hostname;
              }
              this.influxdb = {
                ip: ip,
                username: persistence["username"],
                password: persistence["password"],
                fems: persistence["fems"]
              }
            }
          }
        }
        // store all config
        this.config.next(config);
        //TODO this.things = this.refreshThingsFromConfig();
      }

      // TODO
      // if ("online" in metadata) {
      //   this.online = metadata.online;
      // } else {
      //   this.online = true;
      // }

      if ("state" in metadata) {
        this.state = metadata.state;
      }
    }

    /*
     * currentdata
     */
    if ("currentdata" in message) {
      let data: Data = new Data(<ChannelData>message.currentdata, this.config.getValue());
      this.currentData.next(data);
    }

    /*
     * log
     */
    if ("log" in message) {
      let log = message.log;
      this.log.next(log);
    }

    /*
     * Reply to a query
     */
    if ("queryreply" in message) {
      if (!("requestId" in message)) {
        throw ("No requestId in message: " + message);
      }
      message.queryreply["requestId"] = message.requestId;
      this.queryreply.next(message.queryreply);

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
    }
  }


  /**
   * Promise with timeout
   * Source: https://italonascimento.github.io/applying-a-timeout-to-your-promises/
   */
  private timeoutPromise = function (ms, promise) {
    // Create a promise that rejects in <ms> milliseconds
    let timeout = new Promise((resolve, reject) => {
      let id = setTimeout(() => {
        clearTimeout(id);
        reject('Timed out in ' + ms + 'ms.')
      }, ms)
    })

    // Returns a race between our timeout and the passed in promise
    return Promise.race([
      promise,
      timeout
    ])
  }
}