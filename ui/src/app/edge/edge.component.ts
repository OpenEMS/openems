// @ts-strict-ignore
import { Component, effect, OnDestroy, OnInit } from "@angular/core";
import { ViewWillLeave } from "@ionic/angular";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { Pagination } from "../shared/service/pagination";
import { RouteService } from "../shared/service/route.service";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <ion-content></ion-content>
    <ion-router-outlet id="content"></ion-router-outlet>
    `,
    standalone: false,
})
export class EdgeComponent implements OnDestroy, ViewWillLeave, OnInit {

    protected latestIncident: { message: string | null, id: string } | null = null;

    private edge: Edge | null = null;

    constructor(
        private routeService: RouteService,
        private service: Service,
        private websocket: Websocket,
        private pagination: Pagination,
    ) {

        effect(() => {
            const edge = this.service.currentEdge();
            const edgeId = this.routeService.getRouteParam<string>("edgeId");
            if (!edgeId || !edge) {
                return;
            }

            pagination.subscribeEdge(edge);
        });
    }

    public async ngOnInit() {
        const edgeId = this.routeService.getRouteParam<string>("edgeId");
        this.service.updateCurrentEdge(edgeId);
    }

    public ionViewWillLeave() {
        this.ngOnDestroy();
    }

    public ngOnDestroy(): void {
        this.service.currentEdge.set(null);
        if (!this.edge) {
            return;
        }
        this.edge.unsubscribeAllChannels(this.websocket);

    }
}
