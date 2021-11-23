import { Component, EventEmitter, Output } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { SilentPartnershipModalComponent } from "./modal/modal.component";

@Component({
    selector: 'silent_Partnership',
    templateUrl: './silentpartnership.html'
})
export class SilentPartnershipComponent {

    public title: string = 'Stille Beteiligung';
    @Output() public titleEvent: EventEmitter<String> = new EventEmitter<String>();

    updateParentTitle() {
        this.titleEvent.emit(this.title);
    }
    constructor(
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.updateParentTitle();
    }
    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SilentPartnershipModalComponent,
        });
        return await modal.present();
    }
}