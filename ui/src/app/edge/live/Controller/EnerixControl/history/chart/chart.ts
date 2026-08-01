import { CommonModule } from "@angular/common";
import { Component, ChangeDetectionStrategy } from "@angular/core";
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
    selector: "oe-controller-enerix-control-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
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
export class EnerixControlChartComponent extends AbstractHistoryChart {
    public static getChartData(
        component: EdgeConfig.Component,
        chartType: "line" | "bar",
        translate: TranslateService,
    ): HistoryUtils.ChartData {
        const input: HistoryUtils.InputChannel[] = [
            {
                name: "RemoteControlMode",
                powerChannel: new ChannelAddress(component.id, "RemoteControlMode"),
            },
            {
                name: "CumulatedInactiveTime",
                energyChannel: new ChannelAddress(component.id, "CumulatedInactiveTime"),
            },
            {
                name: "CumulatedNoDischargeTime",
                energyChannel: new ChannelAddress(component.id, "CumulatedNoDischargeTime"),
            },
            {
                name: "CumulatedChargeFromGridTime",
                energyChannel: new ChannelAddress(component.id, "CumulatedChargeFromGridTime"),
            },
        ];

        return {
            input,
            output: (data: HistoryUtils.ChannelData) => {
                if (chartType === "line") {
                    return [
                        {
                            name: translate.instant("GENERAL.STATE"),
                            converter: () =>
                                data["RemoteControlMode"]?.map((val) => {
                                    const value = NumberUtils.multiplySafely(val, 1000);
                                    return value == null ? null : NumberUtils.addSafely(value, 1);
                                }),
                            color: ChartConstants.Colors.RED,
                            stack: 0,
                        },
                    ];
                }

                return [
                    {
                        name: translate.instant("GENERAL.OFF"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/CumulatedInactiveTime"],
                        converter: () =>
                            data["CumulatedInactiveTime"]?.map((val) => {
                                return NumberUtils.multiplySafely(val, 1000);
                            }),
                        color: ChartConstants.Colors.DARK_GREY,
                        stack: 0,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/CumulatedNoDischargeTime"],
                        converter: () =>
                            data["CumulatedNoDischargeTime"]?.map((val) => {
                                return NumberUtils.multiplySafely(val, 1000);
                            }),
                        color: ChartConstants.Colors.YELLOW,
                        stack: 0,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_FROM_GRID"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/CumulatedChargeFromGridTime"],
                        converter: () =>
                            data["CumulatedChargeFromGridTime"]?.map((val) => {
                                return NumberUtils.multiplySafely(val, 1000);
                            }),
                        color: ChartConstants.Colors.RED,
                        stack: 0,
                    },
                ];
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.NO_DECIMALS,
            },
            yAxes: [
                {
                    unit: chartType === "line" ? YAxisType.ENERIX_CONTROL : YAxisType.TIME,
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
        return EnerixControlChartComponent.getChartData(component, this.chartType, this.translate);
    }
}
