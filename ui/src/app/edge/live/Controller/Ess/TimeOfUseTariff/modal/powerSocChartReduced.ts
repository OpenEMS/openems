// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";

@Component({
    selector: "powerSocChartReduced",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class SchedulePowerAndSocChartReducedComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;


    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("powerSoc-chart", service, translate);
    }

    public ngOnChanges() {
        this.updateChart();
    }

    public ngOnInit() {
        this.service.startSpinner(this.spinnerId);
    }

    public ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
        const translate = this.translate;
        this.options.plugins = {
            tooltip: {
                callbacks: {
                    label: function (item: Chart.TooltipItem<any>) {

                        const label = item.dataset.label;
                        const value = item.dataset.data[item.dataIndex];

                        return TimeOfUseTariffUtils.getLabel(value, label, translate);
                    },
                },
            },
        };
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    protected override updateChart() {

        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;

        this.edge.sendRequest(
            this.websocket,
            new ComponentJsonApiRequest({ componentId: this.component.id, payload: new GetScheduleRequest() }),
        ).then(response => {
            const result = (response as GetScheduleResponse).result;
            const schedule = result.schedule;
            const datasets = [];

            // Extracting prices and states from the schedule array
            const { productionArray, consumptionArray, labels } = {
                productionArray: schedule.map(entry => entry.production, 1000),
                consumptionArray: schedule.map(entry => entry.consumption, 1000),

                labels: schedule.map(entry => new Date(entry.timestamp)),
            };

            datasets.push({
                type: "line",
                label: this.translate.instant("General.production"),
                data: productionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                borderColor: ChartConstants.Colors.BLUE,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.consumption"),
                data: consumptionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.YELLOW, 0.2),
                borderColor: ChartConstants.Colors.YELLOW,
            });

            this.datasets = datasets;
            this.loading = false;
            this.labels = labels;
            //this.setLabel();
            this.stopSpinner();
        }).catch((reason) => {
            console.error(reason);
            this.initializeChart();
            return;
        }).finally(async () => {
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions();
        });
    }
    private applyControllerSpecificOptions() {
        const leftYAxis: HistoryUtils.yAxes = {
            position: "left",
            unit: YAxisType.POWER,
            yAxisId: ChartAxis.LEFT,
        };

        // Only apply settings for the left Y-axis
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, leftYAxis, this.translate, "line", this.datasets, true);

        // Remove the right Y-axis
        delete this.options.scales[ChartAxis.RIGHT];
    }


}
