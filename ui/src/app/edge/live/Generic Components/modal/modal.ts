import { Component, Input } from "@angular/core";
import { AbstractModal } from "./abstractModal";

@Component({
    selector: 'oe-modal',
    templateUrl: 'modal.html',
})
export class ModalComponent extends AbstractModal {

    /** Title in Header */
    @Input() title: string;

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        })
        this.formGroup = this.getFormGroup();
    }
    applyChanges() {
        console.log("test 1")
        if (this.edge != null) {
            console.log("test 2")
            // if (this.edge.roleIsAtLeast('owner')) {
            console.log("test 3")
            let updateComponentArray = [];
            Object.keys(this.formGroup.controls).forEach((element, index) => {
                if (this.formGroup.controls[element].dirty) {
                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                }
            })
            this.loading = true;
            this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                this.component.properties['power'] = this.formGroup.controls['power'].value;
                this.component.properties['mode'] = this.formGroup.controls['mode'].value;
                console.log("componentproperties", this.component.properties[this.controlName])
                this.loading = false;
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
                this.formGroup.controls['mode'].setValue(this.component.properties.mode);
                this.formGroup.controls['power'].setValue(this.component.properties['power']);
                this.loading = false;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + 'sopme', 'danger');
                console.warn(reason);
            })
            this.formGroup.markAsPristine()
            // } else {
            //     this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
        }
        // }
    }
}