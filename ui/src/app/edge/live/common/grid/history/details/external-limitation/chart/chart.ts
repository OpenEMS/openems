// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { GridSectionComponent } from "src/app/edge/live/energymonitor/chart/section/grid.component";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";
import { buildAnnotations, createLimiter14aAxis, createRcrAxis, hasData, processRestrictionDatasets } from "../../../shared-grid";

@Component({
    selector: "common-grid-details-external-limitation-chart",
    templateUrl: "../../../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    public static getChartData(config: EdgeConfig, chartType: "line" | "bar", translate: TranslateService, showPhases: boolean): HistoryUtils.ChartData {

        const isLimiter14aInstalled: boolean = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
        const isRcrInstalled: boolean = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");

        const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")[0] ?? null;
        const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")[0] ?? null;

        const input: HistoryUtils.InputChannel[] = [];

        if (isLimiter14aInstalled) {
            input.push({
                name: "Restriction14a",
                powerChannel: ChannelAddress.fromString(controller14a + "/RestrictionMode"),
                energyChannel: ChannelAddress.fromString(controller14a + "/CumulatedRestrictionTime"),
            });
        }
        if (GridSectionComponent.isControllerEnabled(config, "Controller.Ess.EmergencyCapacityReserve")) {
            input.push({
                name: "OffGrid",
                powerChannel: ChannelAddress.fromString("_sum/GridMode"),
                energyChannel: ChannelAddress.fromString("_sum/GridModeOffGridTime"),
            });
        }
        if (isRcrInstalled) {
            input.push({
                name: "RestrictionRcr",
                powerChannel: ChannelAddress.fromString(controllerRcr + "/RestrictionMode"),
                energyChannel: ChannelAddress.fromString(controllerRcr + "/CumulatedRestrictionTime"),
            });
        }

        const yAxes: HistoryUtils.yAxes[] = [];


        return {
            input: input,
            output: (data: HistoryUtils.ChannelData, labels: Date[]) => {

                const { restrictionData14a, restrictionDataRcr } = processRestrictionDatasets(data, chartType);

                const has14aData = hasData(isLimiter14aInstalled, restrictionData14a);
                const hasRcrData = hasData(isRcrInstalled, restrictionDataRcr);

                const datasets: HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>[] = [];

                if (has14aData) {
                    yAxes.push(createLimiter14aAxis(chartType, translate));
                    datasets.push({
                        name: translate.instant("GRID_STATES.CONSUMPTION_LIMITATION"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[controller14a + "/CumulatedRestrictionTime"],
                        converter: () => restrictionData14a,
                        color: ChartConstants.Colors.ORANGE,
                        stack: 2,
                        custom: chartType === "line"
                            ? {
                                unit: YAxisType.RELAY,
                                pluginType: "box",
                                annotations: buildAnnotations(restrictionData14a, labels, "14a", ChartAxis.RIGHT),
                            }
                            : { unit: YAxisType.TIME },
                        yAxisId: ChartAxis.RIGHT,
                    } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>);
                }

                if (hasRcrData) {
                    yAxes.push(createRcrAxis(chartType));
                    datasets.push({
                        name: translate.instant("GRID_STATES.FEED_IN_LIMITATION"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[controllerRcr + "/CumulatedRestrictionTime"],
                        converter: () => restrictionDataRcr,
                        color: ChartConstants.Colors.GREEN,
                        stack: 3,
                        custom: chartType === "line"
                            ? {
                                unit: YAxisType.PERCENTAGE,
                                pluginType: "box",
                                annotations: buildAnnotations(restrictionDataRcr, labels, "rcr", ChartAxis.RIGHT_2, yAxes[yAxes.length - 1]),
                            }
                            : { unit: YAxisType.TIME },
                        yAxisId: ChartAxis.RIGHT_2,
                    } as HistoryUtils.DisplayValue<HistoryUtils.BoxCustomOptions>);
                }


                if (!showPhases) {
                    return datasets;
                }

                return datasets;
            },
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: yAxes,
        };
    }

    public override getChartData() {
        return ChartComponent.getChartData(this.config, this.chartType, this.translate, this.showPhases);
    }
}
