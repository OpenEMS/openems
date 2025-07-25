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
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChannelAddress, ChartConstants, EdgeConfig } from "../../../../../../shared/shared";

@Component({
    selector: "common-storage-details-ess-chart",
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
export class StorageEssChartComponent extends AbstractHistoryChart {

    public static getChartData(translate: TranslateService, essComponent: EdgeConfig.Component, chartType: "line" | "bar", config: EdgeConfig): HistoryUtils.ChartData {

        const yAxes: HistoryUtils.yAxes[] = [{
            unit: YAxisType.ENERGY,
            position: "left",
            yAxisId: ChartAxis.LEFT,
        }];

        if (chartType === "line") {
            yAxes.push({
                unit: YAxisType.PERCENTAGE,
                position: "right",
                yAxisId: ChartAxis.RIGHT,
            });
        }

        const input: HistoryUtils.InputChannel[] = [
            {
                name: essComponent.id + "Charge",
                powerChannel: ChannelAddress.fromString(essComponent.id + "/DcDischargePower"),
                energyChannel: ChannelAddress.fromString(essComponent.id + "/DcChargeEnergy"),
                ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
            },
            {
                name: essComponent.id + "Discharge",
                powerChannel: ChannelAddress.fromString(essComponent.id + "/DcDischargePower"),
                energyChannel: ChannelAddress.fromString(essComponent.id + "/DcDischargeEnergy"),
                ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO }),
            },
            {
                name: "Soc",
                powerChannel: ChannelAddress.fromString(essComponent.id + "/Soc"),
            },
        ];

        const emergencyReserveComponent: EdgeConfig.Component | null = config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component =>
                component != null && component.isEnabled && essComponent.hasPropertyValue("ess.id", essComponent.id))[0] ?? null;
        const isReserveSocEnabled = config.getPropertyFromComponent<boolean>(emergencyReserveComponent, "isReserveSocEnabled");
        if (emergencyReserveComponent && isReserveSocEnabled) {
            input.push({
                name: "EmergencyReserve",
                powerChannel: new ChannelAddress(emergencyReserveComponent.id, "ActualReserveSoc"),
            });
        }

        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                const output: HistoryUtils.DisplayValue[] = [{
                    name: translate.instant("General.CHARGE"),
                    converter: () => data[essComponent.id + "Charge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data[essComponent.id + "/DcChargeEnergy"],
                    stack: 0,
                    color: ChartConstants.Colors.GREEN,
                },
                {
                    name: translate.instant("General.DISCHARGE"),
                    converter: () => data[essComponent.id + "Discharge"]?.map(el => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(el)),
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data[essComponent.id + "/DcDischargeEnergy"],
                    stack: 1,
                    color: ChartConstants.Colors.RED,
                },
                ];

                if (chartType === "line") {
                    output.push({
                        name: translate.instant("General.soc"),
                        converter: () => data["Soc"].map(el => Utils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.GREY,
                        borderDash: [10, 10],
                        yAxisId: ChartAxis.RIGHT,
                    });

                }
                if (emergencyReserveComponent && isReserveSocEnabled) {
                    output.push({
                        name: translate.instant("Edge.Index.EmergencyReserve.EMERGENCY_RESERVE"),
                        converter: () => data["EmergencyReserve"].map(el => Utils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.BLACK,
                        yAxisId: ChartAxis.RIGHT,
                        borderDash: [3, 3],
                    });
                }
                return output;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: yAxes,
        };
    }

    public override getChartData() {
        const component = this.config.getComponent(this.route.snapshot.params.componentId);
        return StorageEssChartComponent.getChartData(this.translate, component, this.chartType, this.config);
    }
}
