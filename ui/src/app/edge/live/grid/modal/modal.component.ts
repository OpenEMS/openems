import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'grid-modal',
    templateUrl: './modal.component.html'
})
export class GridModalComponent {

    private static readonly SELECTOR = "grid-modal";

    @Input() edge: Edge;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        public translate: TranslateService,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, GridModalComponent.SELECTOR, [
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'),
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, GridModalComponent.SELECTOR);
        }
    }
}
