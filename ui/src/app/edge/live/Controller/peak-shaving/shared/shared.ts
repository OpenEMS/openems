import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerPeakShaving {

    export const getFormlyFlatLines = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => ([
        ...getSingleMeasuredLine(translate, component),
        ...getChargeLines(translate, component),
    ]);

    export const getSingleMeasuredLine = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];

        const meterId = component.getPropertyFromComponent<string>("meter.id");
        const activePowerChannel = meterId + "/ActivePower";

        if (activePowerChannel == null || meterId == null) {
            return lines;
        }

        lines.push({
            type: "channel-line",
            name: translate.instant("GENERAL.MEASURED_VALUE"),
            channel: activePowerChannel,
            converter: Converter.GRID_BUY_POWER_OR_ZERO,
        });
        return lines;
    };

    export const getChargeLines = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => ([
        {
            type: "channel-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER"),
            channel: component.id + "/_PropertyPeakShavingPower",
            converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        },
        {
            type: "channel-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER"),
            channel: component.id + "/_PropertyRechargePower",
            converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        },
    ]);

    export const getSettingsInputLines = (translate: TranslateService, edge: Edge): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];
        if (edge.roleIsAtLeast(Role.OWNER)) {
            lines.push(
                {
                    type: "input-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER"),
                    controlName: "peakShavingPower",
                    properties: {
                        unit: "W",
                    },
                },
                {
                    type: "input-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER"),
                    controlName: "rechargePower",
                    properties: {
                        unit: "W",
                    },
                }
            );
        }

        return lines;
    };

    export function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const peakShavingSymmetricComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(peakShavingSymmetricComponent);

        const meterId = peakShavingSymmetricComponent.getPropertyFromComponent<string>("meter.id");

        return Promise.resolve([
            ...(meterId == null ? [] : [
                new ChannelAddress(meterId, "ActivePower"),
                new ChannelAddress(meterId, "ActivePowerL1"),
                new ChannelAddress(meterId, "ActivePowerL2"),
                new ChannelAddress(meterId, "ActivePowerL3"),
            ]),
            new ChannelAddress(peakShavingSymmetricComponent.id, "_PropertyPeakShavingPower"),
            new ChannelAddress(peakShavingSymmetricComponent.id, "_PropertyRechargePower"),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            peakShavingPower: new FormControl(null),
            rechargePower: new FormControl(null),
        });
    }
}

