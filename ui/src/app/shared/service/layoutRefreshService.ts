import { Injectable, NgZone } from "@angular/core";

@Injectable({ providedIn: "root" })
export class LayoutRefreshService {
    private queued = false;

    constructor(
        private zone: NgZone
    ) { }

    request(msDelay: number): void {
        if (this.queued) {
            return;
        }
        this.queued = true;

        setTimeout(() => {
            window.dispatchEvent(new Event("resize"));
            this.queued = false;
        }, msDelay);
    }
}
