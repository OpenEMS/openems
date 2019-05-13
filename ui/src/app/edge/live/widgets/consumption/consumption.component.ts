import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
    selector: 'consumption',
    templateUrl: './consumption.component.html'
})
export class ConsumptionComponent {

    private static readonly SELECTOR = "consumption";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, ConsumptionComponent.SELECTOR, [
                // Consumption
                new ChannelAddress('_sum', 'ConsumptionActivePower'),
                new ChannelAddress('_sum', 'ConsumptionMaxActivePower')
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ConsumptionComponent.SELECTOR);
        }
    }
}
