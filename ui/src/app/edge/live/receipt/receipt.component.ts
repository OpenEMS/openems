import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { ModalController } from '@ionic/angular';
import { ReceiptModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'receipt',
    templateUrl: './receipt.component.html'
})

export class ReceiptComponent {

    private static readonly SELECTOR = "receipt";

    public edge: Edge = null;
    public config: EdgeConfig = null;

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
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ReceiptComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: ReceiptModalComponent,
            componentProps: {
                edge: this.edge,
            }
        });
        return await modal.present();
    }
}