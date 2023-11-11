import { Component } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-input-with-unit',
    template: `
    <ion-grid style="width: 100%;">
        <ion-row class="ion-align-items-center">
            <ion-col>
                <ng-template #fieldComponent></ng-template>
            </ion-col>
            <ion-col size="auto">
                 {{props.unit}}
            </ion-col>
        </ion-row>
    </ion-grid>
`
})
export class FormlyInputWithUnitComponent extends FieldWrapper { }
