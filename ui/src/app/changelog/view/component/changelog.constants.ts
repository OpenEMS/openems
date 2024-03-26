import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";

export class Changelog {

    public static readonly UI_VERSION = "2024.3.2-SNAPSHOT";

    public static readonly GENERAL_OPTIMIZATION = "Optimierungen und Fehlerbehebungen";
    public static readonly EDGE = Changelog.GENERAL_OPTIMIZATION + " am " + environment.edgeShortName + ".";
    public static readonly UI = Changelog.GENERAL_OPTIMIZATION + " am Online-Monitoring. ";
    public static readonly BACKEND = Changelog.GENERAL_OPTIMIZATION + " am Backend für das Online-Monitoring.";

    public static readonly BATTERY_PROTECTION = "Verbesserung des Batterie Lade- und Entladeverhaltens im oberen und unteren Ladezustandsbereich";

    public static product(...products: Product[]) {
        return products.map(product => Changelog.link(product.name, product.url)).join(", ") + '. ';
    }

    public static app(app: App, ...names: string[]) {
        return Changelog.link(app.name, app.url)
            + (names.length === 0 ? "" : " (" + names.join(", ") + ")")
            + ": ";
    }

    public static openems(version: string) {
        return 'Update auf OpenEMS Version ' + version + '. Mehr Details auf ' + Changelog.link('Github', 'https://github.com/OpenEMS/openems/releases/tag/' + version);
    }

    public static openemsComponent(openemsComponent: OpenemsComponent, change: string) {
        return { roleIsAtLeast: Role.ADMIN, change: Changelog.link(openemsComponent.name, openemsComponent.url) + ": " + change };
    }

    public static library(...libraries: Library[]) {
        return 'Aktualisierung externer Programmbibliotheken: ' + libraries.map(library => library.name).join(", ");
    }

    public static link(title: string, url: string) {
        return '<a target="_blank" href="' + url + '">' + title + '</a>';
    }
}

export class Product {
    private static readonly DEPRECATED = "https://fenecon.de/files-fruehere-modelle/";

    // TODO Umsetzung OEM-Links?
    // ESS
    public static readonly HOME_10 = new Product("FENECON Home 10", 'https://fenecon.de/fenecon-home-10/');
    public static readonly HOME_20_30 = new Product("FENECON Home 20 & 30", 'https://fenecon.de/fenecon-home-20-30/');
    public static readonly COMMERCIAL_30 = new Product("FENECON Commercial 30", 'https://fenecon.de/fenecon-commercial-30/');
    public static readonly COMMERCIAL_50 = new Product("FENECON Commercial 50", 'https://fenecon.de/fenecon-commercial-50/');

    // Deprecated ESS
    public static readonly DESS = new Product("FENECON DESS", this.DEPRECATED);
    public static readonly PRO_HYBRID_10 = new Product("FENECON Pro Hybrid 10", this.DEPRECATED);
    public static readonly PRO_HYBRID_GW = new Product("FENECON Pro Hybrid GW", this.DEPRECATED);
    public static readonly PRO_HYBRID_AC_GW = new Product("FENECON Pro Hybrid AC GW", this.DEPRECATED);

    // Industrial
    public static readonly INDUSTRIAL_S = new Product("FENECON Industrial S", 'https://fenecon.de/fenecon-industrial-s/');
    public static readonly INDUSTRIAL_M = new Product("FENECON Industrial M", 'https://fenecon.de/fenecon-industrial-m/');
    public static readonly INDUSTRIAL_L = new Product("FENECON Industrial L", 'https://fenecon.de/fenecon-industrial-l/');

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: any) {
    }
}

export class App {
    // TODO Umsetzung OEM-Links?
    public static readonly NETZDIENLICHE_BELADUNG = new App("FEMS App Netzdienliche Beladung", 'https://fenecon.de/fenecon-fems/fems-app-netzdienliche-beladung/');
    public static readonly REST_JSON_API = new App("FEMS App Rest/JSON lesend", 'https://fenecon.de/fenecon-fems/fems-app-rest-json-lesend/');
    public static readonly MODBUS_TCP_API = new App("FEMS App Modbus/TCP lesend", 'https://fenecon.de/fenecon-fems/fems-app-modbus-tcp-lesend/');
    public static readonly PEAKSHAVING = new App("FEMS App Lastspitzenkappung", 'https://fenecon.de/fenecon-fems/fems-app-lastspitzenkappung/');
    public static readonly HOCHLASTZEITFENSTER = new App("FEMS App Hochlastzeitfenster", 'https://fenecon.de/fenecon-fems/fems-app-hochlastzeitfenster/');
    public static readonly PEAKSHAVING_PHASE = new App("FEMS App Phasengenaue Lastspitzenkappung", 'https://fenecon.de/fenecon-fems/fems-app-phasengenaue-lastspitzenkappung/');
    public static readonly SCHWELLWERTSTEUERUNG = new App("FEMS App Schwellwertsteuerung", 'https://fenecon.de/fenecon-fems/fems-app-schwellwert-steuerung/');
    public static readonly MANUELLE_RELAISSTEUERUNG = new App("FEMS App Manuelle Relaissteuerung", 'https://fenecon.de/fenecon-fems/fems-app-manuelle-relaissteuerung/');

    public static readonly EVCS_AC = new App("FEMS App AC-Ladestation", 'https://fenecon.de/fenecon-fems/fems-app-ac-ladestation/');
    public static readonly KEBA = "KEBA";
    public static readonly HARDY_BARTH = "Hardy Barth eCharge";

    public static readonly EVCS_DC = new App("FEMS App DC-Ladestation", 'https://fenecon.de/fenecon-fems/fems-app-dc-ladestation/');
    public static readonly IES_KEYWATT = "IES Keywatt";
    public static readonly ALPITRONIC = "Alpitronic Hypercharger";

    public static readonly POWER_TO_HEAT = new App("FEMS App Power-to-Heat", 'https://fenecon.de/fenecon-fems/fems-app-power-to-heat/');
    public static readonly HEIZSTAB = "Heizstab";
    public static readonly BHKW = "Blockheizkraftwerk (BHKW)";
    public static readonly WAERMEPUMPE = "SG-Ready Wärmepumpe";

    public static readonly METER = new App("FEMS App Erzeugungs- und Verbrauchszähler", 'https://fenecon.de/fenecon-fems/fems-app-power-to-heat/');
    public static readonly SOCOMEC = "SOCOMEC";
    public static readonly KDK = "KDK";
    public static readonly JANITZA = "Janitza";
    public static readonly CARLO_GAVAZZI = "CARLO GAVAZZI";
    public static readonly DISCOVERGY = "Discovergy";

    public static readonly PV_INVERTER = new App("FEMS App PV-Wechselrichter", 'https://fenecon.de/fenecon-fems/fems-app-pv-wechselrichter/');
    public static readonly KACO = "KACO";
    public static readonly SMA = "SMA";
    public static readonly KOSTAL = "Kostal";
    public static readonly FRONIUS = "Fronius";
    public static readonly SOLAREDGE = "SolarEdge";

    public static readonly TIME_OF_USE = new App("FEMS App Dynamischer Stromtarif", 'https://fenecon.de/fenecon-fems/fems-app-dynamischer-stromtarif/');
    public static readonly AWATTAR = "Awattar HOURLY";
    public static readonly TIBBER = "Tibber";
    public static readonly CORRENTLY = "STROMDAO Corrently";

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: any) {
    }
}

export class OpenemsComponent {
    public static readonly PQ_PLUS_ZAEHLER = new OpenemsComponent('PQ-Plus Zähler', 'https://github.com/OpenEMS/openems/tree/develop/io.openems.edge.meter.pqplus');
    public static readonly SDM630_ZAEHLER = new OpenemsComponent('SDM 630 Zähler', 'https://github.com/OpenEMS/openems/tree/develop/io.openems.edge.meter.microcare.sdm630');

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: any) {
    }
}

export class Library {
    // Java
    public static readonly APACHE_FELIX_FILEINSTALL = new Library('Apache Felix File Install', 'org.apache.felix..fileinstall');
    public static readonly APACHE_FELIX_FRAMEWORK = new Library('Apache Felix Framework', 'org.apache.felix.framework');
    public static readonly APACHE_FELIX_HTTP_JETTY = new Library('Apache Felix HTTP Jetty', 'org.apache.felix.http.jetty');
    public static readonly APACHE_FELIX_INVENTORY = new Library('Apache Felix Inventory', 'org.apache.felix.inventory');
    public static readonly APACHE_FELIX_METATYPE = new Library('Apache Felix MetaType', 'org.apache.felix.metatype');
    public static readonly APACHE_FELIX_SCR = new Library('Apache Felix SCR', 'org.apache.felix.scr');
    public static readonly APACHE_FELIX_WEBCONSOLE = new Library('Apache Felix Webconsole', 'org.apache.felix.webconsole');
    public static readonly APACHE_FELIX_CONFIGADMIN = new Library('Apache Felix Configuration Admin', 'org.apache.felix.configadmin');
    public static readonly BNDTOOLS = new Library('bndtools', 'biz.aQute.bnd'); // https://github.com/bndtools/bnd
    public static readonly CHARGETIME_OCPP = new Library('Open Charge Alliance Java OCPP', 'eu.chargetime.ocpp'); // https://github.com/ChargeTimeEU/Java-OCA-OCPP
    public static readonly ECLIPSE_OSGI = new Library('Eclipse OSGi', 'org.eclipse.osgi');
    public static readonly FASTEXCEL = new Library('fastexcel', 'fastexcel');
    public static readonly GRADLE = new Library('Gradle', 'gradle');
    public static readonly GUAVA = new Library('Guava', 'com.google.guava');
    public static readonly GSON = new Library('GSON', 'com.google.gson');
    public static readonly HIKARI_CP = new Library('HikariCP', 'hikaricp');
    public static readonly INFLUXDB = new Library('influxdb-java', 'influxdb-java');
    public static readonly JNA = new Library('JNA', 'net.java.dev.jna');
    public static readonly JAVA_WEBSOCKET = new Library('Java-WebSocket', 'org.java-websocket');
    public static readonly RETROFIT = new Library('Retrofit', 'com.squareup.retrofit');
    public static readonly MOSHI = new Library('Moshi', 'com.squareup.moshi');
    public static readonly MSGPACK = new Library('MsgPack', 'org.msgpack');
    public static readonly PAX_LOGGING = new Library('PAX Logging', 'org.ops4j.pax.logging');
    public static readonly OSGI_UTIL_FUNCTION = new Library('org.osgi.util.function', 'org.osgi.util.function');
    public static readonly OSGI_UTIL_PROMISE = new Library('org.osgi.util.promise', 'org.osgi.util.promise');
    public static readonly OSGI_SERVICE_JDBC = new Library('org.osgi.service.jdbc', 'org.osgi.service.jdbc');
    public static readonly POSTGRESQL = new Library('Postgresql', 'org.postgresql');
    public static readonly SLF4J = new Library('SLF4j', 'org.slf4j');
    public static readonly RRD4J = new Library('RRD4j', 'org.rrd4j');
    public static readonly OKHTTP = new Library('OkHttp', 'com.squareup.okhttp3');
    public static readonly OKIO = new Library('Okio', 'com.squareup.okio');

    // UI
    public static readonly ANGULAR = new Library('Angular', 'angular');
    public static readonly D3 = new Library('d3', 'd3');
    public static readonly DATE_FNS = new Library('date-fns', 'date-fns'); // https://date-fns.org/
    public static readonly IONIC = new Library('Ionic', 'ionic');
    public static readonly MYDATEPICKER = new Library('MyDatePicker', 'mydatepicker');
    public static readonly NG2_CHARTS = new Library('ng2-charts', 'ng2-charts');
    public static readonly NGX_COOKIE_SERVICE = new Library('ngx-cookie-service', 'ngx-cookie-service');
    public static readonly NGX_FORMLY = new Library('ngx-formly', 'ngx-formly');
    public static readonly NGX_SPINNER = new Library('ngx-spinner', 'ngx-spinner');
    public static readonly RXJS = new Library('RxJs', 'rxjs');
    public static readonly UUID = new Library('UUID', 'uuid');

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: string) {
    }
}
