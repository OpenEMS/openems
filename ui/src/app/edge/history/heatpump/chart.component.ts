// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { ChartAxis } from "src/app/shared/service/utils";

import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChannelAddress, EdgeConfig, Service } from "../../../shared/shared";
import { AbstractHistoryChart } from "../abstracthistorychart";

@Component({
    selector: "heatpumpchart",
    templateUrl: "../abstracthistorychart.html",
})
export class HeatPumpChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public component!: EdgeConfig.Component;


    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("heatpump-chart", service, translate);
    }

    ngOnChanges() {
        this.updateChart();
    }

    ngOnInit() {
        this.startSpinner();
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            const result = response.result;
            // convert labels
            const labels: Date[] = [];
            for (const timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            const datasets = [];

            if (this.component.id + "/Status" in result.data) {

                const stateTimeData = result.data[this.component.id + "/Status"].map(value => {
                    if (value == null) {
                        return null;
                    } else {
                        return value;
                    }
                });

                datasets.push({
                    label: this.translate.instant("General.state"),
                    data: stateTimeData,
                    hidden: false,
                });
                this.colors.push({
                    backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.RED, 0.05),
                    borderColor: ChartConstants.Colors.RED,
                });
            }
            this.datasets = datasets;
            this.loading = false;

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        }).finally(async () => {
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions(this.options);
            this.stopSpinner();
        });
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            resolve([new ChannelAddress(this.component.id, "Status")]);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    private applyControllerSpecificOptions(options: Chart.ChartOptions) {
        const translate = this.translate;
        options.scales[ChartAxis.LEFT]["title"].text = this.translate.instant("General.state");
        options.scales[ChartAxis.LEFT].ticks.callback = function (label, index, labels) {
            switch (label) {
                case -1:
                    return translate.instant("Edge.Index.Widgets.HeatPump.undefined");
                case 0:
                    return translate.instant("Edge.Index.Widgets.HeatPump.lock");
                case 1:
                    return translate.instant("Edge.Index.Widgets.HeatPump.normalOperationShort");
                case 2:
                    return translate.instant("Edge.Index.Widgets.HeatPump.switchOnRecShort");
                case 3:
                    return translate.instant("Edge.Index.Widgets.HeatPump.switchOnComShort");
            }
        };

        options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
            const label = tooltipItem.dataset.label;
            const value = tooltipItem.dataset.data[tooltipItem.dataIndex];
            let toolTipValue;
            switch (value) {
                case -1:
                    toolTipValue = translate.instant("Edge.Index.Widgets.HeatPump.undefined");
                    break;
                case 0:
                    toolTipValue = translate.instant("Edge.Index.Widgets.HeatPump.lock");
                    break;

                case 1:
                    toolTipValue = translate.instant("Edge.Index.Widgets.HeatPump.normalOperation");
                    break;
                case 2:
                    toolTipValue = translate.instant("Edge.Index.Widgets.HeatPump.switchOnRec");
                    break;
                case 3:
                    toolTipValue = translate.instant("Edge.Index.Widgets.HeatPump.switchOnCom");
                    break;
                default:
                    toolTipValue = "";
                    break;
            }
            return label + ": " + toolTipValue; // TODO get locale dynamically
        };

        options.scales[ChartAxis.LEFT].max = 3;
        options.scales[ChartAxis.LEFT]["beginAtZero"] = true;
        this.options = options;
    }

}
