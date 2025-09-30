import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ModalComponent } from "../pages/home";

@Component({
    selector: "oe-controller-evse-single",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: ModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
            },
        });
        return await MODAL.PRESENT();
    }
}
