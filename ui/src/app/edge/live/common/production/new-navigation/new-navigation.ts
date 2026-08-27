import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { EnergySchedulerV2 as EnergyScheduler, EnergySchedulerV2, } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { User } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { TimeLineChartComponent } from "../../../../../shared/components/chart/timeline-chart/timeline-chart";
import { LiveDataService } from "../../../livedataservice";
import { ProductionChartComponent } from "./chart/production-chart-component";

@Component({
    selector: "oe-common-production-new-navigation",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CommonProductionHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected productionMeterComponents: EdgeConfig.Component[] = [];
    protected chargerComponents: EdgeConfig.Component[] = [];

    private readonly userService = inject(UserService);

    public static async getFormlyGeneralView(
        translate: TranslateService,
        service: Service,
        user: User | null,
        edge: Edge,
        energyScheduler: EnergySchedulerV2,
        productionMeterComponents: EdgeConfig.Component[],
        chargerComponents: EdgeConfig.Component[],
    ): Promise<OeFormlyView> {
        await energyScheduler?.updateSchedule(edge, service.websocket);
        const lines: OeFormlyField[] = [];

        if (energyScheduler.schedule !== GetSchedule.Response.empty) {
            // TODO INTERSOLAR
            if (user?.id == "intersolar@fenecon.de" || edge.id == "fems888") {
                const energyToday = energyScheduler.schedule.calculateEnergyFromPower("today", "ProductionActivePower");
                const energyTomorrow = energyScheduler.schedule.calculateEnergyFromPower(
                    "tomorrow",
                    "ProductionActivePower",
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
                    channel: new ChannelAddress("_sum", "ProductionActivePower").toString(),
                    converter: Converter.POWER_IN_KILO_WATT,
                    style: {
                        name: { fontSize: "large" },
                        value: { fontSize: "large" },
                    },
                    cssClass: "ion-padding-top",
                },
                {
                    type: "component-line",
                    component: ProductionChartComponent,
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

        for (const meter of productionMeterComponents) {
            lines.push({
                type: "channel-line",
                name: Name.METER_ALIAS_OR_ID(meter),
                channel: new ChannelAddress(meter.id, "ActivePower").toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }
        for (const charger of chargerComponents) {
            lines.push({
                type: "channel-line",
                name: Name.METER_ALIAS_OR_ID(charger),
                channel: new ChannelAddress(charger.id, "ActualPower").toString(),
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        return {
            title: translate.instant("GENERAL.PRODUCTION"),
            helpKey: "REDIRECT.COMMON_PRODUCTION",
            lines: lines,
            component: new EdgeConfig.Component(),
            isCommonWidget: true,
        };
    }

    protected override async generateView(): Promise<OeFormlyView> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const user = this.userService.currentUser();

        // Get Chargers
        this.chargerComponents = config
            .getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            .filter((component) => component.isEnabled);

        // Get productionMeters
        this.productionMeterComponents = config
            .getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter((component) => component.isEnabled && config.isProducer(component));

        const energy = new EnergyScheduler(config);

        return CommonProductionHomeComponent.getFormlyGeneralView(
            this.translate,
            this.service,
            user,
            edge,
            energy,
            this.productionMeterComponents,
            this.chargerComponents,
        );
    }
}
