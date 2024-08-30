// @ts-strict-ignore
import { Location } from '@angular/common';
import { inject } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { filter, take } from "rxjs/operators";
import { Service } from "../shared";
import { Role } from "../type/role";

/**
 * Determines if user is allowed to navigate to route, dependent on edge role
 *
 * @param route the route snapshot
 * @param state the routerStateSnapshot
 * @returns true, if edge.role equals requiredTole (provided in {@link Route.data} )
 */
export const hasEdgeRole = (role: Role) => {
    return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        const location = inject(Location);
        const service = inject(Service);
        service.currentEdge.pipe(filter(edge => !!edge), take(1)).subscribe((edge) => {
            if (edge) {
                const roleIsAtLeast = Role.isAtLeast(edge.role, role);

                if (!roleIsAtLeast) {
                    console.warn(`Routing Failed. Reason: User not allowed to access [component:${route?.component['SELECTOR'] ?? state.url}]`);
                    location.back();
                }
                return roleIsAtLeast;
            }
            return false;
        });

        return true;
    };
};
