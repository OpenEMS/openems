import { Injectable, signal, WritableSignal } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";

@Injectable()
export class RouteService {

    public currentUrl: WritableSignal<string | null> = signal(null);

    private previousUrl: string | null = null;

    constructor(private router: Router) {
        this.previousUrl = this.currentUrl();
        router.events.subscribe(event => {
            if (event instanceof NavigationEnd) {
                this.previousUrl = this.currentUrl();
                this.currentUrl.set(event.urlAfterRedirects);
            }
        });
    }

    /**
     * Gets the previous url, active before this url
     *
     * @returns the previous url
     */
    public getPreviousUrl() {
        return this.previousUrl;
    }


    /**
     * Gets the current url
     *
     * @returns the current url
     */
    public getCurrentUrl() {
        return this.currentUrl();
    }
}
