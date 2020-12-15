import { Component, SimpleChanges } from '@angular/core';

import { Edge, EdgeConfig, Service, Websocket, ChannelAddress } from '../../../shared/shared';

import { ActivatedRoute } from '@angular/router';
import { environment } from 'src/environments';
import { EdgeRpcRequest } from 'src/app/shared/jsonrpc/request/edgeRpcRequest';
import { UpdateSoftwareAdminRequest } from 'src/app/shared/jsonrpc/request/updateSoftwareAdminRequest';
import { UpdateSoftwareAdminResponse } from 'src/app/shared/jsonrpc/response/updateSoftwareAdminResponse';




@Component({
    selector: 'kacoadmin',
    templateUrl: './kacoadmin.component.html'
})

export class KacoAdminComponent {
    private static readonly SELECTOR = "kacoadmin";

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public env = environment;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        this.service.getConfig().then(config => {
            this.config = config;

        });



    }

    public send(updateedge: boolean, ui: boolean) {
        this.service.getCurrentEdge().then(edge => {
            edge.sendRequest(this.websocket,
                new UpdateSoftwareAdminRequest(
                    { updateEdge: updateedge, updateUi: ui }
                )
            ).then(response => {
                let result = (response as UpdateSoftwareAdminResponse).result;

            }).catch(reason => {
                console.log(reason.error.message);
            })
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, KacoAdminComponent.SELECTOR);
        }
    }

}