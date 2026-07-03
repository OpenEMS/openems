import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";

import { subHours } from "date-fns";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { ChannelAddress, ChartConstants, Edge, Logger, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";
import { GetSchedule } from "./getSchedule";

@Component({
    selector: "oe-production-chart",
    templateUrl: "../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class ProductionChartComponent extends AbstractHistoryChart implements OnChanges {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;

    constructor(
        public override service: Service,
        public override cdRef: ChangeDetectorRef,
        protected override translate: TranslateService,
        protected override route: ActivatedRoute,
        protected override logger: Logger,
        protected override navigationService: NavigationService,
        private websocket: Websocket,
    ) {
        super(service, cdRef, translate, route, logger, navigationService);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.config) {
            return;
        }

        if (changes["refresh"] || changes["edge"]) {
            this.updateChart();
        }
    }

    protected override getChartData(): HistoryUtils.ChartData | null {
        return {
            input: [{
                name: "ProductionActivePower",
                powerChannel: ChannelAddress.fromString("_sum/ProductionActivePower"),
            }],
            output: () => [],
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes: [{
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartHeight(): number | null {
        const fourTimesTheHeight = 400;
        return ViewUtils.getChartContentHeightInVh(window.innerHeight, this.navigationService.position(), fourTimesTheHeight);
    }

    protected override async loadChart(): Promise<void> {
        if (this.edge == null) {
            return;
        }

        this.labels = [];
        this.errorResponse = null;
        this.loading = true;
        this.chartType = "line";
        this.chartObject = this.getChartData();

        try {
            const now = subHours(new Date(), 4);
            const scheduleResponse = await GetSchedule.getSchedule(this.edge, this.websocket, now);

            const data = scheduleResponse.result.data;

            const historyPoints = data
                .filter(entry => entry.type === "HISTORY")
                .map(entry => ({
                    timestamp: new Date(entry.timestamp).getTime(),
                    value: entry._sum.ProductionActivePower != null ? entry._sum.ProductionActivePower / 1000 : null,
                }));

            const forecastPoints = data
                .filter(entry => entry.type === "PREDICTION")
                .map(entry => ({
                    timestamp: new Date(entry.timestamp).getTime(),
                    value: entry._sum.ProductionActivePower != null ? entry._sum.ProductionActivePower / 1000 : null,
                }));

            const allTimestamps = Array.from(new Set([
                ...historyPoints.map(point => point.timestamp),
                ...forecastPoints.map(point => point.timestamp),
            ])).sort((a, b) => a - b);

            const historyMap = new Map<number, number | null>();
            const forecastMap = new Map<number, number | null>();

            historyPoints.forEach(point => historyMap.set(point.timestamp, point.value));
            forecastPoints.forEach(point => forecastMap.set(point.timestamp, point.value));

            const historyData: (number | null)[] = [];
            const forecastData: (number | null)[] = [];

            for (const timestamp of allTimestamps) {
                historyData.push(historyMap.get(timestamp) ?? null);
                forecastData.push(forecastMap.get(timestamp) ?? null);
            }

            this.labels = allTimestamps.map(timestamp => new Date(timestamp));

            this.datasets = [
                {
                    type: "line",
                    label: this.translate.instant("GENERAL.PRODUCTION"),
                    data: historyData,
                    hidden: false,
                    order: 1,
                    yAxisID: ChartAxis.LEFT,
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                    borderColor: ChartConstants.Colors.BLUE,
                    borderWidth: 2,
                    tension: 0,
                    ...ChartConstants.Plugins.Datasets.HOVER_ENHANCE({
                        backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                        borderColor: ChartConstants.Colors.BLUE,
                    }),
                },
                {
                    type: "line",
                    label: this.translate.instant("GENERAL.PRODUCTION"),
                    data: forecastData,
                    hidden: false,
                    order: 1,
                    yAxisID: ChartAxis.LEFT,
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                    borderColor: ChartConstants.Colors.BLUE,
                    borderWidth: 2,
                    borderDash: [6, 6],
                    tension: 0,
                    ...ChartConstants.Plugins.Datasets.HOVER_ENHANCE({
                        backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                        borderColor: ChartConstants.Colors.BLUE,
                    }),
                },
            ];

            this.loading = false;
            this.stopSpinner();

        } catch (error) {
            console.error(error);
            this.initializeChart();
        }
    }
}
