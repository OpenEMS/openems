import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";

import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { EvcsComponent } from "src/app/shared/components/edge/config-components/evcs/evcsComponent";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { User } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { LiveDataService } from "../../../livedataservice";
import { ConsumptionChartComponent } from "./chart/consumption-chart-component";

@Component({
    selector: "oe-common-consumption",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CommonConsumptionHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private readonly userService = inject(UserService);

    private evcss: EvcsComponent[] = [];
    private consumptionMeters: EdgeConfig.Component[] = [];

    public static async getFormlyGeneralView(
        translate: TranslateService,
        service: Service,
        user: User | null,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
        evcss: EvcsComponent[],
        consumptionMeters: EdgeConfig.Component[],
    ): Promise<OeFormlyView> {
        await energyScheduler?.updateSchedule(edge, service.websocket);
        const lines: OeFormlyField[] = [];

        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
            // TODO INTERSOLAR
            if (user?.id == "intersolar@fenecon.de" || edge.id == "fems888") {
                const energyToday = energyScheduler.schedule.calculateEnergyFromPower(
                    "today",
                    "ConsumptionActivePower",
                );
                const energyTomorrow = energyScheduler.schedule.calculateEnergyFromPower(
                    "tomorrow",
                    "ConsumptionActivePower",
                );
                lines.push({
                    type: "stats-line",
                    stats: [
                        {
                            name: translate.instant("EDGE.HISTORY.TODAY"),
                            value: energyToday.history,
                            unit: "kWh",
                            predictionValue: energyToday.prediction,
                        },
                        {
                            name: translate.instant("EDGE.HISTORY.TOMORROW"),
                            value: energyTomorrow.prediction,
                            unit: "kWh",
                        },
                    ],
                });
            }

            lines.push(
                {
                    type: "component-line",
                    component: TimeLineChartComponent,
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
                    channel: new ChannelAddress("_sum", "ConsumptionActivePower").toString(),
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
                channel: ChannelAddress.fromString(consumptionMeter.id + "/ActivePower").toString(),
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
                            Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData),
                        ),
                    channelsToSubscribe: [
                        new ChannelAddress("_sum", "ConsumptionActivePower"),
                        ...consumptionMeters.map((el) => new ChannelAddress(el.id, "ActivePower")),
                        ...evcss.map((el) => el.powerChannel),
                    ],
                },
            );
        }

        return {
            title: translate.instant("GENERAL.CONSUMPTION"),
            helpKey: "REDIRECT.COMMON_CONSUMPTION",
            isCommonWidget: true,
            lines: lines,
            component: new EdgeConfig.Component(),
        };
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const user = this.userService.currentUser();
        const energy = new EnergySchedulerV2(config);

        this.evcss = EvcsComponent.getComponents(config, edge);
        this.consumptionMeters = config
            .getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(
                (component) =>
                    component.isEnabled &&
                    config.isTypeConsumptionMetered(component) &&
                    !this.evcss.some((el) => el.id === component.id),
            );

        return CommonConsumptionHomeComponent.getFormlyGeneralView(
            this.translate,
            this.service,
            user,
            edge,
            energy,
            this.evcss,
            this.consumptionMeters,
        );
    }
}
