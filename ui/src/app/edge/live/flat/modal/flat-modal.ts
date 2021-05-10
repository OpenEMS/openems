import { Component, Input } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Service, Websocket } from "src/app/shared/shared";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: 'oe-flat-modal',
    templateUrl: 'flat-modal.html',
})
export class FlatModalComponent {
    @Input() title: string;
    @Input() icon: Icon;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }
}