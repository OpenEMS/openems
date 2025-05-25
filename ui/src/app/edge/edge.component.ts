// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ViewWillLeave } from "@ionic/angular";
import { SubscribeEdgesRequest } from "src/app/shared/jsonrpc/request/subscribeEdgesRequest";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";
import { DataService } from "../shared/components/shared/dataservice";
import { LiveDataService } from "./live/livedataservice";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <ion-content></ion-content>
         <ion-router-outlet id="content"></ion-router-outlet>
    `,
    standalone: false,
    providers: [{
        useClass: LiveDataService,
        provide: DataService,
    }],
})
export class EdgeComponent implements OnInit, OnDestroy, ViewWillLeave {

    protected latestIncident: { message: string | null, id: string } | null = null;

    private edge: Edge | null = null;

    constructor(
        private router: Router,
        private activatedRoute: ActivatedRoute,
        private service: Service,
        private websocket: Websocket,
        private dataService: DataService,
    ) { }

    public ngOnInit(): void {
        this.activatedRoute.params.subscribe((params) => {
            // Set CurrentEdge in Metadata
            const edgeId = params["edgeId"];
            this.service.updateCurrentEdge(edgeId).then((edge) => {
                this.edge = edge;

                this.checkMessages();
                this.service.websocket.sendRequest(new SubscribeEdgesRequest({ edges: [edgeId] }))
                    .then(() => {

                        // Subscribe on these channels for the state in HeaderComponent
                        this.dataService.getValues([
                            new ChannelAddress("_sum", "State"),
                        ], edge);
                    });
            }).catch(() => {
                this.router.navigate(["index"]);
            });
        });
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
