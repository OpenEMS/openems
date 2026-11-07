// @ts-strict-ignore
import { Location } from "@angular/common";
import { effect, inject, Injector } from "@angular/core";
import { ActivatedRouteSnapshot, CanActivateFn, createUrlTreeFromSnapshot, RedirectCommand, Router, RouterStateSnapshot, } from "@angular/router";
import { environment } from "src/environments";
import { UserService } from "../service/user.service";
import { Service } from "../shared";
import { Role } from "../type/role";

/**
 * Determines if user is allowed to navigate to route, dependent on edge role
 *
 * @param route The route snapshot
 * @param state The routerStateSnapshot
 * @returns True, if edge.role equals requiredTole (provided in {@link Route.data} )
 */
export const hasEdgeRole = (role: Role) => {
    return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        const location = inject(Location);
        const service = inject(Service);
        service.getCurrentEdge().then((edge) => {
            if (edge) {
                const roleIsAtLeast = Role.isAtLeast(edge.role, role);

                if (!roleIsAtLeast) {
                    console.warn(
                        `Routing Failed. Reason: User not allowed to access [component:${route?.component["SELECTOR"] ?? state.url}]`,
                    );
                    location.back();
                }
                return roleIsAtLeast;
            }
            return false;
        });

        return true;
    };
};

/**
 * Determines if user is allowed to navigate to route, dependent on properties in user.settings
 *
 * @param route The route snapshot
 * @param state The routerStateSnapshot
 * @returns True, if user.settings includes property
 */
export const hasUserSettings = (key: string) => {
    return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        const location = inject(Location);
        const userService = inject(UserService);
        const injector = inject(Injector);
        effect(
            () => {
                const user = userService.currentUser();
                if (!user) {
                    return;
                }
                let isAllowed = false;
                if (user && key in user.settings) {
                    isAllowed = user.settings[key] === true;
                }
                if (!isAllowed) {
                    console.warn(
                        `Routing Failed. Reason: User not allowed to access [component:${route?.component?.["SELECTOR"] ?? state.url}]`,
                    );
                    location.back();
                }
                return isAllowed;
            },
            { injector: injector },
        );
    };
};

export const hasEnvironmentBackend = (key: (typeof environment)["backend"]): CanActivateFn => {
    return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
        const location = inject(Location);
        const isAllowed = environment.backend === key;
        if (!isAllowed) {
            console.warn(
                `Routing Failed. Reason: Environment backend not allowed to access [component:${route?.component?.["SELECTOR"] ?? state.url}]`,
            );
            location.back();
            return false;
        }
        return true;
    };
};

export const redirectToParentOnReload = (
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
): boolean | RedirectCommand => {
    const router = inject(Router);

    if (router.navigated) {
        return true;
    }

    const parentRoute = route.parent;
    const hasParentLandingRoute = parentRoute?.routeConfig?.children?.some((child) => child.path === "");

    if (!hasParentLandingRoute) {
        return true;
    }

    return new RedirectCommand(createUrlTreeFromSnapshot(route, ["../"], state.root.queryParams, state.root.fragment), {
        replaceUrl: true,
    });
};
