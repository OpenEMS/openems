import { DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { differenceInHours, differenceInMinutes } from 'date-fns';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { ChannelthresholdModalComponent } from './modal/modal.component';

@Component({
    selector: ChanneltresholdWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ChanneltresholdWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;

    private static readonly SELECTOR = "channelthresholdWidget";

    public activeTimeOverPeriod: string = null;
    public data: Cumulated = null;
    public values: any;
    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        private decimalPipe: DecimalPipe
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
        });

    }

    ngOnDestroy() {
    }

    ngOnChanges() {
        this.updateValues();
    };


    updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.from, this.service.historyPeriod.to);
    };

    // Gather result & timestamps to calculate effective active time in % 
    queryHistoricTimeseriesData(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesDataResponse> {
        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    this.getChannelAddresses(config).then(channelAddresses => {
                        let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses);
                        edge.sendRequest(this.service.websocket, request).then(response => {
                            let result = (response as QueryHistoricTimeseriesDataResponse).result;

                            // convert datasets
                            let datasets = [];
                            for (let channel in result.data) {
                                let data = result.data[channel].map(value => {
                                    if (value == null) {
                                        return null
                                    } else {
                                        return value * 100; // convert to % [0,100]
                                    }
                                });
                                datasets.push({
                                    label: "Ausgang",
                                    data: data
                                });
                            }

                            // calculates the active time of period
                            let activeSum: number = 0;

                            let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']).toString();
                            result.data[outputChannel].forEach(value => {
                                activeSum += value;
                            })

                            let startDate = new Date(result.timestamps[0]);
                            let endDate = new Date(result.timestamps[result.timestamps.length - 1]);
                            let activePercent = activeSum / result.timestamps.length;
                            let activeTimeMinutes = differenceInMinutes(endDate, startDate) * activePercent;
                            if (activeTimeMinutes > 60) {
                                this.activeTimeOverPeriod = this.decimalPipe.transform(Math.round(activeTimeMinutes / 60).toString(), '1.0-1') + ' h'
                            } else {
                                this.activeTimeOverPeriod = this.decimalPipe.transform(activeTimeMinutes.toString(), '1.0-1') + ' m'
                            }

                            if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                                resolve(response as QueryHistoricTimeseriesDataResponse);
                            } else {
                                reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                            }
                        }).catch(reason => reject(reason));
                    }).catch(reason => reject(reason));
                })
            });
        });
    }

    getChannelAddresses(config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']);
            let channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: ChannelthresholdModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                controllerId: this.controllerId
            }
        });
        return await modal.present();
    }
}

