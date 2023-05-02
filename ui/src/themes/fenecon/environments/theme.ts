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

        WARRANTY_LINK_HOME_EN: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_EN_Warranty_conditions_FENECON_Home.pdf",
        WARRANTY_LINK_HOME_DE: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_DE_Garantiebedingungen_FENECON_Home.pdf",
        GTC_LINK_EN: "https://fenecon.de/page/gtc/",
        GTC_LINK_DE: "https://fenecon.de/allgemeine-lieferungs-und-zahlungsbedingungen/",
        WARRANTY_LINK_COMMERCIAL_EN: "https://fenecon.de/wp-content/uploads/2022/08/V2022.03_EN_Warranty_conditions_for-FENECON-Commercial_30_50.pdf",
        WARRANTY_LINK_COMMERCIAL_DE: "https://fenecon.de/wp-content/uploads/2022/07/V2022.03_DE_Garantiebedingungen_FENECON_Commercial_30_50.pdf",



        // Currently the links are different with different prefixes. so adding whole url for Socomec.
        METER_SOCOMEC: 'https://docs.fenecon.de/de/_/latest/_attachments/Benutzerhandbuecher/FEMS_App_Socomec_Zaehler_Benutzerhandbuch.pdf'
    },
};