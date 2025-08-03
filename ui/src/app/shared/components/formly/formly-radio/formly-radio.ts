import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-radio",
  template: `
    <ion-list [ngClass]="field.className">
      <!-- Show label if provided -->
      @if (to.label) {
        <ion-label>{{ to.label }}</ion-label>
      }
    
      <!-- Show description if provided -->
      @if (to.description) {
        <p style="font-size: x-small;" class="ion-margin-bottom ion-text-secondary">{{ to.description }}</p>
      }
    
      <ion-radio-group [formControl]="formControl" [formlyAttributes]="field">
        @for (option of to.options; track option) {
          <ion-item>
            <ion-label>{{ option.label }}</ion-label>
            <ion-radio
              [value]="option.value"
              [slot]="to.radioSlot || 'end'">
            </ion-radio>
          </ion-item>
        }
      </ion-radio-group>
    </ion-list>
    
    <!-- Show required error if validation fails -->
    @if (formControl.invalid && formControl.touched) {
      <ion-text color="danger">
        {{ to.required ? (to.label + 'GENERAL.FORMLY.REQUIRED' | translate) : '' }}
      </ion-text>
    }
    `,
  standalone: false,
})
export class FormlyRadioTypeComponent extends FieldType { }
