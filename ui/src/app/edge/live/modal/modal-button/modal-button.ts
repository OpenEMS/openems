import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModal } from "../abstractModal";
import { AbstractModalLine } from "../modal-line/abstract-modal-line";

@Component({
    selector: 'oe-modal-buttons',
    templateUrl: './modal-button.html',
})
export class ModalButtons extends AbstractModalLine {
    /** Name for parameter, displayed on the left side*/

    @Input() labels: ButtonLabel;

    @Input() icons: Icon[];

    @Input() value;

    /**
     * 
     * @param event get
     * @param labelValue 
     */
    public updateControllerMode(event: CustomEvent, labelValue: string) {
        // console.log("test 1")
        let oldMode = this.value;
        let newMode = event.detail.value;
        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, this.component.id, [
                { name: this.controlName, value: labelValue }
            ]).then(() => {
                this.value = newMode;
                this.formGroup.markAsPristine();
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
                // console.log("test 5")
                this.value = oldMode;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                console.warn(reason);
            });
        }
    }
    /**
     * 
     * @returns 
     */
    protected getFormGroup(): FormGroup {
        this.formGroup = this.formBuilder.group({
            controlName: new FormControl(this.component.properties[this.controlName]),
            // power: new FormControl(this.component.properties.power),
        })
        return this.formGroup
    }

    categoryChanged(value) {
        console.log('segment is', value['detail']['value']);
    }
}

export type ButtonLabel = {
    name: string;
    value: string;
}