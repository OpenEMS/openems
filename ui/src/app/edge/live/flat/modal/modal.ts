import { Component, Input } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Service, Websocket } from "src/app/shared/shared";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: 'oe-modal',
    templateUrl: 'modal.html',
})
export class ModalComponent {
    @Input() title: string;
    @Input() icon: Icon;
    public you_custom_scrollbar_style: string = '@media(pointer: fine) {::-webkit-scrollbar {width: 12px;}::-webkit-scrollbar-track {background: #fff;}::-webkit-scrollbar-track:hover {background: #f7f7f7;}::-webkit-scrollbar-thumb {background: #be1c1c;}::-webkit-scrollbar-thumb:hover {background: #888}.inner-scroll {scrollbar-width: thin}}'

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }
}