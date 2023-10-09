import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { Category } from '../../../shared/category';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour } from '../../abstract-ibn';
import { AbstractCommercial50Ibn } from './abstract-commercial-50';
import { SystemId } from '../../../shared/system';
import { View } from '../../../shared/enums';

export class Commercial50Lastspitzenkappung extends AbstractCommercial50Ibn {

    public override readonly id: SystemId = SystemId.COMMERCIAL_50_PEAK_SHAVING;

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationFeaturesStorageSystem,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationPeakShaving,
            View.ConfigurationLineSideMeterFuse,
            View.ConfigurationCommercialModbuBridge,
            View.ProtocolAdditionalAcProducers,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ], translate);
    }

    public setRequiredControllers() {
        this.requiredControllerIds = [{
            componentId: "ctrlPeakShaving0"
            , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
        }];
    }

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service: Service) {

        const invalidateElementsAfterReadErrors: number = 3;
        const componentConfigurator: ComponentConfigurator =
            super.getCommercial50ComponentConfigurator(edge, config, websocket, invalidateElementsAfterReadErrors, service);

        let factoryId: string;
        let alias: string;
        let dischargeAbove: number;
        let chargeBelow: number;

        if (this.commercial50Feature.feature.type === Category.PEAK_SHAVING_SYMMETRIC) {
            factoryId = 'Controller.Symmetric.PeakShaving';
            alias = Category.toTranslatedString(Category.PEAK_SHAVING_SYMMETRIC, this.translate);
        } else {
            factoryId = 'Controller.Asymmetric.PeakShaving';
            alias = Category.toTranslatedString(Category.PEAK_SHAVING_ASYMMETRIC, this.translate);
        }

        if (this.commercial50Feature.feature.type !== Category.BALANCING) {
            dischargeAbove = this.commercial50Feature.feature.dischargeAbove;
            chargeBelow = this.commercial50Feature.feature.chargeBelow;
        }

        componentConfigurator.add({
            factoryId: factoryId,
            componentId: 'ctrlPeakShaving0',
            alias: alias,
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'meter.id', value: 'meter0' },
                { name: 'peakShavingPower', value: dischargeAbove },
                { name: 'rechargePower', value: chargeBelow }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        return componentConfigurator;
    }
}
