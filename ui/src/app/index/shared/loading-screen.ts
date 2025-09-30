// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { Router } from "@angular/router";

import { AppStateTracker } from "src/app/shared/ngrx-store/states";
import { Environment, environment } from "src/environments";
import { Service, Websocket } from "../../shared/shared";

@Component({
  selector: "index",
  templateUrl: "./loading-SCREEN.HTML",
  standalone: false,
})
export class LoadingScreenComponent {

  protected readonly spinnerId: string = "IndexComponent";
  protected readonly environment: Environment = environment;
  protected backendState: "loading" | "failed" | "authenticated" = "loading";

  constructor(
    public service: Service,
    public websocket: Websocket,
    private router: Router,
    private appStateTracker: AppStateTracker,
  ) {

    effect(() => {
      THIS.BACKEND_STATE = THIS.APP_STATE_TRACKER.LOADING_STATE();
      switch (THIS.BACKEND_STATE) {
        case "loading":
          THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
          break;
        case "failed":
          THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
          break;
        case "authenticated":
          THIS.APP_STATE_TRACKER.NAVIGATE_AFTER_AUTHENTICATION();
          break;
      }
    });
  }
}
