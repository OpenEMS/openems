import { ChannelAddress } from "src/app/shared/type/channeladdress";

import { QueryHistoricTimeseriesEnergyRequest } from "./queryHistoricTimeseriesEnergyRequest";

describe("QueryHistoricTimeseriesEnergyRequest", () => {
    const paramsOf = (request: QueryHistoricTimeseriesEnergyRequest) =>
        request.params as {
            timezone: string;
            fromDate: string;
            toDate: string;
            channels: string[];
        };

    it("serializes the given channels as string array in params", () => {
        const request = new QueryHistoricTimeseriesEnergyRequest(new Date(2026, 0, 1), new Date(2026, 0, 31), [
            new ChannelAddress("_sum", "ActivePower"),
            new ChannelAddress("meter0", "ActivePower"),
        ]);

        expect(request.method).toBe("queryHistoricTimeseriesEnergy");
        expect(paramsOf(request).channels).toEqual(["_sum/ActivePower", "meter0/ActivePower"]);
        expect(paramsOf(request).fromDate).toBe("2026-01-01");
        expect(paramsOf(request).toDate).toBe("2026-01-31");
        expect(typeof paramsOf(request).timezone).toBe("string");
    });

    it("does not leak fromDate, toDate or channels properties onto the request", () => {
        const request = new QueryHistoricTimeseriesEnergyRequest(new Date(2026, 0, 1), new Date(2026, 0, 31), [
            new ChannelAddress("_sum", "ActivePower"),
        ]);
        const serialized = JSON.parse(JSON.stringify(request));

        expect(Object.prototype.hasOwnProperty.call(request, "fromDate")).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(request, "toDate")).toBe(false);
        expect(Object.prototype.hasOwnProperty.call(request, "channels")).toBe(false);
        expect(serialized.fromDate).toBeUndefined();
        expect(serialized.toDate).toBeUndefined();
        expect(serialized.channels).toBeUndefined();
        expect(serialized.params.channels).toEqual(["_sum/ActivePower"]);
    });
});
