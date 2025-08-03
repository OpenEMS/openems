import { Injectable, inject } from "@angular/core";
import { SwUpdate } from "@angular/service-worker";

import { Service } from "./shared/shared";

@Injectable({
  providedIn: "root",
})
export class CheckForUpdateService {
  private update = inject(SwUpdate);
  private service = inject(Service);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() { }
}
// Will be used in Future
@Injectable()
export class LogUpdateService {

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() {
    const updates = inject(SwUpdate);

    updates.versionUpdates.subscribe(evt => {
      switch (evt.type) {
        case "VERSION_DETECTED":
          console.log(`Downloading new app version: ${evt.version.hash}`);
          break;
        case "VERSION_READY":
          console.log(`Current app version: ${evt.currentVersion.hash}`);
          console.log(`New app version ready for use: ${evt.latestVersion.hash}`);
          break;
        case "VERSION_INSTALLATION_FAILED":
          console.log(`Failed to install app version '${evt.version.hash}': ${evt.error}`);
          break;
        default:
          break;
      }
    });
  }
}
