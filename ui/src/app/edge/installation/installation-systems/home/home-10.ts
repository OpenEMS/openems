import { TranslateService } from '@ngx-translate/core';

import { Category } from '../../shared/category';
import { View } from '../../shared/enums';
import { SystemId, SystemType } from '../../shared/system';
import { AbstractHomeIbn } from './abstract-home';

export class Home10FeneconIbn extends AbstractHomeIbn {
    public override maxNumberOfTowers: number = 4;
    public override maxNumberOfModulesPerTower: number = 10;

    public override readonly type: SystemType = SystemType.FENECON_HOME_10;
    public override readonly id: SystemId = SystemId.FENECON_HOME_10;
    public override readonly emsBoxLabel = Category.EMS_BOX_LABEL_HOME;
    public override maxNumberOfPvStrings: number = 2;
    public override maxFeedInLimit: number = 29999;
    public override homeAppId: string = 'App.FENECON.Home';
    public override homeAppAlias: string = 'FENECON Home';

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
            View.Completion,
        ], translate);
    }
}
