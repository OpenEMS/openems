import { NgModule } from "@angular/core";
import { FormControl, ValidationErrors } from "@angular/forms";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { SharedModule } from "src/app/shared/shared.module";
import { InstallationViewComponent } from "./installation-view/installation-view.component";
import { InstallationComponent } from "./installation.component";
import { KeyMask } from "./keymask";
import { CompletionComponent } from "./views/completion/completion.component";
import { ConfigurationEmergencyReserveComponent } from "./views/configuration-emergency-reserve/configuration-emergency-reserve.component";
import { ConfigurationExecuteComponent } from "./views/configuration-execute/configuration-execute.component";
import { ConfigurationLineSideMeterFuseComponent } from "./views/configuration-line-side-meter-fuse/configuration-line-side-meter-fuse.component";
import { ConfigurationSummaryComponent } from "./views/configuration-summary/configuration-summary.component";
import { ConfigurationSystemComponent } from "./views/configuration-system/configuration-system.component";
import { PreInstallationComponent } from "./views/pre-installation/pre-installation.component";
import { ProtocolAdditionalAcProducersComponent } from "./views/protocol-additional-ac-producers/protocol-additional-ac-producers.component";
import { ProtocolCustomerComponent } from "./views/protocol-customer/protocol-customer.component";
import { ProtocolDynamicFeedInLimitation } from "./views/protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component";
import { ProtocolInstallerComponent } from "./views/protocol-installer/protocol-installer.component";
import { ProtocolPv } from "./views/protocol-pv/protocol-pv.component";
import { ProtocolSerialNumbersComponent } from "./views/protocol-serial-numbers/protocol-serial-numbers.component";
import { ProtocolSystemComponent } from "./views/protocol-system/protocol-system.component";

//#region Validators

export function ZipValidator(control: FormControl): ValidationErrors {
  return /^\d{5}$/.test(control.value) ? null : { "zip": true };
}

export function EmailMatchValidator(control: FormControl): ValidationErrors {

  const { email, emailConfirm } = control.value;

  if (email === emailConfirm) {
    return null;
  }

  return { emailMatch: { message: 'E-Mails stimmen nicht überein.' } };
}

export function EmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^[FS]\d{9}$/.test(control.value) ? null : { "emsBoxSerialNumber": true };
}

export function BoxSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^\d{9}$/.test(control.value) ? null : { "boxSerialNumber": true };
}

export function BmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  return /^\d{24}$/.test(control.value) ? null : { "bmsBoxSerialNumber": true };
}

export function BatterySerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^\d{12}$/.test(control.value) ? null : { "batterySerialNumber": true };
}

//#endregion

//#region Validator Messages

export function RequiredValidatorMessage(err, field: FormlyFieldConfig) {
  return "Dies ist ein Pflichtfeld.";
}

export function MinValidatorMessage(err, field: FormlyFieldConfig) {
  return `Nur Werte größer oder gleich ${field.templateOptions.min} sind erlaubt.`;
}

export function MaxValidatorMessage(err, field: FormlyFieldConfig) {
  return `Nur Werte kleiner oder gleich ${field.templateOptions.max} sind erlaubt.`;
}

export function ZipValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${field.formControl.value}" ist keine gültige Postleitzahl.`;
}

export function EmailValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${field.formControl.value}" ist keine gültige E-Mail-Adresse.`;
}

export function EmsBoxSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer.`;
}

export function BoxSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer.`;
}

export function BmsBoxSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 24 Ziffern.`;
}

export function BatterySerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 24 Ziffern.`;
}

//#endregion

@NgModule({
  imports: [
    FormlyModule.forRoot({
      validators: [
        { name: "zip", validation: ZipValidator },
        { name: "emailMatch", validation: EmailMatchValidator },
        { name: "emsBoxSerialNumber", validation: EmsBoxSerialNumberValidator },
        { name: "boxSerialNumber", validation: BoxSerialNumberValidator },
        { name: "bmsBoxSerialNumber", validation: BmsBoxSerialNumberValidator },
        { name: "batterySerialNumber", validation: BatterySerialNumberValidator },
      ],
      validationMessages: [
        { name: "required", message: RequiredValidatorMessage },
        { name: "min", message: MinValidatorMessage },
        { name: "max", message: MaxValidatorMessage },
        { name: "zip", message: ZipValidatorMessage },
        { name: "email", message: EmailValidatorMessage },
        { name: "emsBoxSerialNumber", message: EmsBoxSerialNumberValidatorMessage },
        { name: "boxSerialNumber", message: BoxSerialNumberValidatorMessage },
        { name: "bmsBoxSerialNumber", message: BmsBoxSerialNumberValidatorMessage },
        { name: "batterySerialNumber", message: BatterySerialNumberValidatorMessage }
      ]
    }),
    SharedModule
  ],
  declarations: [
    CompletionComponent,
    ConfigurationEmergencyReserveComponent,
    ConfigurationExecuteComponent,
    ConfigurationLineSideMeterFuseComponent,
    KeyMask,
    ProtocolCustomerComponent,
    ProtocolDynamicFeedInLimitation,
    ProtocolInstallerComponent,
    ProtocolSystemComponent,
    InstallationComponent,
    InstallationViewComponent,
    PreInstallationComponent,
    ConfigurationSystemComponent,
    ProtocolPv,
    ProtocolAdditionalAcProducersComponent,
    ConfigurationSummaryComponent,
    ProtocolSerialNumbersComponent,
  ]
})
export class InstallationModule { }

// TODO rename to Setup or SetupAssistant to be in line with SetupProtocol on Backend side
