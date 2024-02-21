import { TranslateService } from '@ngx-translate/core';

import { View } from '../../../shared/enums';
import { SystemId } from '../../../shared/system';
import { AbstractCommercial50Ibn } from './abstract-commercial-50';

export class Commercial50Balancing extends AbstractCommercial50Ibn {

    public override readonly id: SystemId = SystemId.COMMERCIAL_50_BALANCING;

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationSubSystem,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationLineSideMeterFuse,
            View.ConfigurationCommercialModbuBridge,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion,
        ], translate);
    }
}
