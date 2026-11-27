import { isTomorrow, subHours } from "date-fns";
import { Websocket } from "src/app/shared/shared";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";

import { Edge } from "../../edge";
import { EdgeConfig } from "../../edgeconfig";
import { GetSchedule } from "./getSchedule";

export class EnergyScheduler extends EdgeConfig.Component {
    public static readonly nowToHistoryInHours: number = 4;

    private _schedule: GetSchedule.Response = GetSchedule.Response.empty;

    constructor(private readonly config: EdgeConfig | null) {
        const component = config?.getFirstComponentByFactoryId("Core.Energy") ?? new EdgeConfig.Component();
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

    public static hasRequiredEdgeVersion(edge: Edge) {
        return edge.isVersionAtLeast("2026.6.2");
    }

    /**
     * Gets the index of the first schedule entry that is tomorrow.
     *
     * @param schedule The summarized schedule data
     * @returns The index of the first entry tomorrow, or null if the schedule contains no data for tomorrow, e.g. when
     *   it was fetched in the early morning before the schedule window crosses midnight
     */
    private static getTomorrowIndex(
        schedule: ReturnType<GetSchedule.Response["summarizeData24hForChannel"]>,
    ): number | null {
        const tomorrowIndex = schedule.labels.findIndex((el) => isTomorrow(el));

        return tomorrowIndex === -1 ? null : tomorrowIndex;
    }

    public async updateSchedule(
        edge: Edge,
        websocket: Websocket,
        fromDate: Date = subHours(new Date(), EnergyScheduler.nowToHistoryInHours),
    ): Promise<void> {
        if (this.isEnergySchedulerV1() || !EnergyScheduler.hasRequiredEdgeVersion(edge)) {
            this._schedule = GetSchedule.Response.empty;
            return;
        }
        this._schedule = (await GetSchedule.getSchedule(edge, websocket, fromDate)) ?? GetSchedule.Response.empty;
    }

    public getFutureEnergyTillEndOfDayByChannel(
        channel: keyof GetSchedule.Response["result"]["data"][number]["_sum"],
    ): number | null {
        const schedule = this._schedule.summarizeData24hForChannel(channel);
        const tomorrowIndex = EnergyScheduler.getTomorrowIndex(schedule);

        if (tomorrowIndex === null) {
            return null;
        }
        const futurePowerToday = schedule.prediction.slice(0, tomorrowIndex - 1);
        const futureEnergyToday = NumberUtils.ceilSafely(
            NumberUtils.divideSafely(
                futurePowerToday.reduce((acc, curr) => NumberUtils.addSafely(acc, curr), 0),
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
        const tomorrowIndex = EnergyScheduler.getTomorrowIndex(schedule);

        if (tomorrowIndex === null) {
            return null;
        }
        const futurePowerToday = schedule.prediction.slice(0, tomorrowIndex - 1);
        const cleanedFuturePowerToday = futurePowerToday.map((power) => converter(power));
        const futureEnergyToday = NumberUtils.ceilSafely(
            NumberUtils.divideSafely(
                cleanedFuturePowerToday.reduce((acc, curr) => NumberUtils.addSafely(acc, curr), 0),
                4,
            ),
        );
        return futureEnergyToday;
    }

    public isEnergySchedulerV1(): boolean {
        if (this.config === null) {
            return false;
        }
        return this.config.getComponentSafely("_energy")?.getPropertyFromComponent("version") === "V1_ESS_ONLY";
    }
}
