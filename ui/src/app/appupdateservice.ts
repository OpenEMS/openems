import { inject, Injectable } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
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

    private update: SwUpdate = inject(SwUpdate);

    constructor() {
        this.update.versionUpdates.pipe(takeUntilDestroyed()).subscribe(evt => {
            switch (evt.type) {
                case "VERSION_DETECTED":
                    console.info(`Downloading new app version: ${evt.version.hash}`);
                    break;
                case "VERSION_READY":
                    console.info(`Current app version: ${evt.currentVersion.hash}`);
                    console.info(`New app version ready for use: ${evt.latestVersion.hash}`);
                    break;
                case "VERSION_INSTALLATION_FAILED":
                    console.warn(`Failed to install app version '${evt.version.hash}': ${evt.error}`);
                    break;
                default:
                    break;
            }
        });
    }
}
