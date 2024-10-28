// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { Router } from "@angular/router";

import { AppStateTracker } from "src/app/shared/ngrx-store/states";
import { Environment, environment } from "src/environments";
import { Service, Websocket } from "../../shared/shared";

@Component({
  selector: "index",
  templateUrl: "./loading-screen.html",
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
      this.backendState = this.appStateTracker.loadingState();
      switch (this.backendState) {
        case "loading":
          this.service.startSpinner(this.spinnerId);
          break;
        case "failed":
          this.service.stopSpinner(this.spinnerId);
          break;
        case "authenticated":
          this.appStateTracker.navigateAfterAuthentication();
          break;
      }
    });
  }
}
