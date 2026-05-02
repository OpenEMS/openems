import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

export namespace SharedProduction {

    export function getNavigationTree(edge: Edge, config: EdgeConfig, translate: TranslateService): ConstructorParameters<typeof NavigationTree> | null {
        const chargerComponents =
            config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        const productionMeterComponents =
            config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isProducer(component));

        const sum: EdgeConfig.Component = config.getComponent("_sum");
        sum.alias = translate.instant("EDGE.HISTORY.PHASE_ACCURATE");

        return new NavigationTree("production", { baseString: "common/production" }, { name: "oe-production", color: "production" }, translate.instant("GENERAL.PRODUCTION"), "label", [
            NavigationConstants.CommonNodes.PHASE_ACCURATE(translate, "details", "production"),
            getHistoryNavigationTree(edge, translate, sum, ...chargerComponents, ...productionMeterComponents),
        ], null).toConstructorParams();
    }

    function getHistoryNavigationTree(edge: Edge, translate: TranslateService, sum: EdgeConfig.Component, ...components: EdgeConfig.Component[]): NavigationTree {
        return new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "production" }, translate.instant("GENERAL.HISTORY"), "label", [
            ...getHistorySingleComponentNavigationTree(edge, translate, sum, ...components),
        ], null);
    }

    function getHistorySingleComponentNavigationTree(edge: Edge, translate: TranslateService, sum: EdgeConfig.Component, ...components: EdgeConfig.Component[]): NavigationTree[] {
        return [
            NavigationConstants.CommonNodes.PHASE_ACCURATE(translate, sum.id + "/phase-accurate", "production"),
            ...components.map(el => {
                return new NavigationTree(el.id + "/phase-accurate", { baseString: el.id + "/phase-accurate" }, { name: "stats-chart-outline", color: "production" }, el.alias, "label", [
                    ...(edge.roleIsAtLeast(Role.INSTALLER) ?
                        [new NavigationTree(el.id + "/current-voltage", { baseString: "current-voltage" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), "label", [], null)]
                        : []
                    ),
                ], null);
            }),
        ];
    }
}

