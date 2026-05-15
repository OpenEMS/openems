import { Component } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { format } from "date-fns";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext } from "src/app/shared/components/shared/oe-formly-component";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeFormats, DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { SharedStorage } from "../shared/shared";

@Component({
    selector: "oe-common-storage-owner-guest-installer-details",
    standalone: true,
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",

    imports: [
        CommonUiModule,
        HelpButtonComponent,
        ReactiveFormsModule,
        FormsModule,
        FormlyModule,
        PipeComponentsModule,
        ComponentsBaseModule,
        ModalComponentsModule,
        LocaleProvider,
        LiveDataServiceProvider,
    ],
})
export class CommonStorageSettingsComponent extends AbstractFormlyComponent<any> {

    private static CHANNEL_ID_EMERGENCY_RESERVE_SOC = "_PropertyReserveSoc";
    private static CHANNEL_ID_EMERGENCY_RESERVE_ENABLED = "_PropertyIsReserveSocEnabled";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_IS_RUNNING = "_PropertyIsRunning";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_SPECIFIED = "_PropertyTargetTimeSpecified";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME = "_PropertyTargetTime";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_SOC = "_PropertyTargetSoc";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_BUFFER = "_PropertyTargetTimeBuffer";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_EXPECTED_EPOCH_SECONDS = "ExpectedStartEpochSeconds";
    private static CHANNEL_ID_PREPARE_BATTERY_EXTENSION_CTRL_IS_IN_REFERENCE_CYCLE = "CtrlIsInReferenceCycle";
    private static CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED = "IsEssChargeFromGridAllowed";

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private static CONTROL_NAME_PREFIX = (component: EdgeConfig.Component) => component.id + "-";
    private static FORMCONTROL_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "isEssChargeFromGridAllowed";
    private static FORMCONTROL_EMERGENCY_RESERVE_ENABLED: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "emergencyReserveIsEnabled";
    private static FORMCONTROL_EMERGENCY_RESERVE_SOC: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "emergencyReserveSoc";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionIsRunning";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionTargetTimeSpecified";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionTargetTime";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionTargetSoc";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionTargetTimeBuffer";
    private static FORMCONTROL_PREPARE_BATTERY_EXTENSION_EXPECTED_EPOCH_SECONDS: (essComponent: EdgeConfig.Component) => string = (essComponent) => CommonStorageSettingsComponent.CONTROL_NAME_PREFIX(essComponent) + "prepareBatteryExtensionExpectedStartEpochSeconds";

    private static getMetaIsChargeFromGridAllowedFields(essComponent: EdgeConfig.Component, translate: TranslateService): OeFormlyField[] {
        const key = CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent);
        const lines: OeFormlyField[] = [
            { type: "horizontal-line" },
            { type: "toggle-line", style: { name: { fontWeight: "bold" } }, controlName: CommonStorageSettingsComponent.FORMCONTROL_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED(essComponent), name: translate.instant("EDGE.INDEX.WIDGETS.STORAGE.GRID_CHARGE") },
            { type: "info-line", html: translate.instant("EDGE.INDEX.WIDGETS.STORAGE.GRID_CHARGE_WARNING") },
            { type: "horizontal-line" },
        ];
        return lines.map(el => {
            el.hide = (field) => field[key] == 0;
            return el;
        });
    }

    private static getUpdateComponentPropertyEntry<T = string>(name: string, value: AbstractControl<T> | null): Parameters<Edge["updateComponentConfig"]>[2][number] | null {
        if (value == null) {
            return null;
        }
        return { name, value: value.value };
    }

    public override getFormGroup(): FormGroup {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const essComponents = SharedStorage.getEssComponents(config);
        const formGroup: FormGroup = new FormGroup({});
        for (const essComponent of essComponents) {
            const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (prepareBatteryExtensionCtrl != null) {
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_EXPECTED_EPOCH_SECONDS(essComponent), new FormControl(null));
            }
            const emergencyReserveCtrl = config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (emergencyReserveCtrl != null) {
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent), new FormControl(null));
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_SOC(essComponent), new FormControl(null));
            }

            const meta = new MetaComponent(config);
            if (meta != null) {
                formGroup.addControl(CommonStorageSettingsComponent.FORMCONTROL_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED(essComponent), new FormControl(null));
            }
        }

        return formGroup;
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {

        const lines: OeFormlyField[] = [];
        const essComponents: EdgeConfig.Component[] = SharedStorage.getEssComponents(viewContext.config);

        for (const essComponent of essComponents) {
            const emergencyReserveCtrl = viewContext.config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id) ?? null;
            const prepareBatteryExtensionCtrl = viewContext.config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id) ?? null;

            if (essComponents.length > 1 && emergencyReserveCtrl != null && prepareBatteryExtensionCtrl != null) {
                lines.push({
                    type: "name-line", name: Name.METER_ALIAS_OR_ID(essComponent),
                });
            }

            lines.push(...this.getEmergencyReserveForEss(essComponent, emergencyReserveCtrl, viewContext.config));
            lines.push(...this.getPrepareBatteryExtensionForEss(essComponent, viewContext.config, viewContext.translate));
        }

        return {
            lines: lines,
            title: viewContext.translate.instant("MENU.SETTINGS"),
            component: new EdgeConfig.Component(),
            edge: viewContext.edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        if (config === null) {
            return;
        }

        const essComponents = SharedStorage.getEssComponents(config);
        for (const essComponent of essComponents) {
            const emergencyReserveCtrl = config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (emergencyReserveCtrl != null) {
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent), currentData,
                    new ChannelAddress(emergencyReserveCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_EMERGENCY_RESERVE_ENABLED));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_SOC(essComponent), currentData,
                    new ChannelAddress(emergencyReserveCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_EMERGENCY_RESERVE_SOC));
            }

            const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (prepareBatteryExtensionCtrl != null) {
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent), currentData,
                    new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_IS_RUNNING));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC(essComponent), currentData,
                    new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_SOC));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent), currentData, new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER(essComponent), currentData,
                    new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_BUFFER));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent), currentData,
                    new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_SPECIFIED));
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_EXPECTED_EPOCH_SECONDS(essComponent), currentData,
                    new ChannelAddress(prepareBatteryExtensionCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_EXPECTED_EPOCH_SECONDS));
            }

            const meta = new MetaComponent(config);
            if (meta != null) {
                this.setFormControlSafelyWithChannel(
                    this.form, CommonStorageSettingsComponent.FORMCONTROL_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED(essComponent), currentData,
                    new ChannelAddress(meta.id, CommonStorageSettingsComponent.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED));
            }
        }
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const essComponents = SharedStorage.getEssComponents(config);

        const channelAddresses: ChannelAddress[] = [];
        const hasRequiredEdgeVersion = edge.isVersionAtLeast("2024.12.3");

        for (const essComponent of essComponents) {
            const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (prepareBatteryExtensionCtrl != null) {
                const allChannels = [
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_IS_RUNNING,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_EXPECTED_EPOCH_SECONDS,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_SOC,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_BUFFER,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_TIME_SPECIFIED,
                    CommonStorageSettingsComponent.CHANNEL_ID_PREPARE_BATTERY_EXTENSION_PROPERTY_TARGET_SOC,
                ];
                channelAddresses.push(...allChannels.map(channelId => new ChannelAddress(prepareBatteryExtensionCtrl.id, channelId)));
            }

            const emergencyReserveCtrl = config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);;
            if (emergencyReserveCtrl != null) {
                channelAddresses.push(
                    new ChannelAddress(emergencyReserveCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_EMERGENCY_RESERVE_SOC),
                    new ChannelAddress(emergencyReserveCtrl.id, CommonStorageSettingsComponent.CHANNEL_ID_EMERGENCY_RESERVE_ENABLED),
                );
            }
            const meta = new MetaComponent(config);
            if (meta != null && hasRequiredEdgeVersion) {
                channelAddresses.push(new ChannelAddress(meta.id, CommonStorageSettingsComponent.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED));
            }
        }
        return Promise.resolve(channelAddresses);
    }

    protected override async applyChanges(formGroup: FormGroup, service: Service, websocket: Websocket, _component: EdgeConfig.Component, edge: Edge) {
        if (edge == null || formGroup == null) {
            return;
        }

        const updateArray: Map<string, Parameters<Edge["updateComponentConfig"]>[2]> = new Map();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        const essComponents = SharedStorage.getEssComponents(config);

        for (const essComponent of essComponents) {
            const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (prepareBatteryExtensionCtrl != null) {
                const targetTime = DateTimeUtils.formatToISOZonedDateTime(
                    formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent))?.value ?? null, DateTimeUtils.getLocaleTimeZone());
                const updateObj: Parameters<Edge["updateComponentConfig"]>[2] = [
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("isRunning", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent))),
                    { name: "targetTime", value: targetTime },
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("targetTimeSpecified", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent))),
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("targetSoc", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC(essComponent))),
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("targetTimeBuffer", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER(essComponent))),
                ].filter(entry => entry != null);
                updateArray.set(prepareBatteryExtensionCtrl.id, updateObj);
            }

            const emergencyReserveCtrl = config.getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
            if (emergencyReserveCtrl != null) {
                const updateObj: Parameters<Edge["updateComponentConfig"]>[2] = [
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("isReserveSocEnabled", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent))),
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("reserveSoc", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_SOC(essComponent))),
                ].filter(entry => entry != null);
                updateArray.set(emergencyReserveCtrl.id, updateObj);
            }

            const hasRequiredEdgeVersion = edge.isVersionAtLeast("2024.12.3");
            const meta = new MetaComponent(config);
            if (hasRequiredEdgeVersion && meta) {
                updateArray.set(meta.id, [
                    CommonStorageSettingsComponent.getUpdateComponentPropertyEntry("isEssChargeFromGridAllowed", formGroup.get(CommonStorageSettingsComponent.FORMCONTROL_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED(essComponent))),
                ].filter(el => el != null));
            }
        }

        for (const controllerId of updateArray.keys()) {
            const controllerProperties = updateArray.get(controllerId) ?? [];
            await edge.updateComponentConfig(websocket, controllerId, controllerProperties);
        }
    }

    private getPrepareBatteryExtensionForEss(essComponent: EdgeConfig.Component, config: EdgeConfig, translate: TranslateService): OeFormlyField[] {
        const prepareBatteryExtensionCtrl = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension").find(el => el.getPropertyFromComponent("ess.id") == essComponent.id);
        if (prepareBatteryExtensionCtrl != null) {
            return [{
                type: "toggle-line",
                controlName: CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent),
                name: translate.instant("EDGE.INDEX.RETROFITTING.OPTIMAL"),
                style: { name: { fontWeight: "bold" } },
            },
            {
                type: "buttons-from-form-control-line",
                buttons: [{
                    name: translate.instant("EDGE.INDEX.RETROFITTING.PLANNED_EXPANSION"),
                    value: 1,
                }, {
                    name: translate.instant("EDGE.INDEX.RETROFITTING.INSTANT_START"),
                    value: 0,
                }],
                controlName: CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent),
                hide: (field) => !field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent)],
            },
            {
                type: "date-time-line",
                defaultLabel: translate.instant("EDGE.INDEX.RETROFITTING.SELECT_A_DATE"),
                label: (controlValue: string | number | null): string => {
                    if (typeof controlValue !== "string") {
                        return "";
                    }
                    const date = DateUtils.stringToDate(controlValue);
                    return translate.instant("EDGE.INDEX.RETROFITTING.ON_DATE_AT_TIME", { date: DateTimeUtils.format(date, DateTimeFormats.DAY_MONTH_YEAR), time: DateTimeUtils.format(date, DateTimeFormats.HOUR_MINUTE) });
                },
                controlName: CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent),
                hide: (field) =>
                    !field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent)]
                    || field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent)] != 1,
            },
            {
                type: "info-line",
                icon: { color: "medium", name: "information-outline", size: "large" },
                nameCallback: (field) => {

                    const targetTimeDate = DateUtils.stringToDate(field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent)]);
                    const epochSeconds = field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_EXPECTED_EPOCH_SECONDS(essComponent)];
                    const targetSoc = field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC(essComponent)];
                    const targetTimeBuffer = field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER(essComponent)];

                    if (epochSeconds == null || targetTimeDate == null) {
                        return translate.instant("EDGE.INDEX.RETROFITTING.INFORMATION_FOR_PLANNED_EXPANSION_INVALID_TARGET_DATE", { targetSoc: targetSoc });
                    }

                    return translate.instant("EDGE.INDEX.RETROFITTING.INFORMATION_FOR_PLANNED_EXPANSION", {
                        targetDate: format(targetTimeDate, DateTimeFormats.DAY_MONTH_YEAR),
                        targetTime: format(targetTimeDate, DateTimeFormats.HOUR_MINUTE),
                        targetSoc: targetSoc,
                        targetTimeBuffer: targetTimeBuffer,
                    });
                },
                hide: (field) =>
                    !field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent)]
                    || field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent)] != 1,
                name: "",
            },
            {
                type: "info-line",
                nameCallback: (field) => translate.instant("EDGE.INDEX.RETROFITTING.INFORMATION_FOR_INSTANT_START", {
                    targetSoc: field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_SOC(essComponent)],
                    targetTime: field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME(essComponent)],
                    targetTimeBuffer: field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_BUFFER(essComponent)],
                }),
                name: "",
                icon: {
                    name: "information-outline", color: "medium", size: "large",
                },
                hide: (field) =>
                    !field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_IS_RUNNING(essComponent)]
                    || field[CommonStorageSettingsComponent.FORMCONTROL_PREPARE_BATTERY_EXTENSION_TARGET_TIME_SPECIFIED(essComponent)] != 0,
            }];
        }
        return [];
    }

    private getEmergencyReserveForEss(essComponent: EdgeConfig.Component, emergencyReserveCtrl: EdgeConfig.Component | null, config: EdgeConfig): OeFormlyField[] {
        const lines: OeFormlyField[] = [];
        if (emergencyReserveCtrl != null) {
            lines.push({
                type: "toggle-line-with-formcontrol-value",
                controlName: CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent),
                name: this.translate.instant("EDGE.INDEX.EMERGENCY_RESERVE.EMERGENCY_RESERVE"),
                style: { name: { fontWeight: "bold" } },
                togglePrefix: (value) => { return Converter.STATE_IN_PERCENT(value[CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_SOC(essComponent)]); },
            }, {
                type: "info-line",
                icon: { color: "medium", name: "information-outline", size: "large" },
                name: [{ text: this.translate.instant("EDGE.INDEX.EMERGENCY_RESERVE.INFO_FOR_EMERGENCY_RESERVE_SLIDER"), lineStyle: "font-size: smaller" }],
            },
            {
                type: "range-button-from-form-control-line",
                controlName: CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_SOC(essComponent),
                properties: {
                    tickMin: 0,
                    tickMax: 100,
                    step: 1,
                    tickFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                    pinFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                },
                hide: (field) => field[CommonStorageSettingsComponent.FORMCONTROL_EMERGENCY_RESERVE_ENABLED(essComponent)] == 0,
            },
            ...CommonStorageSettingsComponent.getMetaIsChargeFromGridAllowedFields(essComponent, this.translate),
            );
        }

        return lines;
    }
}
