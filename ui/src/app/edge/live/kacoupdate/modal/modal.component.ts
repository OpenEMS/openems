import { Component, OnInit, Input } from '@angular/core';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress, Utils } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { UpdateSoftwareRequest } from 'src/app/shared/jsonrpc/request/updateSoftwareRequest';
import { UpdateSoftwareResponse } from 'src/app/shared/jsonrpc/response/updateSoftwareResponse';
import { Alerts } from 'src/app/shared/service/alerts';
import { environment } from 'src/environments';

@Component({
    selector: 'kacoupdate-modal',
    templateUrl: './modal.component.html',
})
export class KacoUpdateModalComponent implements OnInit {

    private static readonly SELECTOR = "kacoupdate-modal";

    @Input() edge: Edge;
    @Input() config: EdgeConfig;

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public outputChannel: ChannelAddress[] = null;

    public uiSuccess = false;
    public uiError = false;
    public edgeSuccess = false;
    public edgeError = false;
    public env = environment;
    public btnDisabled = false;
    public isUpdating = false;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
        private alerts: Alerts,
    ) { }

    ngOnInit() {
    }

    updateSoftware() {
        this.btnDisabled = true;
        this.isUpdating = true;

        let request = new UpdateSoftwareRequest();
        this.edge.sendRequest(this.websocket, request).then(response => {

            let result = (response as UpdateSoftwareResponse).result;

            let message = "";
            let restart = false;
            switch (result.Success) {
                case 1:
                    message += this.translate.instant('KacoUpdate.Success1');
                    break;
                case 2:
                    message += this.translate.instant('KacoUpdate.Success2');
                    restart = true;
                    break;
                case 3:
                    message += this.translate.instant('KacoUpdate.Success3');
                    restart = true;
                    break;
                default:
                    break;
            }

            switch (result.Error) {
                case 1:
                    message += this.translate.instant('KacoUpdate.Error1');
                    break;
                case 2:
                    message += this.translate.instant('KacoUpdate.Error2');
                    break;
                case 3:
                    message += this.translate.instant('KacoUpdate.Error3');
                    break;
                default:
                    break;
            }
            this.modalCtrl.dismiss();
            this.alerts.updateConfirm(message, restart);
            this.isUpdating = false;
            this.btnDisabled = false;

        });


    }
}