import { ChronoUnit, Resolution } from "src/app/edge/history/shared";
import { ChannelAddress } from "src/app/shared/type/channeladdress";

import { QueryHistoricTimeseriesEnergyPerPeriodRequest } from "./queryHistoricTimeseriesEnergyPerPeriodRequest";

describe("QueryHistoricTimeseriesEnergyPerPeriodRequest", () => {
    const resolution: Resolution = { value: 15, unit: ChronoUnit.Type.MINUTES };

    const paramsOf = (request: QueryHistoricTimeseriesEnergyPerPeriodRequest) =>
        request.params as {
            timezone: string;
            fromDate: string;
            toDate: string;
            channels: string[];
            resolution: Resolution;
        };

    it("serializes the given channels and resolution as params", () => {
        const request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(
            new Date(2026, 0, 1),
            new Date(2026, 0, 31),
            [new ChannelAddress("_sum", "ActivePower"), new ChannelAddress("meter0", "ActivePower")],
            resolution,
        );

        expect(request.method).toBe("queryHistoricTimeseriesEnergyPerPeriod");
        expect(paramsOf(request).channels).toEqual(["_sum/ActivePower", "meter0/ActivePower"]);
        expect(paramsOf(request).fromDate).toBe("2026-01-01");
        expect(paramsOf(request).toDate).toBe("2026-01-31");
        expect(paramsOf(request).resolution).toEqual({ value: 15, unit: ChronoUnit.Type.MINUTES });
        expect(typeof paramsOf(request).timezone).toBe("string");
    });

    it("does not leak fromDate, toDate, channels or resolution properties onto the request", () => {
        const request = new QueryHistoricTimeseriesEnergyPerPeriodRequest(
            new Date(2026, 0, 1),
            new Date(2026, 0, 31),
            [new ChannelAddress("_sum", "ActivePower")],
            resolution,
        );
        const serialized = JSON.parse(JSON.stringify(request));

        expect(Object.prototype.hasOwnProperty.call(request, "fromDate")).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(request, "toDate")).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(request, "channels")).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(request, "resolution")).toBe(false);
        expect(serialized.fromDate).toBeUndefined();
        expect(serialized.toDate).toBeUndefined();
        expect(serialized.channels).toBeUndefined();
        expect(serialized.resolution).toBeUndefined();
        expect(serialized.params.channels).toEqual(["_sum/ActivePower"]);
        expect(serialized.params.resolution).toEqual({ value: 15, unit: "Minutes" });
    });
});
