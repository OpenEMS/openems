import { History } from "src/app/edge/history/common/energy/chart/channels.spec";
import { DummyConfig, SOCOMEC_GRID_METER } from "src/app/shared/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/genericComponents/shared/testing/common";
import { sharedSetup, TestContext } from "src/app/shared/test/utils.spec";

import { DATA, LABELS } from "../../energy/chart/chart.constants.spec";
import { expectView } from "./chart.constants.spec";

describe('History Grid', () => {
  const defaultEMS = DummyConfig.from(
    SOCOMEC_GRID_METER("meter0", "Netzzähler")
  );

  let TEST_CONTEXT: TestContext;
  beforeEach(() =>
    TEST_CONTEXT = sharedSetup()
  );

  it('#getChartData()', () => {
    {
      // Line - Chart
      expectView(defaultEMS, TEST_CONTEXT, 'line', History.DAY,
        {
          datasets: {
            data: [
              DATA('Einspeisung: 15,6 kWh', [null, null, null, 0, 0, 0.006, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.004, 0, 0, 0, 0.004, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.005, 0, 0, 0, 0, 0, 0.001, 0.002, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.001, 0.004, 0, 0.004, 0, 0, 0, 0, 0.005, 0.013, 0.006, 0.004, 0.017, 0.015, 0.017, 0.011, 0, 0, 0, 0, 0.029, 0.015, 0.013, 0.019, 0.014, 0.007, 0.016, 0, 0.018, 0.022, 0, 0.012, 0.011, 0.007, 0, 0.033, 0.007, 0.003, 0.004, 0.011, 0, 0.038, 0, 0, 0.019, 0, 0.016, 0.014, 0.018, 0, 1.119, 3.453, 3.608, 3.941, 4.392, 3.786, 4.805, 4.688, 3.095, 2.32, 2.851, 3.058, 4.044, 5.011, 2.789, 6.53, 5.029, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
              DATA('Bezug: 0,9 kWh', [null, null, null, 0.031, 0.018, 0, 0.02, 0.016, 0.015, 0.014, 0.009, 0.02, 0.025, 0.025, 0.025, 0.021, 0.012, 0.009, 0.01, 0.011, 0.005, 0.003, 0, 0.015, 0.018, 0.023, 0, 0, 0, 0.002, 0.002, 0.003, 0.015, 0.008, 0.022, 0.027, 0.016, 0.003, 0.002, 0, 0.028, 0.027, 0.017, 0.001, 0, 0, 0, null, null, null, null, 0.011, 0.01, 0.004, 0.006, 0.007, 0.018, 0.008, 0.012, 0.009, 0.004, 0.013, 0.015, 0.012, 0, 0, 0, 0.002, 0, 0.005, 0.001, 0.03, 0.062, 0, 0, 0, 0, 0, 0, 0, 0, 0.015, 0.005, 0.004, 0.007, 0, 0, 0, 0, 0, 0, 0, 0.005, 0, 0, 0, 0, 0, 0, 0.021, 0, 0, 0, 0, 0, 0.003, 0, 0.004, 0, 0, 0.032, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null])
            ],
            labels: LABELS(History.DAY.dataChannelWithValues.result.timestamps),
            options: OeTester.ChartOptions.LINE_CHART_OPTIONS('hour')
          }
        }, false);
    }
    {
      // Line - Chart
      expectView(defaultEMS, TEST_CONTEXT, 'line', History.WEEK,
        {
          datasets: {
            data: [
              DATA('Einspeisung: 119,7 kWh', [0.0023333333333333335, 0, 0, 0, 0, 0, 0, 0.014166666666666666, 0.02808333333333333, 0.9546666666666667, 4.150583333333333, 6.431333333333333, 5.737583333333333, 5.6714166666666666, 5.873333333333333, 5.049083333333333, 3.122, 1.0374166666666667, 0.22808333333333333, 0.02, 0, 0, 0, 0.008333333333333333, 0.0030833333333333333, 0.008333333333333333, 0, 0.007727272727272728, 0, 0, 0.00275, 0.013833333333333335, 0.017416666666666667, 0.006083333333333333, 0.5646666666666667, 2.2251666666666665, 2.03375, 3.99725, 4.990083333333333, 3.0128333333333335, 2.4844166666666667, 1.378, 0.65975, 0, 0.001, 0.006916666666666667, 0.008166666666666666, 0, 0, 0, 0, 0, 0, 0, 0.004083333333333333, 0.010583333333333333, 0.011166666666666667, 1.261, 5.308833333333333, 6.604, 6.321166666666667, 6.488333333333333, 6.78425, 6.052083333333333, 2.5839166666666666, 0.529, 0.01616666666666667, 0.0055, 0, 0.0006666666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0024166666666666664, 0.0125, 0.7065, 5.835416666666667, 4.77025, 6.03925, 6.8445833333333335, 5.370333333333333, 4.490166666666667, 2.3506666666666667, 0.7650833333333333, 0.08583333333333333, 0.011454545454545455, 0, 0, 0.005666666666666667, 0, 0, 0, 0, 0, 0, 0, 0.0033333333333333335, 0.004083333333333333, 0.02033333333333333, 0.02316666666666667, 1.4106666666666667, 0.8588333333333333, 0.0015833333333333333, 0.006583333333333333, 0.010083333333333335, 0.3410833333333333, 2.9290833333333337, 1.1175833333333332, 0.48583333333333334, 0, 0, 0, 0.0006666666666666666, 0.017916666666666668, 0.004, 0, 0, 0.001, 0, 0, 0, 0.02358333333333333, 0.006416666666666667, 0.008166666666666666, 0.0031666666666666666, 0.009916666666666666, 2.7254166666666664, 1.83725, 2.63225, 2.2170833333333335, 0.529, 0, 0, 0, 0, 0, 0.0003333333333333333, 0, 0, 0.011416666666666665, 0.011083333333333334, 0, 0, 0, 0, 0.008333333333333333, 0.008818181818181819, 0.015333333333333334, 0.018857142857142857, 0.024833333333333332, 0.010888888888888889, 2.2174, 3.9214166666666666, 1.6248181818181817, 1.937, 1.789, 0.0195, 0.0143, 0, 0, 0.009, 0.018875]),
              DATA('Bezug: 2,4 kWh', [0, 0.011916666666666666, 0.01633333333333333, 0.00609090909090909, 0.015333333333333334, 0.011666666666666665, 0.0024166666666666664, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.02425, 0.004416666666666667, 0.0035833333333333333, 0, 0, 0, 0.04441666666666667, 0, 0.013111111111111112, 0.001, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0011666666666666668, 0, 0, 0, 0.0015833333333333333, 0.013333333333333334, 0.020416666666666666, 0.01125, 0.019727272727272725, 0.012444444444444445, 0.009583333333333334, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.007666666666666667, 0, 0.0023333333333333335, 0.0125, 0.01609090909090909, 0.02016666666666667, 0.014083333333333333, 0.006363636363636363, 0.01955555555555556, 0.04841666666666666, 0.011166666666666667, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.014222222222222221, 0.00225, 0, 0.0036666666666666666, 0.032916666666666664, 0.014666666666666666, 0.0135, 0.017363636363636362, 0.013333333333333334, 0.022083333333333333, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0009166666666666666, 0, 0.0021666666666666666, 0, 0, 0, 0.0005, 0.04841666666666666, 0, 0.005555555555555556, 0.02716666666666667, 0.017333333333333333, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0023333333333333335, 0.008333333333333333, 0.003, 0.015916666666666666, 0.00325, 0, 0.004333333333333333, 0.001, 0, 0, 0.019545454545454546, 0.0017777777777777776, 0.006416666666666667, 0.017666666666666667, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0058, 0.005625, 0, 0])
            ],
            labels: LABELS(History.WEEK.dataChannelWithValues.result.timestamps),
            options: OeTester.ChartOptions.LINE_CHART_OPTIONS('day')
          }

        }, false);
    }
    {
      // Line - Chart
      expectView(defaultEMS, TEST_CONTEXT, 'bar', History.MONTH,
        {
          datasets: {
            data: [
              DATA('Einspeisung: 12.738 kWh', [603, 590, 551, 572, 69, 236, 626, null, 1003, 261, 518, 698, 640, 388, 471, 373, 373, 677, 286, 406, 249, null, 446, 369, 558, null, 776, 425, 574, null]),
              DATA('Bezug: 773 kWh', [16, 6, 3, 3, 5, 48, 4, null, 5, 26, 17, 62, 8, 66, 13, 21, 4, 3, 18, 27, 29, null, 118, 85, 2, null, 72, 28, 84, null])
            ],
            labels: LABELS(History.MONTH.energyPerPeriodChannelWithValues.result.timestamps),
            options: OeTester.ChartOptions.BAR_CHART_OPTIONS('day')
          }

        }, false);
    }
    {
      // Line - Chart
      expectView(defaultEMS, TEST_CONTEXT, 'bar', History.YEAR,
        {
          datasets: {
            data: [
              DATA('Einspeisung: 30.703 kWh', [20, 86, 677, 3657, 12839, 12738, 627, null, null, null, null, null]),
              DATA('Bezug: 23.209 kWh', [9829, 4812, 2915, 2036, 2712, 773, 94, null, null, null, null, null])
            ],
            labels: LABELS(History.YEAR.energyPerPeriodChannelWithValues.result.timestamps),
            options: OeTester.ChartOptions.BAR_CHART_OPTIONS('month')
          }
        }, false);
    }
  });
});