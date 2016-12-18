import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { LocalstorageService } from './localstorage.service';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { Device } from './device';
import { ToastsManager } from 'ng2-toastr/ng2-toastr';

export { Device } from './device';

const SUBSCRIBE: string = "fenecon_monitor_v1";

class ThingConfig {
  id: String;
  class: String;
}

class SchedulerConfig extends ThingConfig {
  controllers: Object[] = [];
}

class OpenemsConfig {
  _devices: { [id: string]: Device } = {};
  things: ThingConfig[] = [];
  scheduler: SchedulerConfig = new SchedulerConfig();
  persistence: Object[] = [];

  public getInfluxdbPersistence(): InfluxdbPersistence {
    for(let persistence of this.persistence) {
      if(persistence instanceof InfluxdbPersistence) {
        return persistence as InfluxdbPersistence;
      }
    };
    return null;
  }
}

interface Notification {
  type: string;
  message: string;
}

export class InfluxdbPersistence {
  ip: string;
  username: string;
  password: string;
  fems: number;
}

export class Connection {
  public isConnected: boolean = false;
  public username: string;
  public websocket: WebSocket;
  public subject: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  public event: BehaviorSubject<string> = new BehaviorSubject(null);
  public config: OpenemsConfig = new OpenemsConfig();
  public data: Object = {};

  constructor(
    public name: string,
    public url: string,
    private localstorageService: LocalstorageService,
    private toastr: ToastsManager
  ) { }

  public connectWithPassword(password: string) {
    this.connect(password, null);
  }

  public connectWithToken() {
    var token = this.localstorageService.getToken(this.name);
    if (token) {
      this.connect(null, token);
    }
  }

  /**
   * Tries to connect using given password or token.
   */
  private connect(password: string, token: string) {
    // return non-active Connection if no password or token was given
    if (password == null && token == null) {
      this.initialize();
      return;
    }

    // Error description is here:
    var error = null;

    // send "not successful event" if not connected within timeout
    var timeout = setTimeout(() => {
      if (!this.isConnected) {
        error = "Keine Verbindung: Timeout"
        this.event.next(error);
      }
    }, 2000);

    // create a new connection
    var ws = new WebSocket(this.url);

    // define observable
    let observable = Observable.create((obs: Observer<MessageEvent>) => {
      ws.onmessage = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);

      ws.close.bind(ws);
    }).share();

    // define observer
    let observer = {
      next: (data: Object) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      },
    };

    // Authenticate when websocket is opened
    ws.onopen = () => {
      if (password) {
        observer.next({
          authenticate: { password: password }
        });
      } else if (token) {
        observer.next({
          authenticate: { token: token }
        });
      }
    };

    // create subject
    var sj: BehaviorSubject<any> = BehaviorSubject.create(observer, observable);

    sj.subscribe((message: any) => {
      if ("data" in message) {
        let msg = JSON.parse(message.data);

        // Receive authentication token
        if ("authenticate" in msg) {
          if ("token" in msg.authenticate) {
            this.localstorageService.setToken(this.name, msg.authenticate.token);
            if ("username" in msg.authenticate) {
              this.username = msg.authenticate.username;
              this.websocket = ws;
              this.subject = sj;
              this.isConnected = true;
              error = null;
              //this.subscribeNatures();
              this.event.next("Angemeldet als " + this.username + ".");
            }
          } else {
            // close websocket
            this.localstorageService.removeToken(this.name);
            this.initialize();
            clearTimeout(timeout);
            error = "Keine Verbindung: Authentifizierung fehlgeschlagen.";
            this.event.next(error);
          }
        }

        // Receive config
        if ("config" in msg) {
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
            for(let persistence of msg.config.persistence) {
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
          this.event.next(null);
        }

        // Receive data
        if ("data" in msg) {
          var data = msg.data;
          for (let id in data) {
            var channels = data[id];
            if (id in this.config._devices) {
              this.data[id] = {};
              for (let channelid in channels) {
                var channel = channels[channelid];
                this.data[id][channelid] = channel;
              }
            }
          }
          console.log(this.data);
        }

        // receive notification
        if ("notification" in msg) {
          this.showNotification(msg.notification);
        }
      }
    }, (error: any) => {
      this.initialize();
      clearTimeout(timeout);
      if (error == null) {
        error = "Verbindungsfehler."
        this.event.next(error);
      }
    }, (/* complete */) => {
      this.initialize();
      clearTimeout(timeout);
      if (error == null) {
        error = "Verbindung beendet."
        this.event.next(error);
      }
    });
  }

  public send(value: any): void {
    this.subject.next(value);
  }

  /**
   * Closes the connection.
   */
  public close() {
    this.localstorageService.removeToken(this.name);
    this.initialize();
  }

  private initialize() {
    if (this.websocket != null && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.close();
    }
    this.websocket = null;
    this.isConnected = false;
    this.username = null;
    this.config = new OpenemsConfig();
    this.subject = new BehaviorSubject<any>(null);
    this.data = {};
  }

  /**
   * Send "subscribe" message to server 
   */
  private subscribeNatures() {
    this.send({
      subscribe: SUBSCRIBE
    });
  }

  /**
   * send "unsubscribe" message to server
   */
  private unsubscribeNatures() {
    this.send({
      subscribe: ""
    });
  }

  private showNotification(notification: Notification) {
    if(notification.type == "success") {
      this.toastr.success(notification.message);
    } else if(notification.type == "error") {
      this.toastr.error(notification.message);
    } else if(notification.type == "warning") {
      this.toastr.warning(notification.message);
    } else {
      this.toastr.info(notification.message);
    }
  }
}