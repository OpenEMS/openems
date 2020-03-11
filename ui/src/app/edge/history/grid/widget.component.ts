import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { GridModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
    selector: GridComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class GridComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "gridWidget";

    public data: Cumulated = null;
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

    ngOnChanges() {
        this.updateValues();
    };

    updateValues() {
        let channels: ChannelAddress[] = [
            new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
            new ChannelAddress('_sum', 'GridSellActiveEnergy'),
        ];

        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.data = response.result.data;
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    };

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: GridModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}