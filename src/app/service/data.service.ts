import { Injectable } from '@angular/core';
import { WebSocketService, WebsocketContainer } from './websocket.service';
import { LocalstorageService } from './localstorage.service';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Injectable()
export class DataService {
  private container: WebsocketContainer;

  constructor(
    private websocket: WebSocketService,
    private localstorage: LocalstorageService) {
  }

  getWebsocketContainer(): WebsocketContainer {
    return this.getWebsocketContainerWithLogin(null);
  }

  getWebsocketContainerWithLogin(password: string): WebsocketContainer {
    // always close subject if password is given
    if (password) {
      this.container = null;
    }

    if (this.container) {
      /*
       * return existing subject
       */
      return this.container;

    } else {
      /*
       * create new subject
       */
      // get container
      if (password) {
        this.container = this.websocket.getDefaultWithLogin(password);
      } else {
        var token: string = this.localstorage.getToken();
        if (token) {
          this.container = this.websocket.getDefaultWithToken(token);
        } else {
          this.container = null;
        }
      }

      // authenticate
      if (this.container && this.container.subject) {
        this.container.subject.subscribe(message => {
          if ("data" in message) {
            let data = JSON.parse(message.data);
            if ("authenticate" in data) {
              if ("token" in data.authenticate) {
                // store token in local storage
                this.localstorage.setToken(data.authenticate.token);
              } else {
                // delete token from local storage
                console.log("Authentication failed. Delete session token.");
                this.localstorage.removeToken();
              }
            }
          }
        }, error => {
          // error + close subject
          console.error(JSON.stringify(error));
          this.container = null;
        }, (/* complete */) => {
          // close subject
          this.container = null;
        });
      }
      return this.container;
    }
  }
}
