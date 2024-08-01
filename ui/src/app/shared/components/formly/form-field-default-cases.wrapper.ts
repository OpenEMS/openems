// @ts-strict-ignore
import { Component, OnInit } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-wrapper-default-of-cases',
    template: `<ng-container #fieldComponent ></ng-container>`,
})
export class FormlyWrapperDefaultValueWithCasesComponent extends FieldWrapper implements OnInit {

    private casesToSubscribe: FieldDefaultCases[] = [];

    public ngOnInit() {
        this.getOptions().forEach((item: FieldDefaultCases) => {
            this.form.valueChanges.subscribe((value) => {
                const indicesToRemove = [];
                const casesToSub = this.casesToSubscribe;
                this.casesToSubscribe = [];
                casesToSub.forEach((defaultCase, i) => {
                    const control = this.form.get(defaultCase.field);
                    if (control) {
                        this.subscribe(item, control);
                        indicesToRemove.push(i);
                    }
                });
                casesToSub.forEach((a, i) => {
                    if (indicesToRemove.some(c => c === i)) {
                        return;
                    }
                    this.casesToSubscribe.push(a);
                });
            });

            const control = this.form.get(item.field);
            if (control) {
                this.subscribe(item, control);
            } else {
                this.casesToSubscribe.push(item);
            }

            // if value is already set keep current value
            if (this.formControl.value) {
                return;
            }
            const value = this.model[item.field];
            if (!value) {
                return;
            }
            if (this.onChange(item, value)) {
                return;
            }
        });
    }

    private subscribe(item: FieldDefaultCases, control: AbstractControl) {
        control.valueChanges.subscribe((value) => {
            if (this.onChange(item, value)) {
                return;
            }
            // search for first other case
            const options = this.getOptions();
            for (const option of options) {
                const valueOfField = this.form.getRawValue()[option.field];
                if (!valueOfField) {
                    continue;
                }
                for (const optionCase of option.cases) {
                    if (optionCase.case == valueOfField) {
                        this.formControl.setValue(optionCase.defaultValue);
                        return;
                    }
                }
            }
        });
        this.onChange(item, this.form.getRawValue()[item.field]);
    }

    private getOptions(): FieldDefaultCases[] {
        return this.props.defaultValueOptions ?? [];
    }

    private onChange(item: FieldDefaultCases, value: any): boolean {
        const foundCase = item.cases.find(element => element.case == value);
        if (!foundCase) {
            return false;
        }
        if (foundCase.defaultValue === value) {
            return true;
        }
        this.formControl.setValue(foundCase.defaultValue);
        return true;
    }

}

type FieldDefaultCases = { field: string, cases: [{ case: any, defaultValue: any }] };
