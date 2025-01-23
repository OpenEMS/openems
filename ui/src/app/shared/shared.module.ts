// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Injector, NgModule } from "@angular/core";
import { FormControl, FormsModule, ReactiveFormsModule, ValidationErrors } from "@angular/forms";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { FormlyIonicModule } from "@ngx-formly/ionic";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { appRoutingProviders } from "../app-routing.module";
import { ComponentsModule } from "./components/components.module";
import { MeterModule } from "./components/edge/meter/meter.module";
import { FormlyCheckBoxHyperlinkWrapperComponent } from "./components/formly/form-field-checkbox-hyperlink/form-field-checkbox-hyperlink.wrapper";
import { FormlyWrapperDefaultValueWithCasesComponent } from "./components/formly/form-field-default-cases.wrapper";
import { FormlyFieldMultiStepComponent } from "./components/formly/form-field-multi-step/form-field-multi-step";
import { FormlyWrapperFormFieldComponent } from "./components/formly/form-field.wrapper";
import { FormlyFieldCheckboxWithImageComponent } from "./components/formly/formly-field-checkbox-image/formly-field-checkbox-with-image";
import { FormlyFieldModalComponent } from "./components/formly/formly-field-modal/formlyfieldmodal";
import { FormlyFieldRadioWithImageComponent } from "./components/formly/formly-field-radio-with-image/formly-field-radio-with-image";
import { FormlySelectComponent } from "./components/formly/formly-select/formly-select";
import { FormlySelectFieldModalComponent } from "./components/formly/formly-select-field-modal.component";
import { FormlySelectFieldExtendedWrapperComponent } from "./components/formly/formly-select-field.extended";
import { FormlyFieldWithLoadingAnimationComponent } from "./components/formly/formly-skeleton-wrapper";
import { InputTypeComponent } from "./components/formly/input";
import { FormlyInputSerialNumberWrapperComponent as FormlyWrapperInputSerialNumber } from "./components/formly/input-serial-number-wrapper";
import { PanelWrapperComponent } from "./components/formly/panel-wrapper.component";
import { RepeatTypeComponent } from "./components/formly/repeat";
import { AppHeaderComponent } from "./components/header/app-header";
import { HeaderComponent } from "./components/header/header.component";
import { HistoryDataErrorModule } from "./components/history-data-error/history-data-error.module";
import { PercentageBarComponent } from "./components/percentagebar/percentagebar.component";
import { DirectiveModule } from "./directive/directive";
import { ChartOptionsComponent } from "./legacy/chartoptions/chartoptions.component";
import { AppStateTracker } from "./ngrx-store/states";
import { PipeModule } from "./pipe/pipe";
import { Logger } from "./service/logger";
import { PreviousRouteService } from "./service/previousRouteService";
import { Service } from "./service/service";
import { Utils } from "./service/utils";
import { Websocket } from "./shared";

export function IpValidator(control: FormControl): ValidationErrors {
  return /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(control.value) ? null : { "ip": true };
}

export function SubnetmaskValidator(control: FormControl): ValidationErrors {
  return /^(255)\.(0|128|192|224|240|248|252|254|255)\.(0|128|192|224|240|248|252|254|255)\.(0|128|192|224|240|248|252|254|255)/.test(control.value) ? null : { "subnetmask": true };
}

export function IpValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${field.formControl.value}" is not a valid IP Address`;
}

export function SubnetmaskValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${field.formControl.value}" is not a valid Subnetmask`;
}

@NgModule({
  imports: [
    BrowserAnimationsModule,
    CommonModule,
    ComponentsModule,
    DirectiveModule,
    FormlyModule.forRoot({
      wrappers: [
        { name: "form-field", component: FormlyWrapperFormFieldComponent },
        { name: "input-serial-number", component: FormlyWrapperInputSerialNumber },
        { name: "formly-select-extended-wrapper", component: FormlySelectFieldExtendedWrapperComponent },
        { name: "formly-field-radio-with-image", component: FormlyFieldRadioWithImageComponent },
        { name: "form-field-checkbox-hyperlink", component: FormlyCheckBoxHyperlinkWrapperComponent },
        { name: "formly-wrapper-default-of-cases", component: FormlyWrapperDefaultValueWithCasesComponent },
        { name: "panel", component: PanelWrapperComponent },
        { name: "formly-field-modal", component: FormlyFieldModalComponent },
        { name: "formly-field-checkbox-with-image", component: FormlyFieldCheckboxWithImageComponent },
      ],
      types: [
        { name: "input", component: InputTypeComponent },
        { name: "repeat", component: RepeatTypeComponent },
        { name: "multi-step", component: FormlyFieldMultiStepComponent },
        { name: "select", component: FormlySelectComponent },
      ],
      validators: [
        { name: "ip", validation: IpValidator },
        { name: "subnetmask", validation: SubnetmaskValidator },
      ],
      validationMessages: [
        { name: "ip", message: IpValidatorMessage },
        { name: "subnetmask", message: SubnetmaskValidatorMessage },
      ],
    }),
    FormsModule,
    HistoryDataErrorModule,
    IonicModule,
    MeterModule,
    BaseChartDirective,
    NgxSpinnerModule.forRoot({
      type: "ball-clip-rotate-multiple",
    }),
    PipeModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
  ],
  declarations: [
    AppHeaderComponent,
    ChartOptionsComponent,
    FormlyCheckBoxHyperlinkWrapperComponent,
    FormlyFieldCheckboxWithImageComponent,
    FormlyFieldModalComponent,
    FormlyFieldMultiStepComponent,
    FormlyFieldRadioWithImageComponent,
    FormlyFieldWithLoadingAnimationComponent,
    FormlySelectFieldExtendedWrapperComponent,
    FormlySelectFieldModalComponent,
    FormlyWrapperDefaultValueWithCasesComponent,
    FormlyWrapperFormFieldComponent,
    FormlyWrapperInputSerialNumber,
    HeaderComponent,
    InputTypeComponent,
    PanelWrapperComponent,
    PercentageBarComponent,
    RepeatTypeComponent,
    FormlySelectComponent,
  ],
  exports: [
    AppHeaderComponent,
    BrowserAnimationsModule,
    ChartOptionsComponent,
    CommonModule,
    ComponentsModule,
    DirectiveModule,
    FormlyFieldWithLoadingAnimationComponent,
    FormlyIonicModule,
    FormlyModule,
    FormsModule,
    HeaderComponent,
    HistoryDataErrorModule,
    IonicModule,
    MeterModule,
    BaseChartDirective,
    NgxSpinnerModule,
    PercentageBarComponent,
    PipeModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
  ],
  providers: [
    AppStateTracker,
    appRoutingProviders,
    Logger,
    PreviousRouteService,
    Service,
    Utils,
    Websocket,
  ],
})

export class SharedModule {

  public static injector: Injector;

  constructor(private injector: Injector) {
    SharedModule.injector = injector;
  }
}
