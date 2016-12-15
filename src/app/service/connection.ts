import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { LocalstorageService } from './localstorage.service';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { Device } from './device';

export { Device } from './device';

const SUBSCRIBE: string = "fenecon_monitor_v1";

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
  public subject: BehaviorSubject<any>;
  public event: BehaviorSubject<string> = new BehaviorSubject(null);
  public devices: { [id: string]: Device } = {};
  public influxdb: InfluxdbPersistence;

  constructor(
    public name: string,
    public url: string,
    private localstorageService: LocalstorageService
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
              this.subscribeNatures();
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

        // Receive natures
        if ("natures" in msg) {
          this.devices = {}
          for (let id in msg.natures) {
            var device = new Device();
            device.name = id;
            device.natures = msg.natures[id];
            this.devices[id] = device;
          }
        }

        // Receive natures
        if ("persistences" in msg) {
          for (let pers in msg.persistences) {
            var persistence = msg.persistences[pers];
            if ("class" in persistence) {
              var clazz = persistence.class;
              if (clazz == "InfluxdbPersistence") {
                var ip = persistence.ip;
                if(ip == "127.0.0.1") {
                  ip = location.hostname;
                }
                this.influxdb = new InfluxdbPersistence();
                this.influxdb.ip = ip;
                this.influxdb.username = persistence.username;
                this.influxdb.password = persistence.password;
                this.influxdb.fems = persistence.fems;
              }
            }
          }
        }

        // Receive data
        if ("data" in msg) {
          var data = msg.data;
          for (let id in data) {
            var channels = data[id];
            if (id in this.devices) {
              for (let channelid in channels) {
                var channel = channels[channelid];
                this.devices[id][channelid] = channel;
              }
            }
          }
          //console.log(this.devices);
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
    this.influxdb = null;
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
}