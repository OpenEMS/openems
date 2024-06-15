import { Component } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldWrapper } from "@ngx-formly/core";
import { FormlySelectFieldModalComponent } from "./formly-select-field-modal.component";

@Component({
    selector: 'formly-select-extended-wrapper',
    templateUrl: './formly-select-field.extended.html',
})
export class FormlySelectFieldExtendedWrapperComponent extends FieldWrapper {

    // this wrapper is used to display a select which has more
    // detailed information about an item when selecting them
    constructor(
        private modalController: ModalController,
    ) {
        super();

    }

    protected onSelectItem() {
        this.openModal();
    }

    /**
     * Opens the model to select the option.
     */
    private async openModal() {
        const modal = await this.modalController.create({
            component: FormlySelectFieldModalComponent,
            componentProps: {
                title: this.props.label,
                options: this.props.options,
                initialSelectedValue: this.formControl.value,
            },
            cssClass: ['auto-height', 'full-width'],
        });
        modal.onDidDismiss().then(event => {
            if (!event.data) {
                // nothing selected
                return;
            }
            const selectedValue = event.data.selectedValue;
            if (!selectedValue) {
                return;
            }
            this.formControl.setValue(selectedValue);
        });
        return await modal.present();
    }

}
