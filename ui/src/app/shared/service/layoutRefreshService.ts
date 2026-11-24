import { Injectable } from "@angular/core";

@Injectable({ providedIn: "root" })
export class LayoutRefreshService {
    private queued = false;

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
