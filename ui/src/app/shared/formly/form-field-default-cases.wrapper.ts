import { Component, OnInit } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-wrapper-default-of-cases',
    template: `<ng-container #fieldComponent ></ng-container>`
})
export class FormlyWrapperDefaultValueWithCasesComponent extends FieldWrapper implements OnInit {

    ngOnInit() {
        this.props.defaultValueOptions?.forEach((item: FieldDefaultCases) => {
            this.form.get(item["field"]).valueChanges.subscribe((value) => {
                this.onChange(item, value);
            });
            let value = this.model[item.field];
            if (value) {
                this.onChange(item, value);
            }
        });
    }

    private onChange(item: FieldDefaultCases, value: any) {
        const foundCase = item.cases.find(element => element.case == value);
        if (!foundCase) {
            return;
        }
        this.formControl.setValue(foundCase.defaultValue);
    }

}

type FieldDefaultCases = { field: string, cases: [{ case: any, defaultValue: any }] }