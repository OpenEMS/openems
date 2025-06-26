import { JsonRpcUtils } from "./jsonrpcutils";

describe("JsonRpcUtils", () => {

    const productionActivePowerData = [-0.01, -0.1, -0.49, -0.50, -1, null];
    const expectedOutput = [0, 0, 0, -0.5, -1, null];
    it("#normalizeQueryData", () => {
        expect(JsonRpcUtils.normalizeQueryData(productionActivePowerData)).toEqual(expectedOutput);
    });
});
