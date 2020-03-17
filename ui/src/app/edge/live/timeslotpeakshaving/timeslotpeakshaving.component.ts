import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket, EdgeConfig, ChannelAddress } from '../../../shared/shared';
import { TimeslotPeakshavingModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: TimeslotPeakshavingComponent.SELECTOR,
    templateUrl: './timeslotpeakshaving.component.html'
})
export class TimeslotPeakshavingComponent {

    private static readonly SELECTOR = "timeslotpeakshaving";

    @Input() private componentId: string;


    public edge: Edge = null;

    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        protected translate: TranslateService,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
                this.edge.subscribeChannels(this.websocket, TimeslotPeakshavingComponent.SELECTOR, [
                    new ChannelAddress(this.component.properties['meter.id'], 'ActivePower')
                ])
            });
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, TimeslotPeakshavingComponent.SELECTOR);
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: TimeslotPeakshavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge
            }
        });
        return await modal.present();
    }
}