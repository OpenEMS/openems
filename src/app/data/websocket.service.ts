import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { Subject } from 'rxjs/Subject';

@Injectable()
export class WebSocketService {
  private subject: Subject<MessageEvent>;

  public connect(url:string, password:string, subscribe:string): Subject<MessageEvent> {
    if (!this.subject) {
      this.subject = this.create(url, password, subscribe);
    }

    return this.subject;
  }

  private create(url:string, password:string, subscribe:string): Subject<MessageEvent> {
    let ws = new WebSocket(url);
    ws.onopen = () => {
      ws.send(JSON.stringify({
        authenticate: {
          password: password
        },
        subscribe: subscribe
      }))
    };

    let observable = Observable.create((obs: Observer<MessageEvent>) => {
      ws.onmessage = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);

      return ws.close.bind(ws);
    });

    let observer = {
      next: (data: Object) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      },
    };

    return Subject.create(observer, observable);
  }
}