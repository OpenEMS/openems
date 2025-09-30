import { Injectable } from "@angular/core";
import { SwUpdate } from "@angular/service-worker";

import { Service } from "./shared/shared";

@Injectable({
  providedIn: "root",
})
export class CheckForUpdateService {

  constructor(private update: SwUpdate,
    private service: Service,
  ) { }
}
// Will be used in Future
@Injectable()
export class LogUpdateService {

  constructor(updates: SwUpdate) {
    UPDATES.VERSION_UPDATES.SUBSCRIBE(evt => {
      switch (EVT.TYPE) {
        case "VERSION_DETECTED":
          CONSOLE.LOG(`Downloading new app version: ${EVT.VERSION.HASH}`);
          break;
        case "VERSION_READY":
          CONSOLE.LOG(`Current app version: ${EVT.CURRENT_VERSION.HASH}`);
          CONSOLE.LOG(`New app version ready for use: ${EVT.LATEST_VERSION.HASH}`);
          break;
        case "VERSION_INSTALLATION_FAILED":
          CONSOLE.LOG(`Failed to install app version '${EVT.VERSION.HASH}': ${EVT.ERROR}`);
          break;
        default:
          break;
      }
    });
  }
}
