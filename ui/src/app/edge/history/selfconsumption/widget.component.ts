import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { SelfconsumptionModalComponent } from './modal/modal.component';
import { AbstractHistoryTimePeriod } from '../abstracthistorytimeperiod';

@Component({
    selector: SelfconsumptionWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SelfconsumptionWidgetComponent extends AbstractHistoryTimePeriod implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "selfconsumptionWidget";

    public selfconsumptionValue: number = null;
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
                    let result = response.result;
                    this.selfconsumptionValue = CurrentData.calculateSelfConsumption(result.data['_sum/GridSellActiveEnergy'],
                        result.data['_sum/ProductionActiveEnergy'], result.data['_sum/EssActiveDischargeEnergy']);
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridSellActiveEnergy'),
                new ChannelAddress('_sum', 'ProductionActiveEnergy'),
                new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
            ];
            resolve(channels);
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SelfconsumptionModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}

