import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-chp-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
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
export class ControllerChpChartComponent extends AbstractHistoryChart {
    public static getChartData(
        component: EdgeConfig.Component,
        translate: TranslateService,
        chartType: "line" | "bar",
    ): HistoryUtils.ChartData {
        const outputChannel = component.getPropertyFromComponent<string>("outputChannelAddress");
        const outputChannelAddress = ChannelAddress.fromStringSafely(outputChannel);
        AssertionUtils.assertIsDefined(outputChannelAddress);

        const input: HistoryUtils.InputChannel[] = [
            {
                name: "OutputChannel",
                powerChannel: outputChannelAddress,
            },
            {
                name: "CumulatedActiveTime",
                energyChannel: new ChannelAddress(component.id, "CumulatedActiveTime"),
            },
        ];

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [];

                if (chartType === "line") {
                    output.push({
                        name: translate.instant("GENERAL.STATE"),
                        converter: () =>
                            data["OutputChannel"]?.map((val) => {
                                const value = NumberUtils.multiplySafely(val, 1000);
                                return value ?? null;
                            }),
                        color: ChartConstants.Colors.BLUE,
                        stack: 0,
                    });
                }

                if (chartType === "bar") {
                    output.push({
                        name: translate.instant("EDGE.INDEX.WIDGETS.CHANNELTRESHOLD.ACTIVE_TIME_OVER_PERIOD"),
                        nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyQueryResponse?.result.data[component.id + "/CumulatedActiveTime"] ?? null;
                        },
                        converter: () =>
                            data["CumulatedActiveTime"]?.map((val) => {
                                const value = NumberUtils.multiplySafely(val, 1000);
                                return value ?? null;
                            }),
                        color: ChartConstants.Colors.YELLOW,
                    });
                }
                return output;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.NO_DECIMALS,
            },
            yAxes: [
                {
                    unit: chartType === "line" ? YAxisType.RELAY : YAxisType.TIME,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
            ],
        };
    }

    public override getChartData() {
        const edge = this.edge ?? this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = this.config ?? edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = this.component ?? config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);
        return ControllerChpChartComponent.getChartData(component, this.translate, this.chartType);
    }
}
