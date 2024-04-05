import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { DelayedSellToGridModalComponent } from './modal/modal.component';

@Component({
    selector: DelayedSellToGridComponent.SELECTOR,
    templateUrl: './delayedselltogrid.component.html',
})
export class DelayedSellToGridComponent implements OnInit, OnDestroy {

    private static readonly SELECTOR = "delayedselltogrid";

    @Input() public componentId: string;

    public edge: Edge = null;

    public component: EdgeConfig.Component = null;

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
        public service: Service,
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
        this.edge.unsubscribeChannels(this.websocket, DelayedSellToGridComponent.SELECTOR);
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: DelayedSellToGridModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        });
        return await modal.present();
    }
}
