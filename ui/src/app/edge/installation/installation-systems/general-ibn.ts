import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { View } from '../shared/enums';
import { SerialNumberFormData } from '../shared/ibndatatypes';
import { SystemId } from '../shared/system';
import { ComponentConfigurator } from '../views/configuration-execute/component-configurator';
import { AbstractIbn } from './abstract-ibn';

export class GeneralIbn extends AbstractIbn {

    public override readonly id = SystemId.GENERAL;

    public override showViewCount = false;

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationCommercial,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEmergencyReserve,
            View.ConfigurationLineSideMeterFuse,
            View.ProtocolPv,
            View.ProtocolAdditionalAcProducers,
            View.ProtocolFeedInLimitation,
            View.HeckertAppInstaller,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ], translate);
    }

    public override getPreSettingInformationFromEdge(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number; numberOfModulesPerTower: number; }> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getSerialNumberFields(towerNr: number, numberOfModulesPerTower: number): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getPreSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override fillSerialNumberForms(numberOfTowers: number, numberOfModulesPerTower: number, models: any, forms: SerialNumberFormData[]): SerialNumberFormData[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getSerialNumbersFromEdge(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower?: number): Promise<Object> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getFeedInLimitFields(): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service): ComponentConfigurator {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override setRequiredControllers() {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public override setFeedInLimitFields(model: any) {
        throw new Error('This is General Ibn, Method not implemented.');
    }
}
