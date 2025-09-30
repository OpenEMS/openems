// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "modbusTcpApiChart",
    templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(component: EDGE_CONFIG.COMPONENT, config: EdgeConfig, chartType: "line" | "bar", translate: TranslateService, showPhases: boolean): HISTORY_UTILS.CHART_DATA {
        let writeChannels: string[] = [];

        const colors: string[] = [
            "rgb(191, 144, 33)",
            "rgb(162, 191, 33)",
            "rgb(86, 191, 33)",
            "rgb(33, 191, 165)",
            "rgb(33, 115, 191)",
        ];

        const input: HISTORY_UTILS.INPUT_CHANNEL[] =
            [{
                name: "SetActivePowerEquals",
                powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/Ess0SetActivePowerEquals"),
            }];

        if (COMPONENT.PROPERTIES.WRITE_CHANNELS) {
            writeChannels = COMPONENT.PROPERTIES.WRITE_CHANNELS.FILTER(c => !C.INCLUDES("Ess0SetActivePowerEquals"));
            WRITE_CHANNELS.FOR_EACH(c => {
                INPUT.PUSH({
                    name: c,
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + `/${c}`),
                });
            });
        }

        return {
            input,
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
                const values: HISTORY_UTILS.DISPLAY_VALUE[] = [{
                    name: TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS"),
                    converter: () => data["SetActivePowerEquals"],
                    color: "rgb(214, 28, 28)",
                }];
                if (writeChannels) {
                    WRITE_CHANNELS.FOR_EACH((c: string, index: number) => {
                        const name: string = c;
                        // Add translations for active power channels of ess0
                        if (C.INCLUDES("Ess0SetActive")) {
                            const channelName = C.REPLACE("Ess0", "");
                            switch (channelName) {
                                case "SetActivePowerEquals":
                                    return TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS");
                                case "SetActivePowerGreaterOrEquals":
                                    return TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_GREATER_OR_EQUALS");
                                case "SetActivePowerLessOrEquals":
                                    return TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_LESS_OR_EQUALS");
                            }
                        }

                        VALUES.PUSH({
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
            yAxes: [{
                unit: YAXIS_TYPE.ENERGY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
            },
            ],
        };
    }

    public override getChartData() {
        return CHART_COMPONENT.GET_CHART_DATA(THIS.COMPONENT, THIS.CONFIG, THIS.CHART_TYPE, THIS.TRANSLATE, THIS.SHOW_PHASES);
    }
}
