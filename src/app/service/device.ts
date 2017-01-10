import { Subject } from 'rxjs/Subject';

import { Notification } from './webapp.service';
import { Websocket } from './websocket.service';

export class Device {

  public event = new Subject<Notification>();
  public address: string;

  constructor(
    public name: string,
    public websocket: Websocket
  ) {
    if (this.name == 'fems') {
      this.address = this.websocket.name;
    } else {
      this.address = this.websocket.name + ": " + this.name;
    }
  }

  public data: { [thing: string]: any } = {};
}