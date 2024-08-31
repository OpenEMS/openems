// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";

import { Service, Websocket } from "../../shared/shared";

@Component({
  selector: "index",
  templateUrl: "./loading-screen.html",
})
export class LoadingScreenComponent implements OnInit {

  protected readonly spinnerId: string = "IndexComponent";

  constructor(
    public service: Service,
    public websocket: Websocket,
    private router: Router,
  ) { }

  ngOnInit() {

    // TODO add websocket status observable
    const interval = setInterval(() => {
      this.service.startSpinner(this.spinnerId);
      if (this.websocket.status === "online") {
        this.service.stopSpinner(this.spinnerId);
        this.router.navigate(["/overview"]);
        clearInterval(interval);
      }
      if (this.websocket.status === "waiting for credentials") {
        this.service.stopSpinner(this.spinnerId);
        this.router.navigate(["/login"]);
        clearInterval(interval);
      }
    }, 1000);
  }
}
