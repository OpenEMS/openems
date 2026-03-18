// @ts-strict-ignore
import { Component, effect } from "@angular/core";

import { AppStateTracker } from "src/app/shared/ngrx-store/states";
import { TSignalValue } from "src/app/shared/type/utility";
import { Environment, environment } from "src/environments";
import { Service, Websocket } from "../../shared/shared";

@Component({
    selector: "index",
    templateUrl: "./loading-screen.html",
    standalone: false,
})
export class LoadingScreenComponent {

    protected readonly spinnerId: string = "loading-screen";
    protected readonly environment: Environment = environment;
    protected backendState: TSignalValue<AppStateTracker["loadingState"]>;

    constructor(
        public service: Service,
        public websocket: Websocket,
        private appStateTracker: AppStateTracker,
    ) {

        effect(() => {
            this.backendState = appStateTracker.loadingState();
            switch (this.backendState) {
                case "loading":
                    this.service.startSpinner(this.spinnerId);
                    break;
                case "failed":
                    this.service.stopSpinner(this.spinnerId);
                    break;
                case "not_authenticated":
                    break;
                default:
                    this.service.stopSpinner(this.spinnerId);
            }
        });
    }
}
