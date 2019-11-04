import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ModalController } from '@ionic/angular';
import { ChannelthresholdModalComponent } from './modal/modal.component';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';

@Component({
    selector: ChanneltresholdWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ChanneltresholdWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;

    private static readonly SELECTOR = "channelthresholdWidget";

    public timeActiveOverPeriod: number = null;
    public data: Cumulated = null;
    public values: any;
    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
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
                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses);
                        edge.sendRequest(this.service.websocket, request).then(response => {
                            let result = (response as QueryHistoricTimeseriesDataResponse).result;

                            // convert datasets
                            let datasets = [];
                            for (let channel in result.data) {
                                let address = ChannelAddress.fromString(channel);
                                let data = result.data[channel].map(value => {
                                    if (value == null) {
                                        return null
                                    } else {
                                        if (value * 100 > 50) {
                                            value = 1;
                                        } else if (value * 100 < 50) {
                                            value = 0;
                                        }
                                        return value * 100; // convert to % [0,100]
                                    }
                                });
                                datasets.push({
                                    label: "Ausgang",
                                    data: data
                                });
                            }
                            // calculate the effective active time in percent
                            let compareArray = []
                            datasets.forEach(dataset => {
                                Object.values(dataset.data).forEach(data => {
                                    if (data == 100) {
                                        compareArray.push(data)
                                    }
                                })
                            })
                            this.timeActiveOverPeriod = (compareArray.length / (result.timestamps.length / 100));
                            console.log("blabla", compareArray.length / (result.timestamps.length / 100))
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

    getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve, reject) => {
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

