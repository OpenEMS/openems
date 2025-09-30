// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-default-of-cases",
    template: "<ng-container #fieldComponent ></ng-container>",
    standalone: false,
})
export class FormlyWrapperDefaultValueWithCasesComponent extends FieldWrapper implements OnInit {

    private casesToSubscribe: FieldDefaultCases[] = [];

    public ngOnInit() {
        THIS.GET_OPTIONS().forEach((item: FieldDefaultCases) => {
            THIS.FORM.VALUE_CHANGES.SUBSCRIBE((value) => {
                const indicesToRemove = [];
                const casesToSub = THIS.CASES_TO_SUBSCRIBE;
                THIS.CASES_TO_SUBSCRIBE = [];
                CASES_TO_SUB.FOR_EACH((defaultCase, i) => {
                    const control = THIS.FORM.GET(DEFAULT_CASE.FIELD);
                    if (control) {
                        THIS.SUBSCRIBE(item, control);
                        INDICES_TO_REMOVE.PUSH(i);
                    }
                });
                CASES_TO_SUB.FOR_EACH((a, i) => {
                    if (INDICES_TO_REMOVE.SOME(c => c === i)) {
                        return;
                    }
                    THIS.CASES_TO_SUBSCRIBE.PUSH(a);
                });
            });

            const control = THIS.FORM.GET(ITEM.FIELD);
            if (control) {
                THIS.SUBSCRIBE(item, control);
            } else {
                THIS.CASES_TO_SUBSCRIBE.PUSH(item);
            }

            // if value is already set keep current value
            if (THIS.FORM_CONTROL.VALUE) {
                return;
            }
            const value = THIS.MODEL[ITEM.FIELD];
            if (!value) {
                return;
            }
            if (THIS.ON_CHANGE(item, value)) {
                return;
            }
        });
    }

    private subscribe(item: FieldDefaultCases, control: AbstractControl) {
        CONTROL.VALUE_CHANGES.SUBSCRIBE((value) => {
            if (THIS.ON_CHANGE(item, value)) {
                return;
            }
            // search for first other case
            const options = THIS.GET_OPTIONS();
            for (const option of options) {
                const valueOfField = THIS.FORM.GET_RAW_VALUE()[OPTION.FIELD];
                if (!valueOfField) {
                    continue;
                }
                for (const optionCase of OPTION.CASES) {
                    if (OPTION_CASE.CASE == valueOfField) {
                        THIS.FORM_CONTROL.SET_VALUE(OPTION_CASE.DEFAULT_VALUE);
                        return;
                    }
                }
            }
        });
        THIS.ON_CHANGE(item, THIS.FORM.GET_RAW_VALUE()[ITEM.FIELD]);
    }

    private getOptions(): FieldDefaultCases[] {
        return THIS.PROPS.DEFAULT_VALUE_OPTIONS ?? [];
    }

    private onChange(item: FieldDefaultCases, value: any): boolean {
        const foundCase = ITEM.CASES.FIND(element => ELEMENT.CASE == value);
        if (!foundCase) {
            return false;
        }
        if (FOUND_CASE.DEFAULT_VALUE === value) {
            return true;
        }
        THIS.FORM_CONTROL.SET_VALUE(FOUND_CASE.DEFAULT_VALUE);
        return true;
    }

}

type FieldDefaultCases = { field: string, cases: [{ case: any, defaultValue: any }] };
