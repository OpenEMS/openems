import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { GridModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';
import { AbstractHistoryTimePeriod } from '../abstracthistorytimeperiod';

@Component({
    selector: GridComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class GridComponent extends AbstractHistoryTimePeriod implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "gridWidget";

    public data: Cumulated = null;
    public edge: Edge = null;

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
        });
        this.subscribeValueRefresh()
    }

    ngOnDestroy() {
        this.unsubscribeValueRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    this.data = response.result.data;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                new ChannelAddress('_sum', 'GridSellActiveEnergy'),
            ];
            resolve(channels);
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: GridModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}