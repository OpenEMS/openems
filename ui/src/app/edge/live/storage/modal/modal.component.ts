import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress, Utils } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent {


    @Input() edge: Edge | null = null;
    @Input() config: EdgeConfig | null = null;
    @Input() essComponents: EdgeConfig.Component[] = [];
    @Input() chargerComponents: EdgeConfig.Component[] = [];

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public outputChannel: ChannelAddress[] = [];

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }
}