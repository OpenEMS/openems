import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import websocketConnect from 'rxjs-websockets';
import 'rxjs/add/operator/timeout';

import { environment as env } from '../../../environments';
import { Service, Notification } from './service';
import { Utils } from './utils';
import { Device } from '../device/device';
import { Backend } from '../type/backend';
import { ROLES } from '../type/role';
import { DefaultTypes } from '../service/defaulttypes';

@Injectable()
export class Websocket {
  public static readonly TIMEOUT = 5000;

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

  public event = new Subject<Notification>();
  public status: "online" | "connecting" | "failed" = "connecting";

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
    private webappService: Service,
  ) {
    // try to auto connect using token or session_id
    setTimeout(() => {
      this.connectWithTokenOrSessionId(false);
    })
  }

  /**
   * Parses the route params and sets the current device
   */
  public setCurrentDevice(route: ActivatedRoute): Subject<Device> {
    let deviceName = route.snapshot.params["device"];
    this.devices
      .filter(devices => deviceName in devices)
      .first()
      .map(devices => devices[deviceName])
      .subscribe(device => {
        // set current device
        this.currentDevice.next(device);
        // TODO device.setActive();
      }, error => {
        console.error("Error while setting current device: ", error);
      })
    return this.currentDevice;
    // TODO redirect on timeout
    // timeout: redirect to overview
    // this.currentDevice.next(null);
    // this.router.navigate(['/overview']);
    // throw new URIError("Device [" + deviceName + "] not found."); // TODO translate 
  }

  /**
   * Parses the route params, sets the current device and returns it - or 
   * redirects to overview after a timeout if the device is not existing
   */
  public getCurrentDeviceFromRoute(route: ActivatedRoute): Promise<Device> {
    let deviceName = route.snapshot.params["device"];
    let devicePromise: Promise<Device>;
    if (deviceName in this.devices.value) {
      // device is immediately available
      devicePromise = Promise.resolve(this.devices.value[deviceName]);

    } else {
      // wait for devices
      devicePromise = Utils.timeoutPromise(Websocket.TIMEOUT, new Promise<Device>((resolve, reject) => {
        let ngUnsubscribeWaitForDevices = new Subject<any>();
        this.devices.takeUntil(ngUnsubscribeWaitForDevices).subscribe(devices => {
          if (deviceName in devices) {
            // stop waiting for devices
            ngUnsubscribeWaitForDevices.next();
            ngUnsubscribeWaitForDevices.complete();
            // resolve Promise
            resolve(devices[deviceName]);
          }
        })
      }));
    }

    devicePromise.then(device => {
      // ask device to query config
      device.getConfig();
      this.currentDevice.next(device);
    }).catch(reason => {

    });
    return devicePromise;
  }

  /**
   * Clears the current device
   */
  public clearCurrentDevice() {
    this.currentDevice.next(null);
  }

  /**
   * Opens a connection using a password
   */
  public connectWithPassword(password: string) {
    this.connect(password, null);
  }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket
   */
  public connectWithTokenOrSessionId(throwErrorOnDeny: boolean = true) {
    this.connect(null, throwErrorOnDeny);
  }

  /**
   * Tries to connect using given password or token.
   */
  public connect(password: string, throwErrorOnDeny: boolean = true) {
    if (this.messages) {
      return;
    }

    this.messages = websocketConnect(
      env.url,
      this.inputStream = new Subject<any>()
    ).messages.share();

    // send authentication if given
    if (password) {
      let authenticate = {
        mode: "login",
        password: password
      };
      this.send(null, {
        authenticate: authenticate
      });
    }

    /**
     * called on every receive of message from server
     */
    let retryCounter = 0;
    this.websocketSubscription = this.messages.retryWhen(errors => errors.do(error => {
      // on more than 10 tries -> disconnect user and redirect to login
      if (retryCounter == 10) {
        // TODO: reevaluate if we should really stop after 10 tries
        this.status = "failed";
        this.close();
        this.webappService.notify({
          type: "error",
          message: this.webappService.translate.instant('Notifications.Failed')
        });
      }
      retryCounter++;
      return errors.delay(200);

    }).delay(1000))/* TODO what is this delay for? */.subscribe(message => {
      retryCounter = 0;
      console.log(message);

      // Receive authentication token
      if ("authenticate" in message && "mode" in message.authenticate) {
        let mode = message.authenticate.mode;

        if (mode === "allow") {
          // authentication successful
          this.status = "online";

          if ("token" in message.authenticate) {
            // received login token -> save in cookie
            this.webappService.setToken(message.authenticate.token);
          }

          if ("role" in message.authenticate && env.backend == Backend.OpenEMS_Edge) {
            // for OpenEMS Edge we have only one device
            let role = ROLES.getRole(message.authenticate.role);
            let deviceName = "fems";
            let replyStream: { [messageId: string]: Subject<any> } = {};
            this.replyStreams[deviceName] = replyStream;
            this.devices.next({
              fems: new Device(deviceName, "FEMS", "", role, true, replyStream, this)
            });
          }

          // TODO username is deprecated
          if ("username" in message.authenticate) {
            this.username = message.authenticate.username;
            this.event.next({ type: "success", message: this.webappService.translate.instant('Notifications.LoggedInAs', { value: this.username }) });
          } else {
            this.event.next({ type: "success", message: this.webappService.translate.instant('Notifications.LoggedIn') });
          }

        } else {
          // authentication denied -> close websocket
          this.status = "failed";
          this.webappService.removeToken();
          this.initialize();
          if (env.backend == Backend.OpenEMS_Backend) {
            console.log("would redirect...") // TODO fix redirect
            //window.location.href = "/web/login?redirect=/m/overview";
          }
          if (throwErrorOnDeny) {
            let status: Notification = { type: "error", message: this.webappService.translate.instant('Notifications.AuthenticationFailed') };
            this.event.next(status);
          }
        }
      }

      // Receive a reply with a message id -> forward to devices' replyStream
      if ("id" in message && message.id instanceof Array) {
        let id = message.id[0];
        for (let deviceName in this.replyStreams) {
          if (id in this.replyStreams[deviceName]) {
            this.replyStreams[deviceName][id].next(message);
            break;
          }
        }
      }

      // receive metadata
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
            // TODO
            // device.receive({
            //   metadata: newDevice
            // });
          }
          this.devices.next(newDevices);
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
    if (this.status != "online") { // TODO why this if?
      this.websocketSubscription.unsubscribe();
      this.messages = null;
      this.devices.next({});
    }
  }

  /**
   * Closes the connection.
   */
  public close() {
    console.info("Closing websocket");
    if (this.status != "online") { // TODO why this if?
      this.webappService.removeToken();
      this.initialize();
      var status: Notification = { type: "info", message: this.webappService.translate.instant('Notifications.Closed') };
      this.event.next(status);
    }
  }

  /**
   * Sends a message to the websocket
   */
  public send(device: Device, message: any): void {
    if ("id" in message) {
      this.pendingQueryReplies[message.id[0]] = device.name;
    }
    if (device == null) {
      this.inputStream.next(message);
    } else {
      message["device"] = device.name;
      this.inputStream.next(message);
    }
  }
}