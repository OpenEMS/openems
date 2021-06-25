import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "../../flat/flat-widget-line/abstract-flat-widget-line";
import { AbstractModal } from "../abstractModal";
import { AbstractModalLine } from "./abstract-modal-line";


@Component({
    selector: 'oe-modal-line-input',
    templateUrl: './modal-line-input.html',
})
export class ModalLineInput extends AbstractModalLine {
    /** Name for parameter, displayed on the left side*/
    @Input() name: string;
    @Input() input: boolean;
    @Input() formGroup: FormGroup;
    @Input() type: any;
    @Input() component: EdgeConfig.Component

    public loading: boolean;

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        })
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                let updateComponentArray = [];
                Object.keys(this.formGroup.controls).forEach((element, index) => {
                    if (this.formGroup.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                    }
                })
                this.loading = true;
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    this.component.properties[this.controlName] = this.formGroup.controls[this.controlName].value;
                    this.loading = false;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.controls[this.controlName].setValue(this.component.properties[this.controlName]);
                    this.loading = false;
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                })
                this.formGroup.markAsPristine()
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }
}

