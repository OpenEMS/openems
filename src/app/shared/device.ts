import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import * as moment from 'moment';

import { Notification } from './service/webapp.service';
import { Websocket } from './service/websocket.service';
import { Config } from './config';

const SUBSCRIBE: string = "fenecon_monitor_v1";

class Things {
  storage = {};
  production = {};
  grid = {};
  consumption = {};
}

class Summary {
  public storage = {
    soc: null,
    activePower: 0,
    maxActivePower: 0
  };
  public production = {
    powerRatio: 0,
    activePower: 0,
    maxActivePower: 0
  };
  public grid = {
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
  private things: Things
  public event = new Subject<Notification>();
  public address: string;
  public data = new BehaviorSubject<{ [thing: string]: any }>(null);
  public historyData = new BehaviorSubject<any[]>(null);
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

  private isInArray = (array: any, value: any): boolean => {
    if (!array || !value) {
      return false;
    }
    return array.indexOf(value) > -1;
  }

  private refreshThingsFromConfig(): Things {
    let result = new Things();
    let config = this.config.getValue();
    if ("_meta" in config && "natures" in config._meta) {
      let natures = this.config.getValue()._meta.natures;
      for (let thing in natures) {
        let a = natures[thing]["implements"];
        // Ess
        if (this.isInArray(a, "EssNature")) {
          result.storage[thing] = true;
        }
        // Meter
        if (this.isInArray(a, "MeterNature")) {
          // get type
          let type = natures[thing]["channels"]["type"]["value"];
          if (type === "grid") {
            result.grid[thing] = true;
          } else if (type === "production") {
            result.production[thing] = true;
          } else {
            console.warn("Meter without type: " + thing);
          }
        }
      }
    }
    return result;
  }

  private getImportantChannels(): { [thing: string]: [string] } {
    let natures = this.config.getValue()._meta.natures;
    let ignoreNatures = {};
    let result = {}
    for (let thing in natures) {
      let a = natures[thing]["implements"];
      let channels = []

      /*
       * Find important Channels to subscribe
       */
      // Ess
      if (this.isInArray(a, "EssNature")) {
        channels.push("Soc");
      }
      if (this.isInArray(a, "AsymmetricEssNature")) {
        channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
      }
      if (this.isInArray(a, "SymmetricEssNature")) {
        channels.push("ActivePower", "ReactivePower");
      }
      if (this.isInArray(a, "FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
        ignoreNatures["AsymmetricMeterNature"] = true;
      }

      // Meter
      if (this.isInArray(a, "AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
        channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
      }
      if (this.isInArray(a, "SymmetricMeterNature")) {
        channels.push("ActivePower", "ReactivePower");
      }

      result[thing] = channels;
    }
    return result;
  }

  /**
   * Send "subscribe" message to websocket
   */
  public subscribe() {
    this.summary = new Summary();
    let subscribe = this.getImportantChannels();
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

  private getkWhResult(channels: { [thing: string]: [string] }): { [thing: string]: [string] } {
    let kWh = {};
    let thingChannel = [];

    for (let type in this.things) {
      for (let thing in this.things[type]) {
        for (let channel in channels[thing]) {
          if (channels[thing][channel] == "ActivePower") {
            kWh[thing + "/ActivePower"] = type;
          } else if (channels[thing][channel] == "ActivePowerL1" || channels[thing][channel] == "ActivePowerL2" || channels[thing][channel] == "ActivePowerL3") {
            kWh[thing + "/ActivePowerL1"] = type;
            kWh[thing + "/ActivePowerL2"] = type;
            kWh[thing + "/ActivePowerL3"] = type;
          }
        }
      }
    }
    return kWh;
  }

  /**
   * Send "query" message to websocket
   */
  public query(fromDate: any, toDate: any) {
    let obj = {
      mode: "history",
      fromDate: fromDate.format("YYYY-MM-DD"),
      toDate: toDate.format("YYYY-MM-DD"),
      timezone: new Date().getTimezoneOffset() * 60,
      channels: this.getImportantChannels(),
      kWh: this.getkWhResult(this.getImportantChannels())
    };
    console.log(obj);
    this.send({ query: obj });
  }

  /**
   * Calculate summary data from websocket reply
   */
  public calculateSummary(data: any): Summary {
    function getActivePower(o: any): number {
      if ("ActivePowerL1" in o && o.ActivePowerL1 != null && "ActivePowerL2" in o && o.ActivePowerL2 != null && "ActivePowerL3" in o && o.ActivePowerL3 != null) {
        return o.ActivePowerL1 + o.ActivePowerL2 + o.ActivePowerL3;
      } else if ("ActivePower" in o && o.ActivePower != null) {
        return o.ActivePower;
      } else {
        return 0;
      }
    }

    let summary = new Summary();
    {
      /*
       * Storage
       */
      let soc = 0;
      let activePower = 0;
      for (let thing in this.things.storage) {
        if (thing in data) {
          let ess = data[thing];
          soc += ess["Soc"];
          activePower += getActivePower(ess);
        }
      }
      summary.storage.soc = soc / Object.keys(this.things.storage).length;
      summary.storage.activePower = activePower;
    }

    {
      /*
       * Grid
       */
      let powerRatio = 0;
      let activePower = 0;
      let maxActivePower = 0;
      for (let thing in this.things.grid) {
        if (thing in data) {
          let thingChannels = this.config.getValue()._meta.natures[thing]["channels"];
          let meter = data[thing];
          let power = getActivePower(meter);
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
      summary.grid.powerRatio = powerRatio;
      summary.grid.activePower = activePower;
      summary.grid.maxActivePower = maxActivePower;
    }

    {
      /*
       * Production
       */
      let powerRatio = 0;
      let activePower = 0;
      let maxActivePower = 0;
      for (let thing in this.things.production) {
        if (thing in data) {
          let thingChannels = this.config.getValue()._meta.natures[thing]["channels"];
          let meter = data[thing];
          let power = getActivePower(meter);
          powerRatio = (power * 100.) / thingChannels["maxActivePower"]["value"]
          activePower += power;
          maxActivePower += thingChannels["maxActivePower"]["value"];
          // + meter["ActivePowerL1"] + meter["ActivePowerL2"] + meter["ActivePowerL3"];
        }
      }
      summary.production.powerRatio = powerRatio;
      summary.production.activePower = activePower;
      summary.production.maxActivePower = maxActivePower;
    }

    {
      /*
       * Consumption
       */
      let activePower = summary.grid.activePower + summary.production.activePower + summary.storage.activePower;
      let maxActivePower = summary.grid.maxActivePower + summary.production.maxActivePower + summary.storage.maxActivePower;
      summary.consumption.powerRatio = (activePower * 100.) / maxActivePower;
      summary.consumption.activePower = activePower;
    }
    // console.log(JSON.stringify(data), JSON.stringify(summary));
    return summary;
  }

  /**
   * Receive new data from websocket
   */
  public receive(message: any) {
    // console.log("receive: ", message);
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
        this.things = this.refreshThingsFromConfig();
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
      this.summary = this.calculateSummary(data);
      // send event
      this.data.next(data);
    }

    /*
     * Reply to a query
     */
    if ("queryreply" in message) {
      let data = message.queryreply.data;
      console.log(message.queryreply);
      if (data != null) {
        for (let datum of data) {
          let sum = this.calculateSummary(datum.channels);
          datum["summary"] = sum;
        }
      }
      this.historyData.next(data);
    }
  }
}