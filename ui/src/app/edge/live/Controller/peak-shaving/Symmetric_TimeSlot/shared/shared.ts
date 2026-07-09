import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { WEEKDAYS } from "src/app/shared/components/formly/formly-weekday-checkbox/formly-weekday-checkbox";
import { NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { Icon } from "src/app/shared/type/widget";

export namespace SharedControllerTimeslotPeakshaving {
    export const getFormlyFlatLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];
        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MEASURED_VALUE"),
                channel:
                    component.getPropertyFromComponent<string>("meter.id") +
                    "/ActivePower",
                converter: Converter.POWER_IN_KILO_WATT,
            },
            {
                type: "channel-line",
                name: translate.instant(
                    "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                ),
                channel: component.id + "/_PropertyPeakShavingPower",
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
            },
            {
                type: "channel-line",
                name: translate.instant(
                    "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                ),
                channel: component.id + "/_PropertyRechargePower",
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
            },
        );

        return lines;
    };

    export const getFormlySettingsLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];
        const meterId = component.getPropertyFromComponent<string>("meter.id");

        lines.push(
            {
                type: "weekday-checkbox-line",
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MEASURED_VALUE"),
                channel: meterId + "/ActivePower",
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
            },
            {
                type: "horizontal-line",
            },
        );

        if (edge.roleIsAtLeast(Role.OWNER)) {
            lines.push(
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                    ),
                    controlName: "peakShavingPower",
                    properties: {
                        unit: "W",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                    ),
                    controlName: "rechargePower",
                    properties: {
                        unit: "W",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.HYSTERESIS_SO_C",
                    ),
                    controlName: "hysteresisSoc",
                    properties: {
                        unit: "%",
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.START_DATE",
                    ),
                    controlName: "startDate",
                    properties: {
                        unit: "",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.END_DATE",
                    ),
                    controlName: "endDate",
                    properties: {
                        unit: "",
                        type: "date",
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.START_TIME",
                    ),
                    controlName: "startTime",
                    properties: {
                        unit: "",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.END_TIME",
                    ),
                    controlName: "endTime",
                    properties: {
                        unit: "",
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE",
                    ),
                    controlName: "slowChargePower",
                    properties: {
                        unit: "W",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.START_TIME_CHARGE",
                    ),
                    controlName: "slowChargeStartTime",
                    properties: {
                        unit: "",
                    },
                },
            );
        }

        return lines;
    };

    export function getChannelAddresses(
        component: EdgeConfig.Component,
    ): Promise<ChannelAddress[]> {
        const meterId = component.getPropertyFromComponent<string>("meter.id");

        return Promise.resolve([
            ...(meterId == null
                ? []
                : [new ChannelAddress(meterId, "ActivePower")]),
            new ChannelAddress(component.id, "_PropertyPeakShavingPower"),
            new ChannelAddress(component.id, "_PropertyRechargePower"),
            new ChannelAddress(component.id, "_PropertySlowChargeStartTime"),
            new ChannelAddress(component.id, "_PropertySlowChargePower"),
            new ChannelAddress(component.id, "_PropertyEndTime"),
            new ChannelAddress(component.id, "_PropertyStartTime"),
            new ChannelAddress(component.id, "_PropertyEndDate"),
            new ChannelAddress(component.id, "_PropertyStartDate"),
            new ChannelAddress(component.id, "_PropertyHysteresisSoc"),

            new ChannelAddress(component.id, "_PropertyMonday"),
            new ChannelAddress(component.id, "_PropertyTuesday"),
            new ChannelAddress(component.id, "_PropertyWednesday"),
            new ChannelAddress(component.id, "_PropertyThursday"),
            new ChannelAddress(component.id, "_PropertyFriday"),
            new ChannelAddress(component.id, "_PropertySaturday"),
            new ChannelAddress(component.id, "_PropertySunday"),
        ]);
    }

    export function getFormGroup(): FormGroup {
        const weekdayControls = Object.fromEntries(
            WEEKDAYS.map(({ controlName }) => [
                controlName,
                new FormControl(null),
            ]),
        );
        return new FormGroup({
            peakShavingPower: new FormControl(null),
            rechargePower: new FormControl(null),
            slowChargeStartTime: new FormControl(null),
            slowChargePower: new FormControl(null),
            endTime: new FormControl(null),
            startTime: new FormControl(null),
            endDate: new FormControl(null),
            startDate: new FormControl(null),
            hysteresisSoc: new FormControl(null),
            ...weekdayControls,
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            {
                baseString:
                    "controller/peak-shaving-symmetric-time-slot/" +
                    component.id,
            },
            { name: "trending-down-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [
                NavigationConstants.CommonNodes.HISTORY(translate),
                NavigationConstants.CommonNodes.SETTINGS(translate),
            ],
            null,
        ).toConstructorParams();
    }

    export const SHARED_ICON: Icon = {
        name: "trending-down-outline",
        color: "medium",
        size: "large",
    };
}
