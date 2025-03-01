import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-custom-select",
  encapsulation: ViewEncapsulation.None,
  template: `
    <ion-select
        [id]="id"
        [label]="props.label"
        interface="alert"
        [interfaceOptions]="{ cssClass: 'custom-ion-alert' }"
        labelPlacement="start"
        justify="space-between"
        [placeholder]="to.placeholder"
        [formControl]="formControl"
        [formlyAttributes]="field"
        [multiple]="props.multiple ?? false"
      >
                  <ng-container *ngFor="let option of props.options">
                  <ion-select-option [value]="option.value">
                  {{ option.label }}
                  </ion-select-option>
                  </ng-container>
      </ion-select>
  `,
  standalone: false,
  styles: [`
      :host {
        formly-custom-select {
                width: 100%;
              }
          }
        .custom-ion-alert{
            .alert-checkbox-label{
              color: var(--ion-text-color) !important;
            }
        }
      `],
})
export class FormlySelectComponent extends FieldType { }
