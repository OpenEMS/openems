import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { Category } from '../../../shared/category';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour, View } from '../../abstract-ibn';
import { AbstractCommercial50Ibn } from './abstract-commercial-50';

export class Commercial50Lastspitzenkappung extends AbstractCommercial50Ibn {

    public readonly id: string = 'commercial-50-lastspitzenkappung';

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

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket) {

        const componentConfigurator: ComponentConfigurator =
            super.getComponentConfigurator(edge, config, websocket);

        let factoryId: string;
        let alias: string;
        let entladungÜber: number;
        let beladungUnter: number;

        if (this.commercial50Feature.feature.type === Category.PEAK_SHAVING_SYMMETRIC) {
            factoryId = 'Controller.Symmetric.PeakShaving';
            alias = Category.toTranslatedString(Category.PEAK_SHAVING_SYMMETRIC, this.translate);
        } else {
            factoryId = 'Controller.Asymmetric.PeakShaving';
            alias = Category.toTranslatedString(Category.PEAK_SHAVING_ASYMMETRIC, this.translate);
        }

        if (this.commercial50Feature.feature.type !== Category.BALANCING) {
            entladungÜber = this.commercial50Feature.feature.entladungÜber;
            beladungUnter = this.commercial50Feature.feature.beladungUnter;
        }

        componentConfigurator.add({
            factoryId: factoryId,
            componentId: 'ctrlPeakShaving0',
            alias: alias,
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'meter.id', value: 'meter0' },
                { name: 'peakShavingPower', value: entladungÜber },
                { name: 'rechargePower', value: beladungUnter },
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        return componentConfigurator;
    }
}
