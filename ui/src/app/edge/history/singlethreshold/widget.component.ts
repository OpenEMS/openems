import { DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { differenceInHours, differenceInMinutes, startOfDay, endOfDay } from 'date-fns';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { SinglethresholdModalComponent } from './modal/modal.component';

@Component({
    selector: SingletresholdWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SingletresholdWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;

    private static readonly SELECTOR = "singlethresholdWidget";

    public activeTimeOverPeriod: string = null;
    public data: Cumulated = null;
    public values: any;
    public edge: Edge = null;
    public controller: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        private decimalPipe: DecimalPipe
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
            this.service.getConfig().then(config => {
                this.controller = config.getComponent(this.controllerId);
            })
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

                            // calculate active time of period in minutes and hours
                            let activeSum: number = 0;
                            let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']).toString();

                            result.data[outputChannel].forEach(value => {
                                activeSum += value;
                            })

                            // start-/endOfTheDay because last timestamp is 23:55
                            let startDate = startOfDay(new Date(result.timestamps[0]));
                            let endDate = endOfDay(new Date(result.timestamps[result.timestamps.length - 1]));
                            let activePercent = activeSum / result.timestamps.length;
                            let activeTimeMinutes = differenceInMinutes(endDate, startDate) * activePercent;
                            let activeTimeHours: string = (activeTimeMinutes / 60).toFixed(1);

                            if (activeTimeMinutes > 59) {
                                this.activeTimeOverPeriod = activeTimeHours + ' h'
                                // if activeTimeHours is XY.0, removes the '.0' from activeTimeOverPeriod string
                                activeTimeHours.split('').forEach((letter, index) => {
                                    if (index == activeTimeHours.length - 1 && letter == "0" && activeTimeMinutes > 60) {
                                        this.activeTimeOverPeriod = activeTimeHours.slice(0, -2) + ' h'
                                    }
                                });
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
            component: SinglethresholdModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                controllerId: this.controllerId,
                controller: this.controller
            }
        });
        return await modal.present();
    }
}

