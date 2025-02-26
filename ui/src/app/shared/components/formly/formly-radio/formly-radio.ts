import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-radio",
  template: `
    <ion-list [ngClass]="field.className">
      <!-- Show label if provided -->
      <ion-label *ngIf="to.label">{{ to.label }}</ion-label>

      <!-- Show description if provided -->
      <p *ngIf="to.description" style="font-size: x-small;" class="ion-margin-bottom ion-text-secondary">{{ to.description }}</p>

      <ion-radio-group [formControl]="formControl" [formlyAttributes]="field">
        <ion-item *ngFor="let option of to.options">
          <ion-label>{{ option.label }}</ion-label>
          <ion-radio
            [value]="option.value"
            [slot]="to.radioSlot || 'end'">
          </ion-radio>
        </ion-item>
      </ion-radio-group>
    </ion-list>

    <!-- Show required error if validation fails -->
    <ion-text color="danger" *ngIf="formControl.invalid && formControl.touched">
      {{ to.required ? (to.label + 'GENERAL.FORMLY.REQUIRED' | translate) : '' }}
    </ion-text>
  `,
  standalone: false,
})
export class FormlyRadioTypeComponent extends FieldType { }
