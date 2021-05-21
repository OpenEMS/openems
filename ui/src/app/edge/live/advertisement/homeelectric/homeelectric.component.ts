import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { Edge, Service } from "src/app/shared/shared";
import { HomeElectricModalComponent } from "./modal/modal.component";

@Component({
    selector: HomeElectricComponent.SELECTOR,
    templateUrl: './homeelectric.component.html'
})
export class HomeElectricComponent {

    private static readonly SELECTOR = "homeelectric";

    private edge: Edge = null;

    constructor(
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route)
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: HomeElectricModalComponent,
        });
        return await modal.present();
    }
}