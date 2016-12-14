import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { LocalstorageService } from './localstorage.service';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';

const SUBSCRIBE: string = "fenecon_monitor_v1";

export class Connection {
  public isConnected: boolean = false;
  public username: string;
  public natures: Object;
  public websocket: WebSocket;
  public subject: BehaviorSubject<any>;
  public event: BehaviorSubject<string> = new BehaviorSubject(null);

  constructor(
    public name: string,
    public url: string,
    private localstorageService: LocalstorageService
  ) {}

  public connectWithPassword(password: string) {
    this.connect(password, null);
  }

  public connectWithToken() {
    var token = this.localstorageService.getToken(this.name);
    if(token) {
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
        let data = JSON.parse(message.data);

        // Receive authentication token
        if ("authenticate" in data) {
          if ("token" in data.authenticate) {
            this.localstorageService.setToken(this.name, data.authenticate.token);
            if ("username" in data.authenticate) {
              this.username = data.authenticate.username;
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
        if ("natures" in data) {
          this.natures = data.natures;
          console.log("Got natures: " + JSON.stringify(this.natures));
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
    this.natures = null;
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