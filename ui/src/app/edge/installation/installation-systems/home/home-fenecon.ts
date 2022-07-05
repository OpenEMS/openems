import { AbstractHomeIbn } from './abstract-home';
import { View } from '../abstract-ibn';

export class HomeFeneconIbn extends AbstractHomeIbn {
    public readonly type = 'Fenecon-Home';

    public readonly id = 'home';

    public readonly manualLink = 'https://fenecon.de/wp-content/uploads/2022/02/V2022.01.27_DE_Montage-und_Serviceanleitung_Home.pdf';

    constructor() {
        super([
            View.PreInstallation,
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
        ]);
    }
}
