// @ts-strict-ignore
import { NgModule } from "@angular/core";
import { FormControl, ValidationErrors } from "@angular/forms";
import { FORMLY_CONFIG, FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { FormlyLinkComponent } from "./formly/formly-link";
import { FormlyTextComponent } from "./formly/formly-text";
import { FormlyInputWithUnitComponent } from "./formly/input-with-unit";
import { FormlyOptionGroupPickerComponent } from "./formly/option-group-picker/formly-option-group-picker.component";
import { FormlyReorderArrayComponent } from "./formly/reorder-select/formly-reorder-array.component";
import { FormlySafeInputModalComponent } from "./formly/safe-input/formly-safe-input-modal.component";
import { FormlySafeInputWrapperComponent } from "./formly/safe-input/formly-safe-input.extended";
import { KeyModalComponent } from "./keypopup/modal.component";

export function KeyValidator(control: FormControl): ValidationErrors {
    return /^(.{4}-){3}.{4}$/.test(control.value) ? null : { "key": true };
}

export function registerTranslateExtension(translate: TranslateService) {
    return {
        validationMessages: [
            {
                name: "key",
                message() {
                    return translate.stream("EDGE.CONFIG.APP.KEY.INVALID_PATTERN");
                },
            },
        ],
    };
}

@NgModule({
    imports: [
        SharedModule,
        FormlyModule.forRoot({
            wrappers: [
                { name: "formly-safe-input-wrapper", component: FormlySafeInputWrapperComponent },
                { name: "input-with-unit", component: FormlyInputWithUnitComponent },
            ],
            types: [
                { name: "text", component: FormlyTextComponent },
                { name: "link", component: FormlyLinkComponent },
                { name: "formly-option-group-picker", component: FormlyOptionGroupPickerComponent },
                { name: "reorder-array", component: FormlyReorderArrayComponent },
            ],
            validators: [
                { name: "key", validation: KeyValidator },
            ],
            validationMessages: [
                { name: "key", message: "The key doesnt match the pattern!" },
            ],
        }),
    ],
    declarations: [
        KeyModalComponent,
        FormlySafeInputModalComponent,
        FormlySafeInputWrapperComponent,
        FormlyLinkComponent,
        FormlyTextComponent,
        FormlyInputWithUnitComponent,
        FormlyOptionGroupPickerComponent,
        FormlyReorderArrayComponent,
    ],
    exports: [
    ],
    providers: [
    // Use factory for formly. This allows us to use translations in validationMessages.
        { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
    ],
})
export class AppModule { }
