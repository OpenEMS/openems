import { ActivatedRoute } from '@angular/router';
import { calculateActiveTimeOverPeriod } from '../shared';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ChpSocModalComponent } from './modal/modal.component';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { ModalController } from '@ionic/angular';
import { QueryHistoricTimeseriesDataRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';

@Component({
    selector: ChpSocWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ChpSocWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private componentId: string;

    private static readonly SELECTOR = "chpsocWidget";

    public activeTimeOverPeriod: string = null;
    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    private inputChannel = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
                console.log("componento", this.component)
                this.inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
            })
        });
    }

    ngOnChanges() {
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

                            let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                            this.activeTimeOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);

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
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
            let channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: ChpSocModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                component: this.component,
                inputChannel: this.inputChannel
            }
        });
        return await modal.present();
    }
}

