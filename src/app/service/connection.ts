import { BehaviorSubject } from 'rxjs/BehaviorSubject';

export class Connection {
  constructor(
    public name: string,
    public url: string
  ) {}
}

export class ActiveConnection extends Connection {

  constructor(
    name: string,
    url: string,
    public websocket: WebSocket,
    public subject: BehaviorSubject<any>,
    public username: string
  ) {
    super(name, url);
  }

  public send(value: any): void {
    this.subject.next(value);
  }
}