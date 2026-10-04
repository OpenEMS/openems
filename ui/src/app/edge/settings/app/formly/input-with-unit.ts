import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-input-with-unit",
    template: ` <ion-grid style="width: 100%;">
        <ion-input
            labelPlacement="floating"
            [label]="props.label"
            [formControl]="formControl"
            [formlyAttributes]="field"
        >
            @if (props.unit) {
                <span slot="end">
                    {{ props.unit }}
                </span>
            }
        </ion-input>
    </ion-grid>`,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FormlyInputWithUnitComponent extends FieldWrapper {}
