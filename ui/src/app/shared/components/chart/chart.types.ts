import { ChartDataset, ChartOptions } from "chart.js";

export namespace ChartTypes {
    export type ChartConfig = { chartType: "line" | "bar", labels: Label[], datasets: Dataset[], options: ChartOptions | null };
    export type Color = {
        backgroundColor: string, borderColor: string
    };
    export type Label = Date | string | number;
    export type Dataset = ChartDataset;
}
