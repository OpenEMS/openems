import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from './../shared';

@Component({
    selector: 'heatpumpchart',
    templateUrl: '../abstracthistorychart.html'
})
export class HeatPumpChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public component: EdgeConfig.Component;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("heatpump-chart", service, translate);
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            if (this.component.id + '/Status' in result.data) {

                let stateTimeData = result.data[this.component.id + '/Status'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value
                    }
                })

                datasets.push({
                    label: this.translate.instant('General.state'),
                    data: stateTimeData,
                    hidden: false
                })
                this.colors.push({
                    backgroundColor: 'rgba(200,0,0,0.05)',
                    borderColor: 'rgba(200,0,0,1)',
                })
            }
            this.datasets = datasets;
            this.loading = false;
            this.stopSpinner();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            resolve([new ChannelAddress(this.component.id, 'Status')]);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        let translate = this.translate;
        options.scales.yAxes[0].id = 'yAxis1'
        options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.state');
        options.scales.yAxes[0].ticks.callback = function (label, index, labels) {
            switch (label) {
                case -1:
                    return translate.instant('Edge.Index.Widgets.HeatPump.undefined');
                case 0:
                    return translate.instant('Edge.Index.Widgets.HeatPump.lock');
                case 1:
                    return translate.instant('Edge.Index.Widgets.HeatPump.normalOperationShort');
                case 2:
                    return translate.instant('Edge.Index.Widgets.HeatPump.switchOnRecShort');
                case 3:
                    return translate.instant('Edge.Index.Widgets.HeatPump.switchOnComShort');
            }
        }
        options.scales.yAxes[0].ticks.max = 3;
        options.scales.yAxes[0].ticks.stepSize = 1;
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            let toolTipValue;
            switch (value) {
                case -1:
                    toolTipValue = translate.instant('Edge.Index.Widgets.HeatPump.undefined');
                    break;
                case 0:
                    toolTipValue = translate.instant('Edge.Index.Widgets.HeatPump.lock');
                    break;

                case 1:
                    toolTipValue = translate.instant('Edge.Index.Widgets.HeatPump.normalOperation');
                    break;
                case 2:
                    toolTipValue = translate.instant('Edge.Index.Widgets.HeatPump.switchOnRec');
                    break;
                case 3:
                    toolTipValue = translate.instant('Edge.Index.Widgets.HeatPump.switchOnCom');
                    break;
                default:
                    toolTipValue = '';
                    break;
            }
            return label + ": " + toolTipValue; // TODO get locale dynamically
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}