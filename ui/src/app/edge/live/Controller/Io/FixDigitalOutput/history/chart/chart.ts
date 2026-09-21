import { CommonModule } from "@angular/common";
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";

import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-io-digital-output-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
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
export class TotalChartComponent extends AbstractHistoryChart {
    public static getChartData(config: EdgeConfig, chartType: "bar" | "line"): HistoryUtils.ChartData {
        const fixDigitalOutputControllers: EdgeConfig.Component[] = config.getComponentsByFactory(
            "Controller.Io.FixDigitalOutput",
        );
        const singleThresholdControllers: EdgeConfig.Component[] = config.getComponentsByFactory(
            "Controller.IO.ChannelSingleThreshold",
        );
        const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];
        const input: HistoryUtils.InputChannel[] = [];

        for (const controller of controllers) {
            const powerChannel = ChannelAddress.fromString(
                Array.isArray(config.getComponentProperties(controller.id)["outputChannelAddress"])
                    ? config.getComponentProperties(controller.id)["outputChannelAddress"][0]
                    : config.getComponentProperties(controller.id)["outputChannelAddress"],
            );
            input.push({
                name: controller.id,
                powerChannel: powerChannel,
                energyChannel: new ChannelAddress(controller.id, "CumulatedActiveTime"),
            });
        }

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [];

                for (let i = 0; i < controllers.length; i++) {
                    const controller = controllers[i];
                    output.push({
                        name: Name.METER_ALIAS_OR_ID(controller),
                        nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
                            return energyQueryResponse?.result.data[controller.id + "/CumulatedActiveTime"] ?? null;
                        },
                        converter: () => {
                            return (
                                data[controller.id]
                                    // TODO add logic to not have to adjust non power data manually
                                    .map((val) => NumberUtils.multiplySafely(val, 1000))
                            );
                        },
                        color: ChartConstants.Colors.SHADES_OF_YELLOW[
                            i % (ChartConstants.Colors.SHADES_OF_YELLOW.length - 1)
                        ],
                        stack: 0,
                    });
                }
                return output;
            },
            tooltip: {
                formatNumber: "1.0-0",
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

    protected override getChartData(): HistoryUtils.ChartData {
        return TotalChartComponent.getChartData(this.config, this.chartType);
    }
}
