// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'storageESSChart',
    templateUrl: '../abstracthistorychart.html',
})
export class StorageESSChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {


    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;
    @Input({ required: true }) public showPhases!: boolean;

    private moreThanOneProducer: boolean = null;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("storage-ess-chart", service, translate);
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
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
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    const result = response.result;
                    // convert labels
                    const labels: Date[] = [];
                    for (const timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    const datasets = [];
                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            const data = result.data[channelAddress.toString()]?.map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            });
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == "ActivePower") {
                                    datasets.push({
                                        label: this.translate.instant('General.chargeDischarge'),
                                        data: data,
                                        hidden: false,
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(0,223,0,0.05)',
                                        borderColor: 'rgba(0,223,0,1)',
                                    });
                                }
                                if (this.componentId + '/ActivePowerL1' && this.componentId + '/ActivePowerL2' && this.componentId + '/ActivePowerL3' in result.data && this.showPhases == true) {
                                    if (channelAddress.channelId == 'ActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: data,
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: data,
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: data,
                                        });
                                        this.colors.push(this.phase3Color);
                                    }
                                }
                            }
                        });
                    }).finally(async () => {
                        this.datasets = datasets;
                        this.loading = false;
                        this.stopSpinner();
                        await this.setOptions(this.options);
                    });
                }).catch(reason => {
                    console.error(reason); // TODO error message
                    this.initializeChart();
                    return;
                });

            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        const component = config.getComponent(this.componentId);
        const factoryID = component.factoryId;
        const factory = config.factories[factoryID];
        return new Promise((resolve, reject) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ActivePower'),
            ];
            if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                result.push(
                    new ChannelAddress(component.id, 'ActivePowerL1'),
                    new ChannelAddress(component.id, 'ActivePowerL2'),
                    new ChannelAddress(component.id, 'ActivePowerL3'),
                );
            }
            resolve(result);
        });
    }

    protected setLabel() {
        const options = this.createDefaultChartOptions();
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}
