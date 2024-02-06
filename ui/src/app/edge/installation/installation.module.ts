import { NgModule } from "@angular/core";
import { FormControl, ValidationErrors } from "@angular/forms";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { SharedModule } from "src/app/shared/shared.module";
import { SettingsModule } from "../settings/settings.module";
import { InstallationViewComponent } from "./installation-view/installation-view.component";
import { InstallationComponent } from "./installation.component";
import { KeyMaskDirective } from "./keymask";
import { CompletionComponent } from "./views/completion/completion.component";
import { ConfigurationCommercialComponent } from "./views/configuration-commercial-component/configuration-commercial.component";
import { ConfigurationCommercialModbuBridgeComponent } from "./views/configuration-commercial-modbusbridge/configuration-commercial-modbusbridge";
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
import { ProtocolPvComponent } from "./views/protocol-pv/protocol-pv.component";
import { ProtocolSerialNumbersComponent } from "./views/protocol-serial-numbers/protocol-serial-numbers.component";
import { ProtocolSystemComponent } from "./views/protocol-system/protocol-system.component";
import { ConfigurationMpptSelectionComponent } from "./views/configuration-mppt-selection/configuration-mppt-selection.component";

//#region Validators
export function EmailMatchValidator(control: FormControl): ValidationErrors {

  const { email, emailConfirm } = control.value;
  if (email === emailConfirm) {
    return null;
  }

  return { "emailMatch": true };
}

export function OnlyPositiveIntegerValidator(control: FormControl): ValidationErrors {
  return /^[0-9]+$/.test(control.value) ? null : { "onlyPositiveInteger": true };
}

export function BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{16}$/.test(control.value) ? null : { "batteryInverterSerialNumber": true };
}

export function EmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix, Applies for Home and Commercial but not Netztrenstelle.
  return /^F[CEH][FS]\d{9}$/.test(control.value) ? null : { "emsBoxSerialNumber": true };
}

export function EmsBoxNetztrennstelleSerialNumberValidator(control: FormControl): ValidationErrors {
  return /^\d{4}$/.test(control.value) ? null : { "emsBoxNetztrennstelleSerialNumber": true };
}

export function BoxSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^\d{9}$/.test(control.value) ? null : { "boxSerialNumber": true };
}

export function BatteryAndBmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^[a-zA-Z0-9]{24}$/.test(control.value) ? null : { "batteryAndBmsBoxSerialNumber": true };
}

export function CommercialBmsBoxSerialNumberValidator(control: FormControl): ValidationErrors {
  return /^\d{10}$/.test(control.value) ? null : { "commercialBmsBoxSerialNumber": true };
}

export function CommercialBatteryModuleSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator only checks the value after the prefix
  return /^\d{8}$/.test(control.value) ? null : { "commercialBatteryModuleSerialNumber": true };
}

export function Commercial30BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{10}$/.test(control.value) ? null : { "commercialBatteryInverterSerialNumber": true };
}

export function Commercial50BatteryInverterSerialNumberValidator(control: FormControl): ValidationErrors {
  // This validator checks the length of the value
  return /^.{6}$/.test(control.value) ? null : { "commercialBatteryInverterSerialNumber": true };
}

/**
 * This validator checks if the value entered is greater than minimum (default) value.
 * Workaround for default min prop in formly field.
 * User has trouble entering value in a field which is initialized with min prop. especially if a minimum value specified is more than one digit.
 *
 * @param control the form control containing the value that needs to be validated.
 * @param field the specific field of the form.
 * @returns sets the validation to true if the condition is met.
 */
export function DefaultAsMinValueValidator(control: FormControl, field: FormlyFieldConfig): ValidationErrors {
  return control.value >= field.defaultValue ? null : { "defaultAsMinimumValue": true };
}

@NgModule({
  imports: [
    FormlyModule.forRoot({
      validators: [
        { name: "emailMatch", validation: EmailMatchValidator },
        { name: "batteryInverterSerialNumber", validation: BatteryInverterSerialNumberValidator },
        { name: "emsBoxSerialNumber", validation: EmsBoxSerialNumberValidator },
        { name: "emsBoxNetztrennstelleSerialNumber", validation: EmsBoxNetztrennstelleSerialNumberValidator },
        { name: "boxSerialNumber", validation: BoxSerialNumberValidator },
        { name: "batteryAndBmsBoxSerialNumber", validation: BatteryAndBmsBoxSerialNumberValidator },
        { name: "onlyPositiveInteger", validation: OnlyPositiveIntegerValidator },
        { name: "commercialBmsBoxSerialNumber", validation: CommercialBmsBoxSerialNumberValidator },
        { name: "commercialBatteryModuleSerialNumber", validation: CommercialBatteryModuleSerialNumberValidator },
        { name: "commercial30BatteryInverterSerialNumber", validation: Commercial30BatteryInverterSerialNumberValidator },
        { name: "commercial50BatteryInverterSerialNumber", validation: Commercial50BatteryInverterSerialNumberValidator },
        { name: "defaultAsMinimumValue", validation: DefaultAsMinValueValidator },
      ],
    }),
    SharedModule,
    SettingsModule,
  ],
  declarations: [
    CompletionComponent,
    ConfigurationEmergencyReserveComponent,
    ConfigurationExecuteComponent,
    ConfigurationLineSideMeterFuseComponent,
    KeyMaskDirective,
    ProtocolCustomerComponent,
    ProtocolFeedInLimitationComponent,
    ProtocolInstallerComponent,
    ProtocolSystemComponent,
    InstallationComponent,
    InstallationViewComponent,
    PreInstallationComponent,
    PreInstallationUpdateComponent,
    ConfigurationSystemComponent,
    ProtocolPvComponent,
    ProtocolAdditionalAcProducersComponent,
    ConfigurationSummaryComponent,
    ProtocolSerialNumbersComponent,
    HeckertAppInstallerComponent,
    ConfigurationCommercialComponent,
    ConfigurationFeaturesStorageSystemComponent,
    ConfigurationPeakShavingComponent,
    ConfigurationCommercialModbuBridgeComponent,
    ConfigurationMpptSelectionComponent,
  ],
})
export class InstallationModule { }

// TODO rename to Setup or SetupAssistant to be in line with SetupProtocol on Backend side
