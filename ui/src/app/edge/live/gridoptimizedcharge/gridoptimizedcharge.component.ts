import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { GridOptimizedChargeModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
    selector: GridOptimizedChargeComponent.SELECTOR,
    templateUrl: './gridoptimizedcharge.component.html'
})
export class GridOptimizedChargeComponent {

    private static readonly SELECTOR = "gridoptimizedcharge";

    @Input() private componentId: string;

    private edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        public modalController: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);

                edge.subscribeChannels(this.websocket, GridOptimizedChargeComponent.SELECTOR + this.componentId, [
                    new ChannelAddress(this.componentId, "DelayChargeState"),
                    new ChannelAddress(this.componentId, "SellToGridLimitState"),
                    new ChannelAddress(this.componentId, "DelayChargeMaximumChargeLimit"),
                    new ChannelAddress(this.componentId, "SellToGridLimitMinimumChargeLimit"),
                    new ChannelAddress(this.componentId, "RawSellToGridLimitChargeLimit"),
                    new ChannelAddress(this.componentId, "PredictedTargetMinute"),
                    new ChannelAddress(this.componentId, "PredictedTargetMinuteAdjusted"),
                    new ChannelAddress(this.componentId, "TargetMinute"),
                    new ChannelAddress(this.componentId, "TargetEpochSeconds"),
                ]);
            });
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, GridOptimizedChargeComponent.SELECTOR + this.componentId);
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: GridOptimizedChargeModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            }
        });
        return await modal.present();
    }
}