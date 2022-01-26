import { environment } from "src/environments";

export class Changelog {

    public static readonly GENERAL_OPTIMIZATION = "Optimierungen und Fehlerbehebungen";
    public static readonly EDGE = Changelog.GENERAL_OPTIMIZATION + " am " + environment.edgeShortName + ".";
    public static readonly UI = Changelog.GENERAL_OPTIMIZATION + " am Online-Monitoring.";
    public static readonly BACKEND = Changelog.GENERAL_OPTIMIZATION + " am Backend für das Online-Monitoring.";

    public static readonly BATTERY_PROTECTION = "Verbesserung des Batterie Lade- und Entladeverhaltens im oberen und unteren Ladezustandsbereich";

    public static product(...products: Product[]) {
        return products.map(product => Changelog.link(product.name, product.url)).join(", ") + '. ';
    }

    public static openems(version: string) {
        return 'Update auf OpenEMS Version ' + version + '. Mehr Details auf ' + Changelog.link('Github', 'https://github.com/OpenEMS/openems/releases/tag/' + version);
    }

    public static library(...libraries: Library[]) {
        return 'Aktualisierung externer Programmbibliotheken: ' + libraries.map(library => library.name).join(", ");
    }

    public static link(title: string, url: string) {
        return '<a target="_blank" href="' + url + '">' + title + '</a>';
    }
}

export class Product {
    // TODO Umsetzung OEM-Links?
    // ESS
    public static readonly HOME = new Product('FENECON Home', 'https://fenecon.de/home/');
    public static readonly PRO_HYBRID_10 = new Product('FENECON Pro Hybrid 10', 'https://fenecon.de/pro-hybrid-10-serie/');
    public static readonly PRO_HYBRID_GW = new Product('FENECON Pro Hybrid GW', 'https://fenecon.de/pro-hybrid-gw-serie/');
    public static readonly PRO_HYBRID_AC_GW = new Product('FENECON Pro Hybrid AC GW', 'https://fenecon.de/pro-ac-gw-serie/');
    public static readonly COMMERCIAL_30 = new Product('FENECON Commercial 30', 'https://fenecon.de/fenecon-commercial-30-serie/');
    public static readonly COMMERCIAL_50 = new Product('FENECON Commercial 50', 'https://fenecon.de/fenecon-commercial-50-serie/');
    public static readonly COMMERCIAL_BYD = new Product('FENECON Commercial BYD', 'https://fenecon.de/fenecon-commercial-byd-serie/');
    public static readonly ALL_ESS = [Product.HOME, Product.PRO_HYBRID_10, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW, Product.COMMERCIAL_30, Product.COMMERCIAL_50, Product.COMMERCIAL_BYD];
    // Deprecated ESS
    public static readonly DESS = new Product('FENECON DESS', 'https://fenecon.de/produkte/#Heimspeicher');

    // FEMS-App
    public static readonly FEMS_KEBA = new Product('FEMS-App KEBA Ladestation', 'https://fenecon.de/fems-2-2/fems-app-keba-ladestation/');
    public static readonly FEMS_HARDY_BARTH = new Product('FEMS-App eCharge Hardy Barth Ladestation', 'https://fenecon.de/fems-app-echarge-hardy-barth-ladestation/');
    public static readonly ALL_EVCS = [Product.FEMS_KEBA, Product.FEMS_HARDY_BARTH];

    public static readonly FEMS_NETZDIENLICHE_BELADUNG = new Product('FEMS-App Netzdienliche Beladung', 'https://fenecon.de/fems-2-2/fems-app-netzdienliche-beladung/');
    public static readonly FEMS_MODBUS_TCP_API = new Product('FEMS-App Modbus/TCP-Api', 'https://docs.fenecon.de/de/_/latest/fems/apis.html#_fems_app_modbustcp_api_lesend');
    public static readonly FEMS_REST_JSON_API = new Product('FEMS-App REST/JSON-Api', 'https://docs.fenecon.de/de/_/latest/fems/apis.html#_fems_app_restjson_api_lesend');
    public static readonly FEMS_PV_KACO = new Product('FEMS-App KACO PV-Wechselrichter', 'https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/');
    public static readonly FEMS_PV_SMA = new Product('FEMS-App SMA PV-Wechselrichter', 'https://fenecon.de/fems-2-2/fems-app-sma-pv-wechselrichter/');
    public static readonly FEMS_JANITZA_ZAEHLER = new Product('FEMS-App Janitza Zähler', 'https://fenecon.de/fems-2-2/fems-app-janitza-zaehler-2/');
    public static readonly FEMS_PQ_PLUS_ZAEHLER = new Product('FEMS-App PQ-Plus Zähler', 'https://fenecon.de/fems/'); // TODO Add Product Page
    public static readonly FEMS_HEIZSTAB = new Product('FEMS-App Heizstab', 'https://fenecon.de/fems-2-2/fems-app-heizstab-6-kw/');
    public static readonly FEMS_SCHWELLWERT_STEUERUNG = new Product('FEMS-App Schwellwertsteuerung', 'https://fenecon.de/fems-2-2/fems-app-schwellwert-steuerung/');
    public static readonly FEMS_AWATTAR = new Product('FEMS-App Awattar HOURLY Deutschland', 'https://fenecon.de/fems-2-2/fems-app-awattar-hourly-deutschland-2/');
    public static readonly FEMS_CORRENTLY = new Product('FEMS-App STROMDAO Corrently', 'https://fenecon.de/fems/'); // TODO Add Product Page
    public static readonly FEMS_TIBBER = new Product('FEMS-App Tibber', 'https://fenecon.de/fems/'); // TODO Add Product Page
    public static readonly FEMS_ALL_TIME_OF_USE_TARIFF = [Product.FEMS_AWATTAR, Product.FEMS_CORRENTLY, Product.FEMS_TIBBER];
    public static readonly FEMS_HOCHLASTZEITFENSTER = new Product('FEMS-App Hochlastzeitfenster', 'https://fenecon.de/fems/'); // TODO Add Product Page

    // Industrial
    public static readonly INDUSTRIAL = new Product('FENECON Industrial-Serie', 'https://fenecon.de/industrial/');

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: any) {
    }
}

export class Library {
    // Java
    public static readonly APACHE_FELIX_FRAMEWORK = new Library('Apache Felix Framework', 'org.apache.felix.framework');
    public static readonly APACHE_FELIX_HTTP_JETTY = new Library('Apache Felix HTTP Jetty', 'org.apache.felix.http.jetty');
    public static readonly APACHE_FELIX_INVENTORY = new Library('Apache Felix Inventory', 'org.apache.felix.inventory');
    public static readonly APACHE_FELIX_METATYPE = new Library('Apache Felix MetaType', 'org.apache.felix.metatype');
    public static readonly APACHE_FELIX_SCR = new Library('Apache Felix SCR', 'org.apache.felix.scr');
    public static readonly APACHE_FELIX_WEBCONSOLE = new Library('Apache Felix Webconsole', 'org.apache.felix.webconsole');
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