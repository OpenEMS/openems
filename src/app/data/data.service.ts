import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import { WebSocketService } from './websocket.service';

//const WEBSOCKET_URL = 'ws://localhost:8085';
const WEBSOCKET_URL ="ws://" + location.hostname + "/websocket";

@Injectable()
export class DataService {
  public messages: Subject<any>;

  constructor(wsService: WebSocketService) {

    this.messages = <Subject<any>>wsService
      .connect(WEBSOCKET_URL, "owner", "fenecon_monitor_v1")
      .map((response: MessageEvent): any => {
        let data = JSON.parse(response.data);
        return data;
      });
  }
}
