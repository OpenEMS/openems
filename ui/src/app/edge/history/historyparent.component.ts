import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { SubscribeEdgesRequest } from "src/app/shared/jsonrpc/request/subscribeEdgesRequest";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <div class="fullscreen">
        <router-outlet>
        </router-outlet>
    </div>
    `,
    styles: [`
    .fullscreen {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        /* bottom: 0; */
        height: 100%;
    }
    :host ::ng-deep  ion-content{
        position: absolute;
        height: 88%;
    }
`]
})
export class HistoryParentComponent {

}