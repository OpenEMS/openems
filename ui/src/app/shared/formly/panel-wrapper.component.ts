import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: 'formly-wrapper-panel',
    template: `
    <ion-item lines="none" class="ion-no-padding">
        <ion-grid>
            <ion-row>
                <ion-col>
                    <ion-label>{{ to.label }}</ion-label>
                    <ng-template #fieldComponent></ng-template>
                </ion-col>
            </ion-row>
        </ion-grid>
    </ion-item>
    `
})
export class PanelWrapperComponent extends FieldWrapper {
}
