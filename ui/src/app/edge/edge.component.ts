// @ts-strict-ignore
import { Component, effect, OnDestroy, OnInit, inject } from "@angular/core";
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
    private routeService = inject(RouteService);
    private service = inject(Service);
    private websocket = inject(Websocket);
    private pagination = inject(Pagination);


    protected latestIncident: { message: string | null, id: string } | null = null;

    private edge: Edge | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const pagination = this.pagination;


        effect(() => {
            const edge = this.service.currentEdge();
            const edgeId = this.routeService.getRouteParam<string>("edgeId");
            if (!edgeId || !edge) {
                return;
            }

            pagination.subscribeEdge(edge);
            this.checkMessages();
        });
    }

    public async ngOnInit() {
        const edgeId = this.routeService.getRouteParam<string>("edgeId");
        this.service.updateCurrentEdge(edgeId);
    }

    public checkMessages(): void {
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
