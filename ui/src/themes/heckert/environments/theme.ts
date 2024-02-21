import { Theme } from 'src/environments';

export const theme = {
    theme: "Heckert" as Theme,

    uiTitle: "Heckert Solar Symphon-E Online Monitoring",
    edgeShortName: "EMS",
    edgeLongName: "Heckert Solar Symphon-E Energiemanagementsystem",

    // TODO
    docsUrlPrefix: "https://docs.fenecon.de/{language}/_/latest/fems/",
    links: {
        COMMON_STORAGE: "ui/standardwidgets.html#_speicher",
        FORGET_PASSWORD: "https://erp.fenecon.de/web/reset_password?",
        EVCS_HARDY_BARTH: "fems-app/includes/fems-app/includes/FEMS_App_Ladestation_eCharge_Hardy_Barth.html",
        EVCS_KEBA_KECONTACT: "fems-app/includes/FEMS_App_Ladestation_KEBA.html",
        EVCS_OCPP_IESKEYWATTSINGLE: "fems-app/includes/FEMS_App_Ladestation_Ies_Keywatt.html",

        CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: "fems-app/includes/FEMS_App_Netzdienliche_Beladung.html",
        CONTROLLER_CHP_SOC: "fems-app/bhkw.html",
        CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: "fems-app/includes/FEMS_App_Schwellwertsteuerung.html",
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT: "fems-app/includes/FEMS_App_Manuelle_Relaissteuerung.html",
        CONTROLLER_IO_HEAT_PUMP_SG_READY: "fems-app/includes/FEMS_App_Waermepumpe_SG-Ready.html",
        CONTROLLER_IO_HEATING_ELEMENT: "fems-app/includes/FEMS_App_Heizstab.html",
        CONTROLLER_ESS_TIME_OF_USE_TARIFF: "fems-app/OEM_App_TOU.html",

        CONTROLLER_API_MODBUSTCP_READ: "fems-app/OEM_App_Modbus_TCP.html#_lesezugriff",
        CONTROLLER_API_MODBUSTCP_READWRITE: "fems-app/OEM_App_Modbus_TCP.html#_schreibzugriff",

        CONTROLLER_API_REST_READ: "fems-app/OEM_App_REST_JSON.html#_lesezugriff",
        CONTROLLER_API_REST_READWRITE: "fems-app/OEM_App_REST_JSON.html#_schreibzugriff",

        SETTINGS_ALERTING: "ui/settings.html#_benachrichtigung",
        SETTINGS_NETWORK_CONFIGURATION: "ui/settings.html#_netzwerkkonfiguration",
        EVCS_CLUSTER: "fems-app/includes/FEMS_App_Multi-Ladepunkt-Management.html",

        // Currently we are showing same links as what we shown for Fenecon OEM untill we recieve new links.
        WARRANTY: {
            HOME: {
                EN: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_EN_Warranty_conditions_FENECON_Home.pdf",
                DE: "https://fenecon.de/wp-content/uploads/2022/06/V2021.11_DE_Garantiebedingungen_FENECON_Home.pdf",
            },
            COMMERCIAL: {
                EN: "#",
                DE: "#",
            },
        },

        GTC: {
            EN: "https://fenecon.de/page/gtc/",
            DE: "https://fenecon.de/allgemeine-lieferungs-und-zahlungsbedingungen/",
        },

        // Currently the links are different with different prefixes. so adding whole url.
        METER: {
            SOCOMEC: 'https://symphon-e.heckert-solar.com/docs/de/Installationsanleitungen/Symphon-E_Socomec_Countis_E23_Installationsanleitung.pdf',
            KDK: 'https://docs.fenecon.de/de/Installationsanleitungen/FEMS_KDK_2PU_CT_Installationsanleitung.pdf',
        },

        MANUALS: {
            HOME: {
                HOME_10: "https://www.heckertsolar.com/wp-content/uploads/2022/06/Montage_und-Serviceanleitung-Symphon-E-1.pdf",
                HOME_20_30: "#",
            },
            COMMERCIAL: {
                COMMERCIAL_30: "#",
                COMMERCIAL_50: "#",
            },
        },
        APP_CENTER: {
            APP_IMAGE: (language: string, appId: string): string | null => {
                const languageKey = (() => {
                    switch (language) {
                        case 'de': return 'de';
                        case 'en': return 'en';
                        default: return 'en';
                    }
                })();
                return 'https://docs.fenecon.de/_/' + languageKey + '/_images/fenecon/apps/' + appId + '.png';
            },
        },
    },
    PRODUCT_TYPES: () => null,
};
