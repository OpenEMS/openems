import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { TooltipItem } from 'src/app/edge/history/shared';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { TimeOfUseTariffUtils } from 'src/app/shared/service/utils';
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { GetScheduleRequest } from '../../../../../../shared/jsonrpc/request/getScheduleRequest';
import { GetScheduleResponse } from '../../../../../../shared/jsonrpc/response/getScheduleResponse';

@Component({
    selector: 'statePriceChart',
    templateUrl: '../../../../../history/abstracthistorychart.html',
})
export class ScheduleStateAndPriceChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

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

        this.edge.sendRequest(
            this.websocket,
            new ComponentJsonApiRequest({ componentId: this.component.id, payload: new GetScheduleRequest() }),
        ).then(response => {
            const result = (response as GetScheduleResponse).result;
            const length = result.schedule.length;

            // Extracting prices, states, timestamps from the schedule array
            const { priceArray, stateArray, timestampArray } = {
                priceArray: result.schedule.map(entry => entry.price),
                stateArray: result.schedule.map(entry => entry.state),
                timestampArray: result.schedule.map(entry => entry.timestamp),
            };

            let datasets = [];
            const scheduleChartData = TimeOfUseTariffUtils.getScheduleChartData(length, priceArray, stateArray, timestampArray, this.translate, this.component.factoryId);

            datasets = scheduleChartData.datasets;
            this.colors = scheduleChartData.colors;
            this.labels = scheduleChartData.labels;

            this.datasets = datasets;
            this.loading = false;
            this.setLabel();
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

        //x-axis
        options.scales.xAxes[0].time.unit = "hour";
        options.scales.xAxes[0].stacked = true;

        //y-axis
        options.scales.yAxes[0].id = "yAxis1";
        options.scales.yAxes[0].scaleLabel.padding = -2;
        options.scales.yAxes[0].scaleLabel.fontSize = 11;
        options.scales.yAxes[0].ticks.padding = -5;
        options.scales.yAxes[0].ticks.beginAtZero = false; // scale with min and max values.

        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;

            // TODO solve before here
            if (value === undefined || value === null || Number.isNaN(value)) {
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
        return this.service.isSmartphoneResolution
            ? window.innerHeight / 3
            : window.innerHeight / 4;
    }
}
