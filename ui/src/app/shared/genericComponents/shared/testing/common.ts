import { TimeUnit } from "chart.js";
import { OeChartTester } from "src/app/shared/genericComponents/shared/testing/tester";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";

export namespace OeTester {

  export namespace Types {
    export type Channels = {

      /** Always one value for each channel from a {@link QueryHistoricTimeseriesEnergyResponse} */
      energyChannelWithValues: QueryHistoricTimeseriesEnergyResponse,

      /** data from a {@link QueryHistoricTimeseriesEnergyPerPeriodResponse} */
      energyPerPeriodChannelWithValues?: QueryHistoricTimeseriesEnergyPerPeriodResponse,
      /** data from a {@link QueryHistoricTimeseriesDataResponse} */
      dataChannelWithValues?: QueryHistoricTimeseriesDataResponse
    }
  }

  export namespace ChartOptions {
    export const LINE_CHART_OPTIONS = (period: string, title?: string): OeChartTester.Dataset.Option => ({
      type: 'option',
      options: {
        "maintainAspectRatio": false,
        "legend": {
          "labels": {},
          "position": "bottom",
        },
        "elements": {
          "point": {
            "radius": 0,
            "hitRadius": 0,
            "hoverRadius": 0,
          },
          "line": {
            "borderWidth": 2,
            "tension": 0.1,
          },
          "rectangle": {
            "borderWidth": 2,
          },
        },
        "hover": {
          "mode": "point",
          "intersect": true,
        },
        "scales": {
          "yAxes": [
            {
              "id": "left",
              "position": "left",
              "scaleLabel": {
                "display": true,
                "labelString": title ?? "kW",
                "padding": 5,
                "fontSize": 11,
              },
              "gridLines": {
                "display": true,
              },
              "ticks": {
                "beginAtZero": false,
              },
            },
          ],
          "xAxes": [
            {
              "ticks": {},
              "stacked": false,
              "type": "time",
              "time": {
                "minUnit": "hour",
                "displayFormats": {
                  "millisecond": "SSS [ms]",
                  "second": "HH:mm:ss a",
                  "minute": "HH:mm",
                  "hour": "HH:[00]",
                  "day": "DD",
                  "week": "ll",
                  "month": "MM",
                  "quarter": "[Q]Q - YYYY",
                  "year": "YYYY",
                },
                "unit": period as TimeUnit,
              },
              "bounds": "ticks",
            },
          ],
        },
        "tooltips": {
          "mode": "index",
          "intersect": false,
          "axis": "x",
          "callbacks": {},
        },
        "responsive": true,
      },
    });
    export const BAR_CHART_OPTIONS = (period: string, title?: string): OeChartTester.Dataset.Option => ({
      type: 'option',
      options: {
        "maintainAspectRatio": false,
        "legend": {
          "labels": {},
          "position": "bottom",
        },
        "elements": {
          "point": {
            "radius": 0,
            "hitRadius": 0,
            "hoverRadius": 0,
          },
          "line": {
            "borderWidth": 2,
            "tension": 0.1,
          },
          "rectangle": {
            "borderWidth": 2,
          },
        },
        "hover": {
          "mode": "point",
          "intersect": true,
        },
        "scales": {
          "yAxes": [
            {
              "id": "left",
              "position": "left",
              "scaleLabel": {
                "display": true,
                "labelString": title ?? "kWh",
                "padding": 5,
                "fontSize": 11,
              },
              "gridLines": {
                "display": true,
              },
              "ticks": {
                "beginAtZero": false,
              },
              "stacked": true,
            },
          ],
          "xAxes": [
            {
              "ticks": {
                "maxTicksLimit": 12,
                "source": "data",
              },
              "stacked": true,
              "type": "time",
              "time": {
                "minUnit": "hour",
                "displayFormats": {
                  "millisecond": "SSS [ms]",
                  "second": "HH:mm:ss a",
                  "minute": "HH:mm",
                  "hour": "HH:[00]",
                  "day": "DD",
                  "week": "ll",
                  "month": "MM",
                  "quarter": "[Q]Q - YYYY",
                  "year": "YYYY",
                },
                "unit": period as TimeUnit,
              },
              "offset": true,
              "bounds": "ticks",
            },
          ],
        },
        "tooltips": {
          "mode": "x",
          "intersect": false,
          "axis": "x",
          "callbacks": {},
        },
        "responsive": true,
      },
    });
  }



}
