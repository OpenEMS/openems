// @ts-strict-ignore
import { ChangeDetectionStrategy, Component, ElementRef, inject, NgZone, OnDestroy } from "@angular/core";
import { DomSanitizer, SafeHtml } from "@angular/platform-browser";
import { startOfToday } from "date-fns";
import { batteryChargingOutline, batteryHalfOutline, carOutline, ellipsisHorizontal, flameOutline, flashOutline, homeOutline, shieldCheckmarkOutline, snowOutline, speedometerOutline, sunnyOutline, } from "ionicons/icons";
import { v4 as uuidv4 } from "uuid";

import { ChronoUnit } from "src/app/edge/history/shared";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { EvcsComponent } from "src/app/shared/components/edge/config-components/evcs/evcsComponent";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Currency, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { LiveDataService } from "../../livedataservice";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

type NodeId = "production" | "grid" | "storage" | "consumption";

/** One drawn connection between two nodes; dots move from `from` to `to`. */
interface Flow {
    pathId: string;
    path: string;
    /** Power in W */
    power: number;
    /** True = dots move against the drawing direction of the path */
    reverse: boolean;
    color: string;
    strokeWidth: number;
    duration: string;
    dots: string[];
}

interface Consumer {
    name: string;
    icon: string;
    power: number | null;
    x: number;
    y: number;
    /** Radius of the consumer node */
    r: number;
    active: boolean;
    flow: Flow | null;
}

/**
 * Energy flow monitor: nodes for production, grid, storage and consumption, connected by lines whose width and dot
 * speed follow the power that flows between them. Consumers (EVCS, consumption meters) are attached to the consumption
 * node.
 */
@Component({
    selector: "oe-energyflow",
    templateUrl: "./flow.component.html",
    styleUrls: ["./flow.component.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class EnergyflowComponent extends AbstractFlatWidget implements OnDestroy {
    /**
     * Node centers in the SVG viewBox. "wide": 680 x 460 incl. consumer column on the right; "compact" (narrow screens,
     * e.g. smartphones): 400 x 440, consumers are listed below the graphic.
     */
    protected static readonly LAYOUTS: Record<"wide" | "compact", Record<NodeId, { x: number; y: number }>> = {
        wide: {
            production: { x: 300, y: 70 },
            grid: { x: 90, y: 220 },
            consumption: { x: 470, y: 220 },
            storage: { x: 300, y: 370 },
        },
        compact: {
            production: { x: 200, y: 62 },
            grid: { x: 62, y: 215 },
            consumption: { x: 338, y: 215 },
            storage: { x: 200, y: 365 },
        },
    };
    /**
     * Icons used inside the SVG. They are inlined as SVG markup instead of <ion-icon> in a <foreignObject>, because
     * Safari/iOS positions web components in a scaled foreignObject wrongly.
     */
    private static readonly ICONS: Record<string, string> = {
        "sunny-outline": sunnyOutline,
        "flash-outline": flashOutline,
        "home-outline": homeOutline,
        "battery-charging-outline": batteryChargingOutline,
        "battery-half-outline": batteryHalfOutline,
        "car-outline": carOutline,
        "flame-outline": flameOutline,
        "snow-outline": snowOutline,
        "shield-checkmark-outline": shieldCheckmarkOutline,
        "speedometer-outline": speedometerOutline,
        "ellipsis-horizontal": ellipsisHorizontal,
    };
    /** Flows below this power (W) are drawn as idle */
    private static readonly MIN_POWER = 30;
    private static readonly CONSUMER_X = 625;
    /** Rows in the consumer column; with more consumers, those of one category share a row */
    private static readonly MAX_CONSUMER_ROWS = 6;
    private static readonly MAX_CONSUMERS_PER_ROW = 3;
    /** Horizontal distance of consumers in one row */
    private static readonly CONSUMER_SPACING = 66;
    /** Below this component width (px) the compact layout is used */
    private static readonly COMPACT_WIDTH = 560;

    protected NODES = EnergyflowComponent.LAYOUTS.wide;
    protected compact: boolean = false;
    protected readonly RADIUS = 50;
    protected readonly SOC_CIRCUMFERENCE = 2 * Math.PI * 50;

    protected readonly uid = uuidv4().substring(0, 8);

    protected production: number | null = null;
    protected gridBuy: number = 0;
    protected gridSell: number = 0;
    protected storageCharge: number = 0;
    protected storageDischarge: number = 0;
    protected consumption: number | null = null;
    protected soc: number | null = null;
    protected socDash: string = "0 999";
    protected storageEstimate: string | null = null;
    protected gridPrice: string | null = null;
    protected hasStorage: boolean = false;

    protected flows: Flow[] = [];
    protected idlePaths: string[] = [];
    protected consumers: Consumer[] = [];

    protected today: {
        production: number | null;
        consumption: number | null;
        gridBuy: number | null;
        gridSell: number | null;
        autarchy: number | null;
    } | null = null;
    /** Grid cost of today: sum over all quarter hours of grid buy energy x price of that quarter hour */
    protected todayCost: string | null = null;

    protected readonly formatPower = Utils.CONVERT_WATT_TO_KILOWATT;
    protected readonly formatEnergy = EnergyflowComponent.formatEnergyValue;

    private evcss: EvcsComponent[] = [];
    private meters: EdgeConfig.Component[] = [];
    private currencyLabel: Currency.Label | null = null;
    private currencyCode: string = "EUR";
    private todayTimer: ReturnType<typeof setInterval> | null = null;
    private resizeObserver: ResizeObserver | null = null;
    private lastCurrentData: CurrentData | null = null;
    private host = inject(ElementRef);
    private sanitizer = inject(DomSanitizer);
    private iconCache = new Map<string, SafeHtml>();
    private zone = inject(NgZone);

    /** ViewBox of the SVG for the current layout */
    protected get viewBox(): string {
        if (this.compact) {
            return "0 0 400 440";
        }
        if (this.consumers.length === 0) {
            return "0 0 560 460";
        }
        // wider, when consumers share a row
        const right = Math.max(...this.consumers.map((consumer) => consumer.x + consumer.r + 29));
        return "0 0 " + Math.max(680, Math.ceil(right)) + " 460";
    }

    private static formatEnergyValue(value: number | null): string {
        return value == null ? "-" : Utils.CONVERT_TO_KILO_WATTHOURS(value);
    }

    private static strokeWidth(power: number): number {
        return Math.round((2 + Math.min(power / 1000, 8)) * 10) / 10;
    }

    /** More power = faster dots; quantized so the animation does not restart on every small change */
    private static duration(power: number): string {
        const kw = power / 1000;
        const seconds = Math.max(0.8, Math.min(5, 5 - kw * 0.6));
        return Math.round(seconds * 2) / 2 + "s";
    }

    private static consumerIcon(name: string, isEvcs: boolean): string {
        if (isEvcs) {
            return "car-outline";
        }
        const n = (name ?? "").toLowerCase();
        if (n.includes("heiz") || n.includes("heat") || n.includes("wärme") || n.includes("waerme")) {
            return "flame-outline";
        }
        if (n.includes("klima") || n.includes("air") || n.includes("cool")) {
            return "snow-outline";
        }
        if (n.includes("notstrom") || n.includes("emergency") || n.includes("backup")) {
            return "shield-checkmark-outline";
        }
        return "speedometer-outline";
    }

    public override ngOnDestroy(): void {
        if (this.todayTimer != null) {
            clearInterval(this.todayTimer);
        }
        this.resizeObserver?.disconnect();
        super.ngOnDestroy();
    }

    /**
     * Inline SVG markup of an ionicon for use inside the SVG (`<svg viewBox="0 0 512 512" fill="currentColor"
     * [innerHTML]>`). Ionicons style their outlines via CSS classes; these are converted to attributes here. The markup
     * comes from the bundled ionicons package only.
     */
    protected icon(name: string): SafeHtml {
        let markup = this.iconCache.get(name);
        if (markup == null) {
            const data = EnergyflowComponent.ICONS[name] ?? EnergyflowComponent.ICONS["speedometer-outline"];
            const raw = data.substring(data.indexOf(",") + 1);
            let svg = raw;
            try {
                svg = decodeURIComponent(raw);
            } catch {
                // not URI-encoded
            }
            const inner = svg
                .substring(svg.indexOf(">") + 1, svg.lastIndexOf("</svg>"))
                .replace(
                    /class='([^']*)'/g,
                    (_, classes: string) =>
                        (classes.includes("ionicon-fill-none") ? " fill='none' stroke='currentColor'" : "") +
                        (classes.includes("ionicon-stroke-width") ? " stroke-width='32'" : ""),
                );
            markup = this.sanitizer.bypassSecurityTrustHtml(inner);
            this.iconCache.set(name, markup);
        }
        return markup;
    }

    protected override afterIsInitialized(): void {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        const meta = this.config?.getComponent("_meta");
        const currency: string = meta ? this.config.getPropertyFromComponent<string>(meta, "currency") : null;
        this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
        this.currencyCode = currency ?? "EUR";

        this.updateToday();
        this.todayTimer = setInterval(() => this.updateToday(), 5 * 60 * 1000);

        // switch between wide and compact layout depending on the available width
        this.resizeObserver = new ResizeObserver((entries) => {
            const width = entries[0]?.contentRect.width ?? 0;
            const compact = width > 0 && width < EnergyflowComponent.COMPACT_WIDTH;
            if (compact !== this.compact) {
                this.zone.run(() => {
                    this.compact = compact;
                    this.NODES = EnergyflowComponent.LAYOUTS[compact ? "compact" : "wide"];
                    if (this.lastCurrentData != null) {
                        this.onCurrentData(this.lastCurrentData);
                    }
                });
            }
        });
        this.resizeObserver.observe(this.host.nativeElement);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const channels: ChannelAddress[] = [
            new ChannelAddress("_sum", "ProductionActivePower"),
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "GridBuyPrice"),
            new ChannelAddress("_sum", "ConsumptionActivePower"),
            new ChannelAddress("_sum", "EssSoc"),
            new ChannelAddress("_sum", "EssCapacity"),
            new ChannelAddress("_sum", "EssActivePower"),
            new ChannelAddress("_sum", "EssDischargePower"),
        ];

        // same consumer selection as the consumption widget
        this.meters =
            this.config
                ?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter((component) => {
                    const natureIds = this.config?.getNatureIdsByFactoryId(component.factoryId);
                    const isEvcs = natureIds.includes("io.openems.edge.evcs.api.Evcs");
                    return component.isEnabled && this.config?.isTypeConsumptionMetered(component) && isEvcs === false;
                }) ?? [];
        for (const meter of this.meters) {
            channels.push(new ChannelAddress(meter.id, "ActivePower"));
        }

        this.evcss = EvcsComponent.getComponents(this.config, this.edge) ?? [];
        for (const evcs of this.evcss) {
            channels.push(evcs.powerChannel);
        }
        return channels;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.lastCurrentData = currentData;
        const c = currentData.allComponents;

        this.production = c["_sum/ProductionActivePower"] != null ? Math.max(0, c["_sum/ProductionActivePower"]) : null;
        const grid: number = c["_sum/GridActivePower"] ?? 0;
        this.gridBuy = Math.max(0, grid);
        this.gridSell = Math.max(0, -grid);
        // EssDischargePower includes DC-coupled production (hybrid inverters)
        const ess: number = c["_sum/EssDischargePower"] ?? c["_sum/EssActivePower"] ?? 0;
        this.storageDischarge = Math.max(0, ess);
        this.storageCharge = Math.max(0, -ess);
        this.consumption =
            c["_sum/ConsumptionActivePower"] != null ? Math.max(0, c["_sum/ConsumptionActivePower"]) : null;

        this.soc = c["_sum/EssSoc"] ?? null;
        this.hasStorage = this.soc != null;
        this.socDash =
            this.soc != null
                ? `${((this.SOC_CIRCUMFERENCE * Math.min(100, Math.max(0, this.soc))) / 100).toFixed(1)} 999`
                : "0 999";
        this.storageEstimate = this.calculateStorageEstimate(c["_sum/EssCapacity"]);

        const price: number | null = c["_sum/GridBuyPrice"] ?? null;
        this.gridPrice =
            price != null && this.currencyLabel != null
                ? Utils.CONVERT_PRICE_TO_CENT_PER_KWH(0, this.currencyLabel)(price)
                : null;

        this.updateFlows();
        this.updateConsumers(c);
    }

    /** Undirected base path between two main nodes, relative to the current layout */
    private path(a: NodeId, b: NodeId): string {
        const n = this.NODES;
        const A = n[a];
        const B = n[b];
        const r = (v: number) => Math.round(v);
        switch (a + "-" + b) {
            case "production-consumption":
            case "production-grid":
            case "storage-consumption":
                // leave A vertically, arrive at B horizontally
                return `M${A.x},${A.y} C${A.x},${r(A.y + (B.y - A.y) * 0.73)} ${r(A.x + (B.x - A.x) * 0.47)},${B.y} ${B.x},${B.y}`;
            case "production-storage":
                return `M${A.x},${A.y} L${B.x},${B.y}`;
            case "grid-consumption":
                return `M${A.x},${A.y} C${r(A.x + (B.x - A.x) * 0.29)},${A.y + 42} ${r(A.x + (B.x - A.x) * 0.71)},${B.y + 42} ${B.x},${B.y}`;
            case "grid-storage":
                return `M${A.x},${A.y} C${r(A.x + (B.x - A.x) * 0.62)},${A.y} ${B.x},${r(A.y + (B.y - A.y) * 0.27)} ${B.x},${B.y}`;
        }
        return "";
    }

    /** Splits the measured powers into flows between the nodes (production is used first) */
    private updateFlows(): void {
        const p = this.production ?? 0;
        const consumption = this.consumption ?? 0;

        const pvToHome = Math.min(p, consumption);
        let pvRest = p - pvToHome;
        const pvToStorage = Math.min(pvRest, this.storageCharge);
        pvRest -= pvToStorage;
        const pvToGrid = Math.min(pvRest, this.gridSell);

        const storageToHome = Math.min(this.storageDischarge, Math.max(0, consumption - pvToHome));
        const storageToGrid = Math.min(this.storageDischarge - storageToHome, Math.max(0, this.gridSell - pvToGrid));
        const gridToHome = Math.max(0, consumption - pvToHome - storageToHome);
        const gridToStorage = Math.max(0, this.storageCharge - pvToStorage);

        const candidates: { a: NodeId; b: NodeId; power: number; reverse: boolean; color: string }[] = [
            { a: "production", b: "consumption", power: pvToHome, reverse: false, color: "var(--ef-production)" },
            { a: "production", b: "storage", power: pvToStorage, reverse: false, color: "var(--ef-production)" },
            { a: "production", b: "grid", power: pvToGrid, reverse: false, color: "var(--ef-production)" },
            { a: "storage", b: "consumption", power: storageToHome, reverse: false, color: "var(--ef-storage)" },
            { a: "grid", b: "storage", power: storageToGrid, reverse: true, color: "var(--ef-storage)" },
            { a: "grid", b: "consumption", power: gridToHome, reverse: false, color: "var(--ef-grid)" },
            { a: "grid", b: "storage", power: gridToStorage, reverse: false, color: "var(--ef-grid)" },
        ];

        const flows: Flow[] = [];
        const activePaths = new Set<string>();
        for (const f of candidates) {
            if (f.power < EnergyflowComponent.MIN_POWER) {
                continue;
            }
            if (!this.hasStorage && (f.a === "storage" || f.b === "storage")) {
                continue;
            }
            const pathKey = f.a + "-" + f.b;
            activePaths.add(pathKey);
            flows.push(
                this.createFlow(pathKey + (f.reverse ? "-r" : ""), this.path(f.a, f.b), f.power, f.reverse, f.color),
            );
        }
        this.flows = flows;

        // possible but currently unused connections are drawn as dashed lines
        const allPaths: [NodeId, NodeId][] = [
            ["production", "consumption"],
            ["production", "grid"],
            ["grid", "consumption"],
            ...(this.hasStorage
                ? ([
                      ["production", "storage"],
                      ["storage", "consumption"],
                      ["grid", "storage"],
                  ] as [NodeId, NodeId][])
                : []),
        ];
        this.idlePaths = allPaths.filter(([a, b]) => !activePaths.has(a + "-" + b)).map(([a, b]) => this.path(a, b));
    }

    private updateConsumers(c: { [id: string]: any }): void {
        const list: { name: string; icon: string; power: number | null }[] = [];
        for (const evcs of this.evcss) {
            list.push({
                name: evcs.alias,
                icon: EnergyflowComponent.consumerIcon(evcs.alias, true),
                power: c[evcs.powerChannel.toString()] ?? null,
            });
        }
        for (const meter of this.meters) {
            list.push({
                name: meter.alias,
                icon: EnergyflowComponent.consumerIcon(meter.alias, false),
                power: c[meter.id + "/ActivePower"] ?? null,
            });
        }
        if (list.length === 0) {
            this.consumers = [];
            return;
        }

        const known = list.reduce((sum, item) => sum + Math.max(0, item.power ?? 0), 0);
        const other = {
            name: this.translate.instant("GENERAL.OTHER_CONSUMPTION"),
            icon: "ellipsis-horizontal",
            power: Math.max(0, (this.consumption ?? 0) - known),
        };

        // rows of consumers: one consumer per row; with many consumers one row per category,
        // its consumers side by side (each still with its own value)
        const all = [...list, other];
        const rows: (typeof all)[] = [];
        if (all.length <= EnergyflowComponent.MAX_CONSUMER_ROWS || this.compact) {
            rows.push(...all.map((item) => [item]));
        } else {
            for (const icon of new Set(list.map((item) => item.icon))) {
                const members = list.filter((item) => item.icon === icon);
                for (let i = 0; i < members.length; i += EnergyflowComponent.MAX_CONSUMERS_PER_ROW) {
                    rows.push(members.slice(i, i + EnergyflowComponent.MAX_CONSUMERS_PER_ROW));
                }
            }
            rows.push([other]);
        }
        const visibleRows =
            !this.compact && rows.length > EnergyflowComponent.MAX_CONSUMER_ROWS
                ? [...rows.slice(0, EnergyflowComponent.MAX_CONSUMER_ROWS - 1), rows[rows.length - 1]]
                : rows;
        const small = visibleRows.some((row) => row.length > 1);
        const radius = small ? 22 : 26;
        const maxName = small ? 9 : 16;

        // spread rows vertically around the consumption node
        const top = 50;
        const bottom = 390;
        const step = visibleRows.length > 1 ? (bottom - top) / (visibleRows.length - 1) : 0;
        const home = this.NODES.consumption;
        const consumers: Consumer[] = [];
        visibleRows.forEach((row, r) => {
            const y = visibleRows.length > 1 ? top + r * step : home.y;
            row.forEach((item, i) => {
                const x =
                    (small ? EnergyflowComponent.CONSUMER_X - 25 : EnergyflowComponent.CONSUMER_X) +
                    i * EnergyflowComponent.CONSUMER_SPACING;
                // the line to a consumer carries its own power and that of the consumers to its right
                const power = row.slice(i).reduce((sum, member) => sum + Math.max(0, member.power ?? 0), 0);
                const path =
                    i === 0
                        ? `M${home.x},${home.y} C${home.x + 80},${home.y} ${x - 60},${y} ${x},${y}`
                        : `M${x - EnergyflowComponent.CONSUMER_SPACING},${y} L${x},${y}`;
                const name = item.name ?? "";
                const key = "consumer-" + r + "-" + i;
                consumers.push({
                    name: name.length > maxName ? name.substring(0, maxName - 1) + "…" : name,
                    icon: item.icon,
                    power: item.power,
                    x: x,
                    y: y,
                    r: radius,
                    active: Math.max(0, item.power ?? 0) >= EnergyflowComponent.MIN_POWER,
                    flow:
                        power >= EnergyflowComponent.MIN_POWER
                            ? this.createFlow(key, path, power, false, "var(--ef-consumption)")
                            : { ...this.createFlow(key, path, 0, false, "var(--ef-idle)"), dots: [] },
                });
            });
        });
        this.consumers = consumers;
    }

    private createFlow(key: string, path: string, power: number, reverse: boolean, color: string): Flow {
        const duration = EnergyflowComponent.duration(power);
        const seconds = parseFloat(duration);
        const dotCount = 3;
        return {
            pathId: "oe-ef-" + this.uid + "-" + key,
            path: path,
            power: power,
            reverse: reverse,
            color: color,
            strokeWidth: EnergyflowComponent.strokeWidth(power),
            duration: duration,
            dots: Array.from({ length: dotCount }, (_, i) => (-(seconds / dotCount) * i).toFixed(2) + "s"),
        };
    }

    /** Estimated time until the storage is full (charging) or empty (discharging) */
    private calculateStorageEstimate(capacity: number | null): string | null {
        if (capacity == null || capacity <= 0 || this.soc == null) {
            return null;
        }
        let hours: number | null = null;
        let key: string | null = null;
        if (this.storageCharge >= 100 && this.soc < 100) {
            hours = (capacity * (100 - this.soc)) / 100 / this.storageCharge;
            key = "EDGE.INDEX.ENERGYFLOW.FULL_AT";
        } else if (this.storageDischarge >= 100 && this.soc > 0) {
            hours = (capacity * this.soc) / 100 / this.storageDischarge;
            key = "EDGE.INDEX.ENERGYFLOW.EMPTY_AT";
        }
        if (hours == null || hours < 0.05 || hours > 48) {
            return null;
        }
        const at = new Date(Date.now() + hours * 3600 * 1000);
        const time = at.toLocaleTimeString(Language.getCurrentLanguage().i18nLocaleKey, {
            hour: "2-digit",
            minute: "2-digit",
        });
        return this.translate.instant(key, { value: time });
    }

    /**
     * Grid cost of today: the metered grid buy energy of today is distributed over the quarter hours in proportion to
     * the average grid buy power of each quarter hour (the backend does not provide energy per 15 min), then multiplied
     * with the grid buy price of that quarter hour and summed up. Works with dynamic tariffs. Hidden if no prices are
     * available.
     */
    private updateTodayCost(gridBuyToday: number | null): Promise<void> {
        if (this.edge == null || gridBuyToday == null) {
            this.todayCost = null;
            return Promise.resolve();
        }
        const QUARTER = 15 * 60 * 1000;
        const from = startOfToday();
        const request = new QueryHistoricTimeseriesDataRequest(
            new Date(from),
            new Date(from),
            [new ChannelAddress("_sum", "GridActivePower"), new ChannelAddress("_sum", "GridBuyPrice")],
            { value: 15, unit: ChronoUnit.Type.MINUTES },
        );

        return Promise.all([
            this.edge.sendRequest<QueryHistoricTimeseriesDataResponse>(this.websocket, request),
            GetSchedule.getSchedule(this.edge, this.websocket, new Date(from)).catch(() => null),
        ])
            .then(([data, schedule]) => {
                // fallback prices from the energy schedule, by start of quarter hour
                const schedulePrices = new Map<number, number>();
                for (const e of schedule?.data ?? []) {
                    if (e._sum.GridBuyPrice != null) {
                        schedulePrices.set(Math.floor(e.timestamp.getTime() / QUARTER) * QUARTER, e._sum.GridBuyPrice);
                    }
                }
                const power: (number | null)[] = data.result.data["_sum/GridActivePower"] ?? [];
                const prices: (number | null)[] = data.result.data["_sum/GridBuyPrice"] ?? [];
                const weights = power.map((p) => Math.max(0, p ?? 0));
                const weightSum = weights.reduce((a, b) => a + b, 0);
                if (weightSum <= 0) {
                    this.todayCost = gridBuyToday > 0 ? null : this.formatCurrency(0);
                    return;
                }

                let cost = 0;
                let hasPrice = false;
                let lastPrice: number | null = null;
                data.result.timestamps.forEach((timestamp, i) => {
                    const time = Math.floor(new Date(timestamp).getTime() / QUARTER) * QUARTER;
                    const price = prices[i] ?? schedulePrices.get(time) ?? lastPrice;
                    if (price == null) {
                        return;
                    }
                    lastPrice = price;
                    hasPrice = true;
                    const wh = (weights[i] / weightSum) * gridBuyToday;
                    // Wh / 1000 = kWh; x currency/MWh / 1000 = currency
                    cost += ((wh / 1000) * price) / 1000;
                });
                this.todayCost = hasPrice ? this.formatCurrency(cost) : null;
            })
            .catch(() => {
                this.todayCost = null;
            });
    }

    private formatCurrency(value: number): string {
        try {
            return new Intl.NumberFormat(Language.getCurrentLanguage().i18nLocaleKey, {
                style: "currency",
                currency: this.currencyCode,
            }).format(value);
        } catch {
            return value.toFixed(2);
        }
    }

    /** Energy values of the current day */
    private updateToday(): Promise<void> {
        const now = new Date();
        return this.service
            .queryEnergy(new Date(now), new Date(now), [
                new ChannelAddress("_sum", "ProductionActiveEnergy"),
                new ChannelAddress("_sum", "GridBuyActiveEnergy"),
                new ChannelAddress("_sum", "GridSellActiveEnergy"),
                new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
            ])
            .then((response) => {
                const d = response.result.data;
                const consumption = d["_sum/ConsumptionActiveEnergy"];
                const gridBuy = d["_sum/GridBuyActiveEnergy"];
                this.today = {
                    production: d["_sum/ProductionActiveEnergy"] ?? null,
                    consumption: consumption ?? null,
                    gridBuy: gridBuy ?? null,
                    gridSell: d["_sum/GridSellActiveEnergy"] ?? null,
                    autarchy: Utils.calculateAutarchy(gridBuy, consumption),
                };
                return this.updateTodayCost(gridBuy ?? null);
            })
            .catch(() => {
                // keep the last values; the daily summary is optional
            });
    }
}
