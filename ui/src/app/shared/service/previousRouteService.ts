import { Injectable } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";

@Injectable()
export class PreviousRouteService {

    private previousUrl: string;
    private currentUrl: string;

    constructor(private router: Router) {
        this.currentUrl = this.router.url;
        this.previousUrl = this.currentUrl;
        router.events.subscribe(event => {
            if (event instanceof NavigationEnd) {
                this.previousUrl = this.currentUrl;
                this.currentUrl = event.url;
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
}
