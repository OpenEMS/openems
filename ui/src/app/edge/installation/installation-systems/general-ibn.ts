import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { SerialNumberFormData } from '../shared/ibndatatypes';
import { ComponentConfigurator } from '../views/configuration-execute/component-configurator';
import { AbstractIbn, View } from './abstract-ibn';

export class GeneralIbn extends AbstractIbn {

    public override readonly id = 'general';

    public override showViewCount = false;

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationCommercialComponent,
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

    public getLineSideMeterFuseFields(): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getSettings(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number; numberOfModulesPerTower: number; }> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getFields(towerNr: number, numberOfModulesPerTower: number): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public fillForms(numberOfTowers: number, numberOfModulesPerTower: number, models: any, forms: SerialNumberFormData[]): SerialNumberFormData[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower?: number): Promise<Object> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getFeedInLimitFields(): FormlyFieldConfig[] {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service): ComponentConfigurator {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public setRequiredControllers() {
        throw new Error('This is General Ibn, Method not implemented.');
    }
    public setFeedInLimitsFields(model: any) {
        throw new Error('This is General Ibn, Method not implemented.');
    }
}
