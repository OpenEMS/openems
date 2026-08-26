import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";
import { SharedControllerModbusTcpApiReadWrite } from "../../shared/shared";

@Component({
    selector: "oe-controller-modbus-tcp-api-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        BaseChartDirective,
        ReactiveFormsModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ControllerModbusTcpApiChartComponent extends AbstractHistoryChart {
    public static getChartData(
        component: EdgeConfig.Component,
        config: EdgeConfig,
        chartType: "line" | "bar",
        translate: TranslateService,
    ): HistoryUtils.ChartData {
        const writeChannels: SharedControllerModbusTcpApiReadWrite.ChannelId[] | null =
            component.getPropertyFromComponent<SharedControllerModbusTcpApiReadWrite.ChannelId[]>("writeChannels");

        const yAxes: HistoryUtils.yAxes[] = [
            {
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
        ];

        if (chartType === "line") {
            yAxes.push({
                unit: YAxisType.PERCENTAGE,
                position: "right",
                yAxisId: ChartAxis.RIGHT,
            });
        }

        const input: HistoryUtils.InputChannel[] = [];

        if (writeChannels != null) {
            writeChannels.forEach((c) => {
                input.push({
                    name: c,
                    powerChannel: ChannelAddress.fromString(component.id + `/${c}`),
                });
            });
        }

        input.push({
            name: "Soc",
            powerChannel: ChannelAddress.fromString("_sum/EssSoc"),
        });

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                const values: HistoryUtils.DisplayValue[] = [
                    ...(writeChannels
                        ?.filter((channelId) => data[channelId]?.some((value) => value !== null))
                        ?.map((channelId) => {
                            const channel = new SharedControllerModbusTcpApiReadWrite.ModbusTcpApiChannel(
                                component.id,
                                channelId,
                            );
                            return {
                                name: channel.translatedName(translate),
                                converter: () => data[channelId],
                                color: channel.color,
                            };
                        }) ?? []),
                ];

                if (chartType === "line") {
                    values.push({
                        name: translate.instant("GENERAL.SOC"),
                        converter: () => data["Soc"].map((el) => NumberUtils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.GREY,
                        borderDash: [10, 10],
                        yAxisId: ChartAxis.RIGHT,
                    });
                }

                return values;
            },
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes,
        };
    }

    public override getChartData() {
        const component = this.config.getComponentSafely(this.routeService.getRouteParam<string>("componentId"));
        AssertionUtils.assertIsDefined(component);

        return ControllerModbusTcpApiChartComponent.getChartData(
            component,
            this.config,
            this.chartType,
            this.translate,
        );
    }
}
