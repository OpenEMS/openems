import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
    selector: 'grid',
    templateUrl: './grid.component.html'
})
export class GridComponent {

    private static readonly SELECTOR = "grid";

    public edge: Edge = null;

    constructor(
        private service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, GridComponent.SELECTOR, [
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'),
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, GridComponent.SELECTOR);
        }
    }
}
