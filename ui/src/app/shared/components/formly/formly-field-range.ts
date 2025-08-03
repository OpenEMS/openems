import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-range-type",
  template: `
    @if (props.note) {
      <p class="ion-padding-top"> {{ props.note }} </p>
    }
    <ion-range
      class="ion-padding-top"
      [min]="props.min"
      [max]="props.max"
      [step]="props.step || 1"
      [pin]="props.pin ?? props.attributes?.pin"
      [snaps]="props.snaps || false"
      [formControl]="formControl"
      [formlyAttributes]="field"
      [pinFormatter]="boundPinFormatter"
      (ionChange)="onChange($event)"
      >
    
      <ion-label slot="start">{{ props.min }}</ion-label>
      <ion-label slot="end">{{ props.max }}</ion-label>
    </ion-range>
    
    @if (to.description) {
      <p class="description-text">
        {{ to.description }}
      </p>
    }
    
    <!-- Validation errors -->
    @if (showError) {
      <ion-text color="danger">
        @if (formControl.errors?.required) {
          <p>
            {{ props.required ? (props.label + ' ' + ('GENERAL.FORMLY.REQUIRED' | translate)) : '' }}
          </p>
        }
      </ion-text>
    }
    `,
  standalone: false,
})
export class FormlyRangeTypeComponent extends FieldType {
  protected boundPinFormatter = this.pinFormatter.bind(this);

  public onChange(event: any): void {
    if (this.props.change) {
      this.props.change(this.field);
    }
  }

  protected pinFormatter(value: number): string {
    const unit = this.props?.unit || "";
    return `${value}${unit}`;
  }
}
