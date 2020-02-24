import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Edge, Service, EdgeConfig, Websocket, ChannelAddress } from 'src/app/shared/shared';
import { SymmetricPeakshavingModalComponent } from './modal/modal.component';

@Component({
    selector: SymmetricPeakshavingWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SymmetricPeakshavingWidgetComponent implements OnInit {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;


    private static readonly SELECTOR = "symmetricPeakshavingWidget";

    public autarchyValue: number = null;
    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        private websocket: Websocket,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.controllerId);
            });
        });
    }

    ngOnDestroy() {
    }


    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SymmetricPeakshavingModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                component: this.component
            }
        });
        return await modal.present();
    }
}

