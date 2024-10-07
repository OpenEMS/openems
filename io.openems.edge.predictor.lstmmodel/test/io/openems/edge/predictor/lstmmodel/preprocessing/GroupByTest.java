/*
 * package io.openems.edge.predictor.lstm.preprocessing;
 * 
 * import static org.junit.Assert.assertEquals;
 * 
 * import java.time.OffsetDateTime; import java.util.ArrayList; import
 * java.util.Arrays;
 * 
 * import org.junit.Test;
 * 
 * public class GroupByTest {
 * 
 * ArrayList<OffsetDateTime> testDatesxx = new ArrayList<>(Arrays.asList(//
 * OffsetDateTime.parse("2023-01-01T00:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T00:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T01:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T02:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T03:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T04:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T05:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T06:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:15:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:20:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:25:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:30:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:35:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:40:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:45:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:50:00Z"),
 * OffsetDateTime.parse("2023-01-01T07:55:00Z"),
 * OffsetDateTime.parse("2023-01-01T08:00:00Z"),
 * OffsetDateTime.parse("2023-01-01T08:05:00Z"),
 * OffsetDateTime.parse("2023-01-01T08:10:00Z"),
 * OffsetDateTime.parse("2023-01-01T08:15:00Z"),
 * 
 * OffsetDateTime.parse("2023-11-24T00:00:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:05:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:10:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:15:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:20:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:25:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:30:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:35:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:40:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:45:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:50:00+02:00"),
 * OffsetDateTime.parse("2023-11-24T00:55:00+02:00")
 * 
 * ));
 * 
 * 
 * private ArrayList<Double> testDataxx = new ArrayList<>(Arrays.asList(421.0,
 * 408.0, 360.0, 357.0, 330.0, 329.0, 330.0, 376.0, 356.0, 334.0, 352.0, 319.0,
 * 247.0, 185.0, 174.0, 226.0, 317.0, 303.0, 299.0, 368.0, 345.0, 309.0, 302.0,
 * 374.0, 366.0, 343.0, 334.0, 340.0, 348.0, 306.0, 306.0, 370.0, 335.0, 283.0,
 * 283.0, 278.0, 299.0, 250.0, 244.0, 311.0, 290.0, 280.0, 282.0, 324.0, 380.0,
 * 380.0, 372.0, 379.0, 306.0, 296.0, 312.0, 363.0, 367.0, 334.0, 309.0, 312.0,
 * 308.0, 667.0, 386.0, 364.0, 336.0, 312.0, 310.0, 343.0, 317.0, 406.0, 396.0,
 * 371.0, 357.0, 363.0, 318.0, 304.0, 302.0, 343.0, 327.0, 292.0, 283.0, 272.0,
 * 262.0, 311.0, 331.0, 381.0, 401.0, 421.0, 474.0, 463.0, 426.0, 379.0, 801.0,
 * 511.0, 453.0, 351.0, 415.0, 476.0, 508.0, 451.0, 435.0, 421.0, 466.0, 599.0,
 * 421.0, 408.0, 360.0, 357.0, 330.0, 329.0, 330.0, 376.0, 356.0, 334.0, 352.0,
 * 319.0));
 * 
 * 
 * 
 * @Test public void testGroupByHour() {
 * 
 * System.out.println(testDatesxx.size());
 * System.out.println(testDataxx.size()); GroupBy groupBy = new
 * GroupBy(testDataxx, testDatesxx);
 * 
 * groupBy.hour();
 * 
 * assertEquals(2, groupBy.getGroupedDataByHour().size()); assertEquals(2,
 * groupBy.getGroupedDateByHour().size());
 * 
 * assertEquals(Arrays.asList(1.0, 3.0, 5.0),
 * groupBy.getGroupedDataByHour().get(0));
 * assertEquals(Arrays.asList(OffsetDateTime.parse("2022-01-01T10:15:30Z"),
 * OffsetDateTime.parse("2022-01-01T10:45:00Z"), //
 * OffsetDateTime.parse("2022-01-01T10:30:00Z")),
 * groupBy.getGroupedDateByHour().get(0));
 * 
 * assertEquals(Arrays.asList(2.0, 4.0), groupBy.getGroupedDataByHour().get(1));
 * assertEquals(Arrays.asList(OffsetDateTime.parse("2022-01-01T11:30:45Z"),
 * OffsetDateTime.parse("2022-01-01T11:15:00Z")),
 * groupBy.getGroupedDateByHour().get(1)); }
 * 
 * 
 * 
 * @Test public void testGroupByMinute() {
 * 
 * 
 * GroupBy groupBy = new GroupBy(testDataxx, testDatesxx); groupBy.minute();
 * 
 * assertEquals(3, groupBy.getGroupedDataByMinute().size()); assertEquals(3,
 * groupBy.getGroupedDateByMinute().size());
 * 
 * assertEquals(Arrays.asList(1.0, 4.0),
 * groupBy.getGroupedDataByMinute().get(0)); assertEquals(
 * Arrays.asList(OffsetDateTime.parse("2022-01-01T10:15:30Z"),
 * OffsetDateTime.parse("2022-01-01T11:15:00Z")), //
 * groupBy.getGroupedDateByMinute().get(0));
 * 
 * assertEquals(Arrays.asList(2.0, 5.0),
 * groupBy.getGroupedDataByMinute().get(1));
 * assertEquals(Arrays.asList(OffsetDateTime.parse("2022-01-01T11:30:45Z"), //
 * OffsetDateTime.parse("2022-01-01T10:30Z")//
 * 
 * ), groupBy.getGroupedDateByMinute().get(1));
 * 
 * }
 * 
 * 
 * }
 */