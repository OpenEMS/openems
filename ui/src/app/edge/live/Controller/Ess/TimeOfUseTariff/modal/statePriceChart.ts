import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ChartAxis, TimeOfUseTariffUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { calculateResolution } from 'src/app/edge/history/shared';
import { ColorUtils } from 'src/app/shared/utils/color/color.utils';
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

    public ngOnChanges() {
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

    public ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    public ngOnDestroy() {
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
            const scheduleChartData = TimeOfUseTariffUtils.getScheduleChartData(length, priceArray, stateArray, timestampArray, this.translate, this.component.properties.controlMode);

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
        }).finally(async () => {
            this.unit = YAxisTitle.CURRENCY;
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions(this.options);
        });
    }

    private applyControllerSpecificOptions(options: Chart.ChartOptions) {

        options.scales.x['time'].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
        options.scales.x['ticks'] = { source: 'auto', autoSkip: false };
        options.scales.x.ticks.maxTicksLimit = 18;
        options.scales.x['offset'] = false;
        options.scales.x.ticks.callback = function (value, index, values) {
            var date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ':00' : '';
        };

        // options.plugins.
        options.plugins.tooltip.mode = 'index';
        options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
            if (!item) {
                return;
            }
            return {
                borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
                backgroundColor: item.dataset.backgroundColor,
            };
        };

        this.datasets = this.datasets.map((el) => {
            let opacity = el.type === 'line' ? 0.2 : 0.5;

            if (el.backgroundColor && el.borderColor) {
                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(el.backgroundColor.toString(), opacity);
                el.borderColor = ColorUtils.changeOpacityFromRGBA(el.borderColor.toString(), 1);
            }
            return el;
        });

        options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {

            const label = item.dataset.label;
            const value = item.dataset.data[item.dataIndex];

            return TimeOfUseTariffUtils.getLabel(value, label, this.translate, this.currencyLabel);
        };

        options.scales[ChartAxis.LEFT]['title'].display = false;
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
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
