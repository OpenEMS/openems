import { isTomorrow, subHours } from "date-fns";
import { Websocket } from "src/app/shared/shared";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";

import { Edge } from "../../edge";
import { EdgeConfig } from "../../edgeconfig";
import { GetSchedule } from "./getSchedule";

export class EnergySchedulerV2 extends EdgeConfig.Component {
    public static readonly nowToHistoryInHours: number = 4;

    private _schedule: GetSchedule.Response = GetSchedule.Response.empty;

    constructor(private readonly config: EdgeConfig | null) {
        const component =
            config?.getFirstComponentByFactoryId("Core.Energy") ??
            new EdgeConfig.Component();
        super(
            component.id,
            component.alias,
            component.isEnabled,
            false,
            component.factoryId,
            component.properties,
            component.channels,
        );
    }

    public get schedule(): GetSchedule.Response {
        return this._schedule;
    }

    private static hasRequiredEdgeVersion(edge: Edge) {
        return edge.isVersionAtLeast("2026.6.2");
    }

    public async updateSchedule(edge: Edge, websocket: Websocket) {
        if (
            !this.isEnergySchedulerV2() ||
            !EnergySchedulerV2.hasRequiredEdgeVersion(edge)
        ) {
            this._schedule = GetSchedule.Response.empty;
            return;
        }
        this._schedule =
            (await GetSchedule.getSchedule(
                edge,
                websocket,
                subHours(new Date(), EnergySchedulerV2.nowToHistoryInHours),
            )) ?? GetSchedule.Response.empty;
    }

    public getFutureEnergyTillEndOfDayByChannel(
        channel: keyof GetSchedule.Response["result"]["data"][number]["_sum"],
    ): number | null {
        const schedule = this._schedule.summarizeData24hForChannel(channel);
        const tomorrowIndex =
            schedule.labels.findIndex((el) => isTomorrow(el)) ??
            schedule.prediction.length - 1;
        const futurePowerToday = schedule.prediction.slice(
            0,
            tomorrowIndex - 1,
        );
        const futureEnergyToday = NumberUtils.ceilSafely(
            NumberUtils.divideSafely(
                futurePowerToday.reduce(
                    (acc, curr) => NumberUtils.addSafely(acc, curr),
                    0,
                ),
                4,
            ),
        );
        return futureEnergyToday;
    }

    public getFutureEnergyTillEndOfDayByChannelWithConverter(
        channel: keyof GetSchedule.Response["result"]["data"][number]["_sum"],
        converter: (value: number | null) => number | null,
    ): number | null {
        const schedule = this._schedule.summarizeData24hForChannel(channel);
        const tomorrowIndex =
            schedule.labels.findIndex((el) => isTomorrow(el)) ??
            schedule.prediction.length - 1;
        const futurePowerToday = schedule.prediction.slice(
            0,
            tomorrowIndex - 1,
        );
        const cleanedFuturePowerToday = futurePowerToday.map((power) =>
            converter(power),
        );
        const futureEnergyToday = NumberUtils.ceilSafely(
            NumberUtils.divideSafely(
                cleanedFuturePowerToday.reduce(
                    (acc, curr) => NumberUtils.addSafely(acc, curr),
                    0,
                ),
                4,
            ),
        );
        return futureEnergyToday;
    }

    private isEnergySchedulerV2(): boolean {
        if (this.config === null) {
            return false;
        }
        return (
            this.config
                .getComponentSafely("_energy")
                ?.getPropertyFromComponent("version") ===
            "V2_ENERGY_SCHEDULABLE"
        );
    }
}
