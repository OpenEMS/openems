import { Component, OnInit, Input } from '@angular/core';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress, Utils } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { UpdateSoftwareRequest } from 'src/app/shared/jsonrpc/request/updateSoftwareRequest';

@Component({
    selector: 'kacoupdate-modal',
    templateUrl: './modal.component.html',
})
export class KacoUpdateModalComponent implements OnInit {

    private static readonly SELECTOR = "kacoupdate-modal";

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() essComponents: EdgeConfig.Component[];

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public outputChannel: ChannelAddress[] = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
    }

    updateSoftware() {
        let request = new UpdateSoftwareRequest();
        this.edge.sendRequest(this.websocket, request).then(response => {

        })
    }
}