import { Injectable, WritableSignal, effect, signal } from "@angular/core";
import { Router } from "@angular/router";

import { differenceInSeconds } from "date-fns";
import { environment } from "src/environments";
import { RouteService } from "../service/ROUTE.SERVICE";
import { Websocket } from "../shared";

export enum States {
    WEBSOCKET_CONNECTION_CLOSED,
    WEBSOCKET_NOT_YET_CONNECTED,
    WEBSOCKET_CONNECTING,
    WEBSOCKET_CONNECTED,

    // TODO substates
    NOT_AUTHENTICATED,
    AUTHENTICATING_WITH_TOKEN,
    AUTHENTICATION_WITH_CREDENTIALS,
    AUTHENTICATED,
    EDGE_SELECTED,
}

@Injectable({
    providedIn: "root",
})
export class AppStateTracker {
    private static readonly LOG_PREFIX: string = "AppState";
    private static readonly TIME_TILL_TIMEOUT: number = 10;
    private static readonly ENABLE_ROUTING: boolean = true;
    public loadingState: WritableSignal<"failed" | "loading" | "authenticated"> = signal("loading");
    private lastTimeStamp: Date | null = null;

    constructor(
        protected router: Router,
        private websocket: Websocket,
        private routeService: RouteService,
    ) {
        if (!LOCAL_STORAGE.GET_ITEM("AppState")) {
            CONSOLE.LOG(`${AppStateTracker.LOG_PREFIX} Log deactivated`);
        }

        if (!AppStateTracker.ENABLE_ROUTING) {
            CONSOLE.LOG(`${AppStateTracker.LOG_PREFIX} Routing deactivated`);
        }

        effect(() => {
            const state = THIS.WEBSOCKET.STATE();
            THIS.START_STATE_HANDLER(state);
        });
    }

    /**
     * Handles navigation after authentication
     */
    public navigateAfterAuthentication() {

        THIS.ROUTER.NAVIGATE(["overview"]);
        return;
        // const segments = THIS.ROUTER.ROUTER_STATE.SNAPSHOT.URL.SPLIT("/");
        // const previousUrl: string = THIS.ROUTE_SERVICE.GET_PREVIOUS_URL();

        // if ((previousUrl === segments[SEGMENTS.LENGTH - 1]) || previousUrl === "/") {
        //     THIS.ROUTER.NAVIGATE(["./overview"]);
        //     return;
        // }

        // THIS.ROUTER.NAVIGATE(PREVIOUS_URL.SPLIT("/"));
    }

    private startStateHandler(state: States): void {

        if (ENVIRONMENT.DEBUG_MODE && LOCAL_STORAGE.GET_ITEM("AppState")) {
            CONSOLE.LOG(`${AppStateTracker.LOG_PREFIX} [${States[THIS.WEBSOCKET.STATE()]}]`);
        }

        if (!AppStateTracker.ENABLE_ROUTING) {
            return;
        }

        switch (state) {
            case States.WEBSOCKET_CONNECTING:
                THIS.LAST_TIME_STAMP = THIS.HANDLE_WEB_SOCKET_CONNECTING(THIS.LAST_TIME_STAMP);
                break;
            case States.WEBSOCKET_CONNECTION_CLOSED:
                break;
            case STATES.AUTHENTICATED:
                THIS.LOADING_STATE.SET("authenticated");
                break;
            default:
                THIS.LAST_TIME_STAMP = null;
                break;
        }
    }


    private handleWebSocketConnecting(lastTimeStamp: Date | null): Date | null {
        const now = new Date();
        if (lastTimeStamp === null) {
            return now;
        }

        if (differenceInSeconds(now, lastTimeStamp) > AppStateTracker.TIME_TILL_TIMEOUT) {
            CONSOLE.WARN(`Websocket connection couldnt be established in ${AppStateTracker.TIME_TILL_TIMEOUT}s`);
            THIS.LOADING_STATE.SET("failed");
            THIS.ROUTER.NAVIGATE(["index"]);
            return null;
        }

        return lastTimeStamp;
    }
}
