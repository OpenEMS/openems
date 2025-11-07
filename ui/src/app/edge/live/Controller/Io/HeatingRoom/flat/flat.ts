import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ModalComponent } from "../modal/modal";

@Component({
    selector: "oe-controller-io-heating-room",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected modalComponent: Modal | null = null;
    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        };
    };
}
