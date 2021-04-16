import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress, Utils } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent {


    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() essComponents: EdgeConfig.Component[];
    @Input() chargerComponents: EdgeConfig.Component[];

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public outputChannel: ChannelAddress[] = null;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }
}