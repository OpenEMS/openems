import { TranslateService } from '@ngx-translate/core';
import { Filter } from 'src/app/index/filter/filter.component';
import { Theme } from 'src/environments';

export const theme = {
    theme: "FENECON" as Theme,

    uiTitle: "FENECON Online-Monitoring",
    edgeShortName: "FEMS",
    edgeLongName: "FENECON Energiemanagementsystem",

    docsUrlPrefix: "https://docs.fenecon.de/{language}/_/latest/fems/",
    links: {
        COMMON_STORAGE: "ui/standardwidgets.html#_speicher",

        EVCS_HARDY_BARTH: "fems-app/includes/FEMS_App_Ladestation_eCharge_Hardy_Barth.html",
        EVCS_KEBA_KECONTACT: "fems-app/includes/FEMS_App_Ladestation_KEBA.html",
        EVCS_OCPP_IESKEYWATTSINGLE: "fems-app/includes/FEMS_App_Ladestation_Ies_Keywatt.html",

        CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: "fems-app/includes/FEMS_App_Netzdienliche_Beladung.html",
        CONTROLLER_CHP_SOC: "fems-app/bhkw.html",
        CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: "fems-app/includes/FEMS_App_Schwellwertsteuerung.html",
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT: "fems-app/includes/FEMS_App_Manuelle_Relaissteuerung.html",
        CONTROLLER_IO_HEAT_PUMP_SG_READY: "fems-app/includes/FEMS_App_Waermepumpe_SG-Ready.html",

        SETTINGS_ALERTING: "ui/settings.html#_benachrichtigung",
        SETTINGS_NETWORK_CONFIGURATION: "ui/settings.html#_netzwerkkonfiguration",
        EVCS_CLUSTER: "fems-app/includes/FEMS_App_Multi-Ladepunkt-Management.html",

        warranty: {
            home: {
                EN: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_EN_Warranty_conditions_FENECON_Home.pdf",
                DE: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_DE_Garantiebedingungen_FENECON_Home.pdf"
            },
            commercial: {
                EN: "https://fenecon.de/wp-content/uploads/2022/08/V2022.03_EN_Warranty_conditions_for-FENECON-Commercial_30_50.pdf",
                DE: "https://fenecon.de/wp-content/uploads/2022/07/V2022.03_DE_Garantiebedingungen_FENECON_Commercial_30_50.pdf"
            }
        },

        gtc: {
            EN: "https://fenecon.de/page/gtc/",
            DE: "https://fenecon.de/allgemeine-lieferungs-und-zahlungsbedingungen/"
        },

        // Currently the links are different with different prefixes. so adding whole url for Socomec.
        METER_SOCOMEC: 'https://docs.fenecon.de/de/_/latest/_attachments/Benutzerhandbuecher/FEMS_App_Socomec_Zaehler_Benutzerhandbuch.pdf',

        MANUALS: {
            HOME: {
                EN: "https://fenecon.de/wp-content/uploads/2023/06/FENECON-HOME-10-Quick-Installation-Guide-EN.pdf",
                DE: "https://fenecon.de/download/montage-und-serviceanleitung-feneconhome/?wpdmdl=17765&refresh=62a048d9acf401654671577"
            },
            COMMERCIAL: {
                EN: "#",
                DE: "#"
            }
        }
    },
    PRODUCT_TYPES: (translate: TranslateService): Filter => (
        {
            placeholder: translate.instant("Index.type"),
            category: "producttype",
            options: [
                {
                    name: "Home",
                    value: "home"
                },
                {
                    name: "Commercial 30",
                    value: "Commercial 30-Serie"
                },
                {
                    name: "Commercial 50",
                    value: "Commercial 50-Serie"
                },
                {
                    name: "Industrial",
                    value: "INDUSTRIAL"
                },
                {
                    name: "Pro Hybrid GW",
                    value: "Pro Hybrid GW"
                },
                {
                    name: "Pro Hybrid 10",
                    value: "Pro Hybrid 10-Serie"
                },
                {
                    name: "Pro 9-12",
                    value: "Pro 9-12"
                }]
        })
};