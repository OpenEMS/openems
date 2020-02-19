import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket, EdgeConfig, ChannelAddress } from '../../../../shared/shared';
import { SymmetricPeakshavingModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: SymmetricPeakshavingComponent.SELECTOR,
    templateUrl: './symmetricpeakshaving.component.html'
})
export class SymmetricPeakshavingComponent {

    private static readonly SELECTOR = "symmetricpeakshaving";

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
            });
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, SymmetricPeakshavingComponent.SELECTOR);
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SymmetricPeakshavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge
            }
        });
        return await modal.present();
    }
}