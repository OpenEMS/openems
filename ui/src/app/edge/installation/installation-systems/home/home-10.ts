import { TranslateService } from '@ngx-translate/core';

import { Category } from '../../shared/category';
import { View } from '../../shared/enums';
import { SystemId } from '../../shared/system';
import { AbstractHome10Ibn } from './abstract-home-10';

export class Home10FeneconIbn extends AbstractHome10Ibn {

    public override readonly id: SystemId = SystemId.FENECON_HOME_10;
    public override readonly emsBoxLabel = Category.EMS_BOX_LABEL_HOME;

    constructor(public override translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationSystemVariant,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEmergencyReserve,
            View.ConfigurationLineSideMeterFuse,
            View.ProtocolPv,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion,
        ], translate);
    }
}
