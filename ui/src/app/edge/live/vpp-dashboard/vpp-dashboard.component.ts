import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from "@angular/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { LiveDataService } from "../livedataservice";
import { VppAsset, VppAssetStatus, VppDashboardModel } from "./vpp-dashboard.model";

@Component({
    selector: "oe-vpp-dashboard",
    templateUrl: "./vpp-dashboard.component.html",
    styleUrls: ["./vpp-dashboard.component.scss"],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class VppDashboardComponent implements OnInit, OnDestroy {
    @Input({ required: true }) public edge!: Edge;
    @Input({ required: true }) public config!: EdgeConfig;

    protected now = Date.now();
    private freshnessTimer: ReturnType<typeof setInterval> | null = null;

    constructor(private readonly dataService: DataService) {}

    protected get state() {
        return VppDashboardModel.build(
            this.config,
            this.edge,
            this.dataService.currentValue().allComponents,
            this.dataService.lastUpdated(),
            this.now,
        );
    }

    public ngOnInit(): void {
        const channels = VppDashboardModel.getChannelAddresses(this.config, this.edge);
        this.dataService.subscribeChannels(channels, this.edge, "vpp-dashboard");
        this.freshnessTimer = setInterval(() => {
            this.now = Date.now();
        }, 5_000);
    }

    public ngOnDestroy(): void {
        if (this.freshnessTimer != null) {
            clearInterval(this.freshnessTimer);
        }
    }

    protected getStatusKey(status: VppAssetStatus): string {
        return "VPP.STATUS." + status;
    }

    protected getDirectionKey(asset: VppAsset): string {
        if (asset.power == null || asset.power === 0) {
            return "VPP.DIRECTION.IDLE";
        }

        switch (asset.type) {
            case "battery":
                return asset.power > 0 ? "VPP.DIRECTION.DISCHARGING" : "VPP.DIRECTION.CHARGING";
            case "grid":
                return asset.power > 0 ? "VPP.DIRECTION.IMPORTING" : "VPP.DIRECTION.EXPORTING";
            case "ev":
                return asset.power > 0 ? "VPP.DIRECTION.CHARGING" : "VPP.DIRECTION.POWER_FLOW";
            case "pv":
                return "VPP.DIRECTION.GENERATING";
        }
    }

    protected formatPower(power: number): number {
        return Math.abs(power) / 1_000;
    }

    protected getSolplanetStatusKey(): string {
        return "VPP.INTEGRATION." + this.state.solplanetStatus;
    }

    protected getEdgeStatusKey(): string {
        return "VPP.STATUS." + this.state.edgeStatus;
    }

    protected getTelemetryStatusKey(): string {
        return "VPP.STATUS." + this.state.telemetryStatus;
    }

    protected isStatusCurrent(status: VppAssetStatus): boolean {
        return status === "AVAILABLE" || status === "UNKNOWN";
    }
}
