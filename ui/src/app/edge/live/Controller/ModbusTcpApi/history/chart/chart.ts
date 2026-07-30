import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

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
        showPhases: boolean,
    ): HistoryUtils.ChartData {
        let writeChannels: string[] | null = component.getPropertyFromComponent<string[]>("writeChannels");

        const colors: string[] = [
            "rgb(191, 144, 33)",
            "rgb(162, 191, 33)",
            "rgb(86, 191, 33)",
            "rgb(33, 191, 165)",
            "rgb(33, 115, 191)",
        ];

        const input: HistoryUtils.InputChannel[] = [
            {
                name: "SetActivePowerEquals",
                powerChannel: ChannelAddress.fromString(component.id + "/Ess0SetActivePowerEquals"),
            },
        ];

        if (writeChannels != null) {
            writeChannels = writeChannels.filter((c) => !c.includes("Ess0SetActivePowerEquals"));
            writeChannels.forEach((c) => {
                input.push({
                    name: c,
                    powerChannel: ChannelAddress.fromString(component.id + `/${c}`),
                });
            });
        }

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                const values: HistoryUtils.DisplayValue[] = [
                    {
                        name: translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS"),
                        converter: () => data["SetActivePowerEquals"],
                        color: "rgb(214, 28, 28)",
                    },
                ];
                if (writeChannels) {
                    writeChannels.forEach((c: string, index: number) => {
                        const name: string = c;
                        // Add translations for active power channels of ess0
                        if (c.includes("Ess0SetActive")) {
                            const channelName = c.replace("Ess0", "");
                            switch (channelName) {
                                case "SetActivePowerEquals":
                                    return translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS");
                                case "SetActivePowerGreaterOrEquals":
                                    return translate.instant(
                                        "MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_GREATER_OR_EQUALS",
                                    );
                                case "SetActivePowerLessOrEquals":
                                    return translate.instant(
                                        "MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_LESS_OR_EQUALS",
                                    );
                            }
                        }

                        values.push({
                            name: name,
                            converter: () => data[c],
                            color: colors[index],
                        });
                    });
                }
                return values;
            },
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes: [
                {
                    unit: YAxisType.ENERGY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
            ],
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
            this.showPhases,
        );
    }
}
