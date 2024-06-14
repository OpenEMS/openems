// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'storageChargerChart',
    templateUrl: '../abstracthistorychart.html',
})
export class StorageChargerChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private moreThanOneProducer: boolean = null;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("storage-charger-chart", service, translate);
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
        this.colors = [];
        this.loading = true;
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

            Object.keys(result.data).forEach((channel) => {
                const address = ChannelAddress.fromString(channel);
                const chargerData = result.data[channel].map(value => {
                    if (value == null) {
                        return null;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                if (address.channelId == "ActualPower") {
                    datasets.push({
                        label: this.translate.instant('GENERAL.CHARGE_POWER'),
                        data: chargerData,
                        hidden: false,
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(0,223,0,0.05)',
                        borderColor: 'rgba(0,223,0,1)',
                    });
                }
            });
            this.datasets = datasets;
            this.loading = false;
            this.stopSpinner();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        }).finally(async () => {
            await this.setOptions(this.options);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ActualPower'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}
