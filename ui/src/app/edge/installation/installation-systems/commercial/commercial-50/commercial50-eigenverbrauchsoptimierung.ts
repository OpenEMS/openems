import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { FeedInType } from '../../../shared/enums';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour, View } from '../../abstract-ibn';
import { AbstractCommercial50Ibn } from './abstract-commercial-50';

export class Commercial50EigenverbrauchsOptimierung extends AbstractCommercial50Ibn {

    public readonly id: string = 'commercial-50-eigenverbrauchsoptimierung';

    constructor() {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationFeaturesStorageSystem,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationLineSideMeterFuse,
            View.ProtocolAdditionalAcProducers,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ]);
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

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket): ComponentConfigurator {

        const componentConfigurator: ComponentConfigurator =
            super.getComponentConfigurator(edge, config, websocket);

        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            // ctrlGridOptimizedCharge0
            componentConfigurator.add({
                factoryId: 'Controller.Ess.GridOptimizedCharge',
                componentId: 'ctrlGridOptimizedCharge0',
                alias: 'Netzdienliche Beladung',
                properties: [
                    { name: 'enabled', value: true },
                    { name: 'ess.id', value: 'ess0' },
                    { name: 'meter.id', value: 'meter0' },
                    { name: 'sellToGridLimitEnabled', value: true },
                    {
                        name: 'maximumSellToGridPower',
                        value: this.feedInLimitation.maximumFeedInPower,
                    },
                    { name: 'delayChargeRiskLevel', value: 'MEDIUM' },
                    { name: 'mode', value: 'AUTOMATIC' },
                    { name: 'manualTargetTime', value: '17:00' },
                    { name: 'debugMode', value: false },
                    { name: 'sellToGridLimitRampPercentage', value: 2 },
                ],
                mode: ConfigurationMode.RemoveAndConfigure,
            });
        }

        // ctrlBalancing0
        componentConfigurator.add({
            factoryId: 'Controller.Symmetric.Balancing',
            componentId: 'ctrlBalancing0',
            alias: 'Eigenverbrauchsoptimierung',
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'meter.id', value: 'meter0' },
                { name: 'targetGridSetpoint', value: 0 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        return componentConfigurator;
    }
}
