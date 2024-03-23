import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { DEFAULT_TIME_CHART_OPTIONS } from 'src/app/edge/history/shared';
import { AbstractHistoryChart as NewAbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChartConstants } from 'src/app/shared/genericComponents/chart/chart.constants';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisTitle } from 'src/app/shared/service/utils';
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

    public ngOnChanges() {
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
            const datasets = [];

            // Extracting prices and states from the schedule array
            const { gridBuyArray, gridSellArray, productionArray, consumptionArray, essDischargeArray, essChargeArray, socArray, labels } = {
                gridBuyArray: result.schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid)),
                gridSellArray: result.schedule.map(entry => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.grid)),
                productionArray: result.schedule.map(entry => entry.production),
                consumptionArray: result.schedule.map(entry => entry.consumption),
                essDischargeArray: result.schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.ess)),
                essChargeArray: result.schedule.map(entry => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.ess)),
                socArray: result.schedule.map(entry => entry.soc),
                labels: result.schedule.map(entry => new Date(entry.timestamp)),
            };

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.gridBuy'),
                data: gridBuyArray,
                hidden: true,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(0,0,0, 0.2)',
                borderColor: 'rgba(0,0,0, 1)',
            });

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.gridSell'),
                data: gridSellArray,
                hidden: true,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(0,0,200, 0.2)',
                borderColor: 'rgba(0,0,200, 1)',
            });

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

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.chargePower'),
                data: essChargeArray,
                hidden: true,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(0,223,0, 0.2)',
                borderColor: 'rgba(0,223,0, 1)',
            });

            datasets.push({
                type: 'line',
                label: this.translate.instant('General.dischargePower'),
                data: essDischargeArray,
                hidden: true,
                yAxisID: 'yAxis1',
                position: 'right',
                order: 1,
            });
            this.colors.push({
                backgroundColor: 'rgba(200,0,0, 0.2)',
                borderColor: 'rgba(200,0,0, 1)',
            });

            // State of charge data
            datasets.push({
                type: 'line',
                label: this.translate.instant('General.soc'),
                data: socArray,
                hidden: false,
                yAxisID: ChartAxis.RIGHT,
                borderDash: [10, 10],
                order: 1,
                unit: YAxisTitle.PERCENTAGE,
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
        }).finally(async () => {
            this.unit = YAxisTitle.POWER;
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions();
        });
    }

    private applyControllerSpecificOptions() {
        const rightYAxis: HistoryUtils.yAxes = { position: 'right', unit: YAxisTitle.PERCENTAGE, yAxisId: ChartAxis.RIGHT };
        const leftYAxis: HistoryUtils.yAxes = { position: 'left', unit: YAxisTitle.ENERGY, yAxisId: ChartAxis.LEFT };
        const locale = this.service.translate.currentLang;

        const scaleOptionsLeft = ChartConstants.getScaleOptions(this.datasets, leftYAxis);
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYAxis, this.translate, 'line', locale, true, scaleOptionsLeft);
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, leftYAxis, this.translate, 'line', locale, true);

        this.datasets = this.datasets.map((el, index, arr) => {

            // align last element to right yAxis
            if ((arr.length - 1) === index) {
                el['yAxisID'] = ChartAxis.RIGHT;
                el['yAxisId'] = ChartAxis.RIGHT;
            }
            return el;
        });
        this.options.scales.x['ticks'] = { source: 'auto', autoSkip: false };
        this.options.scales.x.ticks.callback = function (value, index, values) {
            var date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ':00' : '';
        };

        this.options.scales[ChartAxis.RIGHT].grid.display = false;
    };

    protected setLabel() {
        const options = <Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        const translate = this.translate;
        options.plugins.tooltip.callbacks.label = function (item: Chart.TooltipItem<any>) {

            const label = item.dataset.label;
            const value = item.dataset.data[item.dataIndex];

            return TimeOfUseTariffUtils.getLabel(value, label, translate);
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
