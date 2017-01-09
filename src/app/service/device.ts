import { Subject } from 'rxjs/Subject';
import { Notification } from './webapp.service';

export class Device {

  public event = new Subject<Notification>();

  constructor(
    public name: string
  ) { }

  public data: { [thing: string]: any } = {};
}