import { Injectable, signal, WritableSignal } from "@angular/core";
import { ActivatedRouteSnapshot, NavigationEnd, Router } from "@angular/router";

@Injectable()
export class RouteService {

    public currentUrl: WritableSignal<string | null> = signal(null);

    private previousUrl: string | null = null;

    constructor(private router: Router) {
        this.previousUrl = this.currentUrl();
        router.events.subscribe(event => {
            if (event instanceof NavigationEnd) {
                this.previousUrl = this.currentUrl();
                this.currentUrl.set(event.urlAfterRedirects);;
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

    /**
     * Gets the current url
    *
    * @returns the current url
    */
    public getCurrentUrl2() {
        this.router.events.subscribe(event => {
            if (event instanceof NavigationEnd) {
                return event.urlAfterRedirects;
            }
        });
    }

    /**
     * Gets the route params
     *
     * @param key the key
     * @returns the value for this key if found, else null
    */
    public getRouteParam<T>(key: string): T | null {
        const route = this.getDeepestRoute(this.router.routerState.snapshot.root);
        const routeParams = Object.entries(route.params)
            .reduce((obj: { [k: string]: any }, [k, v]) => { obj[k] = v; return obj; }, {});
        if (key in routeParams) {
            return routeParams[key] as T;
        }
        return null;
    }

    private getDeepestRoute(routeSnapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
        while (routeSnapshot.firstChild) {
            routeSnapshot = routeSnapshot.firstChild;
        }
        return routeSnapshot;
    }
}
