import { TranslateService } from '@ngx-translate/core';

import { Category } from '../../shared/category';
import { View } from '../abstract-ibn';
import { AbstractHomeIbn } from './abstract-home';

export class HomeFeneconIbn extends AbstractHomeIbn {
    public override readonly type = 'Fenecon-Home';

    public override readonly id = 'home';

    public override readonly emsBoxLabel = Category.EMS_BOX_LABEL_HOME;

    constructor(public override translate: TranslateService) {
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
