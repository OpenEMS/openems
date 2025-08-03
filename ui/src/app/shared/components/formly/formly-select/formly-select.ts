import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-custom-select",
  encapsulation: ViewEncapsulation.None,
  template: `
    <ion-select
      [id]="id"
      [label]="props.label + (to.required ? '*' : '')"
      interface="alert"
      [interfaceOptions]="{ cssClass: 'custom-ion-alert' }"
      justify="space-between"
      [placeholder]="to.placeholder"
      [formControl]="formControl"
      [formlyAttributes]="field"
      [multiple]="props.multiple ?? false"
      >
      @for (option of props.options; track option) {
        <ion-select-option [value]="option.value">
          {{ option.label }}
        </ion-select-option>
      }
    </ion-select>
    @if (to.description) {
      <p style="font-size: x-small;" class="ion-margin-bottom ion-text-secondary">{{ to.description }}</p>
    }
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
        ion-select::part(label) {
          max-width: 100% !important;
          white-space: normal !important;
        }

        ion-select::part(text) {
          flex: 1;
        }
      `],
})
export class FormlySelectComponent extends FieldType { }
