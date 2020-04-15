import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { HeatingelementModalComponent } from './modal/modal.component';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { ModalController } from '@ionic/angular';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';

@Component({
    selector: HeatingelementWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class HeatingelementWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private componentId: string;

    private static readonly SELECTOR = "heatingelementWidget";

    public component: EdgeConfig.Component = null;

    public activeTimeOverPeriodLevel1: number = null;
    public activeTimeOverPeriodLevel2: number = null;
    public activeTimeOverPeriodLevel3: number = null;

    public edge: Edge = null;

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    ngOnChanges() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.from, this.service.historyPeriod.to);
    };

    queryHistoricTimeseriesData(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesDataResponse> {
        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.getChannelAddresses().then(channelAddresses => {
                    let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses);
                    edge.sendRequest(this.service.websocket, request).then(response => {
                        let result = (response as QueryHistoricTimeseriesDataResponse).result;

                        let Level1Time = result.data[this.componentId + '/Level1Time'];
                        let Level2Time = result.data[this.componentId + '/Level2Time']
                        let Level3Time = result.data[this.componentId + '/Level3Time']

                        // takes last value as active time in seconds
                        for (let value of Level1Time.reverse()) {
                            if (value != null && value != 0) {
                                this.activeTimeOverPeriodLevel1 = value;
                                break;
                            }
                        }

                        for (let value of Level2Time.reverse()) {
                            if (value != null && value != 0) {
                                this.activeTimeOverPeriodLevel2 = value;
                                break;
                            }
                        }

                        for (let value of Level3Time.reverse()) {
                            if (value != null && value != 0) {
                                this.activeTimeOverPeriodLevel3 = value;
                                break;
                            }
                        }

                        if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                            resolve(response as QueryHistoricTimeseriesDataResponse);
                        } else {
                            reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                        }
                    }).catch(reason => reject(reason));
                })
            });
        });
    }

    getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channeladdresses = [
                new ChannelAddress(this.componentId, 'Level1Time'),
                new ChannelAddress(this.componentId, 'Level2Time'),
                new ChannelAddress(this.componentId, 'Level3Time'),
            ];
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

