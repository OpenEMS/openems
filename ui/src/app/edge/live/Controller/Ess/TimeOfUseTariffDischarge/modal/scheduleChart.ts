import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { TooltipItem } from 'src/app/edge/history/shared';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';
import { GetScheduleRequest } from '../../../../../../shared/jsonrpc/request/getScheduleRequest';
import { GetScheduleResponse } from '../../../../../../shared/jsonrpc/response/getScheduleResponse';

@Component({
    selector: 'scheduleChart',
    templateUrl: '../../../../../history/abstracthistorychart.html',
})
export class ScheduleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public refresh: boolean;
    @Input() public override edge: Edge;
    @Input() public component: EdgeConfig.Component;

    private currencyLabel: Currency.Label; // Default

    ngOnChanges() {
        this.currencyLabel = Currency.getCurrencyLabelByEdgeId(this.edge.id);
        this.updateChart();
    };

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("schedule-chart", service, translate);
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
        this.colors = [];

        this.edge.sendRequest(
            this.websocket,
            new ComponentJsonApiRequest({ componentId: this.component.id, payload: new GetScheduleRequest() }),
        ).then(response => {
            var result = (response as GetScheduleResponse).result;
            var length = result.schedule.length;

            // Initializing States.
            var barCharge = Array(length).fill(null);
            var barBalancing = Array(length).fill(null);
            var barDelayDischarge = Array(length).fill(null);

            // convert labels
            let labels: Date[] = [];

            for (let index = 0; index < length; index++) {
                let price = Utils.formatPrice(result.schedule[index].price);
                var state = result.schedule[index].state;
                var timestamp = result.schedule[index].timestamp;

                //Only use full hours as a timestamp
                labels.push(new Date(timestamp));

                switch (state) {
                    case 0:
                        // Delay Discharge
                        barDelayDischarge[index] = price;
                        break;
                    case 1:
                        // Allows Discharge
                        barBalancing[index] = price;
                        break;
                    case 3:
                        // Charge
                        barCharge[index] = price;
                        break;
                }
            }

            let datasets = [];

            // Set datasets
            datasets.push({
                type: 'bar',
                label: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.BALANCING'),
                data: barBalancing,
                order: 3,
            });
            this.colors.push({
                // Dark Green
                backgroundColor: 'rgba(51,102,0,0.8)',
                borderColor: 'rgba(51,102,0,1)',
            });

            // Set dataset for Quarterly Prices being charged.
            datasets.push({
                type: 'bar',
                label: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.CHARGE'),
                data: barCharge,
                order: 3,
            });
            this.colors.push({
                // Sky blue
                backgroundColor: 'rgba(0, 204, 204,0.5)',
                borderColor: 'rgba(0, 204, 204,0.7)',
            });

            // Set dataset for buy from grid
            datasets.push({
                type: 'bar',
                label: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE'),
                data: barDelayDischarge,
                order: 3,
            });
            this.colors.push({
                // Black
                backgroundColor: 'rgba(0,0,0,0.8)',
                borderColor: 'rgba(0,0,0,0.9)',
            });

            this.datasets = datasets;
            this.labels = labels;
            this.loading = false;
            this.setLabel();
            // this.service.stopSpinner(this.spinnerId);
            this.stopSpinner();
        }).catch((reason) => {
            console.error(reason);
            this.initializeChart();
            return;
        });
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        const currencyLabel: string = this.currencyLabel;

        // Scale prices y-axis between min-/max-values, not from zero
        options.scales.yAxes[0].ticks.beginAtZero = false;

        //x-axis
        options.scales.xAxes[0].time.unit = "hour";
        options.scales.xAxes[0].stacked = true;

        //y-axis
        options.scales.yAxes[0].id = "yAxis1";
        options.scales.yAxes[0].scaleLabel.padding = -2;
        options.scales.yAxes[0].scaleLabel.fontSize = 11;
        options.scales.yAxes[0].ticks.padding = -5;

        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;

            // TODO solve before here
            if (!value) {
                return;
            }
            return label + ": " + formatNumber(value, 'de', '1.0-4') + ' ' + currencyLabel;
        };
        this.options = options;
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    public getChartHeight(): number {
        return window.innerHeight / 4;
    }
}
