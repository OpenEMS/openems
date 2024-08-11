// @ts-strict-ignore
import { CommonModule } from '@angular/common';
import { Injector, NgModule } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormlyFieldConfig, FormlyModule } from '@ngx-formly/core';
import { FormlyIonicModule } from '@ngx-formly/ionic';
import { TranslateModule } from '@ngx-translate/core';
import { NgChartsModule } from 'ng2-charts';
import { NgxSpinnerModule } from "ngx-spinner";

import { appRoutingProviders } from '../app-routing.module';
import { ComponentsModule } from './components/components.module';
import { MeterModule } from './components/edge/meter/meter.module';
import { FormlyCheckBoxHyperlinkWrapperComponent } from './components/formly/form-field-checkbox-hyperlink/form-field-checkbox-hyperlink.wrapper';
import { FormlyWrapperDefaultValueWithCasesComponent } from './components/formly/form-field-default-cases.wrapper';
import { FormlyWrapperFormFieldComponent } from './components/formly/form-field.wrapper';
import { FormlyFieldCheckboxWithImageComponent } from './components/formly/formly-field-checkbox-image/formly-field-checkbox-with-image';
import { FormlyFieldModalComponent } from './components/formly/formly-field-modal/formlyfieldmodal';
import { FormlyFieldRadioWithImageComponent } from './components/formly/formly-field-radio-with-image/formly-field-radio-with-image';
import { FormlySelectFieldModalComponent } from './components/formly/formly-select-field-modal.component';
import { FormlySelectFieldExtendedWrapperComponent } from './components/formly/formly-select-field.extended';
import { FormlyFieldWithLoadingAnimationComponent } from './components/formly/formly-skeleton-wrapper';
import { InputTypeComponent } from './components/formly/input';
import { FormlyInputSerialNumberWrapperComponent as FormlyWrapperInputSerialNumber } from './components/formly/input-serial-number-wrapper';
import { PanelWrapperComponent } from './components/formly/panel-wrapper.component';
import { RepeatTypeComponent } from './components/formly/repeat';
import { HeaderComponent } from './components/header/header.component';
import { HistoryDataErrorModule } from './components/history-data-error/history-data-error.module';
import { PercentageBarComponent } from './components/percentagebar/percentagebar.component';
import { DirectiveModule } from './directive/directive';
import { ChartOptionsComponent } from './legacy/chartoptions/chartoptions.component';
import { PipeModule } from './pipe/pipe';
import { Logger } from './service/logger';
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './shared';

export function IpValidator(control: FormControl): ValidationErrors {
  return /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(control.value) ? null : { 'ip': true };
}

export function SubnetmaskValidator(control: FormControl): ValidationErrors {
  return /^(255)\.(0|128|192|224|240|248|252|254|255)\.(0|128|192|224|240|248|252|254|255)\.(0|128|192|224|240|248|252|254|255)/.test(control.value) ? null : { 'subnetmask': true };
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
    NgChartsModule,
    CommonModule,
    DirectiveModule,
    FormsModule,
    IonicModule,
    NgxSpinnerModule.forRoot({
      type: 'ball-clip-rotate-multiple',
    }),
    ReactiveFormsModule,
    RouterModule,
    FormlyModule.forRoot({
      wrappers: [
        { name: 'form-field', component: FormlyWrapperFormFieldComponent },
        { name: "input-serial-number", component: FormlyWrapperInputSerialNumber },
        { name: 'formly-select-extended-wrapper', component: FormlySelectFieldExtendedWrapperComponent },
        { name: 'formly-field-radio-with-image', component: FormlyFieldRadioWithImageComponent },
        { name: 'form-field-checkbox-hyperlink', component: FormlyCheckBoxHyperlinkWrapperComponent },
        { name: 'formly-wrapper-default-of-cases', component: FormlyWrapperDefaultValueWithCasesComponent },
        { name: 'panel', component: PanelWrapperComponent },
        { name: 'formly-field-modal', component: FormlyFieldModalComponent },
        { name: 'formly-field-checkbox-with-image', component: FormlyFieldCheckboxWithImageComponent },
      ],
      types: [
        { name: 'input', component: InputTypeComponent },
        { name: 'repeat', component: RepeatTypeComponent },
      ],
      validators: [
        { name: 'ip', validation: IpValidator },
        { name: 'subnetmask', validation: SubnetmaskValidator },
      ],
      validationMessages: [
        { name: 'ip', message: IpValidatorMessage },
        { name: 'subnetmask', message: SubnetmaskValidatorMessage },
      ],
    }),
    PipeModule,
    ComponentsModule,
    TranslateModule,
    HistoryDataErrorModule,
    MeterModule,
  ],
  declarations: [
    // components
    ChartOptionsComponent,
    HeaderComponent,
    PercentageBarComponent,
    // formly
    InputTypeComponent,
    FormlyWrapperFormFieldComponent,
    RepeatTypeComponent,
    FormlyWrapperInputSerialNumber,
    FormlySelectFieldExtendedWrapperComponent,
    FormlySelectFieldModalComponent,
    FormlyFieldRadioWithImageComponent,
    FormlyCheckBoxHyperlinkWrapperComponent,
    FormlyWrapperDefaultValueWithCasesComponent,
    FormlyFieldModalComponent,
    PanelWrapperComponent,
    FormlyFieldWithLoadingAnimationComponent,
    FormlyFieldCheckboxWithImageComponent,
  ],
  exports: [
    // modules
    BrowserAnimationsModule,
    NgChartsModule,
    CommonModule,
    DirectiveModule,
    FormlyIonicModule,
    FormlyModule,
    FormsModule,
    IonicModule,
    NgxSpinnerModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
    PipeModule,
    ComponentsModule,
    MeterModule,
    HistoryDataErrorModule,
    // components
    ChartOptionsComponent,
    HeaderComponent,
    PercentageBarComponent,
    FormlyFieldWithLoadingAnimationComponent,
  ],
  providers: [
    appRoutingProviders,
    Service,
    Utils,
    Websocket,
    Logger,
  ],
})

export class SharedModule {

  public static injector: Injector;

  constructor(private injector: Injector) {
    SharedModule.injector = injector;
  }
}
