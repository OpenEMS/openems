import { Injectable } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";
import { Service } from "../shared";
import { ArrayUtils } from "../utils/array/array.utils";

@Injectable()
export class RouteService {

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

    public static async getRouteAfterAuthentication(service: Service, urlSegments: string[]): Promise<string[]> {
        const user = await service.getCurrentUser();

        if (ArrayUtils.containsStrings(["login", "index", "demo"], urlSegments)) {

            // Initial navigation
            if (!user.hasMultipleEdges) {
                const edges = await service.getEdges(0);
                const edgeId = edges[0]?.id;
                return ["/device", edgeId];
            }
            return ["overview"];
        }

        // Previous navigation
        return urlSegments;
    }
}
