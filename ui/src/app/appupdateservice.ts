import { Injectable } from "@angular/core";
import { SwUpdate } from "@angular/service-worker";

import { Service } from "./shared/shared";

@Injectable({
  providedIn: 'root',
})
export class CheckForUpdateService {

  constructor(private update: SwUpdate,
    private service: Service
  ) { }

  init() {
    let userId: string;
    this.service.metadata.subscribe(entry => {
      userId = entry?.user?.id ?? null;
    });

    setInterval(async () => {
      const updateFound = await this.update.checkForUpdate();
      console.log(updateFound ? 'A new version is available.' : 'Already on the latest version.');

      if (updateFound) {
        window.location.reload()
      }
    }, 10000)
  }
}
// Will be used in Future
@Injectable()
export class LogUpdateService {

  constructor(updates: SwUpdate) {
    updates.versionUpdates.subscribe(evt => {
      switch (evt.type) {
        case 'VERSION_DETECTED':
          console.log(`Downloading new app version: ${evt.version.hash}`);
          break;
        case 'VERSION_READY':
          console.log(`Current app version: ${evt.currentVersion.hash}`);
          console.log(`New app version ready for use: ${evt.latestVersion.hash}`);
          break;
        case 'VERSION_INSTALLATION_FAILED':
          console.log(`Failed to install app version '${evt.version.hash}': ${evt.error}`);
          break;
      }
    });
  }
}