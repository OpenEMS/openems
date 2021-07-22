import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";

import { CompletionComponent } from "./views/completion/completion.component";
import { ConfigurationEmergencyReserveComponent } from "./views/configuration-emergency-reserve/configuration-emergency-reserve.component";
import { ConfigurationExecuteComponent } from "./views/configuration-execute/configuration-execute.component";
import { ConfigurationLineSideMeterFuseComponent } from "./views/configuration-line-side-meter-fuse/configuration-line-side-meter-fuse.component";
import { ProtocolCompletionComponent } from "./views/protocol-completion/protocol-completion.component";
import { ProtocolCustomerComponent } from "./views/protocol-customer/protocol-customer.component";
import { ProtocolInstallerComponent } from "./views/protocol-installer/protocol-installer.component";
import { ProtocolPv } from "./views/protocol-pv/protocol-pv.component";
import { ProtocolSystemComponent } from "./views/protocol-system/protocol-system.component";
import { InstallationComponent } from "./installation.component";
import { InstallationViewComponent } from "./installation-view/installation-view.component";
import { PreInstallationComponent } from "./views/pre-installation/pre-installation.component";
import { ConfigurationSystemComponent } from "./views/configuration-system/configuration-system.component";
import { ProtocolDynamicFeedInLimitation } from "./views/protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component";
import { ProtocolAdditionalAcProducersComponent } from "./views/protocol-additional-ac-producers/protocol-additional-ac-producers.component";

import { FormControl, ValidationErrors } from "@angular/forms";
import { ConfigurationSummaryComponent } from "./views/configuration-summary/configuration-summary.component";
import { ProtocolSerialNumbersComponent } from "./views/protocol-serial-numbers/protocol-serial-numbers.component";

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

//#endregion

@NgModule({
  imports: [
    FormlyModule.forRoot({
      validators: [
        { name: "zip", validation: ZipValidator },
        { name: "emailMatch", validation: EmailMatchValidator }
      ],
      validationMessages: [
        { name: "required", message: RequiredValidatorMessage },
        { name: "min", message: MinValidatorMessage },
        { name: "max", message: MaxValidatorMessage },
        { name: "zip", message: ZipValidatorMessage },
        { name: "email", message: EmailValidatorMessage }
      ]
    }),
    SharedModule
  ],
  declarations: [
    CompletionComponent,
    ConfigurationEmergencyReserveComponent,
    ConfigurationExecuteComponent,
    ConfigurationLineSideMeterFuseComponent,
    ProtocolCompletionComponent,
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
    ProtocolSerialNumbersComponent
  ]
})
export class InstallationModule { }

// TODO rename to Setup or SetupAssistant to be in line with SetupProtocol on Backend side