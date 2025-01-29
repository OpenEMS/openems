import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-custom-select",
  template: `
    <ion-select
        [id]="id"
        [label]="props.label"
        interface="alert"
        labelPlacement="start"
        justify="space-between"
        [placeholder]="to.placeholder"
        [formControl]="formControl"
        [formlyAttributes]="field"
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
      `],
})
export class FormlySelectComponent extends FieldType { }
