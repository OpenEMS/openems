import { FormControl, FormGroup } from "@angular/forms";
import { ChartDataset } from "chart.js";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "src/app/shared/shared";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { DummyConfig } from "../../edge/edgeconfig.spec";
import { OeFormlyField, OeFormlyView } from "../oe-formly-component";
import { OeTester } from "./common";
import { OeChartTester, OeFormlyViewTester } from "./tester";
import { TestContext, TestingUtils } from "./utils.spec";

describe("OeFormlyViewTester range form values", () => {
    const view: OeFormlyView = {
        title: "Range test",
        lines: [
            {
                type: "range-button-from-form-control-line",
                controlName: "limit",
                properties: { tickMin: 0, tickMax: 2000 },
            },
        ],
    };

    function expectRangeValue(form: FormGroup | null, expectedValue: number | null): void {
        expect(OeFormlyViewTester.apply(view, {}, form)).toEqual({
            title: "Range test",
            lines: [
                {
                    type: "range-button-from-form-control-line",
                    controlName: "limit",
                    properties: { tickMin: 0, tickMax: 2000 },
                    expectedValue,
                },
            ],
        });
    }

    it("reads an existing form control value", () => {
        expectRangeValue(new FormGroup({ limit: new FormControl(1000) }), 1000);
    });

    it("preserves a zero form control value", () => {
        expectRangeValue(new FormGroup({ limit: new FormControl(0) }), 0);
    });

    it("returns a null value when the form is missing", () => {
        expectRangeValue(null, null);
    });

    it("returns a null value when the control is missing", () => {
        expectRangeValue(new FormGroup({}), null);
    });
});

describe("OeFormlyViewTester field filtering", () => {
    it("keeps supported fields", () => {
        expect(OeFormlyViewTester.apply({ title: "Test", lines: [{ type: "horizontal-line" }] }, {})).toEqual({
            title: "Test",
            lines: [{ type: "horizontal-line" }],
        });
    });

    it("omits top-level fields rejected by their filter", () => {
        expect(
            OeFormlyViewTester.apply(
                {
                    title: "Test",
                    lines: [{ type: "item", channel: "meter0/ActivePower", filter: () => false }],
                },
                {},
            ),
        ).toEqual({ title: "Test", lines: [] });
    });

    it("continues to omit unsupported fields", () => {
        expect(
            OeFormlyViewTester.apply(
                {
                    title: "Test",
                    lines: [{ type: "value-from-form-control-line", name: "Limit", controlName: "limit" }],
                },
                {},
            ),
        ).toEqual({ title: "Test", lines: [] });
    });

    it("preserves null entries for filtered nested children", () => {
        expect(
            OeFormlyViewTester.apply(
                {
                    title: "Test",
                    lines: [
                        {
                            type: "children-line",
                            name: "Children",
                            channel: new ChannelAddress("meter0", "ActivePower"),
                            filter: () => true,
                            children: [
                                { type: "item", channel: "meter0/ActivePower", filter: () => false },
                                { type: "item", channel: "meter0/ActivePower" },
                            ],
                        },
                    ],
                },
                { "meter0/ActivePower": 1000 },
            ),
        ).toEqual({
            title: "Test",
            lines: [{ type: "children-line", name: "Children", children: [null, { type: "item", value: "1000" }] }],
        });
    });
});

describe("OeFormlyViewTester missing field data", () => {
    it("preserves undefined optional names", () => {
        expect(
            OeFormlyViewTester.apply(
                {
                    title: "Test",
                    lines: [
                        { type: "info-line" },
                        { type: "buttons-from-form-control-line", controlName: "mode", buttons: [] },
                        { type: "value-from-channels-line", channelsToSubscribe: [], value: () => "value" },
                    ],
                },
                {},
            ),
        ).toEqual({
            title: "Test",
            lines: [
                { type: "info-line", name: undefined },
                { type: "buttons-from-form-control-line", name: undefined, controlName: "mode", buttons: [] },
                { type: "value-from-channels-line", name: undefined, value: "value" },
            ],
        });
    });

    it("preserves null items and omits missing channel-line values", () => {
        expect(
            OeFormlyViewTester.apply(
                {
                    title: "Test",
                    lines: [
                        { type: "item", channel: "meter0/ActivePower" },
                        { type: "channel-line", name: "Power", channel: "meter0/ActivePower" },
                    ],
                },
                {},
            ),
        ).toEqual({
            title: "Test",
            lines: [
                { type: "item", value: null },
                { type: "channel-line", name: "Power" },
            ],
        });
    });

    it("preserves missing entries in multi-channel values", () => {
        const field: OeFormlyField.ValueFromChannelsLine = {
            type: "value-from-channels-line",
            channelsToSubscribe: [
                new ChannelAddress("meter0", "ActivePower"),
                new ChannelAddress("meter1", "ActivePower"),
            ],
            value: () => null,
        };
        const context = { "meter0/ActivePower": 0 };
        expect(OeFormlyViewTester.applyValueLineFromChannels(field, context)).toEqual({
            rawValues: [0, null],
            value: null,
            currentData: { allComponents: context },
        });
    });

    it("returns null when a multi-channel field is filtered out", () => {
        expect(
            OeFormlyViewTester.applyValueLineFromChannels(
                {
                    type: "value-from-channels-line",
                    channelsToSubscribe: [],
                    value: () => "unused",
                    filter: () => false,
                },
                {},
            ),
        ).toBeNull();
    });
});

describe("OeChartTester datasets", () => {
    it("preserves labels and null data values", () => {
        const dataset: ChartDataset = { label: "Power", data: [0, null, 1000] };
        const result = OeChartTester.convertChartDatasetsToDatasets([dataset]);
        expect(result).toEqual([{ type: "data", label: "Power", value: [0, null, 1000] }]);
        expect(result[0].value).toBe(dataset.data);
    });

    it("preserves an absent dataset label", () => {
        expect(OeChartTester.convertChartDatasetsToDatasets([{ data: [] }])).toEqual([
            { type: "data", label: undefined, value: [] },
        ]);
    });
});

describe("OeChartTester channel fixtures", () => {
    let context: TestContext;
    beforeEach(async () => {
        context = await TestingUtils.sharedSetup();
    });

    function convertOptions(channels: OeTester.Types.Channels, chartType: "line" | "bar") {
        return OeChartTester.convertChartDataToOptions(
            new HistoryUtils.ChartData(),
            chartType,
            context,
            channels,
            "de",
            DummyConfig.from(),
            [],
        );
    }

    it("reports a missing line-chart fixture", () => {
        expect(() =>
            convertOptions(
                { energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse("0", { data: {} }) },
                "line",
            ),
        ).toThrowError("Missing channel data for line chart");
    });

    it("reports a missing bar-chart fixture", () => {
        expect(() =>
            convertOptions(
                { energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse("0", { data: {} }) },
                "bar",
            ),
        ).toThrowError("Missing channel data for bar chart");
    });

    it("prepares generated chart options with a valid fixture", () => {
        const result = convertOptions(
            {
                energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse("0", { data: {} }),
                dataChannelWithValues: new QueryHistoricTimeseriesDataResponse("0", { data: {}, timestamps: [] }),
            },
            "line",
        );
        expect(result.type).toBe("option");
        expect(result.options.elements?.point?.radius).toBe(0);
        expect("layout" in result.options).toBeFalse();
        expect("indexAxis" in result.options).toBeFalse();
        expect(result.options.plugins?.tooltip?.boxWidth).toBeUndefined();
    });
});
