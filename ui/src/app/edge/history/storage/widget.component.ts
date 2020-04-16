import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { StorageModalComponent } from './modal/modal.component';

@Component({
    selector: StorageComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class StorageComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "storageWidget";

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

    ngOnDestroy() {
    }

    ngOnChanges() {
        this.updateValues();
    };

    updateValues() {
        let channels: ChannelAddress[] = [
            //new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
            //new ChannelAddress('_sum', 'EssActiveDischargeEnergy'),
            new ChannelAddress('_sum', 'EssAmpereHours')
        ];
        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.data = response.result.data;
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    };

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: StorageModalComponent,
            cssClass: 'wide-modal',
        });
        return await modal.present();
    }
}

