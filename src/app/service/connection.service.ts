import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import 'rxjs/add/operator/share';
import { LocalstorageService } from './localstorage.service';
import { Connection } from './connection';
import { ToastsManager } from 'ng2-toastr/ng2-toastr';

export { Connection } from './connection';

const DEFAULT_CONNECTIONS = [{
  name: "Trafostation 1",
  url: "ws://localhost:8085"
}, {
  name: "Trafostation 2",
  url: "ws://localhost:8095"
}
];

const DEFAULT_PASSWORD: string = "guest";
//const DEFAULT = 'ws://localhost:80/websocket';
//const DEFAULT: string = "ws://" + location.hostname + ":" + location.port + "/websocket";

@Injectable()
export class ConnectionService {
  public connections: { [name: string]: Connection } = {};
  public connectionsChanged: BehaviorSubject<null> = new BehaviorSubject(null);

  constructor(
    private localstorageService: LocalstorageService,
    private toastr: ToastsManager
  ) {
    for (var connection of DEFAULT_CONNECTIONS) {
      // load default connections
      var conn = new Connection(connection.name, connection.url, localstorageService, toastr);
      this.connections[connection.name] = conn
      // try to connect using token
      conn.connectWithToken();
    }
    this.connectionsChanged.next(null);
  }

  public closeConnection(connection: Connection) {
    this.close(connection.url);
  }

  /**
   * Closes the connection to the given url.
   * 
   * Replaces an ActiveConnection with a Connection in this.connections
   */
  public close(name: string) {
    if (name in this.connections) {
      var connection: Connection = this.connections[name];
      console.log("Closing websocket[" + name + "; " + connection.url + "]");
      connection.close();
      this.connectionsChanged.next(null);
    }
  }
}