import { effect, inject, Service, untracked } from "@angular/core";
import { MatomoTracker } from "ngx-matomo-client";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { UserService } from "src/app/shared/service/user.service";
import { Service as TService } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { RouteService } from "./route/route.service";

@Service()
export class TrackingService {
    private static readonly TRACKING_EVENT: string = "Metadata";
    /** Null if tracking is not configured for this environment */
    private readonly matomo = inject(MatomoTracker, { optional: true });
    private readonly service = inject(TService);
    private readonly userService = inject(UserService);
    private readonly routeService = inject(RouteService);

    private hasBeenTrackedForThisRoute = false;

    constructor() {
        // Do nothing if tracking is not configured for this environment
        if (this.matomo == null) {
            return;
        }
        // Local copy keeps the non-null narrowing inside the effect closure
        const matomo = this.matomo;

        let prevUrl: string | null = null;

        effect(() => {
            const _currentUrl = this.routeService.currentUrl();
            const edgeId = this.routeService.getRouteParam<string>("edgeId");
            const currentEdge = untracked(() => this.service.currentEdge());
            const currentUser = untracked(() => this.userService.currentUser());
            const config = currentEdge?.getCurrentConfig() ?? null;
            if (prevUrl != _currentUrl) {
                this.hasBeenTrackedForThisRoute = false;
            }

            // Do not track if edge view and no edge available
            if (edgeId != null && currentEdge == null) {
                return;
            }

            if (
                prevUrl === _currentUrl ||
                this.hasBeenTrackedForThisRoute ||
                (currentEdge == null && currentUser == null)
            ) {
                return;
            }

            const details: string[] = [];
            if (currentUser != null) {
                details.push(`role=${currentUser.globalRole}`, `language=${currentUser.language}`);
            }
            if (currentEdge != null) {
                details.push(
                    `edgeRole=${Role[currentEdge.role].toLowerCase()}`,
                    `producttype=${currentEdge.producttype}`,
                );
            }
            if (currentUser != null || config != null) {
                const usesNewUI = NavigationService.isNewNavigation(currentUser, config);
                details.push(`newUI=${usesNewUI}`);
            }

            matomo.trackEvent(
                TrackingService.TRACKING_EVENT,
                currentEdge != null ? "edge" : "global",
                details.join(", "),
            );

            this.hasBeenTrackedForThisRoute = true;
            prevUrl = _currentUrl;
        });
    }
}
