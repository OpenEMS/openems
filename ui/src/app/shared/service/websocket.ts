import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import websocketConnect from 'rxjs-websockets';
import 'rxjs/add/operator/timeout';

import { environment as env } from '../../../environments';
import { Service } from './service';
import { Utils } from './utils';
import { Device } from '../device/device';
import { ROLES } from '../type/role';
import { DefaultTypes } from '../service/defaulttypes';
import { DefaultMessages } from '../service/defaultmessages';

@Injectable()
export class Websocket {
  public static readonly TIMEOUT = 5000;
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

  public status: DefaultTypes.ConnectionStatus = "connecting";
  public noOfConnectedWebsockets: number;

  private username: string = "";
  private messages: Observable<any>;
  private inputStream: Subject<any>;
  private websocketSubscription: Subscription = new Subscription();
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
  private connect() {
    if (this.messages) {
      return;
    }

    const { messages, connectionStatus } = websocketConnect(
      env.url,
      this.inputStream = new Subject<any>()
    );
    connectionStatus.subscribe(noOfConnectedWebsockets => {
      this.noOfConnectedWebsockets = noOfConnectedWebsockets;
      if (noOfConnectedWebsockets == 0 && this.status == 'online') {
        this.service.notify({
          message: "Connection lost. Trying to reconnect.", // TODO translate
          type: 'warning'
        });
        // TODO show spinners everywhere
        this.status = 'connecting';
      }
    });
    this.messages = messages.share();
    this.websocketSubscription = this.messages.retryWhen(errors => {
      return errors.delay(1000);

    }).map(message => JSON.parse(message)).subscribe(message => {
      // called on every receive of message from server
      if (env.debugMode) {
        console.log("RECV", message);
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

          if ("role" in message.authenticate && env.backend === "OpenEMS Edge") {
            // for OpenEMS Edge we have only one device
            let role = ROLES.getRole(message.authenticate.role);
            let replyStream: { [messageId: string]: Subject<any> } = {};
            this.replyStreams[Websocket.DEFAULT_DEVICENAME] = replyStream;
            this.devices.next({
              fems: new Device(Websocket.DEFAULT_DEVICENAME, "FEMS", "", role, true, replyStream, this)
            });
          }

          // TODO username is deprecated
          // if ("username" in message.authenticate) {
          //   this.username = message.authenticate.username;
          //   this.event.next({ type: "success", message: this.service.translate.instant('Notifications.LoggedInAs', { value: this.username }) });
          // } else {
          //   this.event.next({ type: "success", message: this.service.translate.instant('Notifications.LoggedIn') });
          // }

        } else {
          // authentication denied -> close websocket
          this.status = "failed";
          this.service.removeToken();
          this.initialize();
          if (env.backend === "OpenEMS Backend") {
            if (env.production) {
              window.location.href = "/web/login?redirect=/m/overview";
            } else {
              console.log("would redirect...");
            }
          } else if (env.backend === "OpenEMS Edge") {
            this.router.navigate(['/overview']);
          }
        }
      }

      /*
       * Query reply
       */
      if ("id" in message && message.id instanceof Array) {
        let id = message.id[0];
        let deviceName = Websocket.DEFAULT_DEVICENAME;
        if ("device" in message) {
          // Receive a reply with a message id and a device -> forward to devices' replyStream
          deviceName = message.device;
          if (deviceName in this.replyStreams && id in this.replyStreams[deviceName]) {
            this.replyStreams[deviceName][id].next(message);
          }
        } else {
          // Receive a reply with a message id -> find device and forward to devices' replyStream
          for (let deviceName in this.replyStreams) {
            if (id in this.replyStreams[deviceName]) {
              this.replyStreams[deviceName][id].next(message);
              break;
            }
          }
        }
      }

      /*
       * Metadata
       */
      if ("metadata" in message) {
        if ("devices" in message.metadata) {
          let devices = <DefaultTypes.MessageMetadataDevice[]>message.metadata.devices;
          let newDevices = {};
          for (let device of devices) {
            let replyStream: { [messageId: string]: Subject<any> } = {};
            this.replyStreams[device.name] = replyStream;
            let newDevice = new Device(
              device.name,
              device.comment,
              device.producttype,
              ROLES.getRole(device.role),
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
    });
  }

  /**
   * Reset everything to default
   */
  private initialize() {
    this.websocketSubscription.unsubscribe();
    this.messages = null;
    this.devices.next({});
  }

  /**
   * Closes the connection.
   */
  public close() {
    console.info("Closing websocket");
    if (this.status != "online") { // TODO why this if?
      this.service.removeToken();
      this.initialize();
      // TODO
      // var status: DefaultTypes.Notification = { type: "info", message: this.service.translate.instant('Notifications.Closed') };
      // this.event.next(status);
    }
  }

  /**
   * Sends a message to the websocket
   */
  public send(message: any, device?: Device): void {
    if (env.debugMode) {
      console.log("SEND: ", message);
    }
    if (device) {
      if ("id" in message) {
        this.pendingQueryReplies[message.id[0]] = device.name;
      }
      message["device"] = device.name;
    }
    this.inputStream.next(JSON.stringify(message));
  }
}