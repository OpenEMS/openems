import { Injectable, WritableSignal, effect, signal } from "@angular/core";
import { Router } from "@angular/router";

import { differenceInSeconds } from "date-fns";
import { environment } from "src/environments";
import { Pagination } from "../service/pagination";
import { PreviousRouteService } from "../service/previousRouteService";
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
        protected pagination: Pagination,
        private websocket: Websocket,
        private previousRouteService: PreviousRouteService,
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
        }, { allowSignalWrites: true });
    }

    /**
     * Handles navigation after authentication
     */
    public navigateAfterAuthentication() {
        const segments = this.router.routerState.snapshot.url.split("/");
        const previousUrl: string = this.previousRouteService.getPreviousUrl();

        if ((previousUrl === segments[segments.length - 1]) || previousUrl === "/") {
            this.router.navigate(["./overview"]);
            return;
        }

        this.router.navigate(previousUrl.split("/"));
    }

    private startStateHandler(state: States): void {

        if (environment.debugMode && localStorage.getItem("AppState")) {
            console.log(`${AppStateTracker.LOG_PREFIX} [${States[this.websocket.state()]}]`);
        }

        if (!AppStateTracker.ENABLE_ROUTING) {
            return;
        }

        switch (state) {
            case States.WEBSOCKET_CONNECTING:
                this.lastTimeStamp = this.handleWebSocketConnecting(this.lastTimeStamp);
                break;
            case States.WEBSOCKET_CONNECTION_CLOSED:
                break;
            case States.AUTHENTICATED:
                this.loadingState.set("authenticated");
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
            this.router.navigate(["index"]);
            return null;
        }

        return lastTimeStamp;
    }
}
