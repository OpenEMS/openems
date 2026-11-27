import { TranslateService } from "@ngx-translate/core";
import { SharedAutarchy } from "src/app/edge/live/common/autarchy/shared/shared";
import { SharedSelfConsumption } from "src/app/edge/live/common/selfconsumption/shared/shared";
import { NavigationId, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

export namespace EnergyJourneyShared {
    export const historyNavigationTree = (translate: TranslateService) =>
        new NavigationTree(
            NavigationId.HISTORY,
            { baseString: "history" },
            { name: "stats-chart-outline" },
            translate.instant("GENERAL.HISTORY"),
            "label",
            [
                new NavigationTree(
                    "export",
                    { baseString: "export" },
                    { name: "download-outline" },
                    translate.instant("EDGE.CONFIG.INDEX.EXPORT"),
                    "label",
                    [],
                    null,
                ),
            ],
            null,
            { showOrder: "HIGH" },
        );
    export function getNavigationTree(edge: Edge, config: EdgeConfig, translate: TranslateService): NavigationTree {
        const children: NavigationTree[] = [EnergyJourneyShared.historyNavigationTree(translate)];

        if (edge.roleIsAtLeast(Role.OWNER)) {
            children.push(
                new NavigationTree(
                    "appcenter",
                    { baseString: "appcenter" },
                    { name: "add-outline" },
                    "AppCenter",
                    "label",
                    [
                        new NavigationTree(
                            "single",
                            { baseString: "single" },
                            { name: "add-outline" },
                            translate.instant("EDGE.CONFIG.APP.APP_DETAILS"),
                            "label",
                            [
                                new NavigationTree(
                                    "install",
                                    { baseString: "install" },
                                    { name: "add-outline" },
                                    translate.instant("EDGE.CONFIG.APP.CREATE_APP"),
                                    "label",
                                    [],
                                    null,
                                    { showOrder: "HIDE", hideFavorite: true },
                                ),
                                new NavigationTree(
                                    "update",
                                    { baseString: "update" },
                                    { name: "add-outline" },
                                    translate.instant("EDGE.CONFIG.APP.MODIFY_APP"),
                                    "label",
                                    [],
                                    null,
                                    { showOrder: "HIDE", hideFavorite: true },
                                ),
                            ],
                            null,
                            { showOrder: "HIDE", hideFavorite: true },
                        ),
                    ],
                    null,
                    { hideFavorite: true },
                ),
            );
        }

        // Show Autarchy and Self-Consumption only for systems with Producers
        if (config.hasProducer()) {
            children.push(
                new NavigationTree(...SharedAutarchy.getNavigationTree(translate)),
                new NavigationTree(...SharedSelfConsumption.getNavigationTree(translate)),
            );
        }

        const tree = new NavigationTree(
            "energy-journey",
            { baseString: "energy-journey" },
            { name: "oe-energy-journey" },
            { desktop: translate.instant("SETTINGS.ENERGY_JOURNEY.YOUR_ENERYJOURNEY"), mobile: "Journey" },
            "label",
            children,
            null,
        );

        return tree;
    }
}
