import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

/**  This wrapper is used to display a loading animation for a line until the async call is finished, the @input show is true, respectively.

* @input show: is used to determine when the async call has finished
* @input fields the formlyfields
* @input form the formGroup
* @input model the model
*/
@Component({
  selector: 'formly-skeleton-wrapper',
  template: `
<div>
  <ion-list *ngIf="!show">
    <ion-item *ngFor="let field of fields">
      <ion-skeleton-text [animated]="true" style="width: 100%"></ion-skeleton-text>
    </ion-item>
  </ion-list>
  <formly-form *ngIf="show" [form]="form" [fields]="fields" [model]="model"></formly-form>
</div>
  `
})
export class FormlyFieldWithLoadingAnimationComponent {
  @Input() public show: boolean = false;
  @Input() public fields: FormlyFieldConfig[];
  @Input() public form: FormGroup;
  @Input() public model: any;
}