import { HttpClient } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { retry, switchMap, timer } from "rxjs";
import { CachetComponentStatus } from "src/app/app.component";
import { environment } from "src/environments";

@Injectable({
    providedIn: "root",
})
export class SystemStateService {

    private _isSystemOutage = signal<boolean>(false);
    private http: HttpClient = inject(HttpClient);

    public get isSystemOutage() {
        return this._isSystemOutage.asReadonly();
    }

    /**
     * Get system status from https://status.fenecon.de
     */
    public pollSystemStatus(): void {
        if (environment.backend === "OpenEMS Edge") {
            console.log("System status polling disabled locally and in dev mode");
            return;
        }

        timer(1 /* immediately */, 5 * 60 * 1000 /* and then every 5 minutes */)
            .pipe(
                switchMap(() => this.http.get<CachetComponentStatus>("https://status.fenecon.de/api/v1/components/3")),
                retry(
                    { count: 5, delay: 5000 }
                ))
            .subscribe((data) => {
                switch (data.data.status) {
                    case 2:
                    case 3:
                    case 4:
                        this.setSystemOutage(true);
                        break;
                    case 1:
                        this.setSystemOutage(false);
                        break;
                }
            });
    }

    private setSystemOutage(value: boolean) {
        this._isSystemOutage.set(value);
    }
}
