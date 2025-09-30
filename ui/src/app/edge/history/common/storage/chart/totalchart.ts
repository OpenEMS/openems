import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { ChannelAddress, ChartConstants, EdgeConfig } from "../../../../../shared/shared";

@Component({
    selector: "common-storage-total-chart",
    templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
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

export class StorageTotalChartComponent extends AbstractHistoryChart {

    public static getChartData(translate: TranslateService, chartType: string, config: EdgeConfig): HISTORY_UTILS.CHART_DATA {

        const essComponents = config?.getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
            .filter(component => !COMPONENT.FACTORY_ID.INCLUDES("ESS.CLUSTER"));

        const yAxes: HISTORY_UTILS.Y_AXES[] = [{
            unit: YAXIS_TYPE.ENERGY,
            position: "left",
            yAxisId: CHART_AXIS.LEFT,
        }];

        if (chartType === "line") {
            Y_AXES.PUSH({
                unit: YAXIS_TYPE.PERCENTAGE,
                position: "right",
                yAxisId: CHART_AXIS.RIGHT,
            });
        }

        const input: HISTORY_UTILS.INPUT_CHANNEL[] = [];
        INPUT.PUSH(
            {
                name: "_sum/EssActivePower",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/EssActivePower"),
            },
            {
                name: "_sum/Charge",
                energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/EssDcChargeEnergy"),
            },
            {
                name: "_sum/Discharge",
                energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/EssDcDischargeEnergy"),
            },
            {
                name: "_sum/ProductionDcActualPower",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionDcActualPower"),
                energyChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionActiveEnergy"),
            },
            {
                name: "Soc",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/EssSoc"),
            });


        const emergencyReserveComponent: EDGE_CONFIG.COMPONENT | null = config
            .getComponentsByFactory("CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE")
            .filter(component => COMPONENT.IS_ENABLED)[0] ?? null;
        const isReserveSocEnabled = CONFIG.GET_PROPERTY_FROM_COMPONENT<boolean>(emergencyReserveComponent, "isReserveSocEnabled");

        if (ESS_COMPONENTS.LENGTH === 1 && emergencyReserveComponent != null && isReserveSocEnabled) {
            INPUT.PUSH({
                name: "EmergencyReserve",
                powerChannel: new ChannelAddress(EMERGENCY_RESERVE_COMPONENT.ID, "ActualReserveSoc"),
            });
        }

        return {
            input: input,
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {

                let totalData: number[] = [];

                if (chartType === "line") {
                    if (CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER").length > 0) {
                        data["_sum/ProductionDcActualPower"]?.forEach((value, index) => {
                            if (data["_sum/ProductionDcActualPower"][index] != null && data["_sum/ProductionDcActualPower"][index] != undefined) {
                                totalData[index] = UTILS.SUBTRACT_SAFELY(data["_sum/EssActivePower"][index], value) as number;
                            }
                        });
                    } else {
                        totalData = data["_sum/EssActivePower"];
                    }
                }

                const output: HISTORY_UTILS.DISPLAY_VALUE[] = [{
                    name: TRANSLATE.INSTANT("GENERAL.CHARGE"),
                    converter: () => chartType === "line" ? totalData?.map(value => HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(value)) : data["_sum/Charge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_RESPONSE.RESULT.DATA["_sum/EssDcChargeEnergy"],
                    color: CHART_CONSTANTS.COLORS.GREEN,
                    stack: 0,
                },
                {
                    name: TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
                    converter: () => chartType === "line" ? totalData?.map(value => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(value)) : data["_sum/Discharge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_RESPONSE.RESULT.DATA["_sum/EssDcDischargeEnergy"],
                    color: CHART_CONSTANTS.COLORS.RED,
                    stack: 1,
                }];

                if (chartType === "line") {
                    OUTPUT.PUSH({
                        name: TRANSLATE.INSTANT("GENERAL.SOC"),
                        converter: () => data["Soc"]?.map(el => UTILS.MULTIPLY_SAFELY(el, 1000)),
                        color: CHART_CONSTANTS.COLORS.GREY,
                        borderDash: [10, 10],
                        yAxisId: CHART_AXIS.RIGHT,
                    });
                }

                if (emergencyReserveComponent != null && isReserveSocEnabled) {
                    OUTPUT.PUSH({
                        name: TRANSLATE.INSTANT("EDGE.INDEX.EMERGENCY_RESERVE.EMERGENCY_RESERVE"),
                        converter: () => data["EmergencyReserve"]?.map(el => UTILS.MULTIPLY_SAFELY(el, 1000)),
                        color: CHART_CONSTANTS.COLORS.BLACK,
                        yAxisId: CHART_AXIS.RIGHT,
                        borderDash: [3, 3],
                    });
                }
                return output;
            },
            tooltip: {
                formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.ZERO_TO_TWO,
            },
            yAxes: yAxes,
        };
    }
    public override getChartData() {
        return STORAGE_TOTAL_CHART_COMPONENT.GET_CHART_DATA(THIS.TRANSLATE, THIS.CHART_TYPE, THIS.CONFIG);
    }
}
