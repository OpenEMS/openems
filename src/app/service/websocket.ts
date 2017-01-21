import { EventEmitter } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';

import { Device } from './device';
import { WebappService, Notification } from './webapp.service';

export class Websocket {
  public isConnected: boolean = false;
  public event: Subject<Notification> = new Subject<Notification>();
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
        if ("all_devices" in message) {
          this.devices = {};
          for (let deviceName in message.all_devices) {
            let device = new Device(deviceName, this);
            device.receive(message.all_devices[deviceName]);
            this.devices[deviceName] = device;
          }
        }

        // Receive device info
        if ("devices" in message) {
          for (let deviceName in message.devices) {
            if (this.devices[deviceName]) {
              this.devices[deviceName].receive(message.devices[deviceName]);
            }
          }
        }

        // receive notification
        if ("notification" in message) {
          this.webappService.notify(message.notification);
        }
        if ("devices" in message) {
          for (let deviceName in message.devices) {
            if ("notification" in message.devices[deviceName]) {
              this.webappService.notify(message.devices[deviceName].notification);
            }
          }
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
  public send(device: Device, value: any): void {
    let message = {
      devices: {}
    }
    message.devices[device.name] = value;
    console.log("SENDING", message);
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