// @ts-strict-ignore
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";

import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { SetupProtocol, SubmitSetupProtocolRequest } from "src/app/shared/jsonrpc/request/submitSetupProtocolRequest";
import { Edge, Websocket } from "src/app/shared/shared";
import { Country } from "src/app/shared/type/country";
import { Category } from "../../../shared/category";
import { ExternalLimitationType, FeedInSetting, GridSetting, View } from "../../../shared/enums";
import { GridSettingUtils } from "../../../shared/gridsetting.utils";
import { ComponentData, DcPv, EmsType } from "../../../shared/ibndatatypes";
import { Meter } from "../../../shared/meter";
import { System, SystemId } from "../../../shared/system";
import { ViewUtils } from "../../../shared/view.utils";
import { SafetyCountry } from "../../../views/configuration-execute/safety-country";
import { AbstractHomeIbn } from "../../home/abstract-home";

export type Commercial50Gen3App = {
    SAFETY_COUNTRY: SafetyCountry,
    FEED_IN_SETTING: string,
    MAX_FEED_IN_POWER?: number,
    HAS_EMERGENCY_RESERVE: boolean,
    EMERGENCY_RESERVE_ENABLED?: boolean,
    EMERGENCY_RESERVE_SOC?: number,
    SHADOW_MANAGEMENT_DISABLED?: boolean,
    HAS_ESS_LIMITER_14A: boolean,
    NA_PROTECTION_ENABLED: boolean,
    FEED_IN_TYPE: ExternalLimitationType,
    GRID_CODE: string,
    CT_RATIO_FIRST: number,
    HAS_MPPT_1: boolean,
    ALIAS_MPPT_1?: string,
    HAS_MPPT_2: boolean,
    ALIAS_MPPT_2?: string,
    HAS_MPPT_3: boolean,
    ALIAS_MPPT_3?: string,
    HAS_MPPT_4: boolean,
    ALIAS_MPPT_4?: string,
};

export class Commercial50Gen3Ibn extends AbstractHomeIbn {

    public static override BASE_IMAGE_PATH = "assets/img/commercial/commercial-50-mppt/";
    public override readonly systemId: SystemId = SystemId.COMMERCIAL_50_GEN_3;
    public override readonly defaultNumberOfModules: number = 2;
    public override readonly minNumberOfTowers: number = 2;
    public override readonly maxNumberOfTowers: number = 5;
    public override readonly maxNumberOfModulesPerTower: number = 15;
    public override readonly minNumberOfModulesPerTower: number = 5;
    public override readonly maxNumberOfPvStrings: number = 8;
    public override readonly maxNumberOfMppt: number = 4;
    public override readonly isDoubleSocketValid: boolean = true;
    public gridSetting: GridSetting | null = null;

    public override readonly emsBoxLabel = Category.EMS_BOX_LABEL_HOME;
    public override appId: string = "App.FENECON.Commercial.50.Gen3";
    public override appAlias: string = "FENECON Commercial 50";
    public override defaultEnergyFlowMeter: Meter.GridMeterCategory = Meter.GridMeterCategory.COMMERCIAL_METER;

    public override energyFlowMeter: {
        meter: Meter.GridMeterCategory;
        value?: number;
    } = {
            meter: Meter.GridMeterCategory.COMMERCIAL_METER,
        };

    // configuration-emergency-reserve
    public override emergencyReserve? = {
        isStsBoxAvailable: false,
        isEnabled: false,
        minValue: 5,
        value: 20,
        isReserveSocEnabled: false,
        stsBoxSerialNumber: null,
    };

    public mppt: {
        mppt1: boolean;
        mppt2: boolean;
        mppt3: boolean,
        mppt4: boolean,
    } = {
            mppt1: false,
            mppt2: false,
            mppt3: false,
            mppt4: false,
        };

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationSubSystem,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEssLimiter14aComponent,
            View.ConfigurationEnergyFlowMeter,
            View.ConfigurationMpptSelection,
            View.ProtocolPv,
            View.ConfigurationGridSetting,
            View.ProtocolFeedInLimitation,
            View.ConfigurationStsBox,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion,
        ], translate);
    }

    public override getAppProperties(safetyCountry: SafetyCountry, feedInSetting: FeedInSetting): {} {
        const dc1: DcPv = this.pv.dc[0];
        const dc2: DcPv = this.pv.dc[1];
        const dc3: DcPv = this.pv.dc[2];
        const dc4: DcPv = this.pv.dc[3];

        // base properties
        const commercial50Gen3AppProperties: Partial<Commercial50Gen3App> = {
            ...this.commonProperties(safetyCountry, feedInSetting),
            FEED_IN_TYPE: this.feedInLimitation.feedInType,
            GRID_CODE: this.gridSetting,
            CT_RATIO_FIRST: this.energyFlowMeter.value,
            HAS_MPPT_1: dc1.isSelected,
            HAS_MPPT_2: dc2.isSelected,
            HAS_MPPT_3: dc3.isSelected,
            HAS_MPPT_4: dc4.isSelected,
        };

        // Conditionally add the alias properties
        if (dc1.isSelected) {
            commercial50Gen3AppProperties.ALIAS_MPPT_1 = dc1.alias;
        }
        if (dc2.isSelected) {
            commercial50Gen3AppProperties.ALIAS_MPPT_2 = dc2.alias;
        }
        if (dc3.isSelected) {
            commercial50Gen3AppProperties.ALIAS_MPPT_3 = dc3.alias;
        }
        if (dc4.isSelected) {
            commercial50Gen3AppProperties.ALIAS_MPPT_4 = dc4.alias;
        }

        return commercial50Gen3AppProperties;
    }

    public override getEnergyFlowMeterFields() {
        const type: string = this.translate.instant("INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.TYPE.STANDARD");

        return [
            {
                value: Meter.GridMeterCategory.COMMERCIAL_METER, //
                label: Meter.toGridMeterCategoryLabelString(Meter.GridMeterCategory.COMMERCIAL_METER, this.translate, type),
            },
        ];
    }

    public override setRequiredControllers() {
        this.requiredControllerIds = [];
    }

    public getMpptLabel(mppt: number, pv: number) {
        return this.getTwoStringMpptLabel(mppt, pv);
    }

    public getImageUrl(mppt: number): string {
        return `${Commercial50Gen3Ibn.BASE_IMAGE_PATH}MPPT${mppt}.jpg`;
    }

    public override populateFromData(ibnString: any) {

        super.populateFromData(ibnString);

        this.isController14aActivated = ibnString.isController14aActivated;
        this.gridSetting = ibnString.gridSetting ?? null;
        this.emergencyReserve = ibnString.emergencyReserve;
    }

    public override removeOrAddView(edge: Edge): void {
        const location: Country = this.location?.isEqualToCustomerData ? this.customer.country : this.location?.country;

        super.removeOrAddView(edge);
        // Limiter 14a view
        ViewUtils.handleViewPresence({
            location,
            targetCountry: Country.GERMANY,
            views: this.views,
            viewToToggle: View.ConfigurationEssLimiter14aComponent,
            insertIndex: this.indexOf14aLimiterControllerView,
            shouldClear: () => {
                this.isController14aActivated = false;
            },
            shouldSet: () => {
                this.isController14aActivated = true;
            },
        });

        // Grid setting view
        ViewUtils.handleViewPresence({
            location,
            targetCountry: Country.GERMANY,
            views: this.views,
            viewToToggle: View.ConfigurationGridSetting,
            insertIndex: 9,
            shouldClear: () => {
                this.gridSetting = null;
            },
        });
    }

    public override getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
        const protocol: SetupProtocol = super.getCommonProtocolItems(edge);
        this.addProtocolItems(protocol);

        return new Promise((resolve, reject) => {
            websocket
                .sendRequest(SubmitSetupProtocolRequest.translateFrom(protocol, this.translate))
                .then((response: JsonrpcResponseSuccess) => {
                    resolve(response.result["setupProtocolId"]);
                })
                .catch((reason) => {
                    reject(reason);
                });
        });
    }

    public override addCustomBatteryInverterData(): ComponentData[] {
        const batteryInverterData: ComponentData[] = super.addCustomBatteryInverterData();
        const safetyCountry = this.location.isEqualToCustomerData ? this.customer.country : this.location.country;

        if (safetyCountry === Country.GERMANY) {
            batteryInverterData.push({ label: this.translate.instant("INSTALLATION.CONFIGURATION_GRID_SETTING.TITLE"), value: this.gridSetting });
        }

        batteryInverterData.push({
            label: this.translate.instant("INSTALLATION.CONFIGURATION_STS_BOX.STS_BOX_AVAILABLE"),
            value: this.emergencyReserve.isStsBoxAvailable ? this.translate.instant("General.yes") : this.translate.instant("General.no"),
        });

        if (this.emergencyReserve.isStsBoxAvailable) {
            batteryInverterData.push({
                label: this.translate.instant("INSTALLATION.PROTOCOL_AVU_BOX.SERIAL_NUMBER"),
                value: this.emergencyReserve.stsBoxSerialNumber,
            });
        }

        return batteryInverterData;
    }

    public override getSubSystemFields(): FormlyFieldConfig[] {
        const emsType: EmsType = this.getEmsType();

        return System.getSubSystemFields(emsType, this.translate);
    }

    public override addProtocolItems(protocol: SetupProtocol) {
        super.addProtocolItems(protocol);

        const emergencyReserve = this.emergencyReserve;

        protocol.items.push({
            category: Category.STS_BOX,
            name: this.translate.instant("INSTALLATION.CONFIGURATION_STS_BOX.STS_BOX_AVAILABLE"),
            value: emergencyReserve.isStsBoxAvailable ? this.translate.instant("General.yes") : this.translate.instant("General.no"),
        });

        if (emergencyReserve.isStsBoxAvailable) {

            protocol.items.push({
                category: Category.STS_BOX,
                name: this.translate.instant("INSTALLATION.PROTOCOL_AVU_BOX.SERIAL_NUMBER"),
                value: emergencyReserve.stsBoxSerialNumber,
            });
        }
    }

    public override getGridSettingOptions() {
        return [
            { label: GridSettingUtils.getDisplayName(GridSetting.VDE_4105), value: GridSetting.VDE_4105 },
        ];
    }
}
