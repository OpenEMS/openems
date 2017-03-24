import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import * as moment from 'moment';

import { Notification } from './service/webapp.service';
import { Websocket } from './service/websocket.service';
import { Config } from './config';

const SUBSCRIBE: string = "fenecon_monitor_v1";

class Summary {
  public storage = {
    things: {},
    soc: null,
    activePower: 0,
    maxActivePower: 0
  };
  public production = {
    things: {},
    powerRatio: 0,
    activePower: 0,
    maxActivePower: 0
  };
  public grid = {
    things: {},
    powerRatio: 0,
    activePower: 0,
    maxActivePower: 0
  };
  public consumption = {
    powerRatio: 0,
    activePower: 0
  };
}

export class Device {

  public event = new Subject<Notification>();
  public address: string;
  public data = new BehaviorSubject<{ [thing: string]: any }>(null);
  public socData = new BehaviorSubject<any[]>(null);
  public config = new BehaviorSubject<Config>(null);
  private online = false;
  private influxdb: {
    ip: string,
    username: string,
    password: string,
    fems: string
  }

  private summary: Summary = new Summary();

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
    let isInArray = (array: any, value: any): boolean => {
      return array.indexOf(value) > -1;
    }
    let subscribe = {}
    let natures = this.config.getValue()._meta.natures;
    let ignoreNatures = {};
    this.summary = new Summary();
    for (let thing in natures) {
      let a = natures[thing]["implements"];
      let channels = []

      /*
       * Find important Channels to subscribe
       */
      // Ess
      if (isInArray(a, "EssNature")) {
        channels.push("Soc");
        this.summary.storage.things[thing] = true;
      }
      if (isInArray(a, "AsymmetricEssNature")) {
        channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
      }
      if (isInArray(a, "SymmetricEssNature")) {
        channels.push("ActivePower", "ReactivePower");
      }
      if (isInArray(a, "FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
        ignoreNatures["AsymmetricMeterNature"] = true;
      }

      // Meter
      if (isInArray(a, "MeterNature")) {
        // get type
        let type = natures[thing]["channels"]["type"]["value"];
        if (type === "grid") {
          this.summary.grid.things[thing] = true;
        } else if (type === "production") {
          this.summary.production.things[thing] = true;
        } else {
          console.warn("Meter without type: " + thing);
        }
      }
      if (isInArray(a, "AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
        channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
      }
      if (isInArray(a, "SymmetricMeterNature")) {
        channels.push("ActivePower", "ReactivePower");
      }

      subscribe[thing] = channels;
    }

    this.send({
      subscribe: subscribe
    });
  }

  /**
   * Send "unsubscribe" message to websocket
   */
  public unsubscribe() {
    this.send({
      subscribe: {}
    });
  }

  /**
   * Send "query" message to websocket
   */
  public query(fromDate: any, toDate: any, channels: { [thing: string]: string[] }) {
    let offset = new Date().getTimezoneOffset();
    offset = offset * 60;

    let obj = {
      mode: "history",
      fromDate: fromDate.format("YYYY-MM-DD"),
      toDate: toDate.format("YYYY-MM-DD"),
      timezone: offset,
      channels: channels
    };
    console.log(obj);
    this.send({ query: obj });
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
        let config = metadata.config;

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
      }

      if ("online" in metadata) {
        this.online = metadata.online;
      } else {
        this.online = true;
      }
    }

    /*
     * data
     */
    if ("currentdata" in message) {
      let data = message.currentdata;

      {
        /*
         * Storage
         */
        let soc = 0;
        let activePower = 0;
        for (let thing in this.summary.storage.things) {
          if (thing in data) {
            let ess = data[thing];
            soc += ess["Soc"];
            activePower += ess["ActivePower"];
          }
        }
        this.summary.storage.soc = soc / Object.keys(this.summary.storage.things).length;
        this.summary.storage.activePower = activePower;
      }

      {
        /*
         * Grid
         */
        let powerRatio = 0;
        let activePower = 0;
        let maxActivePower = 0;
        for (let thing in this.summary.grid.things) {
          if (thing in data) {
            let thingChannels = this.config.getValue()._meta.natures[thing]["channels"];
            let meter = data[thing];
            let power = meter["ActivePower"];
            if (activePower > 0) {
              powerRatio = (power * 50.) / thingChannels["maxActivePower"]["value"]
            } else {
              powerRatio = (power * -50.) / thingChannels["minActivePower"]["value"]
            }
            activePower += power;
            maxActivePower += thingChannels["maxActivePower"]["value"];
            // + meter["ActivePowerL1"] + meter["ActivePowerL2"] + meter["ActivePowerL3"];
          }
        }
        this.summary.grid.powerRatio = powerRatio;
        this.summary.grid.activePower = activePower;
        this.summary.grid.maxActivePower = maxActivePower;
      }

      {
        /*
         * Production
         */
        let powerRatio = 0;
        let activePower = 0;
        let maxActivePower = 0;
        for (let thing in this.summary.production.things) {
          if (thing in data) {
            let thingChannels = this.config.getValue()._meta.natures[thing]["channels"];
            let meter = data[thing];
            let power = meter["ActivePower"];
            powerRatio = (power * 100.) / thingChannels["maxActivePower"]["value"]
            activePower += power;
            maxActivePower += thingChannels["maxActivePower"]["value"];
            // + meter["ActivePowerL1"] + meter["ActivePowerL2"] + meter["ActivePowerL3"];
          }
        }
        this.summary.production.powerRatio = powerRatio;
        this.summary.production.activePower = activePower;
        this.summary.production.maxActivePower = maxActivePower;
      }

      {
        /*
         * Consumption
         */
        let activePower = this.summary.grid.activePower + this.summary.production.activePower + this.summary.storage.activePower;
        let maxActivePower = this.summary.grid.maxActivePower + this.summary.production.maxActivePower + this.summary.storage.maxActivePower;
        this.summary.consumption.powerRatio = (activePower * 100.) / maxActivePower
        this.summary.consumption.activePower = activePower;
      }

      // send event
      this.data.next(data);
    }

    /*
     * Reply to a query
     */
    if ("queryreply" in message) {
      let result = message.queryreply;
      console.log(result);
      this.socData.next(result);
    }
  }
}