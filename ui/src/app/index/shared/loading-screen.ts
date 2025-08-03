// @ts-strict-ignore
import { Component, effect, inject } from "@angular/core";
import { Router } from "@angular/router";

import { AppStateTracker } from "src/app/shared/ngrx-store/states";
import { Environment, environment } from "src/environments";
import { Service, Websocket } from "../../shared/shared";

@Component({
  selector: "index",
  templateUrl: "./loading-screen.html",
  standalone: false,
})
export class LoadingScreenComponent {
  service = inject(Service);
  websocket = inject(Websocket);
  private router = inject(Router);
  private appStateTracker = inject(AppStateTracker);


  protected readonly spinnerId: string = "IndexComponent";
  protected readonly environment: Environment = environment;
  protected backendState: "loading" | "failed" | "authenticated" = "loading";

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {

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
