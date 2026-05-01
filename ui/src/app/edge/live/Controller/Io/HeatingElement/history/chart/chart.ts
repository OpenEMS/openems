// @ts-strict-ignore
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-io-heatingelement-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ControllerIoHeatingElementChartComponent extends AbstractHistoryChart {
    public static getChartData(config: EdgeConfig, translate: TranslateService, component: EdgeConfig.Component, phaseColors: string[], chartType: "line" | "bar"): HistoryUtils.ChartData {

        const consumptionMeter: EdgeConfig.Component = config.getComponentSafely(component.properties["meter.id"]);

        const input: HistoryUtils.InputChannel[] = [
            { name: component.id, powerChannel: new ChannelAddress(component.id, "Level") },
        ];

        if (consumptionMeter != null && consumptionMeter.isEnabled) {
            input.push({
                name: consumptionMeter.id + "/ActivePower",
                powerChannel: ChannelAddress.fromString(consumptionMeter.id + "/ActivePower"),
                energyChannel: ChannelAddress.fromString(consumptionMeter.id + "/ActiveProductionEnergy"),
            });
        }

        for (const level of [1, 2, 3]) {
            input.push({
                name: component.id + level,
                powerChannel: new ChannelAddress(component.id, "Level"),
                energyChannel: new ChannelAddress(component.id, "Level" + level + "CumulatedTime"),
            });
        }

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                const output: HistoryUtils.DisplayValue[] = [];

                if (chartType === "line") {
                    output.push({
                        name: "Level",
                        converter: () => data[component.id].map(val => Utils.multiplySafely(val, 1000)),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                        yAxisId: ChartAxis.LEFT,
                    });

                }

                if (chartType === "bar") {
                    for (const level of [1, 2, 3]) {
                        output.push({
                            name: "Level " + level,
                            nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) =>
                                energyQueryResponse?.result.data[component.id + "/Level" + level + "CumulatedTime"] ?? null,
                            converter: () => data[component.id + level]
                                // TODO add logic to not have to adjust non power data manually
                                .map(val => Utils.multiplySafely(val, 1000)),
                            color: phaseColors[level % phaseColors.length],
                            stack: 0,
                            yAxisId: ChartAxis.LEFT,
                        });
                    }
                }

                if (consumptionMeter != null && consumptionMeter.isEnabled) {
                    output.push({
                        name: translate.instant("GENERAL.CONSUMPTION"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[consumptionMeter.id + "/ActiveProductionEnergy"],
                        converter: () =>
                            data[consumptionMeter.id + "/ActivePower"] ?? null,
                        color: ChartConstants.Colors.YELLOW,
                        stack: 1,
                        yAxisId: ChartAxis.RIGHT,
                    });
                }

                return output;
            },
            tooltip: {
                formatNumber: "1.0-2",
            },
            yAxes:
                consumptionMeter && consumptionMeter.isEnabled ?
                    [
                        {
                            unit: YAxisType.ENERGY,
                            position: "right",
                            yAxisId: ChartAxis.RIGHT,
                        },
                        {
                            unit: chartType === "line"
                                ? YAxisType.HEATING_ELEMENT
                                : YAxisType.TIME,
                            position: "left",
                            yAxisId: ChartAxis.LEFT,

                        },
                    ]
                    :
                    [
                        {
                            unit: chartType === "line"
                                ? YAxisType.HEATING_ELEMENT
                                : YAxisType.TIME,
                            position: "left",
                            yAxisId: ChartAxis.LEFT,

                        },
                    ],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        const edge = this.edge ?? this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = this.config ?? edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = this.component ?? config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);
        return ControllerIoHeatingElementChartComponent.getChartData(this.config, this.translate, component, AbstractHistoryChart.phaseColors, this.chartType);
    }
}
