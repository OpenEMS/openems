import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-input-with-unit",
    template: `
    <ion-grid style="width: 100%;">
        <ion-row class="ion-align-items-center" style="flex-wrap: nowrap;">
            <ion-col>
                <ng-template #fieldComponent></ng-template>
            </ion-col>

            <ion-col style="flex: 0 0 5rem; max-width: 5rem;"
                class="ion-text-start">
                {{ props.unit }}
            </ion-col>
        </ion-row>
    </ion-grid>`,
    standalone: false,
})
export class FormlyInputWithUnitComponent extends FieldWrapper { }
