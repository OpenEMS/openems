import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

/**  This wrapper is used to display a loading animation for a line until the async call is finished, the @input show is true, respectively.

* @input show: is used to determine when the async call has finished
* @input fields the formlyfields
* @input form the formGroup
* @input model the model
*/
@Component({
  selector: "oe-formly-skeleton-wrapper",
  template: `
<div>
  @if (!show) {
    <ion-list>
      @for (field of fields; track field) {
        <ion-item>
          <ion-skeleton-text [animated]="true" style="width: 100%"></ion-skeleton-text>
        </ion-item>
      }
    </ion-list>
  }
  @if (show) {
    <formly-form [form]="form" [fields]="fields" [model]="model"></formly-form>
  }
</div>
`,
  standalone: false,
})
export class FormlyFieldWithLoadingAnimationComponent {
  @Input() public show: boolean = false;
  @Input({ required: true }) public fields!: FormlyFieldConfig[];
  @Input({ required: true }) public form!: FormGroup;
  @Input({ required: true }) public model!: any;
}
