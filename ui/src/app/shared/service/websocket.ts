import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import websocketConnect from 'rxjs-websockets';

import { environment as env } from '../../../environments';
import { Service, Notification } from './service';
import { Device } from '../device/device';
import { Backend } from '../type/backend';
import { ROLES } from '../type/role';

@Injectable()
export class Websocket {
  public devices: { [name: string]: Device } = {};
  public event = new Subject<Notification>();
  public currentDevice = new BehaviorSubject<Device>(null);
  public status: "online" | "connecting" | "failed" = "connecting";

  private username: string = "";
  private messages: Observable<any>;
  private inputStream: Subject<any>;
  private websocketSubscription: Subscription = new Subscription();

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
   * Parses the route params, sets the current device and returns it - or redirects to overview and returns null
   */
  public setCurrentDevice(params: Params): BehaviorSubject<Device> {
    let timeout = null;
    let retryCounter = 0;
    let worker = (params: Params): boolean => {
      retryCounter++;
      if ('device' in params) {
        let deviceName = params['device'];
        let device = this.getDevice(deviceName);
        if (device) {
          // found it -> we quit here
          this.currentDevice.next(device);
          device.send({ connect: true });
          return;
        }
      }
      if (retryCounter < 10) {
        // retry 10 times
        timeout = setTimeout(() => {
          worker(params);
        }, 1000);
      } else {
        // failed -> redirect to /overview
        this.currentDevice.next(null);
        this.router.navigate(['/overview']);
      }
    }
    worker(params);
    return this.currentDevice;
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
            this.devices = {
              fems: new Device("fems", "FEMS", "", role, true, this)
            }
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

      // receive metadata
      if ("metadata" in message) {
        if ("devices" in message.metadata) {
          let newDevices = {};
          for (let deviceParam of message.metadata.devices) {
            let newDevice = new Device(
              deviceParam["name"],
              deviceParam["comment"],
              deviceParam["producttype"],
              deviceParam["role"],
              deviceParam["online"], this
            );
            newDevices[newDevice.name] = newDevice;
            // TODO
            // device.receive({
            //   metadata: newDevice
            // });
          }
          this.devices = newDevices;
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
    if (this.status != "online") { // TODO why this if?
      this.websocketSubscription.unsubscribe();
      this.messages = null;
      this.devices = {};
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