import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-current-voltage-asymmetric-chart",
    templateUrl: "../../../../../components/chart/abstracthistorychart.html",
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
export class CurrentVoltageAsymmetricChartComponent extends AbstractHistoryChart {

    protected override getChartData(): HistoryUtils.ChartData {

        const component = this.config.getComponent(this.route.snapshot.params.componentId);
        const currentPhasesColors: string[] = ["rgb(246, 180, 137)", "rgb(238, 120, 42)", "rgb(118, 52, 9)"];
        const voltagePhasesColors: string[] = ["rgb(255, 0, 0)", "rgb(133, 0, 0)", "rgb(71, 0, 0)"];
        const chartObject: HistoryUtils.ChartData = {
            input: [
                ...Phase.THREE_PHASE.map((phase) => ({
                    name: "Current" + phase,
                    powerChannel: ChannelAddress.fromString(component.id + "/Current" + phase),
                })),
                ...Phase.THREE_PHASE.map((phase) => ({
                    name: "Voltage" + phase,
                    powerChannel: ChannelAddress.fromString(component.id + "/Voltage" + phase),
                })),
            ],
            output: (data: HistoryUtils.ChannelData) => [
                ...Phase.THREE_PHASE.map((phase, index) => ({
                    name: this.translate.instant("EDGE.HISTORY.CURRENT") + " " + phase,
                    converter: () => {
                        return data["Current" + phase];
                    },
                    hideShadow: true,
                    color: currentPhasesColors[index],
                    yAxisId: ChartAxis.LEFT,
                })),
                ...Phase.THREE_PHASE.map((phase, index) => ({
                    name: this.translate.instant("EDGE.HISTORY.VOLTAGE") + " " + phase,
                    converter: () => {
                        return data["Voltage" + phase];
                    },
                    hideShadow: true,
                    color: voltagePhasesColors[index],
                    yAxisId: ChartAxis.RIGHT,
                })),
            ],
            tooltip: {
                formatNumber: "1.1-2",
                afterTitle: this.translate.instant("GENERAL.TOTAL"),
            },
            yAxes: [{
                unit: YAxisType.VOLTAGE,
                position: "right",
                yAxisId: ChartAxis.RIGHT,
                displayGrid: false,
                scale: {
                    dynamicScale: true,
                },
            },
            {
                unit: YAxisType.CURRENT,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
            ],
        };

        return chartObject;
    }
}
