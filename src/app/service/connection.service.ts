import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import 'rxjs/add/operator/share';
import { LocalstorageService } from './localstorage.service';

const SUBSCRIBE: string = "fenecon_monitor_v1";

export class Connection {
  constructor(
    public name: string,
    public url: string
  ) { }
}

export class ActiveConnection extends Connection {
  public username: string;
  public natures: Object;

  constructor(
    name: string,
    url: string,
    public websocket: WebSocket,
    public subject: BehaviorSubject<any>
  ) {
    super(name, url);

    // make sure to subscribe to openems natures on init; 
    // TODO: on first init it happens twice
    this.subscribeNatures();

    subject.subscribe((message: any) => {
      this.subscribeNatures();
      if ("data" in message) {
        var msg: any = JSON.parse(message.data);
        // Natures
        if ("natures" in msg) {
          console.log("got natures: " + this.natures);
          this.natures = msg.natures;
        }
      }
    });
  }

  public send(value: any): void {
    this.subject.next(value);
  }

  /**
   * Send "subscribe" message to server 
   */
  private subscribeNatures() {
    if (this.natures == null) {
      this.send({
        subscribe: SUBSCRIBE
      });
    }
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

const DEFAULT_CONNECTION: Connection = {
  name: "fems",
  url: "ws://localhost:8085"
}

const DEFAULT_PASSWORD: string = "guest";
//const DEFAULT = 'ws://localhost:80/websocket';
//const DEFAULT: string = "ws://" + location.hostname + ":" + location.port + "/websocket";

@Injectable()
export class ConnectionService {
  public connections: { [url: string]: Connection } = {};
  public connectionsChanged: BehaviorSubject<null> = new BehaviorSubject(null);

  constructor(
    private localstorageService: LocalstorageService
  ) {
    this.connections[DEFAULT_CONNECTION.url] = DEFAULT_CONNECTION;
    this.connectionsChanged.next(null);
  }

  public getDefault(): Connection {
    return this.get(DEFAULT_CONNECTION);
  }

  public get(connection: Connection): Connection {
    return this.getOrCreate(connection, null, null);
  }

  public getDefaultWithLogin(password: string): Connection {
    return this.getWithLogin(DEFAULT_CONNECTION, password);
  }

  public getWithLogin(connection: Connection, password: string): Connection {
    this.closeConnection(connection);
    return this.getOrCreate(connection, password, null);
  }

  /**
   * Gets default connection using session token
   */
  public getDefaultWithToken(token: string): Connection {
    return this.getWithToken(DEFAULT_CONNECTION, token);
  }

  /**
   * Tries to open a connection using a session token. Closes the existing connection if existent. 
   */
  public getWithToken(connection: Connection, token: string): Connection {
    this.closeConnection(connection);
    return this.getOrCreate(connection, null, token);
  }

  /**
   * Closes the default connection
   */
  public closeDefault() {
    this.closeConnection(DEFAULT_CONNECTION);
  }

  public closeConnection(connection: Connection) {
    this.close(DEFAULT_CONNECTION.url);
  }

  /**
   * Closes the connection to the given url.
   * 
   * Replaces an ActiveConnection with a Connection in this.connections
   */
  public close(url: string) {
    if (url in this.connections) {
      console.log("Closing websocket[" + url + "]");
      var connection: Connection = this.connections[url];
      if (connection instanceof ActiveConnection) {
        var websocket = connection.websocket;
        if (websocket != null && websocket.readyState === WebSocket.OPEN) {
          websocket.close()
        }
        this.connections[url] = {
          name: connection.name,
          url: connection.url
        }
        this.connectionsChanged.next(null);
      }
    }
  }

  /**
   * Gets an ActiveConnection if one is existing. Otherwise tries to connect to a connection,
   * using given password or token and adds it to this.connections. Otherwise returns a closed 
   * Connection object 
   */
  private getOrCreate(connection: Connection, password: string, token: string): Connection {
    // return an existing ActiveConnection
    if (connection.url in this.connections) {
      var conn: Connection = this.connections[connection.url];
      if (conn instanceof ActiveConnection) {
        return conn;
      }
    }

    // try to get token from local storage if none was provided
    if (token == null) {
      token = this.localstorageService.getToken();
    }

    // return non-active Connection if no password or token was given
    if (password == null && token == null) {
      return connection;
    }

    // create a new connection
    var ws = new WebSocket(connection.url);

    // define observable
    let observable = Observable.create((obs: Observer<MessageEvent>) => {
      ws.onmessage = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);

      return ws.close.bind(ws);
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
    var subject: BehaviorSubject<any> = BehaviorSubject.create(observer, observable);

    subject.subscribe((message: any) => {
      if ("data" in message) {
        let data = JSON.parse(message.data);

        // Receive authentication token
        if ("authenticate" in data) {
          if ("token" in data.authenticate) {
            this.localstorageService.setToken(data.authenticate.token);
            if ("username" in data.authenticate) {
              var conn: Connection = this.connections[connection.url];
              if (conn instanceof ActiveConnection) {
                conn.username = data.authenticate.username
                this.connectionsChanged.next(null);
              }
            }
          } else {
            // close websocket
            this.localstorageService.removeToken();
            console.log("Authentication failed. Close websocket.");
            ws.close();
          }
        }
      }
    });

    // create ActiveConnection, save it and announce listeners
    var activeConn = new ActiveConnection(
      connection.name,
      connection.url,
      ws,
      subject
    );
    this.connections[activeConn.url] = activeConn;
    this.connectionsChanged.next(null);

    return activeConn;
  }
}