import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import 'rxjs/add/operator/share';

const DEFAULT_URL = "ws://localhost:8085";
const DEFAULT_NAME = "fems";
//const DEFAULT = 'ws://localhost:80/websocket';
//const DEFAULT: string = "ws://" + location.hostname + ":" + location.port + "/websocket";

export class WebsocketContainer {
  websocket: WebSocket;
  subject: BehaviorSubject<any>;
  username: string;
  url: string;
  name: string;
}

@Injectable()
export class WebSocketService {
  public containers: { [url: string]: WebsocketContainer } = {};
  public containersChanged: BehaviorSubject<null> = new BehaviorSubject(null);

  public getDefault(): WebsocketContainer {
    return this.get(DEFAULT_NAME, DEFAULT_URL);
  }

  public get(name: string, url: string): WebsocketContainer {
    if (url in this.containers) {
      return this.containers[url];
    } else {
      return null;
    }
  }

  public getDefaultWithLogin(password: string): WebsocketContainer {
    return this.getWithLogin(DEFAULT_NAME, DEFAULT_URL, password);
  }

  public getWithLogin(name: string, url: string, password: string): WebsocketContainer {
    var container: WebsocketContainer = this.get(name, url);
    if (!container) {
      var container: WebsocketContainer = this.create(name, url, password, null);
      this.containers[url] = container;
      this.containersChanged.next(null);
      return container;
    }
  }

  public getDefaultWithToken(token: string): WebsocketContainer {
    return this.getWithToken(DEFAULT_NAME, DEFAULT_URL, token);
  }

  public getWithToken(name: string, url: string, token: string): WebsocketContainer {
    var subcontainerject: WebsocketContainer = this.get(name, url);
    if (!container) {
      var container: WebsocketContainer = this.create(name, url, null, token);
      this.containers[url] = container;
      this.containersChanged.next(null);
      return container;
    }
  }

  public closeDefault() {
    this.close(DEFAULT_URL);
  }

  public close(url: string) {
    if (url in this.containers) {
      console.log("Closing websocket[" + url + "]");
      var websocket: WebSocket = this.containers[url].websocket;
      if(websocket != null && websocket.readyState === WebSocket.OPEN) {
        this.containers[url].websocket.close()
      }
      delete this.containers[url];
      this.containersChanged.next(null);
    }
  }
  
  private create(name: string, url: string, password: string, token: string): WebsocketContainer {
    var ws = new WebSocket(url);
    
    let observable = Observable.create((obs: Observer<MessageEvent>) => {
      ws.onmessage = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);

      return ws.close.bind(ws);
    }).share();

    let observer = {
      next: (data: Object) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      },
    };

    ws.onopen = () => {
      // Authenticate
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

    var subject: BehaviorSubject<any> = BehaviorSubject.create(observer, observable);
    subject.subscribe((message: any) => {
      if ("data" in message) {
        let data = JSON.parse(message.data);

        // Receive authentication token
        if ("authenticate" in data) {
          if ("token" in data.authenticate) {
            if ("username" in data.authenticate) {
              this.containers[url].username = data.authenticate.username
              this.containersChanged.next(null);
            }
          } else {
            // close websocket
            console.log("Authentication failed. Close websocket.");
            ws.close();
          }
        }
      }
    });

    return {
      websocket: ws,
      subject: subject,
      username: null,
      url: url,
      name: name
    };
  }
}