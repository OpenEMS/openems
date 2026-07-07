import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ModalComponent } from "../pages/home";

@Component({
    selector: "oe-controller-evse-single",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected readonly modalComponent: Modal = {
        component: ModalComponent,
        componentProps: {
            component: this.component,
        },
    };

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        });
        return await modal.present();
    }
}
