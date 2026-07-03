import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerPeakShaving {
    const PEAK_SHAVING_POWER_CHANNEL = "_PropertyPeakShavingPower";
    const RECHARGE_POWER_CHANNEL = "_PropertyRechargePower";

    type SetFormControlWithChannel = (
        form: FormGroup,
        controlName: string,
        currentData: CurrentData,
        channelAddress: ChannelAddress,
    ) => void;

    export const getFormlyFlatLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => [
        ...getSingleMeasuredLine(translate, component),
        ...getChargeLines(translate, component),
    ];

    export const getSingleMeasuredLine = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];

        const meterId = component.getPropertyFromComponent<string>("meter.id");
        if (meterId == null) {
            return lines;
        }

        const activePowerChannel = meterId + "/ActivePower";

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.MEASURED_VALUE"),
            channel: activePowerChannel,
            converter: Converter.GRID_BUY_POWER_OR_ZERO,
        });
        return lines;
    };

    export const getChargeLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView["lines"] => [
        {
            type: "channel-line",
            name: translate.instant(
                "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
            ),
            channel: component.id + "/" + PEAK_SHAVING_POWER_CHANNEL,
            converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        },
        {
            type: "channel-line",
            name: translate.instant(
                "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
            ),
            channel: component.id + "/" + RECHARGE_POWER_CHANNEL,
            converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        },
    ];

    export function getWidgetChannelAddresses(
        meterId: string | null | undefined,
        componentId: string,
    ): ChannelAddress[] {
        return [
            ...(meterId == null
                ? []
                : [new ChannelAddress(meterId, "ActivePower")]),
            new ChannelAddress(componentId, PEAK_SHAVING_POWER_CHANNEL),
            new ChannelAddress(componentId, RECHARGE_POWER_CHANNEL),
        ];
    }

    export function getWidgetValues(
        currentData: CurrentData,
        meterId: string | null | undefined,
        peakShavingPower: number,
        rechargePower: number,
    ): {
        activePower: number;
        peakShavingPower: number;
        rechargePower: number;
    } {
        const activePower =
            meterId == null
                ? 0
                : Math.max(
                      0,
                      currentData.allComponents[meterId + "/ActivePower"] ?? 0,
                  );

        return {
            activePower,
            peakShavingPower,
            rechargePower,
        };
    }

    export function setSettingsCurrentData(
        form: FormGroup,
        currentData: CurrentData,
        componentId: string,
        setFormControlSafelyWithChannel: SetFormControlWithChannel,
    ): void {
        setFormControlSafelyWithChannel(
            form,
            "peakShavingPower",
            currentData,
            new ChannelAddress(componentId, PEAK_SHAVING_POWER_CHANNEL),
        );
        setFormControlSafelyWithChannel(
            form,
            "rechargePower",
            currentData,
            new ChannelAddress(componentId, RECHARGE_POWER_CHANNEL),
        );
    }

    export const getSettingsInputLines = (
        translate: TranslateService,
        edge: Edge,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];
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
            );
        }

        return lines;
    };

    export function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const peakShavingSymmetricComponent =
            component ??
            config.getComponentSafely(
                routeService.getRouteParam("componentId"),
            );
        AssertionUtils.assertIsDefined(peakShavingSymmetricComponent);

        const meterId =
            peakShavingSymmetricComponent.getPropertyFromComponent<string>(
                "meter.id",
            );

        return Promise.resolve([
            ...(meterId == null
                ? []
                : [
                      new ChannelAddress(meterId, "ActivePower"),
                      new ChannelAddress(meterId, "ActivePowerL1"),
                      new ChannelAddress(meterId, "ActivePowerL2"),
                      new ChannelAddress(meterId, "ActivePowerL3"),
                  ]),
            new ChannelAddress(
                peakShavingSymmetricComponent.id,
                PEAK_SHAVING_POWER_CHANNEL,
            ),
            new ChannelAddress(
                peakShavingSymmetricComponent.id,
                RECHARGE_POWER_CHANNEL,
            ),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            peakShavingPower: new FormControl(null),
            rechargePower: new FormControl(null),
        });
    }
}
