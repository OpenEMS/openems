import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from 'src/app/edge/history/shared';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';
import { GetScheduleRequest } from '../../../../../../shared/jsonrpc/request/getScheduleRequest';
import { GetScheduleResponse } from '../../../../../../shared/jsonrpc/response/getScheduleResponse';

@Component({
    selector: 'powerSocChart',
    templateUrl: '../../../../../history/abstracthistorychart.html',
})
export class SchedulePowerAndSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public refresh: boolean;
    @Input() public override edge: Edge;
    @Input() public component: EdgeConfig.Component;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("powerSoc-chart", service, translate);
    }

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
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
            const datasets = [];

            // Extracting prices and states from the schedule array
            const { productionArray, consumptionArray, socArray, labels } = {
                productionArray: result.schedule.map(entry => entry.production),
                consumptionArray: result.schedule.map(entry => entry.consumption),
                socArray: result.schedule.map(entry => entry.soc),
                labels: result.schedule.map(entry => new Date(entry.timestamp)),
            };

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.production'),
                data: productionArray,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(45,143,171, 0.2)',
                borderColor: 'rgba(45,143,171, 1)',
            });

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.consumption'),
                data: consumptionArray,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(253,197,7,0.2)',
                borderColor: 'rgba(253,197,7,1)',
            });

            // State of charge data
            datasets.push({
                type: 'line',
                label: this.translate.instant('General.soc'),
                data: socArray,
                hidden: false,
                yAxisID: 'yAxis2',
                position: 'right',
                borderDash: [10, 10],
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(189, 195, 199,0.2)',
                borderColor: 'rgba(189, 195, 199,1)',
            });


            this.datasets = datasets;
            this.loading = false;
            this.labels = labels;
            this.setLabel();
            this.stopSpinner();
        }).catch((reason) => {
            console.error(reason);
            this.initializeChart();
            return;
        });
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        let translate = this.translate;

        //x-axis
        options.scales.xAxes[0].time.unit = "hour";

        //y-axis
        options.scales.yAxes[0].id = "yAxis1";
        options.scales.yAxes[0].scaleLabel.padding = -2;
        options.scales.yAxes[0].scaleLabel.fontSize = 11;
        options.scales.yAxes[0].ticks.padding = -5;
        options.scales.yAxes[0].ticks.beginAtZero = false; // scale with min and max values.

        // Adds second y-axis to chart
        options.scales.yAxes.push({
            id: 'yAxis2',
            position: 'right',
            scaleLabel: {
                display: true,
                labelString: "%",
                padding: -2,
                fontSize: 11,
            },
            gridLines: {
                display: false,
            },
            ticks: {
                beginAtZero: true,
                max: 100,
                padding: -5,
                stepSize: 20,
            },
        });
        options.layout = {
            padding: {
                left: 2,
                right: 2,
                top: 0,
                bottom: 0,
            },
        };

        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;

            // TODO solve before here
            if (value === undefined || value === null || Number.isNaN(value)) {
                return;
            }

            if (label === translate.instant('General.soc')) {
                return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
            }

            return label + ": " + formatNumber(value, 'de', '1.0-0') + ' ' + 'W';
        };
        this.options = options;
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    public getChartHeight(): number {
        return this.service.isSmartphoneResolution
            ? window.innerHeight / 3
            : window.innerHeight / 4;
    }
}
