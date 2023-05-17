import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { SubscribeEdgesRequest } from "src/app/shared/jsonrpc/request/subscribeEdgesRequest";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <ion-content></ion-content>
         <ion-router-outlet id="content"></ion-router-outlet>
    `,
})
export class EdgeComponent implements OnInit, OnDestroy {

    private edge: Edge | null = null;

    constructor(
        private router: Router,
        private activatedRoute: ActivatedRoute,
        private service: Service,
        private websocket: Websocket
    ) { }

    public ngOnInit(): void {
        this.activatedRoute.params.subscribe((params) => {

            // Set CurrentEdge in Metadata
            const edgeId = params['edgeId'];
            this.service.updateCurrentEdge(edgeId).then((edge) => {
                this.edge = edge;
                this.service.websocket.sendRequest(new SubscribeEdgesRequest({ edges: [edgeId] }))
                    .then(() => {

                        // Subscribe on these channels for the state in HeaderComponent
                        edge.subscribeChannels(this.websocket, '', [
                            new ChannelAddress('_sum', 'State'),
                        ]);
                    });
            }).catch(() => {
                this.router.navigate(['index']);
            });
        });
    }

    public ngOnDestroy(): void {
        if (!this.edge) {
            return;
        }
        this.edge.unsubscribeChannels(this.websocket, '');
    }

}