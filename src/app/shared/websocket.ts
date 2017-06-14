import { EventEmitter } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { QueueingSubject } from 'queueing-subject';
import { Subscription } from 'rxjs/Subscription';

import websocketConnect from 'rxjs-websockets';

import 'rxjs/add/operator/retryWhen';
import 'rxjs/add/operator/delay';

import { Device } from './device';
import { WebappService, Notification } from './service/webapp.service';

export class Websocket {
  public isConnected: boolean = false;
  public event: Subject<Notification> = new Subject<Notification>();
  public devices: { [name: string]: Device } = {};

  private username: string = "";
  private messages: Observable<any>;
  private inputStream: QueueingSubject<any>;
  private websocketSubscription: Subscription = new Subscription();

  constructor(
    public name: string,
    public url: string,
    public backend: "femsserver" | "openems",
    private webappService: WebappService,
  ) { }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket
   */
  public connectWithTokenOrSessionId() {
    var token = this.webappService.getToken(this.name);
    if (token) {
      this.connect(null, token);
    } else if (document.cookie.indexOf("session_id=") != -1) {
      this.connect(null, null);
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
    if (this.messages) {
      return;
    }

    this.messages = websocketConnect(
      this.url,
      this.inputStream = new QueueingSubject<any>()
    ).messages.share();

    // Status description is here:
    let status: Notification = null;

    let a = {
      mode: "login"
    };
    if (password) {
      a["password"] = password;
    } else if (token) {
      a["token"] = token;
    }

    let authenticate = {
      authenticate: a
    };

    this.send(null, authenticate);

    /**
     * called on every receive of message from server
     */
    let retryCounter = 0;
    this.websocketSubscription = this.messages.retryWhen(errors => errors.do(error => {
      // Websocket tries to reconnect ==> send authentication only one time
      if (retryCounter == 0) {
        this.inputStream.next(authenticate);
      } else if (retryCounter == 10) {
        // disconnect user and redirect to login
        this.isConnected = false;
        this.close();
        this.webappService.notify({
          type: "error",
          message: "Verbindungsaufbau fehlgeschlagen."
        });
      }
      retryCounter++;
    }).delay(1000)).subscribe(message => {
      retryCounter = 0;
      // Receive authentication token
      if ("authenticate" in message && "mode" in message.authenticate) {
        let mode = message.authenticate.mode;
        if (mode === "allow") {
          // this.inputStream = new QueueingSubject<any>();
          this.isConnected = true;
          if ("token" in message.authenticate) {
            this.webappService.setToken(this.name, message.authenticate.token);
          }
          if ("username" in message.authenticate) {
            this.username = message.authenticate.username;
            this.event.next({ type: "success", message: "Angemeldet als " + this.username + "." });
          } else {
            this.event.next({ type: "success", message: "Angemeldet." });
          }
        } else {
          // close websocket
          this.webappService.removeToken(this.name);
          this.isConnected = false;
          status = { type: "error", message: "Keine Verbindung: Authentifizierung fehlgeschlagen." };
          this.event.next(status);
          this.initialize();
        }
      }

      // receive metadata
      if ("metadata" in message) {
        if ("devices" in message.metadata) {
          // receive device specific data
          let newDevices = message.metadata.devices;
          for (let newDevice of newDevices) {
            let name = newDevice["name"];
            let device;
            if (name in this.devices) {
              device = this.devices[name];
            } else {
              device = new Device(name, this, this.username);
              this.devices[name] = device;
            }
            device.receive({
              metadata: newDevice
            });
          }
        } else {
          // only one device
          if (!("fems" in this.devices)) {
            // device was not existing
            this.devices = {
              fems: new Device("fems", this, this.username)
            }
          }
        }
        if ("backend" in message.metadata) {
          this.backend = message.metadata.backend;
        }

      }

      // receive device specific data
      if ("device" in message) {
        // device was specified -> forward
        if (this.devices[message.device]) {
          let device = this.devices[message.device];
          device.receive(message);
        }
      } else if (Object.keys(this.devices).length == 1) {
        // device was not specified, but we have only one
        for (let key in this.devices) {
          this.devices[key].receive(message);
        }
      }

      // receive notification
      if ("notification" in message) {
        this.webappService.notify(message.notification);
      }

    });
  }

  /**
   * Reset everything to default
   */
  private initialize() {
    if (!this.isConnected) {
      this.websocketSubscription.unsubscribe();
      this.messages = null;
      this.devices = {};
    }
  }

  /**
   * Closes the connection.
   */
  public close() {
    if (!this.isConnected) {
      this.webappService.removeToken(this.name);
      this.initialize();
      var status: Notification = { type: "error", message: "Verbindung beendet." };
      this.event.next(status);
    }
  }

  /**
   * Sends a message to the websocket
   */
  public send(device: Device, message: any): void {
    if (device == null) {
      this.inputStream.next(message);
    } else {
      message["device"] = device.name;
      this.inputStream.next(message);
    }
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