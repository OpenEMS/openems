import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "../../flat/flat-widget-line/abstract-flat-widget-line";
import { AbstractModal } from "../abstractModal";


@Component({
    selector: 'oe-modal-line-input',
    templateUrl: './modal-line-input.html',
})
export class ModalLineInput extends AbstractModal {
    /** Name for parameter, displayed on the left side*/
    @Input() name: string;
    @Input() input: boolean;
    @Input() formGroup: FormGroup;
    @Input() type: any;
    @Input() component: EdgeConfig.Component

    public formGroup2: FormGroup;
    public loading: boolean;

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        })
        this.formGroup2 = this.formBuilder.group({
            // mode: new FormControl(this.component.properties.mode),
            power: this.formGroup.controls[this.controlName].value,
        })
    }

    applyChanges() {
        console.log("test 0", this.edge)
        if (this.edge != null) {
            console.log("test 0.5")
            if (this.edge.roleIsAtLeast('owner')) {
                console.log("test 0.73")
                let updateComponentArray = [];
                Object.keys(this.formGroup2.controls).forEach((element, index) => {
                    if (this.formGroup2.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup2.controls)[index], value: this.formGroup2.controls[element].value })
                    }
                })
                console.log("test 1")
                this.loading = true;
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    console.log("test 2", this.component.id, updateComponentArray)
                    this.component.properties[this.controlName] = this.formGroup2.controls[this.controlName].value;
                    // this.component.properties.power = this.formGroup2.controls[this.controlName].value;
                    console.log("test component", this.component.properties)
                    this.loading = false;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                    console.log("test 3")
                    console.log("test 4 ", this.component.properties)
                    // }).catch(reason => {
                    //     this.formGroup.controls['mode'].setValue(this.component.properties.mode);
                    //     console.log("test 4")
                    //     this.formGroup.controls['power'].setValue(this.component.properties.power);
                    //     this.loading = false;
                    //     this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    //     console.log("test 5")
                    //     console.warn(reason);
                    // })
                    this.formGroup2.markAsPristine()
                    console.log("test 5")
                    // } else {
                    // this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
                })

            }
        }
    }
}

