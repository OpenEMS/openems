import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChannelAddress, ChartConstants, EdgeConfig } from "../../../../../shared/shared";

@Component({
    selector: "common-storage-total-chart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    imports: [
        CommonUiModule,
        BaseChartDirective,
        ReactiveFormsModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})

export class StorageTotalChartComponent extends AbstractHistoryChart {

    public static getChartData(translate: TranslateService, chartType: string, config: EdgeConfig): HistoryUtils.ChartData {

        const essComponents = config?.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => !component.factoryId.includes("Ess.Cluster"));

        const essComponent: EdgeConfig.Component | null = essComponents?.length === 1 ? essComponents[0] : null;

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

        const input: HistoryUtils.InputChannel[] = [];
        input.push(
            {
                name: "_sum/EssActivePower",
                powerChannel: ChannelAddress.fromString("_sum/EssActivePower"),
            },
            {
                name: "_sum/Charge",
                energyChannel: ChannelAddress.fromString("_sum/EssDcChargeEnergy"),
            },
            {
                name: "_sum/Discharge",
                energyChannel: ChannelAddress.fromString("_sum/EssDcDischargeEnergy"),
            },
            {
                name: "_sum/ProductionDcActualPower",
                powerChannel: ChannelAddress.fromString("_sum/ProductionDcActualPower"),
                energyChannel: ChannelAddress.fromString("_sum/ProductionActiveEnergy"),
            },
            {
                name: "Soc",
                powerChannel: ChannelAddress.fromString("_sum/EssSoc"),
            });


        const emergencyReserveComponent: EdgeConfig.Component | null = config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component => component.isEnabled)[0] ?? null;
        const isReserveSocEnabled = config.getPropertyFromComponent<boolean>(emergencyReserveComponent, "isReserveSocEnabled");

        if (essComponents.length === 1 && emergencyReserveComponent != null && isReserveSocEnabled) {
            input.push({
                name: "EmergencyReserve",
                powerChannel: new ChannelAddress(emergencyReserveComponent.id, "ActualReserveSoc"),
            });
        }

        if (essComponent !== null && config.hasComponentNature("io.openems.edge.ess.api.AsymmetricEss", essComponent.id)) {
            input.push(...Phase.THREE_PHASE.map((phase, i) => ({
                name: translate.instant("General.phase") + " " + phase,
                powerChannel: new ChannelAddress(essComponent.id, "ActivePower" + phase),
            })));
        }
        return {
            input: input,
            output: (data: HistoryUtils.ChannelData) => {

                let totalData: number[] = [];

                if (chartType === "line") {
                    if (config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").length > 0) {
                        data["_sum/ProductionDcActualPower"]?.forEach((value, index) => {
                            if (data["_sum/ProductionDcActualPower"][index] != null && data["_sum/ProductionDcActualPower"][index] != undefined) {
                                totalData[index] = Utils.subtractSafely(data["_sum/EssActivePower"][index], value) as number;
                            }
                        });
                    } else {
                        totalData = data["_sum/EssActivePower"];
                    }
                }

                const output: HistoryUtils.DisplayValue[] = [{
                    name: translate.instant("General.CHARGE"),
                    converter: () => chartType === "line" ? totalData?.map(value => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(value)) : data["_sum/Charge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data["_sum/EssDcChargeEnergy"],
                    color: ChartConstants.Colors.GREEN,
                    stack: 0,
                },
                {
                    name: translate.instant("General.DISCHARGE"),
                    converter: () => chartType === "line" ? totalData?.map(value => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value)) : data["_sum/Discharge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => energyResponse.result.data["_sum/EssDcDischargeEnergy"],
                    color: ChartConstants.Colors.RED,
                    stack: 1,
                }];

                if (essComponent !== null && config.hasComponentNature("io.openems.edge.ess.api.AsymmetricEss", essComponent.id)) {
                    output.push(...Phase.THREE_PHASE.map((phase, i) => ({
                        name: translate.instant("General.phase") + " " + phase,
                        converter: () => data[essComponent.id + "/ActivePower" + phase],
                        stack: 1,
                        color: ChartConstants.Colors.DEFAULT_PHASES_COLORS[i],
                    })));
                }

                if (chartType === "line") {
                    output.push({
                        name: translate.instant("General.soc"),
                        converter: () => data["Soc"]?.map(el => Utils.multiplySafely(el, 1000)),
                        color: ChartConstants.Colors.GREY,
                        borderDash: [10, 10],
                        yAxisId: ChartAxis.RIGHT,
                    });
                }

                if (emergencyReserveComponent != null && isReserveSocEnabled) {
                    output.push({
                        name: translate.instant("Edge.Index.EmergencyReserve.EMERGENCY_RESERVE"),
                        converter: () => data["EmergencyReserve"]?.map(el => Utils.multiplySafely(el, 1000)),
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
        return StorageTotalChartComponent.getChartData(this.translate, this.chartType, this.config);
    }
}
