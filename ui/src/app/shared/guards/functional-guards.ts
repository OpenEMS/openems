// @ts-strict-ignore
import { Location } from "@angular/common";
import { inject } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Service } from "../shared";
import { Role } from "../type/role";

/**
 * Determines if user is allowed to navigate to route, dependent on edge role
 *
 * @param route the route snapshot
 * @param state the routerStateSnapshot
 * @returns true, if EDGE.ROLE equals requiredTole (provided in {@link ROUTE.DATA} )
 */
export const hasEdgeRole = (role: Role) => {
    return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        const location = inject(Location);
        const service = inject(Service);
        SERVICE.GET_CURRENT_EDGE().then(edge => {
            if (edge) {
                const roleIsAtLeast = ROLE.IS_AT_LEAST(EDGE.ROLE, role);

                if (!roleIsAtLeast) {
                    CONSOLE.WARN(`Routing Failed. Reason: User not allowed to access [component:${route?.component["SELECTOR"] ?? STATE.URL}]`);
                    LOCATION.BACK();
                }
                return roleIsAtLeast;
            }
            return false;
        });

        return true;
    };
};
