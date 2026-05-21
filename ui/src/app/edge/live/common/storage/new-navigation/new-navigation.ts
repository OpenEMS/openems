import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { LiveDataService } from "../../../livedataservice";
import { SharedStorage } from "../shared/shared";
import { CommonStoragePercentagebarComponent } from "./percentagebar/percentagebar";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class CommonStorageHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    public static getFormlyGeneralView(translate: TranslateService, config: EdgeConfig): OeFormlyView {
        return {
            title: translate.instant("GENERAL.STORAGE_SYSTEM"),
            helpKey: "REDIRECT.COMMON_STORAGE",
            lines: CommonStorageHomeComponent.getLines(translate, config),
            component: new EdgeConfig.Component(),
            useDefaultPrefix: false,
            isCommonWidget: true,
        };
    }

    private static getLines(translate: TranslateService, config: EdgeConfig): OeFormlyField[] {
        const essComponents: EdgeConfig.Component[] = SharedStorage.getEssComponents(config);

        const emergencyReserveComponents: { [essId: string]: EdgeConfig.Component } = config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});

        const prepareBatteryExtensionCtrl: { [essId: string]: EdgeConfig.Component } = config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});

        return essComponents.reduce((arr: OeFormlyField[] = [], ess, i) => {
            if (essComponents.length > 1) {
                arr.push({
                    type: "name-line",
                    name: Name.METER_ALIAS_OR_ID(ess),
                });
            }

            const emergencyReserveCtrl = emergencyReserveComponents[ess.id];
            arr.push(
                {
                    type: "component-line",
                    component: CommonStoragePercentagebarComponent,
                    inputs: {
                        essComponentId: ess.id,
                        emergencyReserveController: emergencyReserveCtrl,
                    },
                },
                ...SharedStorage.getChargeDischargeLinesInKw(ess, config, translate)
            );

            const prepareBatteryExtensionCtrlForEss = ess.id in prepareBatteryExtensionCtrl ? prepareBatteryExtensionCtrl[ess.id] : null;

            if (prepareBatteryExtensionCtrlForEss !== null) {
                arr.push(
                    { type: "horizontal-line" },
                    {
                        type: "value-from-channels-line",
                        channelsToSubscribe: [
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/_PropertyIsRunning"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsBlockingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsChargingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsDischargingEss"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/CtrlIsInReferenceCycle"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/_PropertyTargetTimeSpecified"),
                            ChannelAddress.fromString(prepareBatteryExtensionCtrlForEss.id + "/_PropertyTargetTime"),
                        ],
                        singleLine: true,
                        value: (currentData: CurrentData) => SharedStorage.getBatteryCapacityExtensionStatus(translate, currentData, prepareBatteryExtensionCtrlForEss.id)?.text ?? null,
                        filter: (currentData: CurrentData) => SharedStorage.getBatteryCapacityExtensionStatus(translate, currentData, prepareBatteryExtensionCtrlForEss.id)?.text != null,
                    });
            }

            if (i < (essComponents.length - 1)) {
                arr.push({
                    type: "horizontal-line",
                });
            }

            return arr;
        }, []);
    }

    public override getFormGroup(): FormGroup {
        return new FormGroup({
            soc: new FormControl(null),
        });
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        return CommonStorageHomeComponent.getFormlyGeneralView(this.translate, config);
    }


    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithValue(this.form, "soc", currentData.allComponents["_sum/EssSoc"]);
    }

    protected override getChannelAddresses(): Promise<ChannelAddress[]> {
        return Promise.resolve([new ChannelAddress("_sum", "EssSoc")]);
    }

}
