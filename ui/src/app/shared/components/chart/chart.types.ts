import { ChartDataset, ChartOptions } from "CHART.JS";

export namespace ChartTypes {
    export type ChartConfig = { chartType: "line" | "bar", labels: (Date | string)[], datasets: ChartDataset[], options: ChartOptions | null };
    export type Color = {
        backgroundColor: string, borderColor: string
    };
}
