import { Theme } from 'src/environments';
import { EMPTY_URL } from 'src/environments/theme';

export const theme = {
    theme: "Heckert" as Theme,

    uiTitle: "Heckert Solar Symphon-E Online Monitoring",
    edgeShortName: "EMS",
    edgeLongName: "Heckert Solar Symphon-E Energiemanagementsystem",

    // TODO 
    docsUrlPrefix: "https://docs.fenecon.de/{language}/_/latest/fems/fems-app/includes/",
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

        // Currently we are showing same links as what we shown for Fenecon OEM untill we recieve new links.
        warranty: {
            home: {
                EN: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_EN_Warranty_conditions_FENECON_Home.pdf",
                DE: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_DE_Garantiebedingungen_FENECON_Home.pdf"
            },
            commercial: {
                EN: EMPTY_URL,
                DE: EMPTY_URL,
            }
        },

        gtc: {
            EN: "https://fenecon.de/page/gtc/",
            DE: "https://fenecon.de/allgemeine-lieferungs-und-zahlungsbedingungen/",
        },

        // Currently the links are different with different prefixes. so adding whole url.
        METER_SOCOMEC: 'https://docs.intranet.fenecon.de/feature/OEM/de/_/latest/_attachments/Benutzerhandbuecher/Heckert_App_Socomec_Zaehler_Benutzerhandbuch.pdf'
    }
};