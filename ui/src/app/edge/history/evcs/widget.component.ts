import { Component, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress } from '../../../shared/shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { EvcsModalComponent } from './modal/modal.component';

@Component({
    selector: EvcsWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class EvcsWidgetComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "evcsWidget";

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
        let channels: ChannelAddress[] = [];
        this.service.getConfig().then(config => {
            // find all EVCS components
            for (let componentId of config.getComponentIdsImplementingNature("io.openems.edge.evcs.api.Evcs")) {
                channels.push(new ChannelAddress(componentId, 'ChargePower'));
            }
        })
        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.data = response.result.data;
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    };

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: EvcsModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}

