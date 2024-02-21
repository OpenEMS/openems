import { TranslateService } from '@ngx-translate/core';

import { Category } from '../../shared/category';
import { View } from '../../shared/enums';
import { SystemId } from '../../shared/system';
import { AbstractHomeIbn } from './abstract-home';

export class Home10FeneconIbn extends AbstractHomeIbn {

    public override readonly id: SystemId = SystemId.FENECON_HOME_10;
    public override readonly emsBoxLabel = Category.EMS_BOX_LABEL_HOME;
    public override readonly homeAppAlias: string = 'FENECON Home';
    public override readonly homeAppId: string = 'App.FENECON.Home';
    public override readonly maxFeedInLimit: number = 29999;
    public override readonly maxNumberOfModulesPerTower: number = 10;
    public override readonly maxNumberOfPvStrings: number = 2;
    public override readonly maxNumberOfTowers: number = 3;
    public override readonly minNumberOfModulesPerTower: number = 4;

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
