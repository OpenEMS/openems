import { Component, ViewEncapsulation } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-custom-select",
  encapsulation: VIEW_ENCAPSULATION.NONE,
  template: `
    <ion-select
        [id]="id"
        [label]="PROPS.LABEL + (TO.REQUIRED ? '*' : '')"
        interface="alert"
        [interfaceOptions]="{ cssClass: 'custom-ion-alert' }"
        justify="space-between"
        [placeholder]="TO.PLACEHOLDER"
        [formControl]="formControl"
        [formlyAttributes]="field"
        [multiple]="PROPS.MULTIPLE ?? false"
    >
      <ng-container *ngFor="let option of PROPS.OPTIONS">
        <ion-select-option [value]="OPTION.VALUE">
          {{ OPTION.LABEL }}
        </ion-select-option>
      </ng-container>
    </ion-select>
    <p *ngIf="TO.DESCRIPTION" style="font-size: x-small;" class="ion-margin-bottom ion-text-secondary">{{ TO.DESCRIPTION }}</p>
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
