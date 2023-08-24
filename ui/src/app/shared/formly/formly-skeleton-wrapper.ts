import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';


/// to use the skeleton, simply pass the form data to this component selectoro with a boolean indicator to show information.
@Component({

  selector: 'formly-skeleton-wrapper',
  template: `
<div>

<ion-list *ngIf="!show">
  <ion-item *ngFor="let field of fields">
    <ion-skeleton-text [animated]="true" style="width: 100%"></ion-skeleton-text>
  </ion-item>
  </ion-list>
  <formly-form *ngIf="show" [form]="form" [fields]="fields" [model]="model" style="color: primary" color="primary"></formly-form>
</div>
  `
})
export class SkeletonWrapperComponent {
  @Input() public show: boolean;
  @Input() public fields: FormlyFieldConfig[];
  @Input() public form: FormGroup;
  @Input() public model: any;
}