// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { calculateResolution, ChronoUnit } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";

import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-chp-cost-optimization-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, component: EdgeConfig.Component, translate: TranslateService): HistoryUtils.ChartData {

        const meterId = config.getPropertyFromComponent<string>(component, "meter.id");
        const input: HistoryUtils.InputChannel[] = [
            {
                name: "GridConsumption",
                powerChannel: new ChannelAddress(meterId, "ActivePower"),

            },
            {
                name: "ActivePowerTarget",
                powerChannel: new ChannelAddress(component.id, "ActivePowerTarget"),
            },
            {
                name: "ChpActivePower",
                powerChannel: new ChannelAddress(component.id, "ChpActivePower"),
            },
            {
                name: "EnergyCosts",
                powerChannel: new ChannelAddress(component.id, "EnergyCosts"),
            },
            {
                name: "EnergyCostsWithoutChp",
                powerChannel: new ChannelAddress(component.id, "EnergyCostsWithoutChp"),
            },
            {
                name: "StateMachine",
                powerChannel: new ChannelAddress(component.id, "StateMachine"),
            },
        ];
        //console.log("All Data/Input CHP Cost Optimization Chart:", input);

        return {

            input: input,
            output: (data: HistoryUtils.ChannelData) => {
                //console.log("Alle Channel-Daten:", data);
                return [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.GRID_CONSUMPTION"),
                        color: ChartConstants.Colors.BLUE_GREY,
                        converter: () => data["GridConsumption"],
                        hideShadow: false,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.CHP.ACTIVE_POWER_TARGERT"),
                        color: ChartConstants.Colors.SHADES_OF_YELLOW[0],
                        borderDash: [3, 3],
                        converter: () => data["ActivePowerTarget"],
                        hideShadow: false,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.CHP.ACTIVE_POWER"),
                        color: ChartConstants.Colors.YELLOW,
                        converter: () => data["ChpActivePower"],
                        nameSuffix: () => "bla",
                        hideShadow: true,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.CHP.CURRENT_ENERGY_COSTS"),
                        color: ChartConstants.Colors.ORANGE,
                        converter: () => data["EnergyCosts"]?.map(value => Utils.multiplySafely(value, 1000)),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyValues?.result.data[component.id + "/currentEnergyCosts"];
                        },
                        hideShadow: true,
                        yAxisId: ChartAxis.RIGHT,
                        custom: {
                            type: "line",
                            customTitle: "€/h",
                            formatNumber: "1.0-0",
                        },
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.CHP.CURRENT_ENERGY_COSTS_WITHOUT_CHP"),
                        color: ChartConstants.Colors.GREEN,
                        borderDash: [3, 3],
                        converter: () => data["EnergyCostsWithoutChp"]?.map(value => Utils.multiplySafely(value, 1000)),
                        hideShadow: true,
                        yAxisId: ChartAxis.RIGHT,
                        custom: {
                            type: "line",
                            customTitle: "€/h",
                            formatNumber: "1.0-0",
                        },
                    },
                ];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.ENERGY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
                {
                    unit: YAxisType.NONE,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                    customTitle: "€/h",
                    scale: {
                        dynamicScale: true,
                    },
                },
            ],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ChartComponent.getChartData(this.config, this.component, this.translate);
    }

    protected override loadChart(): Promise<void> {
        const unit: ChronoUnit.Type = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;
        this.loadLineChart(unit);
        return;
    }
}
