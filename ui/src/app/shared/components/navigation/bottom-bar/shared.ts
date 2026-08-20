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
        const favorites = getFavoritesNavigationTree(edge, config, translate);
        return [
            ...OverViewComponent.getNavigationTree(user, translate),
            getCockpitNavigationTree(edge, config, translate, platFormService),
            favorites,
            EnergyJourneyShared.getNavigationTree(edge, config, translate),
            getProfileNavigationTree(service, translate),
        ];
    }

    export function getFavoritesNavigationTree(
        edge: Edge,
        config: EdgeConfig,
        translate: TranslateService,
    ): NavigationTree {
        const favoritesChildren = getFavoritesChildrenNavigationTrees(config, edge, translate);
        const favoriteTree = new NavigationTree(
            "favorites",
            { baseString: "favorites" },
            { name: "oe-favorites" },
            "Favoriten",
            "label",
            favoritesChildren,
            null,
            { accordionOpenedOnDefault: true },
        );
        if (favoritesChildren.length === 0) {
            return favoriteTree;
        }

        favoriteTree.children = favoritesChildren;
        return favoriteTree;
    }

    export function getFavoritesChildrenNavigationTrees(
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
        );

        const heatComponents = config?.getComponentsImplementingNature("io.openems.edge.heat.api.Heat");
        const allComponents = [...consumptionMeters, ...consumptionMetered, ...heatComponents];

        const newConf: EdgeConfig = new EdgeConfig(edge, {
            factories: structuredClone(config.factories),
            components: Object.entries(structuredClone(config.components)).reduce(
                (acc: EdgeConfig["components"], [id, component]) => {
                    if (allComponents.some((c) => c.id === id)) {
                        acc[id] = component;
                    }
                    return acc;
                },
                {},
            ),
        } as EdgeConfig);

        return Widgets.getControllerNavigationTrees(edge, translate, newConf).map((el) => new NavigationTree(...el));
    }

    export function getCockpitNavigationTree(
        edge: Edge,
        config: EdgeConfig,
        translate: TranslateService,
        platFormService: PlatFormService,
    ): NavigationTree {
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
            { accordionOpenedOnDefault: true },
        );
    }

    export function getProfileNavigationTree(service: Service, translate: TranslateService): NavigationTree {
        return new NavigationTree(
            "profile",
            { baseString: "profile" },
            { name: "person-outline" },
            "Profil",
            "icon",
            [
                UserComponent.getNavigationTree(service, translate),
                new NavigationTree(
                    "settings",
                    { baseString: "settings" },
                    { name: "cog-outline" },
                    translate.instant("MENU.EDGE_SYSTEM_SETTINGS"),
                    "label",
                    [],
                    null,
                ),
                NavigationConstants.CommonNodes.INFO(translate),
            ],
            null,
        );
    }
}
