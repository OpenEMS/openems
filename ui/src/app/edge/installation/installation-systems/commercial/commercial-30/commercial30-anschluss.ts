import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { FeedInType } from '../../../shared/enums';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour, View } from '../../abstract-ibn';
import { AbstractCommercial30Ibn } from './abstract-commercial-30';

export class Commercial30AnschlussIbn extends AbstractCommercial30Ibn {

    public readonly id: string = 'commercial-30-anschluss';

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationCommercialComponent,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationLineSideMeterFuse,
            View.ConfigurationCommercialModbuBridgeComponent,
            View.ProtocolAdditionalAcProducers,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ], translate);
    }

    public setRequiredControllers() {
        this.requiredControllerIds = [];
        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            this.requiredControllerIds.push({
                componentId: "ctrlGridOptimizedCharge0"
                , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
            });
        }
        this.requiredControllerIds.push({
            componentId: "ctrlBalancing0"
            , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
        });
    }

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket) {
        const invalidateElementsAfterReadErrors: number = 3;
        const componentConfigurator: ComponentConfigurator = this.getCommercial30ComponentConfigurator(edge, config, websocket, invalidateElementsAfterReadErrors);

        // ess0
        componentConfigurator.add({
            factoryId: 'Ess.Generic.ManagedSymmetric',
            componentId: 'ess0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.STORAGE_SYSTEM'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'startStop', value: 'START' },
                { name: 'batteryInverter.id', value: 'batteryInverter0' },
                { name: 'battery.id', value: 'battery0' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        }, 7);

        return componentConfigurator;
    }

    public getFields(stringNr: number, numberOfModulesPerString: number) {

        const fields: FormlyFieldConfig[] = this.getCommercial30SerialNumbersFields(stringNr, numberOfModulesPerString);

        if (stringNr === 0) {

            // Adds the ems box field only for Initial String.
            const emsbox: FormlyFieldConfig = {
                key: 'emsbox',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_CONNECTION_BOX_COMMERCIAL30', { edgeShortName: environment.edgeShortName }),
                    required: true,
                    prefix: 'FC',
                    placeholder: 'xxxxxxxxx'
                },
                validators: {
                    validation: ['emsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            };

            // ems box field is added at a specific position in array, because it is always displayed at specific position in UI.
            fields.splice(1, 0, emsbox);
        }

        return fields;
    }
}
