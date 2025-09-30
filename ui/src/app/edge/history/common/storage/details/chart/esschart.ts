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
import { ChannelAddress, ChartConstants, EdgeConfig } from "../../../../../../shared/shared";

@Component({
    selector: "common-storage-details-ess-chart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
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

    public static getChartData(translate: TranslateService, essComponent: EDGE_CONFIG.COMPONENT, chartType: "line" | "bar", config: EdgeConfig): HISTORY_UTILS.CHART_DATA {

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

        const input: HISTORY_UTILS.INPUT_CHANNEL[] = [
            {
                name: ESS_COMPONENT.ID + "Charge",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(ESS_COMPONENT.ID + "/DcDischargePower"),
                energyChannel: CHANNEL_ADDRESS.FROM_STRING(ESS_COMPONENT.ID + "/DcChargeEnergy"),
                ...(chartType === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
            },
            {
                name: ESS_COMPONENT.ID + "Discharge",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(ESS_COMPONENT.ID + "/DcDischargePower"),
                energyChannel: CHANNEL_ADDRESS.FROM_STRING(ESS_COMPONENT.ID + "/DcDischargeEnergy"),
                ...(chartType === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO }),
            },
            {
                name: "Soc",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(ESS_COMPONENT.ID + "/Soc"),
            },
        ];

        const emergencyReserveComponent: EDGE_CONFIG.COMPONENT | null = config
            .getComponentsByFactory("CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE")
            .filter(component =>
                component != null && COMPONENT.IS_ENABLED && ESS_COMPONENT.HAS_PROPERTY_VALUE("ESS.ID", ESS_COMPONENT.ID))[0] ?? null;
        const isReserveSocEnabled = CONFIG.GET_PROPERTY_FROM_COMPONENT<boolean>(emergencyReserveComponent, "isReserveSocEnabled");
        if (emergencyReserveComponent && isReserveSocEnabled) {
            INPUT.PUSH({
                name: "EmergencyReserve",
                powerChannel: new ChannelAddress(EMERGENCY_RESERVE_COMPONENT.ID, "ActualReserveSoc"),
            });
        }

        return {
            input: input,
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {

                const output: HISTORY_UTILS.DISPLAY_VALUE[] = [{
                    name: TRANSLATE.INSTANT("GENERAL.CHARGE"),
                    converter: () => data[ESS_COMPONENT.ID + "Charge"],
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_RESPONSE.RESULT.DATA[ESS_COMPONENT.ID + "/DcChargeEnergy"],
                    stack: 0,
                    color: CHART_CONSTANTS.COLORS.GREEN,
                },
                {
                    name: TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
                    converter: () => data[ESS_COMPONENT.ID + "Discharge"]?.map(el => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(el)),
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_RESPONSE.RESULT.DATA[ESS_COMPONENT.ID + "/DcDischargeEnergy"],
                    stack: 1,
                    color: CHART_CONSTANTS.COLORS.RED,
                },
                ];

                if (chartType === "line") {
                    OUTPUT.PUSH({
                        name: TRANSLATE.INSTANT("GENERAL.SOC"),
                        converter: () => data["Soc"].map(el => UTILS.MULTIPLY_SAFELY(el, 1000)),
                        color: CHART_CONSTANTS.COLORS.GREY,
                        borderDash: [10, 10],
                        yAxisId: CHART_AXIS.RIGHT,
                    });

                }
                if (emergencyReserveComponent && isReserveSocEnabled) {
                    OUTPUT.PUSH({
                        name: TRANSLATE.INSTANT("EDGE.INDEX.EMERGENCY_RESERVE.EMERGENCY_RESERVE"),
                        converter: () => data["EmergencyReserve"].map(el => UTILS.MULTIPLY_SAFELY(el, 1000)),
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
        const component = THIS.CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
        return STORAGE_ESS_CHART_COMPONENT.GET_CHART_DATA(THIS.TRANSLATE, component, THIS.CHART_TYPE, THIS.CONFIG);
    }
}
