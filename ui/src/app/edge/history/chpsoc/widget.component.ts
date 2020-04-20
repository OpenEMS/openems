import { ActivatedRoute } from '@angular/router';
import { calculateActiveTimeOverPeriod } from '../shared';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ChpSocModalComponent } from './modal/modal.component';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { AbstractHistoryTimePeriod } from '../abstracthistorytimeperiod';

@Component({
    selector: ChpSocWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ChpSocWidgetComponent extends AbstractHistoryTimePeriod implements OnInit, OnChanges {

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
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
                this.inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
            })
        });
        this.subscribeValueRefresh()
    }

    ngOnDestroy() {
        this.unsubscribeValueRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    // Gather result & timestamps to calculate effective active time in % 
    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.from, this.service.historyPeriod.to).then(response => {
            this.service.getConfig().then(config => {
                let result = (response as QueryHistoricTimeseriesDataResponse).result;
                let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                this.activeTimeOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);
            });
        });
    };

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
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

