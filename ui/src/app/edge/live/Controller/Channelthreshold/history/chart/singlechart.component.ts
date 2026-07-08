import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-channel-threshold-single-chart",
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
export class SingleChartComponent extends AbstractHistoryChart {

    public static getChartData(_config: EdgeConfig, component: EdgeConfig.Component): HistoryUtils.ChartData {
        const controllerId = component.id;
        const powerChannel = component.getPropertyFromComponent<ChannelAddress>("outputChannelAddress");
        if (powerChannel == null) {
            return HistoryUtils.ChartData.EMPTY;
        }

        return {
            input: [
                {
                    name: controllerId,
                    powerChannel: powerChannel,
                    energyChannel: new ChannelAddress(controllerId, "CumulatedActiveTime"),
                },
            ],
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [{
                    name: powerChannel.channelId ?? controllerId,
                    nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
                        return energyQueryResponse?.result.data[controllerId + "/CumulatedActiveTime"] ?? null;
                    },
                    converter: () => {
                        return data[controllerId]
                            .map(val => Utils.multiplySafely(val, 1000));
                    },
                    color: ChartConstants.Colors.SHADES_OF_YELLOW[0],
                }];

                return output;
            },
            tooltip: {
                formatNumber: "1.0-0",
            },
            yAxes: [{
                unit: YAxisType.RELAY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return SingleChartComponent.getChartData(this.config, this.component);
    }
}
