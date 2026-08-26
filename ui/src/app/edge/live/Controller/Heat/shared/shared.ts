import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { environment } from "../../../../../../environments";
import { TimeOfUseTariffUtils } from "../../../../../shared/utils/utils";
import { HeatConverter } from "../new-navigation/converter";

export namespace SharedControllerHeat {
    export const getFormlyModalView = (
        translate: TranslateService,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): OeFormlyView => {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            helpKey: getHelpKey(component),
            lines: getLegacyViewLines(translate, component),
            component,
            edge,
        };
    };

    /**
     * @deprecated Temporary fallback for legacy heat views without EnergyScheduler data, especially Heat.MyPv.AcThor9s.
     *   Remove when legacy heat UI support is dropped.
     */
    export const getLegacyViewLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => {
        const isAskoma = component.factoryId === "Heat.Askoma";
        const isMyPv = component.factoryId === "Heat.MyPv.AcThor9s" || component.factoryId === "Heat.MyPv";

        return [
            ...getFormlySharedLines(translate, component),
            ...(isMyPv ? getMyPVInfoLine(translate) : []),
            ...(isAskoma ? getAskomaIcon() : []),
            ...(isMyPv ? getMyPvIcon() : []),
        ];
    };

    export const getFormlySharedLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => [
        {
            type: "channel-line",
            name: translate.instant("GENERAL.STATUS"),
            channel: component.id + "/Status",
            converter: HeatConverter.CONVERT_POWER_2_HEAT_STATE(translate),
        },
        {
            type: "channel-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
            channel: component.id + "/ActivePower",
            converter: Converter.POWER_IN_WATT,
        },
        {
            type: "channel-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE"),
            channel: component.id + "/Temperature",
            converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
        },
        {
            type: "channel-line" as const,
            name: translate.instant("HEAT.HOME.CHOSEN_MODE"),
            channel: component.id + "/Mode",
            converter: (value: number | null) => CONVERT_CHANNEL_MODE_TO_LABEL(translate)(value),
            filter: (value: number | null) => value != null,
        },
    ];

    export const getMyPVInfoLine = (translate: TranslateService): OeFormlyView["lines"] => [
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.CHANGES_MY_PV_INFO"),
            icon: {
                name: "information-outline",
                color: "primary",
                size: "small",
            },
        },
    ];

    export const getAskomaIcon = (): OeFormlyView["lines"] => [
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
        },
    ];

    export const getMyPvIcon = (): OeFormlyView["lines"] => [
        {
            type: "image-line",
            img: {
                url: environment.images.HEAT.MYPV.HEATING_ELEMENT,
                width: 50,
                style: {
                    maxWidth: "30rem",
                    justifySelf: "center",
                    paddingBottom: "var(--ion-padding)",
                },
            },
        },
    ];

    export const getFormlySettingsLines = (translate: TranslateService): OeFormlyView["lines"] => [
        {
            type: "radio-buttons-from-form-control-line",
            name: "select-mode",
            controlName: "mode",
            buttons: getHeatModeButtons(translate),
        },
    ];

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
            new ChannelAddress(component.id, "Mode"),
            new ChannelAddress(component.id, "_PropertyMode"),
        ];
    }

    export function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const heatComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(heatComponent);
        const channelAddresses: ChannelAddress[] = getChannelAddressesForComponent(heatComponent);

        return Promise.resolve(channelAddresses);
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
        isAskoma: boolean,
    ): ConstructorParameters<typeof NavigationTree> {
        const isWritable = component.properties?.readOnly !== true;
        const children = [];

        if (isWritable && isAskoma) {
            children.push(
                new NavigationTree(
                    "forecast",
                    { baseString: "forecast" },
                    { name: "stats-chart-outline", color: "success" },
                    translate.instant("HEAT.FORECAST.FORECAST"),
                    "label",
                    [],
                    null,
                ),
            );
        }

        children.push(
            new NavigationTree(
                "history",
                { baseString: "history" },
                {
                    name: "stats-chart-outline",
                    color: "warning",
                },
                translate.instant("GENERAL.HISTORY"),
                "label",
                [],
                null,
            ),
        );

        if (isWritable) {
            children.push(
                new NavigationTree(
                    "schedule",
                    { baseString: "schedule" },
                    {
                        name: "calendar-outline",
                        color: "warning",
                    },
                    translate.instant("HEAT.SCHEDULE.SCHEDULE"),
                    "label",
                    [
                        new NavigationTree(
                            "edit-task",
                            { baseString: "edit-task" },
                            { name: "create-outline" },
                            translate.instant("JS_SCHEDULE.EDIT_EVENT"),
                            "label",
                            [],
                            null,
                            { showOrder: "HIDE" },
                        ),
                        new NavigationTree(
                            "add-task",
                            { baseString: "add-task" },
                            { name: "add-outline" },
                            translate.instant("JS_SCHEDULE.ADD_EVENT"),
                            "label",
                            [],
                            null,
                            { showOrder: "HIDE" },
                        ),
                    ],
                    null,
                ),
                new NavigationTree(
                    "settings",
                    { baseString: "settings" },
                    { name: "cog-outline" },
                    translate.instant("MENU.SETTINGS"),
                    "label",
                    [],
                    null,
                ),
            );
        }

        return new NavigationTree(
            component.id,
            { baseString: "controller/heat/" + component.id },
            {
                name: "oe-heating-element",
                color: "normal",
            },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            children,
            null,
        ).toConstructorParams();
    }

    export type ScheduleChartData = {
        datasets: ChartDataset[];
        colors: any[];
        labels: Date[];
    };

    export function getScheduleChartData(
        length: number,
        prices: number[],
        modes: number[],
        timestamps: string[],
        translate: TranslateService,
    ) {
        const colors: any[] = [];
        const datasets: ChartDataset[] = [];
        const labels: Date[] = [];

        // Initializing States.
        const barOff = new Array(length).fill(null);
        const barSurplus = new Array(length).fill(null);
        const barFastHeat = new Array(length).fill(null);

        for (let index = 0; index < length; index++) {
            const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(prices[index]);
            const mode = modes[index];
            labels.push(new Date(timestamps[index]));

            const modeStates = Object.keys(PropertyMode);
            if (mode !== null) {
                switch (mode) {
                    case modeStates.indexOf(PropertyMode.OFF):
                        barOff[index] = quarterlyPrice;
                        break;
                    case modeStates.indexOf(PropertyMode.SURPLUS):
                        barSurplus[index] = quarterlyPrice;
                        break;
                    case modeStates.indexOf(PropertyMode.FAST_HEAT):
                        barFastHeat[index] = quarterlyPrice;
                        break;
                }
            }
        }

        // Set datasets
        datasets.push({
            type: "bar",
            label: translate.instant("HEAT.SETTINGS.MODE.OFF.TITLE"),
            data: barOff,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(0,0,0,0.8)",
            borderColor: "rgba(0,0,0,0.9)",
        });

        datasets.push({
            type: "bar",
            label: translate.instant("HEAT.SETTINGS.MODE.SURPLUS.TITLE"),
            data: barSurplus,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(51,102,0,0.8)",
            borderColor: "rgba(51,102,0,1)",
        });

        datasets.push({
            type: "bar",
            label: translate.instant("HEAT.SETTINGS.MODE.FAST_HEAT.TITLE"),
            data: barFastHeat,
            order: 1,
        });
        colors.push({
            backgroundColor: "rgba(0, 204, 204,0.5)",
            borderColor: "rgba(0, 204, 204,0.7)",
        });

        const scheduleChartData: ScheduleChartData = {
            colors: colors,
            datasets: datasets,
            labels: labels,
        };

        return scheduleChartData;
    }

    export function getHelpKey(component: EdgeConfig.Component | null): string | null {
        if (component == null) {
            return null;
        }
        const isAskoma = component.factoryId === "Heat.Askoma";
        const isMyPv = component.factoryId === "Heat.MyPv.AcThor9s" || component.factoryId === "Heat.MyPv";

        if (isAskoma) {
            return "REDIRECT.CONTROLLER_HEAT_ASKOMA";
        }
        if (isMyPv) {
            return "REDIRECT.CONTROLLER_HEAT_MYPV";
        }
        return "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT";
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

export const CONVERT_CHANNEL_MODE_TO_LABEL = (translate: TranslateService) => {
    return (value: number | null): string => {
        switch (value) {
            case ChannelMode.FAST_HEAT:
                return translate.instant("HEAT.SETTINGS.MODE.FAST_HEAT.TITLE");
            case ChannelMode.SURPLUS:
                return translate.instant("HEAT.SETTINGS.MODE.SURPLUS.TITLE");
            case ChannelMode.OFF:
                return translate.instant("HEAT.SETTINGS.MODE.OFF.TITLE");
            default:
                return Converter.HIDE_VALUE(value);
        }
    };
};

export enum HeatStatus {
    UNDEFINED = -1,
    STANDBY = 0,
    EXCESS = 1,
    CONTROL_NOT_ALLOWED = 2,
    TEMPERATURE_REACHED = 3,
    NO_CONTROL_SIGNAL = 4,
    ERROR = 5,
}

export enum ChannelMode {
    UNDEFINED = -1, //
    OFF = 1, //
    FAST_HEAT = 2, //
    SURPLUS = 3, //
}

export enum PropertyMode {
    OFF = "OFF", //
    FAST_HEAT = "FAST_HEAT", //
    SURPLUS = "SURPLUS", //
}

export enum Mode {
    OFF = "OFF", //
    FAST_HEAT = "FAST_HEAT", //
    SURPLUS = "SURPLUS", //
}
