import { ActivatedRoute } from '@angular/router';
import { AutarchyModalComponent } from './modal/modal.component';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';

@Component({
    selector: AutarchyWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class AutarchyWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "autarchyWidget";

    public autarchyValue: number = null;
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
            new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
        ];
        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.service.getConfig().then(() => {
                let result = response.result;
                this.autarchyValue = CurrentData.calculateAutarchy(result.data['_sum/GridBuyActiveEnergy'] / 1000, result.data['_sum/ConsumptionActiveEnergy'] / 1000)
            }).catch(reason => {
                console.error(reason); // TODO error message
            });
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AutarchyModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}

