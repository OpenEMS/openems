import { Injectable, WritableSignal, effect, signal } from "@angular/core";
import { Router } from "@angular/router";

import { differenceInSeconds } from "date-fns";
import { environment } from "src/environments";
import { Websocket } from "../shared";

export enum States {
    WEBSOCKET_CONNECTION_CLOSED,
    WEBSOCKET_NOT_YET_CONNECTED,
    WEBSOCKET_CONNECTING,
    WEBSOCKET_CONNECTED,
    AUTHENTICATING,

    // TODO substates
    AUTHENTICATION_FAILED,
    NOT_AUTHENTICATED,
    AUTHENTICATING_WITH_TOKEN,
    AUTHENTICATION_WITH_CREDENTIALS,
    AUTHENTICATED,
    EDGE_SELECTED,
}

export namespace States {
    /**
    * Evaluates whether "State 1" is equal or more privileged than "State 2".
    *
    * @param state1     the State 1
    * @param state2     the State 2
    * @return true if "State 1" is equal or more privileged than "State 2"
    */
    export function isAtLeast(state1: States | null, state2: States | null): boolean {
        if (state1 == null || state2 == null) {
            return false;
        }
        return state1 >= state2;
    }
}

@Injectable({
    providedIn: "root",
})
export class AppStateTracker {
    private static readonly LOG_PREFIX: string = "AppState";
    private static readonly TIME_TILL_TIMEOUT: number = 10;
    private static readonly ENABLE_ROUTING: boolean = true;
    public loadingState: WritableSignal<"failed" | "loading" | "authenticated" | "not_authenticated"> = signal("loading");
    private lastTimeStamp: Date | null = null;

    constructor(
        protected router: Router,
        private websocket: Websocket,
    ) {
        if (!localStorage.getItem("AppState")) {
            console.log(`${AppStateTracker.LOG_PREFIX} Log deactivated`);
        }

        if (!AppStateTracker.ENABLE_ROUTING) {
            console.log(`${AppStateTracker.LOG_PREFIX} Routing deactivated`);
        }

        effect(() => {
            const state = this.websocket.state();
            this.startStateHandler(state);
        });
    }

    /**
     * Handles navigation after authentication
     */
    public navigateAfterAuthentication() {
        // this.router.navigate(["overview"]);
        return;
        // const segments = this.router.routerState.snapshot.url.split("/");
        // const previousUrl: string = this.routeService.getPreviousUrl();

        // if ((previousUrl === segments[segments.length - 1]) || previousUrl === "/") {
        //     this.router.navigate(["./overview"]);
        //     return;
        // }

        // this.router.navigate(previousUrl.split("/"));
    }

    private startStateHandler(state: States): void {

        if (environment.debugMode && localStorage.getItem("AppState")) {
            console.log(`${AppStateTracker.LOG_PREFIX} [${States[this.websocket.state()]}]`);
        }

        if (!AppStateTracker.ENABLE_ROUTING) {
            return;
        }

        switch (state) {
            case States.AUTHENTICATION_FAILED:
                this.loadingState.set("failed");
                break;
            case States.WEBSOCKET_CONNECTING:
                this.lastTimeStamp = this.handleWebSocketConnecting(this.lastTimeStamp);
                break;
            case States.WEBSOCKET_CONNECTED:
                this.loadingState.set("not_authenticated");
                break;
            case States.AUTHENTICATED:
                this.loadingState.set("authenticated");
                this.navigateAfterAuthentication();
                break;
            default:
                this.lastTimeStamp = null;
                break;
        }
    }

    private handleWebSocketConnecting(lastTimeStamp: Date | null): Date | null {
        const now = new Date();
        if (lastTimeStamp === null) {
            return now;
        }

        if (differenceInSeconds(now, lastTimeStamp) > AppStateTracker.TIME_TILL_TIMEOUT) {
            console.warn(`Websocket connection couldnt be established in ${AppStateTracker.TIME_TILL_TIMEOUT}s`);
            this.loadingState.set("failed");
            return null;
        }

        return lastTimeStamp;
    }
}
