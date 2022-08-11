import { NgModule } from "@angular/core";
import { FormControl, ValidationErrors } from "@angular/forms";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { SharedModule } from "src/app/shared/shared.module";
import { SettingsModule } from "../settings/settings.module";
import { InstallationViewComponent } from "./installation-view/installation-view.component";
import { InstallationComponent } from "./installation.component";
import { KeyMask } from "./keymask";
import { CompletionComponent } from "./views/completion/completion.component";
import { ConfigurationCommercialComponent } from "./views/configuration-commercial-component/configuration-commercial.component";
import { ConfigurationEmergencyReserveComponent } from "./views/configuration-emergency-reserve/configuration-emergency-reserve.component";
import { ConfigurationExecuteComponent } from "./views/configuration-execute/configuration-execute.component";
import { ConfigurationFeaturesStorageSystemComponent } from "./views/configuration-features-storage-system/configuration-features-storage-system.component";
import { ConfigurationLineSideMeterFuseComponent } from "./views/configuration-line-side-meter-fuse/configuration-line-side-meter-fuse.component";
import { ConfigurationPeakShavingComponent } from "./views/configuration-peak-shaving/configuration-peak-shaving.component";
import { ConfigurationSummaryComponent } from "./views/configuration-summary/configuration-summary.component";
import { ConfigurationSystemComponent } from "./views/configuration-system/configuration-system.component";
import { HeckertAppInstallerComponent } from "./views/heckert-app-installer/heckert-app-installer.component";
import { PreInstallationUpdateComponent } from "./views/pre-installation-update/pre-installation-update.component";
import { PreInstallationComponent } from "./views/pre-installation/pre-installation.component";
import { ProtocolAdditionalAcProducersComponent } from "./views/protocol-additional-ac-producers/protocol-additional-ac-producers.component";
import { ProtocolCustomerComponent } from "./views/protocol-customer/protocol-customer.component";
import { ProtocolFeedInLimitationComponent } from "./views/protocol-feed-in-limitation/protocol-feed-in-limitation.component";
import { ProtocolInstallerComponent } from "./views/protocol-installer/protocol-installer.component";
import { ProtocolPv } from "./views/protocol-pv/protocol-pv.component";
import { ProtocolSerialNumbersComponent } from "./views/protocol-serial-numbers/protocol-serial-numbers.component";
import { ProtocolSystemComponent } from "./views/protocol-system/protocol-system.component";

//#region Validators
export function EmailMatchValidator(control: FormControl): ValidationErrors {

  const { email, emailConfirm } = control.value;
  if (email === emailConfirm) {
    return null;
  }

  return { emailMatch: { message: 'E-Mails stimmen nicht überein.' } };
}

export function BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{16}$/.test(control.value) ? null : { "batteryInverterSerialNumber": true };
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

export function OnlyPositiveIntegerValidator(control: FormControl): ValidationErrors {
  return /^[0-9]+$/.test(control.value) ? null : { "onlyPositiveInteger": true }
}

export function CommercialBmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  return /^\d{10}$/.test(control.value) ? null : { "commercialBmsBoxSerialNumber": true };
}

export function CommercialBatteryModuleSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^\d{10}$/.test(control.value) ? null : { "commercialBatteryModuleSerialNumber": true };
}

export function Commercial30BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{10}$/.test(control.value) ? null : { "commercial30BatteryInverterSerialNumber": true };
}

export function Commercial50BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{6}$/.test(control.value) ? null : { "commercial50BatteryInverterSerialNumber": true };
}

//#region Validator Messages
export function OnlyPositiveIntegerValidatorMessage(err, field: FormlyFieldConfig) {
  return `Nur ganze positive Zahlen sind erlaubt.`
}

export function RequiredValidatorMessage(err, field: FormlyFieldConfig) {
  return "Dies ist ein Pflichtfeld.";
}

export function MinValidatorMessage(err, field: FormlyFieldConfig) {
  return `Nur Werte größer oder gleich ${field.templateOptions.min} sind erlaubt.`;
}

export function MaxValidatorMessage(err, field: FormlyFieldConfig) {
  return `Nur Werte kleiner oder gleich ${field.templateOptions.max} sind erlaubt.`;
}

export function EmailValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${field.formControl.value}" ist keine gültige E-Mail-Adresse.`;
}

export function BatteryInverterSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer.`;
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
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 12 Ziffern.`;
}

export function CommercialBmsBoxSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 14 Ziffern.`;
}

export function CommercialBatteryModuleSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 10 Ziffern.`;
}

export function Commercial30BatteryInverterSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer.`;
}

export function Commercial50BatteryInverterSerialNumberValidatorMessage(err, field: FormlyFieldConfig) {
  return `"${(field.templateOptions.prefix ?? "") + field.formControl.value}" ist keine gültige Seriennummer. Eine gültige Seriennummer besteht aus 6 Ziffern.`;
}

@NgModule({
  imports: [
    FormlyModule.forRoot({
      validators: [
        { name: "emailMatch", validation: EmailMatchValidator },
        { name: "batteryInverterSerialNumber", validation: BatteryInverterSerialNumberValidator },
        { name: "emsBoxSerialNumber", validation: EmsBoxSerialNumberValidator },
        { name: "boxSerialNumber", validation: BoxSerialNumberValidator },
        { name: "bmsBoxSerialNumber", validation: BmsBoxSerialNumberValidator },
        { name: "batterySerialNumber", validation: BatterySerialNumberValidator },
        { name: "onlyPositiveInteger", validation: OnlyPositiveIntegerValidator },
        { name: "commercialBmsBoxSerialNumber", validation: CommercialBmsBoxSerialNumberValidator },
        { name: "commercialBatteryModuleSerialNumber", validation: CommercialBatteryModuleSerialNumberValidator },
        { name: "commercial30BatteryInverterSerialNumber", validation: Commercial30BatteryInverterSerialNumberValidator },
        { name: "commercial50BatteryInverterSerialNumber", validation: Commercial50BatteryInverterSerialNumberValidator },
      ],
      validationMessages: [
        { name: "required", message: RequiredValidatorMessage },
        { name: "min", message: MinValidatorMessage },
        { name: "max", message: MaxValidatorMessage },
        { name: "email", message: EmailValidatorMessage },
        { name: "batteryInverterSerialNumber", message: BatteryInverterSerialNumberValidatorMessage },
        { name: "emsBoxSerialNumber", message: EmsBoxSerialNumberValidatorMessage },
        { name: "boxSerialNumber", message: BoxSerialNumberValidatorMessage },
        { name: "bmsBoxSerialNumber", message: BmsBoxSerialNumberValidatorMessage },
        { name: "batterySerialNumber", message: BatterySerialNumberValidatorMessage },
        { name: "onlyPositiveInteger", message: OnlyPositiveIntegerValidatorMessage },
        { name: "commercialBmsBoxSerialNumber", message: CommercialBmsBoxSerialNumberValidatorMessage },
        { name: "commercialBatteryModuleSerialNumber", message: CommercialBatteryModuleSerialNumberValidatorMessage },
        { name: "commercial30BatteryInverterSerialNumber", message: Commercial30BatteryInverterSerialNumberValidatorMessage },
        { name: "commercial50BatteryInverterSerialNumber", message: Commercial50BatteryInverterSerialNumberValidatorMessage }
      ]
    }),
    SharedModule,
    SettingsModule
  ],
  declarations: [
    CompletionComponent,
    ConfigurationEmergencyReserveComponent,
    ConfigurationExecuteComponent,
    ConfigurationLineSideMeterFuseComponent,
    KeyMask,
    ProtocolCustomerComponent,
    ProtocolFeedInLimitationComponent,
    ProtocolInstallerComponent,
    ProtocolSystemComponent,
    InstallationComponent,
    InstallationViewComponent,
    PreInstallationComponent,
    PreInstallationUpdateComponent,
    ConfigurationSystemComponent,
    ProtocolPv,
    ProtocolAdditionalAcProducersComponent,
    ConfigurationSummaryComponent,
    ProtocolSerialNumbersComponent,
    HeckertAppInstallerComponent,
    ConfigurationCommercialComponent,
    ConfigurationFeaturesStorageSystemComponent,
    ConfigurationPeakShavingComponent,
  ]
})
export class InstallationModule { }

// TODO rename to Setup or SetupAssistant to be in line with SetupProtocol on Backend side