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
import { calculateActiveTimeOverPeriod } from '../shared';

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
        private decimalPipe: DecimalPipe,
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

                            let outputChannel1 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress1']);
                            let outputChannel2 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress2']);
                            let outputChannel3 = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress3']);

                            this.activeTimeOverPeriodL1 = calculateActiveTimeOverPeriod(outputChannel1, result);
                            this.activeTimeOverPeriodL2 = calculateActiveTimeOverPeriod(outputChannel2, result);
                            this.activeTimeOverPeriodL3 = calculateActiveTimeOverPeriod(outputChannel3, result);

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

