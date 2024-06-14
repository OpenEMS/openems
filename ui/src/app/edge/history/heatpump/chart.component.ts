// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis } from 'src/app/shared/service/utils';

import { ChannelAddress, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'heatpumpchart',
    templateUrl: '../abstracthistorychart.html',
})
export class HeatPumpChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public component: EdgeConfig.Component;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("heatpump-chart", service, translate);
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
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

            if (this.component.id + '/Status' in result.data) {

                const stateTimeData = result.data[this.component.id + '/Status'].map(value => {
                    if (value == null) {
                        return null;
                    } else {
                        return value;
                    }
                });

                datasets.push({
                    label: this.translate.instant('GENERAL.STATE'),
                    data: stateTimeData,
                    hidden: false,
                });
                this.colors.push({
                    backgroundColor: 'rgba(200,0,0,0.05)',
                    borderColor: 'rgba(200,0,0,1)',
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
            resolve([new ChannelAddress(this.component.id, 'Status')]);
        });
    }

    private applyControllerSpecificOptions(options: Chart.ChartOptions) {
        const translate = this.translate;
        options.scales[ChartAxis.LEFT]['title'].text = this.translate.instant('GENERAL.STATE');
        options.scales[ChartAxis.LEFT].ticks.callback = function (label, index, labels) {
            switch (label) {
                case -1:
                    return translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED');
                case 0:
                    return translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK');
                case 1:
                    return translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION_SHORT');
                case 2:
                    return translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC_SHORT');
                case 3:
                    return translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM_SHORT');
            }
        };

        options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
            const label = tooltipItem.dataset.label;
            const value = tooltipItem.dataset.data[tooltipItem.dataIndex];
            let toolTipValue;
            switch (value) {
                case -1:
                    toolTipValue = translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.UNDEFINED');
                    break;
                case 0:
                    toolTipValue = translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK');
                    break;

                case 1:
                    toolTipValue = translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION');
                    break;
                case 2:
                    toolTipValue = translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC');
                    break;
                case 3:
                    toolTipValue = translate.instant('EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM');
                    break;
                default:
                    toolTipValue = '';
                    break;
            }
            return label + ": " + toolTipValue; // TODO get locale dynamically
        };

        options.scales[ChartAxis.LEFT].max = 3;
        options.scales[ChartAxis.LEFT]['beginAtZero'] = true;
        this.options = options;
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
