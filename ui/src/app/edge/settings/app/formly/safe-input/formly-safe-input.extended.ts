import { Component, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldWrapper } from "@ngx-formly/core";
import { FormlySafeInputModalComponent } from "./formly-safe-input-modal.component";

@Component({
    selector: 'formly-safe-input-wrapper',
    templateUrl: './formly-safe-input.extended.html',
})
export class FormlySafeInputWrapperComponent extends FieldWrapper implements OnInit {

    protected pathToDisplayValue: string

    constructor(
        private modalController: ModalController
    ) {
        super();
    }

    ngOnInit(): void {
        this.pathToDisplayValue = this.props["pathToDisplayValue"]
    }

    protected onSelectItem() {
        this.openModal();
    }

    /**
     * Opens the model to select the option.
     */
    private async openModal() {
        const modal = await this.modalController.create({
            component: FormlySafeInputModalComponent,
            componentProps: {
                title: this.props.label,
                fields: this.field.fieldGroup,
                model: this.model
            },
            cssClass: ['auto-height']
        });
        modal.onDidDismiss().then(event => {
            if (!event.data) {
                // nothing selected
                return;
            }
            for (const [key, value] of Object.entries(event.data)) {
                this.model[key] = value
            }
            // update values in model so ui also gets updated
            this.form.setValue(this.form.getRawValue())
        });
        return await modal.present();
    }

}
