import { Component } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldWrapper } from "@ngx-formly/core";
import { FormlySelectFieldModalComponent } from "./formly-select-field-MODAL.COMPONENT";

@Component({
    selector: "formly-select-extended-wrapper",
    templateUrl: "./formly-select-FIELD.EXTENDED.HTML",
    standalone: false,
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
        THIS.OPEN_MODAL();
    }

    /**
     * Opens the model to select the option.
     */
    private async openModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: FormlySelectFieldModalComponent,
            componentProps: {
                title: THIS.PROPS.LABEL,
                options: THIS.PROPS.OPTIONS,
                initialSelectedValue: THIS.FORM_CONTROL.VALUE,
            },
            cssClass: ["auto-height", "full-width"],
        });
        MODAL.ON_DID_DISMISS().then(event => {
            if (!EVENT.DATA) {
                // nothing selected
                return;
            }
            const selectedValue = EVENT.DATA.SELECTED_VALUE;
            if (!selectedValue) {
                return;
            }
            THIS.FORM_CONTROL.SET_VALUE(selectedValue);
        });
        return await MODAL.PRESENT();
    }

}
