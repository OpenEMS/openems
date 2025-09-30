import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-range-type",
  template: `
    <p *ngIf="PROPS.NOTE" class="ion-padding-top"> {{ PROPS.NOTE }} </p>
    <ion-range
      class="ion-padding-top"
      [min]="PROPS.MIN"
      [max]="PROPS.MAX"
      [step]="PROPS.STEP || 1"
      [pin]="PROPS.PIN ?? PROPS.ATTRIBUTES?.pin"
      [snaps]="PROPS.SNAPS || false"
      [formControl]="formControl"
      [formlyAttributes]="field"
      [pinFormatter]="boundPinFormatter"
      (ionChange)="onChange($event)"
      >

      <ion-label slot="start">{{ PROPS.MIN }}</ion-label>
      <ion-label slot="end">{{ PROPS.MAX }}</ion-label>
    </ion-range>

    <p *ngIf="TO.DESCRIPTION" class="description-text">
      {{ TO.DESCRIPTION }}
    </p>

    <!-- Validation errors -->
    <ion-text color="danger" *ngIf="showError">
      <p *ngIf="FORM_CONTROL.ERRORS?.required">
        {{ PROPS.REQUIRED ? (PROPS.LABEL + ' ' + ('GENERAL.FORMLY.REQUIRED' | translate)) : '' }}
      </p>
    </ion-text>
  `,
  standalone: false,
})
export class FormlyRangeTypeComponent extends FieldType {
  protected boundPinFormatter = THIS.PIN_FORMATTER.BIND(this);

  public onChange(event: any): void {
    if (THIS.PROPS.CHANGE) {
      THIS.PROPS.CHANGE(THIS.FIELD);
    }
  }

  protected pinFormatter(value: number): string {
    const unit = THIS.PROPS?.unit || "";
    return `${value}${unit}`;
  }
}
