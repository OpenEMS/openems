import { EventEmitter } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';

import { Device } from './device';
import { WebappService, Notification } from './webapp.service';

export class Websocket {
  public isConnected: boolean = false;
  public event = new Subject<Notification>();
  public subject: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  public devices: { [name: string]: Device } = {};

  private websocket: WebSocket;

  constructor(
    public name: string,
    public url: string,
    private webappService: WebappService
  ) { }

  /**
   * Opens a connection using the stored token for this websocket
   */
  public connectWithToken() {
    var token = this.webappService.getToken(this.name);
    if (token) {
      this.connect(null, token);
    }
  }

  /**
   * Opens a connection using a password
   */
  public connectWithPassword(password: string) {
    this.connect(password, null);
  }

  /**
   * Tries to connect using given password or token.
   */
  private connect(password: string, token: string) {
    // return non-active Websocket if no password or token was given
    if (password == null && token == null) {
      this.initialize();
      return;
    }

    // Status description is here:
    var status: Notification = null;

    // send "not successful event" if not connected within Timeout
    var timeout = setTimeout(() => {
      if (!this.isConnected) {
        this.event.next({ type: "error", message: "Keine Verbindung: Timeout" });
      }
    }, 2000);

    // create a new websocket connection
    let websocket = new WebSocket(this.url);

    // define observable
    let observable = Observable.create((obs: Observer<MessageEvent>) => {
      websocket.onmessage = obs.next.bind(obs);
      websocket.onerror = obs.error.bind(obs);
      websocket.onclose = obs.complete.bind(obs);

      websocket.close.bind(websocket);
    }).share();

    // define observer
    let observer = {
      next: (data: Object) => {
        if (websocket.readyState === WebSocket.OPEN) {
          websocket.send(JSON.stringify(data));
        }
      },
    };

    // immediately authenticate when websocket is opened
    websocket.onopen = () => {
      let authenticate = {}
      if (password) {
        authenticate["password"] = password;
      } else if (token) {
        authenticate["token"] = token;
      }
      observer.next({
        authenticate: authenticate
      });
    };

    // create subject
    let subject: BehaviorSubject<any> = BehaviorSubject.create(observer, observable);

    subject
      .map(message => JSON.parse(message.data))
      .subscribe((message: any) => {

        // Receive authentication token
        if ("authenticate" in message) {
          if ("token" in message.authenticate) {
            this.webappService.setToken(this.name, message.authenticate.token);
            if ("username" in message.authenticate) {
              let username = message.authenticate.username;
              this.websocket = websocket;
              this.subject = subject;
              this.isConnected = true;
              this.event.next({ type: "success", message: "Angemeldet als " + username + "." });
            }
          } else {
            // close websocket
            this.webappService.removeToken(this.name);
            this.initialize();
            clearTimeout(timeout);
            status = { type: "error", message: "Keine Verbindung: Authentifizierung fehlgeschlagen." };
            this.event.next(status);
          }
        }

        // Receive connected devices
        if ("devices" in message) {
          this.devices = {};
          for (let deviceName in message.devices) {
            this.devices[deviceName] = new Device(deviceName, this);
            for (let key in message.devices[deviceName]) {
              this.devices[deviceName][key] = message.devices[deviceName][key];
            }
          }
        }

        //TODO reveice config of all FEMSes
        /*
        // Receive config
        if ("config" in message) {
          this.config = new OpenemsConfig();
          // device natures
          if ("_devices" in msg.config) {
            this.config._devices = {}
            for (let id in msg.config._devices) {
              var device = new Device();
              device._name = id;
              device._natures = msg.config._devices[id];
              this.config._devices[id] = device;
            }
          }
          // controllers
          if ("_controllers" in msg.config) {
            this.config._controllers = msg.config._controllers;
          }
          // things
          if ("things" in msg.config) {
            this.config.things = msg.config.things;
          }
          // scheduler
          if ("scheduler" in msg.config) {
            this.config.scheduler = msg.config.scheduler;
          }
          // persistences
          if ("persistence" in msg.config) {
            for (let persistence of msg.config.persistence) {
              if (persistence.class == "io.openems.impl.persistence.influxdb.InfluxdbPersistence") {
                var ip = persistence.ip;
                if (ip == "127.0.0.1") { // rewrite localhost to remote ip
                  ip = location.hostname;
                }
                var influxdb = new InfluxdbPersistence();
                influxdb.ip = ip;
                influxdb.username = persistence.username;
                influxdb.password = persistence.password;
                influxdb.fems = persistence.fems;
                this.config.persistence.push(influxdb);
              } else {
                this.config.persistence.push(persistence);
              }
            }
          }
          this.event.emit(null);
        }
        */

        // TODO Receive data of all FEMSes
        /*
        if ("data" in msg) {
          var data = msg.data;
          var newData: Object = {}
          for (let id in data) {
            var channels = data[id];
            if (id in this.config._devices) {
              newData[id] = {};
              for (let channelid in channels) {
                var channel = channels[channelid];
                newData[id][channelid] = channel;
              }
            }
          }
          this.data.next(newData);
          console.log(newData);
        }
        */

        // receive notification
        if ("notification" in message) {
          this.webappService.notify(message.notification);
        }
      }, (error: any) => {
        this.initialize();
        clearTimeout(timeout);
        if (!status) {
          status = { type: "error", message: "Verbindungsfehler." };
          this.event.next(status);
        }
      }, (/* complete */) => {
        this.initialize();
        clearTimeout(timeout);
        if (status == null) {
          status = { type: "error", message: "Verbindung beendet." };
          this.event.next(status);
        }
      });
  }

  /**
   * Reset everything to default
   */
  private initialize() {
    if (this.websocket != null && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.close();
    }
    this.websocket = null;
    this.isConnected = false;
    //this.config = new OpenemsConfig();
    this.subject = new BehaviorSubject<any>(null);
    //this.data = new BehaviorSubject<any>(null);
    this.devices = {}
  }

  /**
   * Closes the connection.
   */
  public close() {
    this.webappService.removeToken(this.name);
    this.initialize();
    var status: Notification = { type: "error", message: "Verbindung beendet." };
    this.event.next(status);
  }

  /**
   * Sends a message to the websocket
   */
  private send(value: any): void {
    this.subject.next(value);
  }

  /**
   * Returns the websocket with the given name
   */
  public getDevice(name: string) {
    if (name in this.devices) {
      return this.devices[name];
    } else {
      return null;
    }
  }
}