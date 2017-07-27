import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs/Observable';

export class MyTranslateLoader implements TranslateLoader {

    constructor() { }

    getTranslation(lang: string): Observable<any> {
        if (lang === 'de') {
            return Observable.of(
                /*
                 * German translation
                 */
                {
                    General: {
                        Grid: "Netz",
                        GridBuy: "Netzbezug",
                        GridSell: "Netzeinspeisung",
                        Production: "Erzeugung",
                        Consumption: "Verbrauch",
                        Power: "Leistung",
                        StorageSystem: "Speichersystem"
                    },
                    Overview: {
                        ConnectionSuccessful: "Verbindung zu {{value}} hergestellt.",
                        ConnectionFailed: "Verbindung zu {{value}} getrennt.",
                        ToEnergymonitor: "Zum Energiemonitor...",
                        IsOffline: "FEMS ist offline!"
                    },
                    DeviceOverview: {
                        Energymonitor: {
                            Title: "Energiemonitor",
                            ConsumptionWarning: "Verbrauch & unbekannte Erzeuger",
                            Storage: "Speicher",
                        },
                        Energytable: {
                            Title: "Energietabelle"
                        }
                    }
                }
            );
        } else {
            return Observable.of(
                /*
                 * English translation
                 */
                {
                    General: {
                        GridBuy: "Buy from grid",
                        GridSell: "Sell to grid",
                        Production: "Production",
                        Consumption: "Consumption",
                        Power: "Power",
                        Grid: "Grid",
                        StorageSystem: "Storage System"
                    },
                    Overview: {
                        ConnectionSuccessful: "Successfully connected to {{value}}.",
                        ConnectionFailed: "Connection to {{value}} failed.",
                        ToEnergymonitor: "To Energymonitor...",
                        IsOffline: "FEMS is offline!"
                    },
                    DeviceOverview: {
                        Energymonitor: {
                            Title: "Energymonitor",
                            ConsumptionWarning: "Consumption & unknown producers",
                            Storage: "Storage"
                        },
                        Energytable: {
                            Title: "Energytable"
                        }
                    }
                }
            );
        }
    }
}
