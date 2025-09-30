import { Role } from "src/app/shared/type/role";

export class Changelog {

    public static readonly UI_VERSION = "2025.11.0-SNAPSHOT";

    public static product(...products: Product[]) {
        return PRODUCTS.MAP(product => CHANGELOG.LINK(PRODUCT.NAME, PRODUCT.URL)).join(", ") + ". ";
    }

    public static app(app: App, ...names: string[]) {
        return CHANGELOG.LINK(APP.NAME, APP.URL)
            + (NAMES.LENGTH === 0 ? "" : " (" + NAMES.JOIN(", ") + ")")
            + ": ";
    }

    public static openems(version: string) {
        return "Update auf OpenEMS Version " + version + ". Mehr Details auf " + CHANGELOG.LINK("Github", "https://GITHUB.COM/OpenEMS/openems/releases/tag/" + version);
    }

    public static openemsComponent(openemsComponent: OpenemsComponent, change: string) {
        return { roleIsAtLeast: ROLE.ADMIN, change: CHANGELOG.LINK(OPENEMS_COMPONENT.NAME, OPENEMS_COMPONENT.URL) + ": " + change };
    }

    public static library(...libraries: Library[]) {
        return "Aktualisierung externer Programmbibliotheken: " + LIBRARIES.MAP(library => LIBRARY.NAME).join(", ");
    }

    public static link(title: string, url: string) {
        return "<a class='ion-link' target=\"_blank\" href=\"" + url + "\">" + title + "</a>";
    }
}

export class Product {
    public static readonly OPENEMS_EDGE = new Product("OpenEMS Edge", "https://GITHUB.COM/OpenEMS/openems");
    public static readonly OPENEMS_UI = new Product("OpenEMS Edge", "https://GITHUB.COM/OpenEMS/openems");
    public static readonly OPENEMS_BACKEND = new Product("OpenEMS Edge", "https://GITHUB.COM/OpenEMS/openems");

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: string) {
    }
}

export class App {
    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: string) {
    }
}

export class OpenemsComponent {
    public static readonly PQ_PLUS_ZAEHLER = new OpenemsComponent("PQ-Plus Zähler", "https://GITHUB.COM/OpenEMS/openems/tree/develop/IO.OPENEMS.EDGE.METER.PQPLUS");
    public static readonly SDM630_ZAEHLER = new OpenemsComponent("SDM 630 Zähler", "https://GITHUB.COM/OpenEMS/openems/tree/develop/IO.OPENEMS.EDGE.METER.MICROCARE.SDM630");

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: string) {
    }
}

export class Library {
    // Java
    public static readonly APACHE_FELIX_FILEINSTALL = new Library("Apache Felix File Install", "ORG.APACHE.FELIX..fileinstall");
    public static readonly APACHE_FELIX_FRAMEWORK = new Library("Apache Felix Framework", "ORG.APACHE.FELIX.FRAMEWORK");
    public static readonly APACHE_FELIX_HTTP_JETTY = new Library("Apache Felix HTTP Jetty", "ORG.APACHE.FELIX.HTTP.JETTY");
    public static readonly APACHE_FELIX_INVENTORY = new Library("Apache Felix Inventory", "ORG.APACHE.FELIX.INVENTORY");
    public static readonly APACHE_FELIX_METATYPE = new Library("Apache Felix MetaType", "ORG.APACHE.FELIX.METATYPE");
    public static readonly APACHE_FELIX_SCR = new Library("Apache Felix SCR", "ORG.APACHE.FELIX.SCR");
    public static readonly APACHE_FELIX_WEBCONSOLE = new Library("Apache Felix Webconsole", "ORG.APACHE.FELIX.WEBCONSOLE");
    public static readonly APACHE_FELIX_CONFIGADMIN = new Library("Apache Felix Configuration Admin", "ORG.APACHE.FELIX.CONFIGADMIN");
    public static readonly CHARGETIME_OCPP = new Library("Open Charge Alliance Java OCPP", "EU.CHARGETIME.OCPP"); // https://GITHUB.COM/ChargeTimeEU/Java-OCA-OCPP
    public static readonly ECLIPSE_OSGI = new Library("Eclipse OSGi", "ORG.ECLIPSE.OSGI");
    public static readonly FASTEXCEL = new Library("fastexcel", "fastexcel");
    public static readonly GRADLE = new Library("Gradle", "gradle");
    public static readonly GUAVA = new Library("Guava", "COM.GOOGLE.GUAVA");
    public static readonly GSON = new Library("GSON", "COM.GOOGLE.GSON");
    public static readonly HIKARI_CP = new Library("HikariCP", "hikaricp");
    public static readonly INFLUXDB = new Library("influxdb-java", "influxdb-java");
    public static readonly JNA = new Library("JNA", "NET.JAVA.DEV.JNA");
    public static readonly JAVA_WEBSOCKET = new Library("Java-WebSocket", "ORG.JAVA-websocket");
    public static readonly RETROFIT = new Library("Retrofit", "COM.SQUAREUP.RETROFIT");
    public static readonly MOSHI = new Library("Moshi", "COM.SQUAREUP.MOSHI");
    public static readonly MSGPACK = new Library("MsgPack", "ORG.MSGPACK");
    public static readonly PAX_LOGGING = new Library("PAX Logging", "ORG.OPS4J.PAX.LOGGING");
    public static readonly OSGI_UTIL_FUNCTION = new Library("ORG.OSGI.UTIL.FUNCTION", "ORG.OSGI.UTIL.FUNCTION");
    public static readonly OSGI_UTIL_PROMISE = new Library("ORG.OSGI.UTIL.PROMISE", "ORG.OSGI.UTIL.PROMISE");
    public static readonly OSGI_SERVICE_JDBC = new Library("ORG.OSGI.SERVICE.JDBC", "ORG.OSGI.SERVICE.JDBC");
    public static readonly POSTGRESQL = new Library("Postgresql", "ORG.POSTGRESQL");
    public static readonly SLF4J = new Library("SLF4j", "ORG.SLF4J");
    public static readonly RRD4J = new Library("RRD4j", "ORG.RRD4J");
    public static readonly OKHTTP = new Library("OkHttp", "COM.SQUAREUP.OKHTTP3");
    public static readonly OKIO = new Library("Okio", "COM.SQUAREUP.OKIO");

    // UI
    public static readonly ANGULAR = new Library("Angular", "angular");
    public static readonly D3 = new Library("d3", "d3");
    public static readonly DATE_FNS = new Library("date-fns", "date-fns"); // https://date-FNS.ORG/
    public static readonly IONIC = new Library("Ionic", "ionic");
    public static readonly MYDATEPICKER = new Library("MyDatePicker", "mydatepicker");
    public static readonly NG2_CHARTS = new Library("ng2-charts", "ng2-charts");
    public static readonly NGX_COOKIE_SERVICE = new Library("ngx-cookie-service", "ngx-cookie-service");
    public static readonly NGX_FORMLY = new Library("ngx-formly", "ngx-formly");
    public static readonly NGX_SPINNER = new Library("ngx-spinner", "ngx-spinner");
    public static readonly RXJS = new Library("RxJs", "rxjs");
    public static readonly UUID = new Library("UUID", "uuid");

    // private to disallow creating other instances of this type
    private constructor(public readonly name: string, public readonly url: string) {
    }
}
