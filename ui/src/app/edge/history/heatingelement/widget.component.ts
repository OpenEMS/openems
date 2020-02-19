import { DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { differenceInMinutes, startOfDay, endOfDay } from 'date-fns';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { HeatingelementModalComponent } from './modal/modal.component';

@Component({
    selector: HeatingelementWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class HeatingelementWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;

    private static readonly SELECTOR = "heatingelementWidget";

    public component: EdgeConfig.Component = null;
    public activeTimeOverPeriod = []
    public activeTimeOverPeriodL1: string = null;
    public activeTimeOverPeriodL2: string = null;
    public activeTimeOverPeriodL3: string = null;
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
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.controllerId);
            });
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

                            // TODO use loops

                            // calculate active time of period in minutes and hours
                            let activeSum1: number = 0;
                            let activeSum2: number = 0;
                            let activeSum3: number = 0;

                            let outputChannel1 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress1']).toString();
                            let outputChannel2 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress2']).toString();
                            let outputChannel3 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress3']).toString();


                            result.data[outputChannel1].forEach(value => {
                                activeSum1 += value;
                            })
                            result.data[outputChannel2].forEach(value => {
                                activeSum2 += value;
                            })
                            result.data[outputChannel3].forEach(value => {
                                activeSum3 += value;
                            })

                            // start-/endOfTheDay because last timestamp is 23:55
                            let startDate = startOfDay(new Date(result.timestamps[0]));
                            let endDate = endOfDay(new Date(result.timestamps[result.timestamps.length - 1]));
                            let activePercent1 = activeSum1 / result.timestamps.length;
                            let activePercent2 = activeSum2 / result.timestamps.length;
                            let activePercent3 = activeSum3 / result.timestamps.length;

                            let activeTimeMinutes1 = differenceInMinutes(endDate, startDate) * activePercent1;
                            let activeTimeMinutes2 = differenceInMinutes(endDate, startDate) * activePercent2;
                            let activeTimeMinutes3 = differenceInMinutes(endDate, startDate) * activePercent3;

                            let activeTimeHours1: string = (activeTimeMinutes1 / 60).toFixed(1);
                            let activeTimeHours2: string = (activeTimeMinutes2 / 60).toFixed(1);
                            let activeTimeHours3: string = (activeTimeMinutes3 / 60).toFixed(1);


                            if (activeTimeMinutes1 > 59) {
                                this.activeTimeOverPeriodL1 = activeTimeHours1 + ' h'
                                // if activeTimeHours is XY.0, removes the '.0' from activeTimeOverPeriod string
                                activeTimeHours1.split('').forEach((letter, index) => {
                                    if (index == activeTimeHours1.length - 1 && letter == "0" && activeTimeMinutes1 > 60) {
                                        this.activeTimeOverPeriodL1 = activeTimeHours1.slice(0, -2) + ' h'
                                    }
                                });
                            } else {
                                this.activeTimeOverPeriodL1 = this.decimalPipe.transform(activeTimeMinutes1.toString(), '1.0-1') + ' m'
                            }

                            if (activeTimeMinutes2 > 59) {
                                this.activeTimeOverPeriodL2 = activeTimeHours2 + ' h'
                                // if activeTimeHours is XY.0, removes the '.0' from activeTimeOverPeriod string
                                activeTimeHours2.split('').forEach((letter, index) => {
                                    if (index == activeTimeHours2.length - 1 && letter == "0" && activeTimeMinutes2 > 60) {
                                        this.activeTimeOverPeriodL2 = activeTimeHours2.slice(0, -2) + ' h'
                                    }
                                });
                            } else {
                                this.activeTimeOverPeriodL2 = this.decimalPipe.transform(activeTimeMinutes2.toString(), '1.0-1') + ' m'
                            }

                            if (activeTimeMinutes3 > 59) {
                                this.activeTimeOverPeriodL3 = activeTimeHours3 + ' h'
                                // if activeTimeHours is XY.0, removes the '.0' from activeTimeOverPeriod string
                                activeTimeHours3.split('').forEach((letter, index) => {
                                    if (index == activeTimeHours3.length - 1 && letter == "0" && activeTimeMinutes3 > 60) {
                                        this.activeTimeOverPeriodL3 = activeTimeHours3.slice(0, -2) + ' h'
                                    }
                                });
                            } else {
                                this.activeTimeOverPeriodL3 = this.decimalPipe.transform(activeTimeMinutes3.toString(), '1.0-1') + ' m'
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
            const outputChannel1 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress1']);
            const outputChannel2 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress2']);
            const outputChannel3 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress3']);
            let channeladdresses = [outputChannel1, outputChannel2, outputChannel3];
            resolve(channeladdresses);
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: HeatingelementModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                component: this.component,
            }
        });
        return await modal.present();
    }
}

