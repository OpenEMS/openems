import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { SingleXAxisComponent } from "src/app/shared/components/chart/single-xaxis/single-xaxis";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";

import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { EvcsComponent } from "src/app/shared/components/edge/config-components/evcs/evcsComponent";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { ConsumptionChartComponent } from "./chart/consumption-chart-component";

@Component({
    selector: "oe-common-consumption",
    templateUrl:
        "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CommonConsumptionHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper:
        | "formly-field-modal"
        | "formly-field-navigation" = "formly-field-navigation";

    private evcss: EvcsComponent[] = [];
    private consumptionMeters: EdgeConfig.Component[] = [];

    public static async getFormlyGeneralView(
        translate: TranslateService,
        service: Service,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
        evcss: EvcsComponent[],
        consumptionMeters: EdgeConfig.Component[],
    ): Promise<OeFormlyView> {
        await energyScheduler?.updateSchedule(edge, service.websocket);

        const lines: OeFormlyField[] = [];

        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
            lines.push(
                {
                    type: "component-line",
                    component: SingleXAxisComponent,
                    inputs: {
                        data: energyScheduler?.schedule,
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.POWER"),
                    channel: new ChannelAddress(
                        "_sum",
                        "ProductionActivePower",
                    ).toString(),
                    converter: Converter.POWER_IN_KILO_WATT,
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: ConsumptionChartComponent,
                    inputs: {
                        edge: edge,
                        refresh: false,
                        data: energyScheduler?.schedule,
                    },
                },
                {
                    type: "horizontal-line",
                },
                {
                    type: "name-line",
                    name: translate.instant("GENERAL.DETAILS"),
                    style: {
                        name: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
            );
        }

        for (const evcs of evcss) {
            lines.push({
                type: "channel-line",
                name: evcs.alias ?? evcs.id,
                channel: evcs.powerChannel.toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }
        for (const consumptionMeter of consumptionMeters) {
            lines.push({
                type: "channel-line",
                name: consumptionMeter.alias ?? consumptionMeter.id,
                channel: ChannelAddress.fromString(
                    consumptionMeter.id + "/ActivePower",
                ).toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        if (evcss.length !== 0 || consumptionMeters.length !== 0) {
            lines.push(
                {
                    type: "horizontal-line",
                },
                {
                    type: "value-from-channels-line",
                    name: translate.instant("GENERAL.OTHER_CONSUMPTION"),
                    value: (currentData: CurrentData) =>
                        Converter.POSITIVE_POWER_IN_KILO_WATT(
                            Converter.CALCULATE_CONSUMPTION_OTHER_POWER(
                                evcss,
                                consumptionMeters,
                                currentData,
                            ),
                        ),
                    channelsToSubscribe: [
                        new ChannelAddress("_sum", "ConsumptionActivePower"),
                        ...consumptionMeters.map(
                            (el) => new ChannelAddress(el.id, "ActivePower"),
                        ),
                        ...evcss.map((el) => el.powerChannel),
                    ],
                },
            );
        }

        return {
            title: translate.instant("GENERAL.CONSUMPTION"),
            helpKey: "REDIRECT.COMMON_CONSUMPTION",
            useDefaultPrefix: false,
            isCommonWidget: true,
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const energy = new EnergySchedulerV2(config);

        this.evcss = EvcsComponent.getComponents(config, edge);
        this.consumptionMeters = config
            .getComponentsImplementingNature(
                "io.openems.edge.meter.api.ElectricityMeter",
            )
            .filter(
                (component) =>
                    component.isEnabled &&
                    config.isTypeConsumptionMetered(component),
            );

        return CommonConsumptionHomeComponent.getFormlyGeneralView(
            this.translate,
            this.service,
            edge,
            energy,
            this.evcss,
            this.consumptionMeters,
        );
    }
}
