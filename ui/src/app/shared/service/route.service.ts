import { DestroyRef, inject, Injectable, signal, WritableSignal } from "@angular/core";
import { ActivatedRouteSnapshot, NavigationCancel, NavigationEnd, NavigationError, NavigationStart, ResolveEnd, Router, } from "@angular/router";
import { CookieService } from "ngx-cookie-service";
import { ObjectUtils } from "../utils/object/object-utils";
import { StringUtils } from "../utils/string/string.utils";
import { OAuthCallBackComponent } from "./auth/oauthcallback.component";

@Injectable()
export class RouteService {
    public currentUrl: WritableSignal<string | null> = signal(null);

    private previousUrl: string | null = null;
    private queryParams: WritableSignal<URLSearchParams | null> = signal(null);
    private cookieService = inject(CookieService);
    private destroyRef: DestroyRef = inject(DestroyRef);
    private router: Router = inject(Router);

    constructor() {
        this.previousUrl = this.currentUrl();
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                if (this.previousUrl === event.urlAfterRedirects) {
                    return;
                }

                this.setQueryParams(event.urlAfterRedirects);
                this.previousUrl = this.currentUrl();
                this.currentUrl.set(event.urlAfterRedirects);
            }

            if (event instanceof (NavigationStart || NavigationError || NavigationCancel || ResolveEnd)) {
                if (this.previousUrl === event.url) {
                    return;
                }
                this.setQueryParams(event.url);
                this.previousUrl = this.currentUrl();
                this.currentUrl.set(event.url);
            }
        });
    }

    /**
     * Gets the previous url, active before this url
     *
     * @returns The previous url
     */
    public getPreviousUrl() {
        return this.previousUrl;
    }

    /**
     * Gets the current url
     *
     * @returns The current url
     */
    public getCurrentUrl() {
        return this.currentUrl();
    }

    /**
     * Gets the route params, defined in routing modules
     *
     * @example
     *     retrieve :componentId by "componentId"
     *
     * @param key The key
     * @returns The value for this key if found, else null
     */
    public getRouteParam<T>(key: string): T | null {
        const routeParams = this.getRouteParams();
        if (key in routeParams) {
            return routeParams[key] as T;
        }
        return null;
    }

    /**
     * Gets the route params, defined in routing modules
     *
     * @example
     *     retrieve :componentId by "componentId"
     *
     * @param key The key
     * @returns The value for this key if found, else null
     */
    public getRouteParams(): Record<string, string> {
        const route = this.getDeepestRoute(this.router.routerState.snapshot.root);
        const routeParams = Object.entries(route.params).reduce((obj: { [k: string]: any }, [k, v]) => {
            const routeParamValue = typeof v === "string" ? v : null;
            const cleanedRouteParam =
                StringUtils.splitBy(StringUtils.splitBy(routeParamValue, "%")?.[0] ?? "", "?")?.[0] ?? "";
            obj[k] = cleanedRouteParam;
            return obj;
        }, {});

        return routeParams;
    }

    /**
     * Gets a query param.
     *
     * @param key The key
     * @returns The value for this key if found, else null
     */
    public getQueryParam<T>(key: string): T | null {
        const params = this.queryParams();
        if (params == null) {
            return null;
        }
        const value = params.get(key);
        return value as T;
    }

    /**
     * Gets the query params signal.
     *
     * @returns The query params signal
     */
    public getQueryParams() {
        return this.queryParams.asReadonly();
    }

    public navigateAfterAuthentication() {
        const oauthredirectstate = this.cookieService.get("oauthredirectstate");
        const oauthRedirectStateHref = ObjectUtils.parseFromString<{ href: string }>(oauthredirectstate)?.href ?? null;
        if (oauthRedirectStateHref != null && oauthRedirectStateHref != OAuthCallBackComponent.ID) {
            this.router.navigate([oauthRedirectStateHref]);
            return;
        }

        const initialUrl = this.router.lastSuccessfulNavigation()?.initialUrl ?? null;
        const isAuthenticatedNavi = (initialUrl?.toString()?.split("/")?.length ?? 0) > 2;
        if (isAuthenticatedNavi && initialUrl != null) {
            this.router.navigate([initialUrl.toString()]);
            return;
        }

        // Fallback
        this.router.navigate(["/overview"]);
    }

    private getDeepestRoute(routeSnapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
        while (routeSnapshot.firstChild) {
            routeSnapshot = routeSnapshot.firstChild;
        }
        return routeSnapshot;
    }

    private setQueryParams(url: string) {
        const queryParams = url.split("?")?.[1] ?? null;
        if (queryParams != null) {
            this.queryParams.set(new URLSearchParams(queryParams));
        }
    }
}
