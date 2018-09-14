import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject, Observable, Subscription } from 'rxjs';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { WebSocketSubject, webSocket } from 'rxjs/webSocket';
import { map, retryWhen, takeUntil, filter, first, delay } from 'rxjs/operators';

import { environment as env } from '../../../environments';
import { Service } from './service';
import { Edge } from '../edge/edge';
import { Role } from '../type/role';
import { DefaultTypes } from '../service/defaulttypes';
import { DefaultMessages } from '../service/defaultmessages';

@Injectable()
export class Websocket {
  public static readonly TIMEOUT = 15000;
  private static readonly DEFAULT_EDGEID = 0;
  private static readonly DEFAULT_EDGENAME = "fems";

  // holds references of edge names (=key) to Edge objects (=value)
  private _edges: BehaviorSubject<{ [name: string]: Edge }> = new BehaviorSubject({});
  public get edges() {
    return this._edges;
  }

  // holds the currently selected edge
  private _currentEdge: BehaviorSubject<Edge> = new BehaviorSubject<Edge>(null);
  public get currentEdge() {
    return this._currentEdge;
  }

  private socket: WebSocketSubject<any>;
  public status: DefaultTypes.ConnectionStatus = "connecting";
  public isWebsocketConnected: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private username: string = "";
  // private messages: Observable<string>;
  private queryreply = new Subject<{ id: string[] }>();
  private stopOnInitialize: Subject<void> = new Subject<void>();

  // holds stream per edge (=key1) and message-id (=key2); triggered on message reply for the edge
  private replyStreams: { [edgeName: string]: { [messageId: string]: Subject<any> } } = {};

  // tracks which message id (=key) is connected with which edgeName (=value)
  private pendingQueryReplies: { [id: string]: string } = {};

  constructor(
    private router: Router,
    private service: Service,
  ) {
    // try to auto connect using token or session_id
    setTimeout(() => {
      this.connect();
    })
  }

  /**
   * Parses the route params and sets the current edge
   */
  public setCurrentEdge(route: ActivatedRoute): Subject<Edge> {
    let onTimeout = () => {
      // Timeout: redirect to index
      this.router.navigate(['/index']);
      subscription.unsubscribe();
    }

    let edgeName = route.snapshot.params["edgeName"];
    let subscription = this.edges
      .pipe(filter(edges => edgeName in edges),
        first(),
        map(edges => edges[edgeName]))
      .subscribe(edge => {
        if (edge == null || !edge.online) {
          onTimeout();
        } else {
          // set current edge
          this.currentEdge.next(edge);
          edge.markAsCurrentEdge();
        }
      }, error => {
        console.error("Error while setting current edge: ", error);
      })
    setTimeout(() => {
      let edge = this.currentEdge.getValue();
      if (edge == null || !edge.online) {
        onTimeout();
      }
    }, Websocket.TIMEOUT);
    return this.currentEdge;
  }

  /**
   * Clears the current edge
   */
  public clearCurrentEdge() {
    this.currentEdge.next(null);
  }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket. Called once by constructor
   */
  private connect(): BehaviorSubject<boolean> {
    if (this.socket != null) {
      return this.isWebsocketConnected;
    }

    if (env.debugMode) {
      console.info("Websocket connect to URL [" + env.url + "]");
    }

    this.socket = webSocket({
      url: env.url,
      openObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection opened");
          }
          this.isWebsocketConnected.next(true);
          if (this.status == 'online') {
            this.service.notify({
              message: "Connection lost. Trying to reconnect.", // TODO translate
              type: 'warning'
            });
            // TODO show spinners everywhere
            this.status = 'connecting';
          } else {
            this.status = 'waiting for authentication';
          }
        }
      },
      closeObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection closed");
          }
          this.isWebsocketConnected.next(false);
        }
      }
    });

    // this.socket = WebSocketSubject.create({
    //   url: env.url,
    //   openObserver: {
    //     next: (value) => {
    //       if (env.debugMode) {
    //         console.info("Websocket connection opened");
    //       }
    //       this.isWebsocketConnected.next(true);
    //       if (this.status == 'online') {
    //         this.service.notify({
    //           message: "Connection lost. Trying to reconnect.", // TODO translate
    //           type: 'warning'
    //         });
    //         // TODO show spinners everywhere
    //         this.status = 'connecting';
    //       } else {
    //         this.status = 'waiting for authentication';
    //       }
    //     }
    //   },
    //   closeObserver: {
    //     next: (value) => {
    //       if (env.debugMode) {
    //         console.info("Websocket connection closed");
    //       }
    //       this.isWebsocketConnected.next(false);
    //     }
    //   }
    // });

    this.socket.pipe(
      retryWhen(errors => {
        console.warn("Websocket was interrupted. Retrying in 2 seconds.");
        return errors.pipe(delay(2000));
      })).subscribe(message => {
        // called on every receive of message from server
        if (env.debugMode) {
          console.info("RECV", message);
        }

        /*
         * Authenticate
         */
        if ("authenticate" in message && "mode" in message.authenticate) {
          let mode = message.authenticate.mode;

          if (mode === "allow") {
            // authentication successful
            this.status = "online";

            if ("token" in message.authenticate) {
              // received login token -> save in cookie
              this.service.setToken(message.authenticate.token);
            }

          } else {
            // authentication denied -> close websocket
            this.status = "failed";
            this.service.removeToken();
            this.initialize();
            if (env.backend === "OpenEMS Backend") {
              if (env.production) {
                window.location.href = "/web/login?redirect=/m/index";
              } else {
                console.info("would redirect...");
              }
            } else if (env.backend === "OpenEMS Edge") {
              this.router.navigate(['/index']);
            }
          }
        }

        /*
         * Query reply
         */
        if ("messageId" in message && "ui" in message.messageId) {
          // Receive a reply with a message id -> find edge and forward to edges' replyStream
          let messageId = message.messageId.ui;
          for (let edgeName in this.replyStreams) {
            if (messageId in this.replyStreams[edgeName]) {
              this.replyStreams[edgeName][messageId].next(message);
              break;
            }
          }
        }

        /*
         * Metadata
         */
        if ("metadata" in message) {
          if ("edges" in message.metadata) {
            let edges = <DefaultTypes.MessageMetadataEdge[]>message.metadata.edges;
            let newEdges = {};
            for (let edge of edges) {
              let replyStream: { [messageId: string]: Subject<any> } = {};
              this.replyStreams[edge.name] = replyStream;
              let newEdge = new Edge(
                edge.id,
                edge.name,
                edge.comment,
                edge.producttype,
                ("version" in edge) ? edge["version"] : "0.0.0",
                Role.getRole(edge.role),
                edge.online,
                replyStream,
                this
              );
              newEdges[newEdge.name] = newEdge;
            }
            this.edges.next(newEdges);
          }
        }

        /*
         * receive notification
         */
        if ("notification" in message) {
          let notification = message.notification;
          let n: DefaultTypes.Notification;
          let notify: boolean = true;
          if ("code" in notification) {
            // handle specific notification codes - see Java source for details
            let code = notification.code;
            let params = notification.params;
            if (code == 100 /* edge disconnected -> mark as offline */) {
              let edgeId = params[0];
              if (edgeId in this.edges.getValue()) {
                this.edges.getValue()[edgeId].setOnline(false);
              }
            } else if (code == 101 /* edge reconnected -> mark as online */) {
              let edgeId = params[0];
              if (edgeId in this.edges.getValue()) {
                let edge = this.edges.getValue()[edgeId];
                edge.setOnline(true);
              }
            } else if (code == 103 /* authentication by token failed */) {
              let token: string = params[0];
              if (token !== "") {
                // remove old token
                this.service.removeToken();
              }
              // ask for authentication info
              this.status = "waiting for authentication";
              notify = false;
              setTimeout(() => {
                this.clearCurrentEdge();
                this.router.navigate(["/index"]);
              });
            }
          }
          if (notify) {
            this.service.notify(<DefaultTypes.Notification>notification);
          }
        }
      }, error => {
        console.error("Websocket error", error);
      }, () => {
        console.info("Websocket finished");
      })
    return this.isWebsocketConnected;
  }

  /**
   * Reset everything to default
   */
  private initialize() {
    this.stopOnInitialize.next();
    this.stopOnInitialize.complete();
    this.edges.next({});
  }

  /**
   * Opens the websocket and logs in
   */
  public logIn(password: string) {
    if (this.isWebsocketConnected.getValue()) {
      // websocket was connected
      this.send(DefaultMessages.authenticateLogin(password));
    } else {
      // websocket was NOT connected
      this.connect()
        .pipe(takeUntil(this.stopOnInitialize),
          filter(isConnected => isConnected),
          first())
        .subscribe(isConnected => {
          setTimeout(() => {
            this.send(DefaultMessages.authenticateLogin(password))
          }, 500);
        });
    }
  }

  /**
   * Logs out and closes the websocket
   */
  public logOut() {
    // TODO this is kind of working for now... better would be to not close the websocket but to handle session validity serverside
    this.send(DefaultMessages.authenticateLogout());
    this.status = "waiting for authentication";
    this.service.removeToken();
    this.initialize();
  }

  /**
   * Sends a message to the websocket
   */
  public send(message: any): void {
    if (env.debugMode) {
      console.info("SEND: ", message);
    }
    this.socket.next(message);
  }
}
