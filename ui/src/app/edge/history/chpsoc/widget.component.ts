import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, ChannelAddress } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ModalController } from '@ionic/angular';
import { ChpSocModalComponent } from './modal/modal.component';

@Component({
    selector: ChpSocWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ChpSocWidgetComponent implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private componentId: string;

    private static readonly SELECTOR = "chpsocWidget";

    public controller: EdgeConfig.Component = null;

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
        this.service.getConfig().then(config => {
            this.controller = config.components[this.componentId];
        })
    }

    ngOnDestroy() {
    }

    ngOnChanges() {
        this.updateValues();
    };

    updateValues() {
        let channels: ChannelAddress[] = [
        ];

        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
            this.data = response.result.data;
        }).catch(reason => {
            console.error(reason); // TODO error message
        });
    };

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: ChpSocModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                controller: this.controller,
            }
        });
        return await modal.present();
    }
}

