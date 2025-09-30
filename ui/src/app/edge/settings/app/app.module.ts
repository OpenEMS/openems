// @ts-strict-ignore
import { NgModule } from "@angular/core";
import { FormControl, ValidationErrors } from "@angular/forms";
import { FORMLY_CONFIG, FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { FormlyTextComponent } from "./formly/formly-text";
import { FormlyInputWithUnitComponent } from "./formly/input-with-unit";
import { FormlyOptionGroupPickerComponent } from "./formly/option-group-picker/formly-option-group-PICKER.COMPONENT";
import { FormlyReorderArrayComponent } from "./formly/reorder-select/formly-reorder-ARRAY.COMPONENT";
import { FormlySafeInputModalComponent } from "./formly/safe-input/formly-safe-input-MODAL.COMPONENT";
import { FormlySafeInputWrapperComponent } from "./formly/safe-input/formly-safe-INPUT.EXTENDED";
import { IndexComponent } from "./INDEX.COMPONENT";
import { InstallAppComponent } from "./INSTALL.COMPONENT";
import { KeyModalComponent } from "./keypopup/MODAL.COMPONENT";
import { SingleAppComponent } from "./SINGLE.COMPONENT";
import { UpdateAppComponent } from "./UPDATE.COMPONENT";

export function KeyValidator(control: FormControl): ValidationErrors {
  return /^(.{4}-){3}.{4}$/.test(CONTROL.VALUE) ? null : { "key": true };
}

export function registerTranslateExtension(translate: TranslateService) {
  return {
    validationMessages: [
      {
        name: "key",
        message() {
          return TRANSLATE.STREAM("EDGE.CONFIG.APP.KEY.INVALID_PATTERN");
        },
      },
    ],
  };
}

@NgModule({
  imports: [
    SharedModule,
    FORMLY_MODULE.FOR_ROOT({
      wrappers: [
        { name: "formly-safe-input-wrapper", component: FormlySafeInputWrapperComponent },
        { name: "input-with-unit", component: FormlyInputWithUnitComponent },
      ],
      types: [
        { name: "text", component: FormlyTextComponent },
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
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
    KeyModalComponent,
    FormlySafeInputModalComponent,
    FormlySafeInputWrapperComponent,
    FormlyTextComponent,
    FormlyInputWithUnitComponent,
    FormlyOptionGroupPickerComponent,
    FormlyReorderArrayComponent,
  ],
  exports: [
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
  ],
  providers: [
    // Use factory for formly. This allows us to use translations in validationMessages.
    { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
  ],
})
export class AppModule { }
