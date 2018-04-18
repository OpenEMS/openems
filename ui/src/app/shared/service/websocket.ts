import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import { WebSocketSubject } from 'rxjs/observable/dom/WebSocketSubject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/timeout';
import 'rxjs/add/operator/takeUntil';

import { environment as env } from '../../../environments';
import { Service } from './service';
import { Utils } from './utils';
import { Device } from '../device/device';
import { Role } from '../type/role';
import { DefaultTypes } from '../service/defaulttypes';
import { DefaultMessages } from '../service/defaultmessages';

@Injectable()
export class Websocket {
  public static readonly TIMEOUT = 15000;
  private static readonly DEFAULT_EDGEID = 0;
  private static readonly DEFAULT_DEVICENAME = "fems";

  // holds references of device names (=key) to Device objects (=value)
  private _devices: BehaviorSubject<{ [name: string]: Device }> = new BehaviorSubject({});
  public get devices() {
    return this._devices;
  }

  // holds the currently selected device
  private _currentDevice: BehaviorSubject<Device> = new BehaviorSubject<Device>(null);
  public get currentDevice() {
    return this._currentDevice;
  }

  private socket: WebSocketSubject<any>;
  public status: DefaultTypes.ConnectionStatus = "connecting";
  public isWebsocketConnected: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private username: string = "";
  // private messages: Observable<string>;
  private queryreply = new Subject<{ id: string[] }>();

  // holds stream per device (=key1) and message-id (=key2); triggered on message reply for the device
  private replyStreams: { [deviceName: string]: { [messageId: string]: Subject<any> } } = {};

  // tracks which message id (=key) is connected with which deviceName (=value)
  private pendingQueryReplies: { [id: string]: string } = {};

  constructor(
    private router: Router,
    private service: Service,
  ) {
    // try to auto connect using token or session_id
    setTimeout(() => {
      this.connect();
    })
  }

  /**
   * Parses the route params and sets the current device
   */
  public setCurrentDevice(route: ActivatedRoute): Subject<Device> {
    let onTimeout = () => {
      // Timeout: redirect to overview
      this.router.navigate(['/overview']);
      subscription.unsubscribe();
    }

    let deviceName = route.snapshot.params["device"];
    let subscription = this.devices
      .filter(devices => deviceName in devices)
      .first()
      .map(devices => devices[deviceName])
      .subscribe(device => {
        if (device == null || !device.online) {
          onTimeout();
        } else {
          // set current device
          this.currentDevice.next(device);
          device.markAsCurrentDevice();
        }
      }, error => {
        console.error("Error while setting current device: ", error);
      })
    setTimeout(() => {
      let device = this.currentDevice.getValue();
      if (device == null || !device.online) {
        onTimeout();
      }
    }, Websocket.TIMEOUT);
    return this.currentDevice;
  }

  /**
   * Clears the current device
   */
  public clearCurrentDevice() {
    this.currentDevice.next(null);
  }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket. Called once by constructor
   */
  private connect(): BehaviorSubject<boolean> {
    if (this.socket != null) {
      return this.isWebsocketConnected;
    }

    if (env.debugMode) {
      console.info("Websocket connect to URL [" + env.url + "]");
    }

    this.socket = WebSocketSubject.create({
      url: env.url,
      openObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection opened");
          }
          this.isWebsocketConnected.next(true);
          if (this.status == 'online') {
            this.service.notify({
              message: "Connection lost. Trying to reconnect.", // TODO translate
              type: 'warning'
            });
            // TODO show spinners everywhere
            this.status = 'connecting';
          } else {
            this.status = 'waiting for authentication';
          }
        }
      },
      closeObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection closed");
          }
          this.isWebsocketConnected.next(false);
        }
      }
    });

    this.socket.retryWhen(errors => {
      console.warn("Websocket was interrupted. Retrying in 2 seconds.");
      return errors.delay(2000);
    }).subscribe(message => {
      // called on every receive of message from server
      if (env.debugMode) {
        console.info("RECV", message);
      }

      /*
       * Authenticate
       */
      if ("authenticate" in message && "mode" in message.authenticate) {
        let mode = message.authenticate.mode;

        if (mode === "allow") {
          // authentication successful
          this.status = "online";

          if ("token" in message.authenticate) {
            // received login token -> save in cookie
            this.service.setToken(message.authenticate.token);
          }

        } else {
          // authentication denied -> close websocket
          this.status = "failed";
          this.service.removeToken();
          this.initialize();
          if (env.backend === "OpenEMS Backend") {
            if (env.production) {
              window.location.href = "/web/login?redirect=/m/overview";
            } else {
              console.info("would redirect...");
            }
          } else if (env.backend === "OpenEMS Edge") {
            this.router.navigate(['/overview']);
          }
        }
      }

      /*
       * Query reply
       */
      if ("messageId" in message && "ui" in message.messageId) {
        // Receive a reply with a message id -> find device and forward to devices' replyStream
        let messageId = message.messageId.ui;
        for (let deviceName in this.replyStreams) {
          if (messageId in this.replyStreams[deviceName]) {
            this.replyStreams[deviceName][messageId].next(message);
            break;
          }
        }
      }

      /*
       * Metadata
       */
      if ("metadata" in message) {
        if ("edges" in message.metadata) {
          let devices = <DefaultTypes.MessageMetadataDevice[]>message.metadata.edges;
          let newDevices = {};
          for (let device of devices) {
            let replyStream: { [messageId: string]: Subject<any> } = {};
            this.replyStreams[device.name] = replyStream;
            let newDevice = new Device(
              device.id,
              device.name,
              device.comment,
              device.producttype,
              Role.getRole(device.role),
              device.online,
              replyStream,
              this
            );
            newDevices[newDevice.name] = newDevice;
          }
          this.devices.next(newDevices);
        }
      }

      /*
       * receive notification
       */
      if ("notification" in message) {
        let notification = message.notification;
        let n: DefaultTypes.Notification;
        let notify: boolean = true;
        if ("code" in notification) {
          // handle specific notification codes - see Java source for details
          let code = notification.code;
          let params = notification.params;
          if (code == 100 /* device disconnected -> mark as offline */) {
            let deviceId = params[0];
            if (deviceId in this.devices.getValue()) {
              this.devices.getValue()[deviceId].setOnline(false);
            }
          } else if (code == 101 /* device reconnected -> mark as online */) {
            let deviceId = params[0];
            if (deviceId in this.devices.getValue()) {
              let device = this.devices.getValue()[deviceId];
              device.setOnline(true);
            }
          } else if (code == 103 /* authentication by token failed */) {
            let token: string = params[0];
            if (token !== "") {
              // remove old token
              this.service.removeToken();
            }
            // ask for authentication info
            this.status = "waiting for authentication";
            notify = false;
            setTimeout(() => {
              this.clearCurrentDevice();
              this.router.navigate(["/overview"]);
            });
          }
        }
        if (notify) {
          this.service.notify(<DefaultTypes.Notification>notification);
        }
      }
    }, error => {
      console.error("Websocket error", error);
    }, () => {
      console.info("Websocket finished");
    })
    return this.isWebsocketConnected;
  }

  /**
   * Reset everything to default
   */
  private initialize() {
    this.devices.next({});
  }

  /**
   * Opens the websocket and logs in
   */
  public logIn(password: string) {
    if (this.isWebsocketConnected.getValue()) {
      // websocket was connected
      this.send(DefaultMessages.authenticateLogin(password));
    } else {
      // websocket was NOT connected
      this.connect()
        .filter(isConnected => isConnected)
        .first()
        .subscribe(isConnected => {
          setTimeout(() => {
            this.send(DefaultMessages.authenticateLogin(password))
          }, 500);
        });
    }
  }

  /**
   * Logs out and closes the websocket
   */
  public logOut() {
    // TODO this is kind of working for now... better would be to not close the websocket but to handle session validity serverside
    this.send(DefaultMessages.authenticateLogout());
    this.status = "waiting for authentication";
    this.service.removeToken();
    this.initialize();
  }

  /**
   * Sends a message to the websocket
   */
  public send(message: any): void {
    if (env.debugMode) {
      console.info("SEND: ", message);
    }
    this.socket.socket.send(JSON.stringify(message));
  }
}