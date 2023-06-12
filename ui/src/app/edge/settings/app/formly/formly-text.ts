import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType, FieldTypeConfig } from "@ngx-formly/core";

@Component({
    selector: 'formly-text',
    styles: ['.warning {color: red}'],
    template: `
    <ion-item lines="none">
        <ion-text [innerHTML]="props.description"></ion-text>
    </ion-item>
    `,
    encapsulation: ViewEncapsulation.None
})
export class FormlyTextComponent extends FieldType<FieldTypeConfig>  {

    constructor(
    ) {
        super();
    }

}
