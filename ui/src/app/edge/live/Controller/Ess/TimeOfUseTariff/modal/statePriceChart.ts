// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { AbstractHistoryChart as NewAbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisTitle } from 'src/app/shared/service/utils';
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
    }

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
            const schedule = result.schedule;

            // Extracting prices, states, timestamps from the schedule array
            const { priceArray, stateArray, timestampArray, gridBuyArray, socArray } = {
                priceArray: schedule.map(entry => entry.price),
                stateArray: schedule.map(entry => entry.state),
                timestampArray: schedule.map(entry => entry.timestamp),
                gridBuyArray: schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid)),
                socArray: schedule.map(entry => entry.soc),
            };

            const scheduleChartData = Controller_Ess_TimeOfUseTariff.getScheduleChartData(schedule.length, priceArray,
                stateArray, timestampArray, gridBuyArray, socArray, this.translate, this.component.properties.controlMode);

            this.colors = scheduleChartData.colors;
            this.labels = scheduleChartData.labels;

            this.datasets = scheduleChartData.datasets;
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
            this.applyControllerSpecificOptions();
        });
    }

    private applyControllerSpecificOptions() {
        const locale = this.service.translate.currentLang;
        const rightYaxisSoc: HistoryUtils.yAxes = { position: 'right', unit: YAxisTitle.PERCENTAGE, yAxisId: ChartAxis.RIGHT };
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYaxisSoc, this.translate, 'line', locale);

        const rightYAxisPower: HistoryUtils.yAxes = { position: 'right', unit: YAxisTitle.POWER, yAxisId: ChartAxis.RIGHT_2 };
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYAxisPower, this.translate, 'line', locale);

        this.options.scales.x['time'].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
        this.options.scales.x['ticks'] = { source: 'auto', autoSkip: false };
        this.options.scales.x.ticks.maxTicksLimit = 30;
        this.options.scales.x['offset'] = false;
        this.options.scales.x.ticks.callback = function (value) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ':00' : '';
        };

        // options.plugins.
        this.options.plugins.tooltip.mode = 'index';
        this.options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
            if (!item) {
                return;
            }
            return {
                borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
                backgroundColor: item.dataset.backgroundColor,
            };
        };

        this.options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {

            const label = item.dataset.label;
            const value = item.dataset.data[item.dataIndex];

            return TimeOfUseTariffUtils.getLabel(value, label, this.translate, this.currencyLabel);
        };

        this.datasets = this.datasets.map((el) => {
            const opacity = el.type === 'line' ? 0.2 : 0.5;

            if (el.backgroundColor && el.borderColor) {
                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(el.backgroundColor.toString(), opacity);
                el.borderColor = ColorUtils.changeOpacityFromRGBA(el.borderColor.toString(), 1);
            }
            return el;
        });

        this.datasets = this.datasets.map((el: Chart.ChartDataset) => {

            // align particular dataset element to right yAxis
            if (el.label == this.translate.instant('General.gridBuy')) {
                el['yAxisID'] = ChartAxis.RIGHT_2;
            } else if (el.label == this.translate.instant('General.soc')) {
                el['yAxisID'] = ChartAxis.RIGHT;
            }

            return el;
        });

        this.options.scales[ChartAxis.LEFT]['title'].text = this.currencyLabel;
        this.options.scales[ChartAxis.RIGHT].grid.display = false;
        this.options.scales[ChartAxis.RIGHT_2].suggestedMin = 0;
        this.options.scales[ChartAxis.RIGHT_2].suggestedMax = 1;
        this.options.scales[ChartAxis.RIGHT_2].grid.display = false;
        this.options['animation'] = false;
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    public getChartHeight(): number {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
    }
}
