import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModal } from "../abstractModal";

@Component({
    selector: 'oe-modal-buttons',
    templateUrl: './modal-button.html',
})
export class ModalButtons extends AbstractModal {
    /** Name for parameter, displayed on the left side*/

    @Input() labels: ButtonLabel;

    @Input() icons: Icon[];

    @Input() component;

    @Input() value;

    @Input() formGroup: FormGroup;

    public updateControllerMode(event: CustomEvent, labelValue: string) {
        console.log("test 1")
        let oldMode = this.value;
        let newMode = event.detail.value;
        // console.log("test 2", this.edge)
        if (this.edge != null) {
            console.log("test 3", this.controlName, labelValue, this.componentId)
            this.edge.updateComponentConfig(this.websocket, this.component.id, [
                { name: this.controlName, value: labelValue }
            ]).then(() => {
                console.log("test 4")
                this.value = newMode;
                this.formGroup.markAsPristine();
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
                console.log("test 5")
                this.value = oldMode;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                console.warn(reason);
            });
        }
    }
}

export type ButtonLabel = {
    name: string;
    value: string;
}