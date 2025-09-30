// @ts-strict-ignore
import { Component, effect, OnDestroy, OnInit } from "@angular/core";
import { ViewWillLeave } from "@ionic/angular";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { Pagination } from "../shared/service/pagination";
import { RouteService } from "../shared/service/ROUTE.SERVICE";

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
            const edge = THIS.SERVICE.CURRENT_EDGE();
            const edgeId = THIS.ROUTE_SERVICE.GET_ROUTE_PARAM<string>("edgeId");
            if (!edgeId || !edge) {
                return;
            }

            PAGINATION.SUBSCRIBE_EDGE(edge);
            THIS.CHECK_MESSAGES();
        });
    }

    public async ngOnInit() {
        const edgeId = THIS.ROUTE_SERVICE.GET_ROUTE_PARAM<string>("edgeId");
        THIS.SERVICE.UPDATE_CURRENT_EDGE(edgeId);
    }

    public checkMessages(): void {
    }

    public ionViewWillLeave() {
        THIS.NG_ON_DESTROY();
    }

    public ngOnDestroy(): void {
        THIS.SERVICE.CURRENT_EDGE.SET(null);
        if (!THIS.EDGE) {
            return;
        }
        THIS.EDGE.UNSUBSCRIBE_ALL_CHANNELS(THIS.WEBSOCKET);

    }
}
