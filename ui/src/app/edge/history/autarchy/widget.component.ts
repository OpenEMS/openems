import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ModalController } from '@ionic/angular';
import { AutarchyModalComponent } from './modal/modal.component';
import { CurrentData } from 'src/app/shared/edge/currentdata';

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

    ngOnDestroy() {
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
            this.service.getConfig().then(config => {
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

