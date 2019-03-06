import { Component } from '@angular/core';
import { FieldArrayType } from '@ngx-formly/core';

@Component({
    selector: 'formly-repeat-section',
    templateUrl: './repeat.html'
})
export class RepeatTypeComponent extends FieldArrayType { }