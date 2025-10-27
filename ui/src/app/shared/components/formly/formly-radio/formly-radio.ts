import { Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
  selector: "formly-radio",
  templateUrl: "./formly-radio.html",
  standalone: false,
  styles: [
    `
    :host {
      width: 100%;
    }
    `,
  ],
})
export class FormlyRadioTypeComponent extends FieldType { }
