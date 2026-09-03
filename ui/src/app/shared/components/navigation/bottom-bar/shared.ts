import { TranslateService } from "@ngx-translate/core";
import { SharedConsumption } from "src/app/edge/live/common/consumption/shared/shared";
import { SharedGrid } from "src/app/edge/live/common/grid/shared/shared";
import { SharedProduction } from "src/app/edge/live/common/production/shared/shared";
import { SharedStorage } from "src/app/edge/live/common/storage/shared/shared";
import { SharedWeather } from "src/app/edge/live/common/weather/shared/shared";
import { EnergyJourneyShared } from "src/app/edge/settings/energy-journey/shared/shared";
import { OverViewComponent } from "src/app/index/overview/overview.component";
import { PlatFormService } from "src/app/platform.service";
import { User } from "src/app/shared/jsonrpc/shared";
import { Service } from "src/app/shared/shared";
import { Widgets } from "src/app/shared/type/widgets";
import { UserComponent } from "src/app/user/user.component";
import { Edge } from "../../edge/edge";
import { EdgeConfig } from "../../edge/edgeconfig";
import { NavigationTree, NavigationId, NavigationConstants } from "../shared";

export namespace SharedBottomNavigationBar {
    export function getNavigationTree(
        edge: Edge,
        config: EdgeConfig,
        translate: TranslateService,
        service: Service,
        user: User,
        platFormService: PlatFormService,
    ): NavigationTree[] {
        return [
            ...OverViewComponent.getNavigationTree(user, translate),
            getCockpitNavigationTree(edge, config, translate),
            getFavoritesNavigationTree(translate),
            EnergyJourneyShared.getNavigationTree(edge, config, translate),
            getProfileNavigationTree(user, translate),
        ].filter((e) => e !== null);
    }

    export function getFavoritesNavigationTree(translate: TranslateService): NavigationTree {
        return new NavigationTree(
            NavigationId.FAVORITES,
            { baseString: "favorites" },
            { name: "oe-favorites", color: "dark" },
            translate.instant("MENU.FAVORITES"),
            "label",
            [],
            null,
            { accordionOpenedOnDefault: true, hideFavorite: true },
        );
    }

    export function getConsumptionChildren(
        config: EdgeConfig,
        edge: Edge,
        translate: TranslateService,
    ): NavigationTree[] {
        const consumptionMeters = config
            .getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter((component) => component.isEnabled && config.isTypeConsumptionMetered(component));

        const consumptionMetered = config.getComponentsByFactories(
            "Evse.Controller.Single",
            "Controller.IO.HeatingElement",
            "Controller.Io.HeatPump.SgReady",
            "Controller.ChannelThreshold",
            "Controller.IO.ChannelSingleThreshold",
            "Controller.Io.FixDigitalOutput",
            "Controller.CHP.SoC",
        );

        const heatComponents = config?.getComponentsImplementingNature("io.openems.edge.heat.api.Heat");
        const allComponents = [...consumptionMeters, ...consumptionMetered, ...heatComponents];
        const componentIds = new Set(allComponents.map((component) => component.id));

        const widgets = (config?.widgets?.list ?? []).filter((widget) => componentIds.has(widget.componentId));

        return Widgets.getControllerNavigationTrees(edge, translate, config, widgets).map(
            (el) => new NavigationTree(...el),
        );
    }

    export function getCockpitNavigationTree(
        edge: Edge,
        config: EdgeConfig,
        translate: TranslateService,
    ): NavigationTree | null {
        if (!edge.isOnline) {
            return null;
        }

        const weatherComponent = config.getFirstComponentByFactoryId("Weather.OpenMeteo");
        const storage = SharedStorage.getNavigationTree(edge, translate, config);
        const grid = SharedGrid.getNavigationTree(edge, config, translate);
        const production = SharedProduction.getNavigationTree(edge, config, translate);
        const consumption = SharedConsumption.getNavigationTree(edge, config, translate);
        const weather = weatherComponent !== null ? SharedWeather.getNavigationTree(translate, weatherComponent) : null;

        return new NavigationTree(
            NavigationId.LIVE,
            { baseString: "live" },
            { name: "home-outline" },
            translate.instant("MENU.COCKPIT"),
            "icon",
            [production, consumption, storage, grid, weather]
                .filter((el) => el !== null)
                .map((el) => new NavigationTree(...el)),
            null,
            { accordionOpenedOnDefault: true, hideFavorite: true },
        );
    }

    export function getProfileNavigationTree(user: User, translate: TranslateService): NavigationTree {
        return new NavigationTree(
            "profile",
            { baseString: "profile" },
            { name: "person-outline" },
            "Profil",
            "icon",
            [
                UserComponent.getNavigationTree(user),
                new NavigationTree(
                    "profile-settings",
                    { baseString: "settings" },
                    { name: "cog-outline" },
                    translate.instant("MENU.EDGE_SYSTEM_SETTINGS"),
                    "label",
                    [],
                    null,
                    { hideFavorite: true },
                ),
                NavigationConstants.CommonNodes.INFO(translate, "profile"),
            ],
            null,
            { isCommonWidget: true, hideFavorite: true },
        );
    }
}
