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
import { FormlyCurrentUserAlertingComponent } from "../edge/settings/alerting/formly/formly-current-user-alerting";
import { FormlyOtherUsersAlertingComponent } from "../edge/settings/alerting/formly/formly-other-users-alerting";
import { ComponentsModule } from "./components/components.module";
import { MeterModule } from "./components/edge/meter/meter.module";
import { FlatWidgetButtonComponent } from "./components/flat/flat-widget-button/flat-widget-button";
import { FormlyCheckBoxHyperlinkWrapperComponent } from "./components/formly/form-field-checkbox-hyperlink/form-field-checkbox-hyperlink.wrapper";
import { FormlyWrapperDefaultValueWithCasesComponent } from "./components/formly/form-field-default-cases.wrapper";
import { FormlyFieldMultiStepComponent } from "./components/formly/form-field-multi-step/form-field-multi-step";
import { FormlyWrapperFormFieldComponent } from "./components/formly/form-field.wrapper";
import { CheckboxButtonTypeComponent } from "./components/formly/formly-checkbox-with-button/formly-checkbox-with-button";
import { FormlyFieldCheckboxWithImageComponent } from "./components/formly/formly-field-checkbox-image/formly-field-checkbox-with-image";
import { FormlyFieldModalComponent } from "./components/formly/formly-field-modal/formly-field-modal";
import { FormlyFieldNavigationComponent } from "./components/formly/formly-field-navigation/formly-field-navigation";
import { FormlyFieldRadioWithImageComponent } from "./components/formly/formly-field-radio-with-image/formly-field-radio-with-image";
import { FormlyRangeTypeComponent } from "./components/formly/formly-field-range";
import { FormlyRadioTypeComponent } from "./components/formly/formly-radio/formly-radio";
import { FormlySelectComponent } from "./components/formly/formly-select/formly-select";
import { FormlySelectFieldModalComponent } from "./components/formly/formly-select-field-modal.component";
import { FormlySelectFieldExtendedWrapperComponent } from "./components/formly/formly-select-field.extended";
import { FormlyFieldWithLoadingAnimationComponent } from "./components/formly/formly-skeleton-wrapper";
import { FormlyTariffTableTypeComponent } from "./components/formly/formly-tariff-table/formly-custom-tariff-table";
import { FormlyFieldCheckboxWithLabelComponent } from "./components/formly/help-popover-label-with-description-and-checkbox/help-popover-label-with-description-and-checkbox";
import { InputTypeComponent } from "./components/formly/input";
import { FormlyInputSerialNumberWrapperComponent as FormlyWrapperInputSerialNumber } from "./components/formly/input-serial-number-wrapper";
import { PanelWrapperComponent } from "./components/formly/panel-wrapper.component";
import { RepeatTypeComponent } from "./components/formly/repeat";
import { AppHeaderComponent } from "./components/header/app-header";
import { HeaderComponent } from "./components/header/header.component";
import { HistoryDataErrorModule } from "./components/history-data-error/history-data-error.module";
import { ModalComponentsModule } from "./components/modal/modal.module";
import { PercentageBarComponent } from "./components/percentagebar/percentagebar.component";
import { PickDateTimeRangeComponent } from "./components/pick-date-time-range/pick-date-time-range";
import { PickdateComponentModule } from "./components/pickdate/pickdate.module";
import { HelpPopoverButtonComponent } from "./components/shared/view-component/help-popover/help-popover";
import { DirectiveModule } from "./directive/directive";
import { ChartOptionsComponent } from "./legacy/chartoptions/chartoptions.component";
import { AppStateTracker } from "./ngrx-store/states";
import { PipeModule } from "./pipe/pipe.module";
import { Logger } from "./service/logger";
import { RouteService } from "./service/route.service";
import { Service } from "./service/service";
import { Utils, Websocket } from "./shared";

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
        PickdateComponentModule,
        BaseChartDirective,
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
                { name: "formly-field-navigation", component: FormlyFieldNavigationComponent },
                { name: "formly-field-checkbox-with-image", component: FormlyFieldCheckboxWithImageComponent },
                { name: "formly-current-user-alerting", component: FormlyCurrentUserAlertingComponent },
                { name: "formly-other-users-alerting", component: FormlyOtherUsersAlertingComponent },
            ],
            types: [
                { name: "help-popover-label-with-description-and-checkbox", component: FormlyFieldCheckboxWithLabelComponent },
                { name: "input", component: InputTypeComponent },
                { name: "repeat", component: RepeatTypeComponent },
                { name: "multi-step", component: FormlyFieldMultiStepComponent },
                { name: "select", component: FormlySelectComponent },
                { name: "checkbox-button", component: CheckboxButtonTypeComponent },
                { name: "radio", component: FormlyRadioTypeComponent },
                { name: "tariff-table", component: FormlyTariffTableTypeComponent },
                { name: "range", component: FormlyRangeTypeComponent },
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
        HelpPopoverButtonComponent,
        FlatWidgetButtonComponent,
    ],
    declarations: [
        AppHeaderComponent,
        ChartOptionsComponent,
        FormlyCheckBoxHyperlinkWrapperComponent,
        FormlyFieldCheckboxWithImageComponent,
        FormlyFieldCheckboxWithLabelComponent,
        FormlyFieldModalComponent,
        FormlyFieldNavigationComponent,
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
        CheckboxButtonTypeComponent,
        FormlyRadioTypeComponent,
        FormlyTariffTableTypeComponent,
        PickDateTimeRangeComponent,
        FormlyRangeTypeComponent,
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
        ModalComponentsModule,
        BaseChartDirective,
        NgxSpinnerModule,
        PercentageBarComponent,
        PipeModule,
        ReactiveFormsModule,
        RouterModule,
        TranslateModule,
        PickDateTimeRangeComponent,
    ],
    providers: [
        AppStateTracker,
        Logger,
        RouteService,
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
