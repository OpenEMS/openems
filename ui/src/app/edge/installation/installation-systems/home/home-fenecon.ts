import { AbstractHomeIbn } from './abstract-home';
import { View } from '../abstract-ibn';
import { TranslateService } from '@ngx-translate/core';

export class HomeFeneconIbn extends AbstractHomeIbn {
    public readonly type = 'Fenecon-Home';

    public readonly id = 'home';

    constructor(public translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEmergencyReserve,
            View.ConfigurationLineSideMeterFuse,
            View.ProtocolPv,
            View.ProtocolAdditionalAcProducers,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ], translate);
    }
}
