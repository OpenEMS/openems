import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-radio",
  template: `
    <ion-list [ngClass]="FIELD.CLASS_NAME">
      <!-- Show label if provided -->
      <ion-label *ngIf="TO.LABEL">{{ TO.LABEL }}</ion-label>

      <!-- Show description if provided -->
      <p *ngIf="TO.DESCRIPTION" style="font-size: x-small;" class="ion-margin-bottom ion-text-secondary">{{ TO.DESCRIPTION }}</p>

      <ion-radio-group [formControl]="formControl" [formlyAttributes]="field">
        <ion-item *ngFor="let option of TO.OPTIONS">
          <ion-label>{{ OPTION.LABEL }}</ion-label>
          <ion-radio
            [value]="OPTION.VALUE"
            [slot]="TO.RADIO_SLOT || 'end'">
          </ion-radio>
        </ion-item>
      </ion-radio-group>
    </ion-list>

    <!-- Show required error if validation fails -->
    <ion-text color="danger" *ngIf="FORM_CONTROL.INVALID && FORM_CONTROL.TOUCHED">
      {{ TO.REQUIRED ? (TO.LABEL + 'GENERAL.FORMLY.REQUIRED' | translate) : '' }}
    </ion-text>
  `,
  standalone: false,
})
export class FormlyRadioTypeComponent extends FieldType { }
