import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket, EdgeConfig } from '../../../shared/shared';
import { SymmetricPeakshavingModalComponent } from './modal/modal.component';

@Component({
    selector: SymmetricPeakshavingComponent.SELECTOR,
    templateUrl: './symmetricpeakshaving.component.html'
})
export class SymmetricPeakshavingComponent {

    private static readonly SELECTOR = "symmetricpeakshaving";

    @Input() private componentId: string;


    private edge: Edge = null;

    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
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
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SymmetricPeakshavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge
            }
        });
        console.log("component", this.component.properties)
        return await modal.present();
    }
}