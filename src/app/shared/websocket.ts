import { EventEmitter } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';

import { Device } from './device';
import { WebappService, Notification } from './service/webapp.service';

export class Websocket {
  public isConnected: boolean = false;
  public event: Subject<Notification> = new Subject<Notification>();
  public subject: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  public devices: { [name: string]: Device } = {};
  public backend: "femsserver" | "openems" = null;

  private websocket: WebSocket;
  private username: string = "";

  constructor(
    public name: string,
    public url: string,
    private webappService: WebappService
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
    // Status description is here:
    let status: Notification = null;

    // send "not successful event" if not connected within Timeout
    let timeout = setTimeout(() => {
      if (!this.isConnected) {
        this.event.next({ type: "error", message: "Keine Verbindung: Timeout" });
      }
    }, 10000);

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
      let authenticate = {
        mode: "login"
      }
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
        if ("authenticate" in message && "mode" in message.authenticate) {
          let mode = message.authenticate.mode;
          if (mode === "allow") {
            this.websocket = websocket;
            this.subject = subject;
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
            this.initialize();
            clearTimeout(timeout);
            status = { type: "error", message: "Keine Verbindung: Authentifizierung fehlgeschlagen." };
            this.event.next(status);
          }
        }

        // receive metadata
        if ("metadata" in message) {
          if ("devices" in message.metadata) {
            // receive device specific data
            let newDevices = message.metadata.devices;
            this.devices = {};
            for (let newDevice of newDevices) {
              let name = newDevice["name"];
              let device = new Device(name, this, this.username);
              device.receive({
                metadata: newDevice
              });
              this.devices[name] = device;
            }
          } else {
            // only one device
            this.devices = {
              fems: new Device("fems", this, this.username)
            };
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
        // REDIRECT if current device was this one
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
    this.subject = new BehaviorSubject<any>(null);
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
  public send(device: Device, message: any): void {
    message["device"] = device.name;
    // console.log(message);
    this.subject.next(message);
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