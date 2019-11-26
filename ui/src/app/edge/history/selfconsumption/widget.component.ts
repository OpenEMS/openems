import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';
import { SelfconsumptionModalComponent } from './modal/modal.component';

@Component({
    selector: SelfconsumptionWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SelfconsumptionWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "selfconsumptionWidget";

    public selfconsumptionValue: number = null;
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
            new ChannelAddress('_sum', 'GridSellActiveEnergy'),
            new ChannelAddress('_sum', 'ProductionActiveEnergy'),
            new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
        ];
        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.service.getConfig().then(() => {
                let result = response.result;
                this.selfconsumptionValue = CurrentData.calculateSelfConsumption(result.data['_sum/GridSellActiveEnergy'],
                    result.data['_sum/ProductionActiveEnergy'], result.data['_sum/EssActiveDischargeEnergy']);
            }).catch(reason => {
                console.error(reason); // TODO error message
            });
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    };

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SelfconsumptionModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}

