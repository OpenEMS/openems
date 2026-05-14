import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import {environment} from "../../../../../../environments";

export namespace SharedControllerHeat {

    export const getFormlyModalView = (translate: TranslateService, component: EdgeConfig.Component | null, edge: Edge | null): OeFormlyView => {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const isAskoma = component.factoryId === "Heat.Askoma";
        const isMyPv = component.factoryId === "Heat.MyPv.AcThor9s";

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            icon: { name: "flame", color: "normal", size: "normal" },
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            lines: [
                ...getFormlySharedLines(translate, component, isAskoma),
                ...(isMyPv ? getMyPVInfoLine(translate) : []),
                ...(isAskoma ? getAskomaIcon() : []),
            ],
            component,
            edge,
        };
    };

    export const getFormlySharedLines = (translate: TranslateService, component: EdgeConfig.Component, isAskoma: boolean): OeFormlyView["lines"] => ([{
        type: "channel-line",
        name: translate.instant("GENERAL.STATUS"),
        channel: component.id + "/Status",
        converter: Converter.CONVERT_POWER_2_HEAT_STATE(translate),
    }, {
        type: "channel-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
        channel: component.id + "/ActivePower",
        converter: Converter.POWER_IN_WATT,
    }, {
        type: "channel-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE"),
        channel: component.id + "/Temperature",
        converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
    },
    ...(isAskoma ? [{
        type: "channel-line" as const,
        name: translate.instant("HEAT.HOME.CHOSEN_MODE"),
        channel: component.id + "/_PropertyMode",
        converter: (value: number | null) => convertAskomaMode(translate, value),
    }] : [])]);

    export const getMyPVInfoLine = (translate: TranslateService): OeFormlyView["lines"] => ([{
        type: "info-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.CHANGES_MY_PV_INFO"),
        icon: { name: "information-outline", color: "primary", size: "small" },
    }]);

    export const getAskomaIcon = (): OeFormlyView["lines"] => ([
        {
            type: "image-line",
            img: {
                url: environment.images.HEAT.ASKOMA.HEATING_ELEMENT,
                width: 50,
                style: {
                    maxWidth: "30rem",
                    justifySelf: "center",
                    paddingBottom: "var(--ion-padding)",
                },
            },
        }]);

    export const getFormlySettingsLines = (translate: TranslateService, isAskoma: boolean): OeFormlyView["lines"] => isAskoma ? [
        {
            type: "radio-buttons-from-form-control-line",
            name: "select-mode",
            controlName: "mode",
            buttons: getHeatModeButtons(translate),
        }] : [];

    export const getHeatModeButtons = (translate: TranslateService): ButtonLabel[] => [
        {
            value: Mode.FAST_HEAT,
            name: translate.instant("HEAT.SETTINGS.MODE.FAST_HEAT.TITLE"),
            description: translate.instant("HEAT.SETTINGS.MODE.FAST_HEAT.DESCRIPTION"),
            icon: { color: "success", name: "oe-consumption", size: "medium" },
        },
        {
            value: Mode.SURPLUS,
            name: translate.instant("HEAT.SETTINGS.MODE.SURPLUS.TITLE"),
            description: translate.instant("HEAT.SETTINGS.MODE.SURPLUS.DESCRIPTION"),
            icon: { color: "primary", name: "oe-production", size: "medium" },
        },
        {
            value: Mode.OFF,
            name: translate.instant("HEAT.SETTINGS.MODE.OFF.TITLE"),
        },
    ];

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
        });
    }

    export function getChannelAddressesForComponent(component: EdgeConfig.Component): ChannelAddress[] {
        return [
            new ChannelAddress(component.id, "Status"),
            new ChannelAddress(component.id, "ActivePower"),
            new ChannelAddress(component.id, "Temperature"),
            ...(component.factoryId === "Heat.Askoma" ? [new ChannelAddress(component.id, "_PropertyMode")] : []),
        ];
    }

    export function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const heatComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(heatComponent);
        const channelAddresses: ChannelAddress[] = getChannelAddressesForComponent(heatComponent);

        return Promise.resolve(channelAddresses);
    }

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        const isAskoma = component.factoryId === "Heat.Askoma";
        const isAskomaWritable = isAskoma && component.properties?.readOnly !== true;

        const children = [
            new NavigationTree("history", {baseString: "history"}, {
                name: "stats-chart-outline",
                color: "warning",
            }, translate.instant("GENERAL.HISTORY"), "label", [], null),
        ];

        if (isAskomaWritable) {
            children.push(
                new NavigationTree("schedule", {baseString: "schedule"}, {
                    name: "calendar-outline",
                    color: "warning",
                }, translate.instant("HEAT.SCHEDULE.SCHEDULE"), "label", [
                    new NavigationTree("edit-task", {baseString: "edit-task"}, {name: "create-outline"}, translate.instant("JS_SCHEDULE.EDIT_TASK"), "label", [], null, "HIDE"),
                    new NavigationTree("add-task", {baseString: "add-task"}, {name: "add-outline"}, translate.instant("JS_SCHEDULE.ADD_TASK"), "label", [], null, "HIDE"),
                ], null),
                new NavigationTree("settings", {baseString: "settings"}, {name: "cog-outline"}, translate.instant("MENU.SETTINGS"), "label", [], null),
            );
        }

        return new NavigationTree(component.id, {baseString: "controller/heat/" + component.id}, {
            name: "flame",
            color: "normal",
        }, Name.METER_ALIAS_OR_ID(component), "label", children, null).toConstructorParams();
    }
}

export const CONVERT_TO_MODE_LABEL = (translate: TranslateService) => {
    return (value: string | null): string => {
        switch (value) {
            case "FAST_HEAT":
                return translate.instant("HEAT.SETTINGS.MODE.FAST_HEAT.TITLE");
            case "SURPLUS":
                return translate.instant("HEAT.SETTINGS.MODE.SURPLUS.TITLE");
            case "OFF":
                return translate.instant("HEAT.SETTINGS.MODE.OFF.TITLE");
            default:
                return Converter.HIDE_VALUE(value);
        }
    };
};

function convertAskomaMode(translate: TranslateService, value: number | null): string {
    return CONVERT_TO_MODE_LABEL(translate)(value as unknown as string | null);
}

export enum Mode {
    OFF = "OFF", //
    FAST_HEAT = "FAST_HEAT", //
    SURPLUS = "SURPLUS", //
}
