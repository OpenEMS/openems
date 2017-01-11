import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { Notification } from './webapp.service';
import { Websocket } from './websocket.service';

const SUBSCRIBE: string = "fenecon_monitor_v1";

export class Device {

  public event = new Subject<Notification>();
  public address: string;
  public data = new BehaviorSubject<{ [thing: string]: any }>(null);
  private config: {
    _availableControllers: [{
      channels: [{ name }],
      class: string,
      text: string,
      title: string
    }],
    _deviceNatures: { [thing: string]: string[] },
    persistence: [{ class: string }],
    scheduler: {
      id: string,
      class: string,
      controllers: [{
        id: string,
        class: string
      }]
    },
    things: [{
      id: string,
      class: string,
      devices: [{
        id: string,
        class: string
      }]
    }]
  };
  private influxdb: {
    ip: string,
    username: string,
    password: string,
    fems: string
  }

  constructor(
    public name: string,
    public websocket: Websocket
  ) {
    if (this.name == 'fems') {
      this.address = this.websocket.name;
    } else {
      this.address = this.websocket.name + ": " + this.name;
    }
  }

  public send(value: any) {
    this.websocket.send(this, value);
  }

  /**
   * Send "subscribe" message to websocket
   */
  public subscribe() {
    this.send({
      subscribe: SUBSCRIBE
    });
  }

  /**
   * Send "unsubscribe" message to websocket
   */
  public unsubscribe() {
    this.send({
      subscribe: ""
    });
  }

  /**
   * Receive new data from websocket
   */
  public receive(message: any) {
    /*
     * config
     */
    if ("config" in message) {
      // store all config
      this.config = message.config;
      // parse influxdb connection
      if ("persistence" in this.config) {
        for (let persistence of this.config.persistence) {
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
    }

    /*
     * data
     */
    if ("data" in message) {
      this.data.next(message.data);
    }
  }
}