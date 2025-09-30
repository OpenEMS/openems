import { Component, OnChanges, OnInit, SimpleChanges } from "@angular/core";
import { FieldType } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

interface YearData {
  year: number;
  tariffs: {
    low: number;
    standard: number;
    high: number;
  };
  quarters: Quarter[];
}

interface Quarter {
  quarter: number;
  dailySchedule: DailySchedule[];
  formattedDateRange?: string | null;
  key?: string; // Added key for direct lookup
  // Add precomputed schedules
  lowSchedules?: DailySchedule[];
  highSchedules?: DailySchedule[];
}

interface DailySchedule {
  tariff: string;
  from: string;
  to: string;
  originalIndex?: number;
}

@Component({
  selector: "formly-tariff-table",
  templateUrl: "./formly-custom-tariff-TABLE.HTML",
  standalone: false,
  styleUrls: ["./tariff-TABLE.SCSS"],
})
export class FormlyTariffTableTypeComponent extends FieldType implements OnInit, OnChanges {

  protected tariffData: YearData[] = [];
  protected expandedQuarters: { [key: string]: boolean } = {};
  protected currentYear: number = new Date().getFullYear();
  protected togglePriceItems: boolean = true;
  protected tariffLabels: {
    low: string;
    standard: string;
    high: string;
  } = {
      low: "FORMLY_FORM.TARIFF_TABLE.TARIFFS.LOW",
      standard: "FORMLY_FORM.TARIFF_TABLE.TARIFFS.STANDARD",
      high: "FORMLY_FORM.TARIFF_TABLE.TARIFFS.HIGH",
    };

  constructor(private translate: TranslateService, private service: Service) {
    super();
  }

  ngOnInit() {
    THIS.PARSE_INITIAL_DATA();
    THIS.INITIALIZE_FORM();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (CHANGES.PROPS) {
      // Re-parse data if props change, ensuring new data is processed
      THIS.PARSE_INITIAL_DATA();
    }
  }

  protected addYear() {
    const lastYear = THIS.TARIFF_DATA[THIS.TARIFF_DATA.LENGTH - 1]?.year || THIS.CURRENT_YEAR - 1;
    const newYear = lastYear + 1;

    THIS.TARIFF_DATA.PUSH({
      year: newYear,
      tariffs: { low: 0, standard: 0, high: 0 },
      quarters: THIS.CREATE_QUARTERS_FOR_YEAR(newYear),
    });

    // After adding a new year, ensure all its schedules (even if empty initially) are filtered
    THIS.TARIFF_DATA[THIS.TARIFF_DATA.LENGTH - 1].QUARTERS.FOR_EACH(q => THIS.UPDATE_FILTERED_SCHEDULES(q));

    THIS.UPDATE_FORM_CONTROL();
  }

  protected removeYear(yearIndex: number) {
    if (THIS.TARIFF_DATA.LENGTH > 1) {
      THIS.TARIFF_DATA.SPLICE(yearIndex, 1);
      THIS.UPDATE_FORM_CONTROL();
    }
  }

  /**
   * Toggles the visibility state of a quarter's daily schedule section using its pre-calculated key.
   *
   * @param quarterKey The unique key for the quarter (E.G., "y2024_q1").
   */
  protected toggleQuarterVisibilityByKey(quarterKey: string) {
    THIS.EXPANDED_QUARTERS[quarterKey] = !THIS.EXPANDED_QUARTERS[quarterKey];
  }

  /**
   * Checks if a quarter's daily schedule section is expanded using its pre-calculated key.
   *
   * @param quarterKey The unique key for the quarter (E.G., "y2024_q1").
   * @returns True if the quarter is expanded, false otherwise.
   */
  protected isQuarterExpandedByKey(quarterKey: string): boolean {
    return !!THIS.EXPANDED_QUARTERS[quarterKey];
  }

  protected updateTariffPrice(yearIndex: number, tariffKey: keyof YearData["tariffs"], value: number) {
    THIS.TARIFF_DATA[yearIndex].tariffs[tariffKey] = value;
    THIS.UPDATE_FORM_CONTROL();
  }

  /**
   * Removes a specific time range entry from the daily schedule.
   * Includes defensive checks to prevent errors if intermediate objects (yearData, quarterData, dailySchedule) are null or undefined.
   *
   * @param yearIndex The index of the year in the tariffData array.
   * @param quarterIndex The index of the quarter within the specified year.
   * @param scheduleIndex The index of the schedule item to remove within the dailySchedule array.
   */
  protected removeTimeRange(yearIndex: number, quarterIndex: number, scheduleIndex: number) {
    const yearData = THIS.TARIFF_DATA[yearIndex];
    if (!yearData) {
      CONSOLE.ERROR(`Attempted to remove time range from non-existent year: Index ${yearIndex}`);
      THIS.SERVICE.TOAST("Error: Year data not found for removal.", "danger");
      return;
    }

    const quarterData = YEAR_DATA.QUARTERS[quarterIndex];
    if (!quarterData) {
      CONSOLE.ERROR(`Attempted to remove time range from non-existent quarter: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      THIS.SERVICE.TOAST("Error: Quarter data not found for removal.", "danger");
      return;
    }

    const dailyScheduleArray = QUARTER_DATA.DAILY_SCHEDULE;
    if (!dailyScheduleArray || !ARRAY.IS_ARRAY(dailyScheduleArray)) {
      CONSOLE.ERROR(`Attempted to remove time range from missing or invalid daily schedule array: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      THIS.SERVICE.TOAST("Error: Daily schedule not found or is invalid.", "danger");
      return;
    }

    // Check if the scheduleIndex is valid before splicing
    if (scheduleIndex >= 0 && scheduleIndex < DAILY_SCHEDULE_ARRAY.LENGTH) {
      DAILY_SCHEDULE_ARRAY.SPLICE(scheduleIndex, 1);
      THIS.UPDATE_FILTERED_SCHEDULES(quarterData);
      THIS.UPDATE_FORM_CONTROL();
    } else {
      CONSOLE.ERROR(`Attempted to remove time range with invalid schedule index: Index ${scheduleIndex}, Array Length ${DAILY_SCHEDULE_ARRAY.LENGTH}`);
      THIS.SERVICE.TOAST("Error: Invalid time slot index for removal.", "danger");
    }
  }

  protected updateTimeRange(
    yearIndex: number,
    quarterIndex: number,
    scheduleIndex: number,
    field: "from" | "to",
    value: string
  ) {
    const quarter = THIS.TARIFF_DATA[yearIndex]?.quarters[quarterIndex];
    if (!quarter || !QUARTER.DAILY_SCHEDULE) {
      CONSOLE.ERROR(`Cannot update time range: Quarter or dailySchedule missing. Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      THIS.SERVICE.TOAST("Error: Could not update time slot.", "danger");
      return;
    }

    const scheduleToUpdate = QUARTER.DAILY_SCHEDULE[scheduleIndex];
    if (scheduleToUpdate) {
      scheduleToUpdate[field] = value;
      // No need to re-filter here, as only the content of an existing schedule changed, not its tariff type or addition/removal
      THIS.UPDATE_FORM_CONTROL();
    } else {
      CONSOLE.ERROR(`Cannot update time range: Schedule not found at index ${scheduleIndex}.`);
      THIS.SERVICE.TOAST("Error: Time slot not found for update.", "danger");
    }
  }

  protected isTimeRangeInvalid(schedule: DailySchedule): boolean {
    // Ensuring direct value check as pipe converts it to HH:mm string
    const fromTime = SCHEDULE.FROM.INCLUDES("T") ? SCHEDULE.FROM.SUBSTRING(11, 16) : SCHEDULE.FROM;
    const toTime = SCHEDULE.TO.INCLUDES("T") ? SCHEDULE.TO.SUBSTRING(11, 16) : SCHEDULE.TO;

    if (fromTime && toTime && FROM_TIME.INCLUDES(":") && TO_TIME.INCLUDES(":")) {
      const inValid = (fromTime >= toTime) && (toTime !== "00:00");
      if (inValid) {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("FORMLY_FORM.TARIFF_TABLE.ERROR_MESSAGES.INVALID_TIME_INPUT"), "danger", 5000);
      }
      return inValid;
    }
    return false;
  }

  /**
   * Helper to get the start and end Date objects for a given year and quarter.
   * @param year The year number.
   * @param quarter The quarter number (1-4).
   * @returns A tuple containing the start and end Date objects for the quarter.
   */
  protected getQuarterDates(year: number, quarter: number): [Date, Date] {
    switch (quarter) {
      case 1: return [new Date(year, 0, 1), new Date(year, 2, 31)];  // Jan 1 - Mar 31
      case 2: return [new Date(year, 3, 1), new Date(year, 5, 30)];  // Apr 1 - Jun 30
      case 3: return [new Date(year, 6, 1), new Date(year, 8, 30)];  // Jul 1 - Sep 30
      case 4: return [new Date(year, 9, 1), new Date(year, 11, 31)]; // Oct 1 - Dec 31
      default: return [new Date(), new Date()];
    }
  }

  protected getSchedulesForTariff(schedules: DailySchedule[], tariff: string): DailySchedule[] {
    return SCHEDULES.FILTER(s => S.TARIFF === tariff);
  }

  /**
   * Adds a new time range entry to the daily schedule of a specific quarter and tariff type.
   * Ensures the dailySchedule array exists before pushing.
   *
   * @param yearIndex The index of the year in the tariffData array.
   * @param quarterIndex The index of the quarter within the specified year.
   * @param tariff The tariff type ('low', 'standard', 'high') for the new time range.
   */
  protected addTimeRange(yearIndex: number, quarterIndex: number, tariff: string) {
    const quarter = THIS.TARIFF_DATA[yearIndex]?.quarters[quarterIndex];

    if (quarter) {
      if (!QUARTER.DAILY_SCHEDULE) {
        QUARTER.DAILY_SCHEDULE = [];
      }

      QUARTER.DAILY_SCHEDULE.PUSH(
        THIS.CREATE_DAILY_SCHEDULE(
          tariff,
          THIS.TRANSLATE.INSTANT("GENERAL.FROM"),
          THIS.TRANSLATE.INSTANT("GENERAL.TO"),
          QUARTER.DAILY_SCHEDULE.LENGTH // Assign originalIndex as current length
        )
      );
      THIS.UPDATE_FILTERED_SCHEDULES(quarter);
      THIS.UPDATE_FORM_CONTROL();
    } else {
      CONSOLE.ERROR(`Attempted to add time range to non-existent quarter: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      THIS.SERVICE.TOAST("Error: Could not find quarter", "danger", 3000);
    }
  }

  /**
   * A trackBy function for ngFor loops over DailySchedule items.
   * Uses the originalIndex to help Angular track items efficiently, especially after filtering or reordering.
   *
   * @param index The current index in the loop.
   * @param schedule The DailySchedule object.
   * @returns The originalIndex of the schedule.
   */
  protected trackByScheduleOriginalIndex(index: number, schedule: DailySchedule): number {
    return SCHEDULE.ORIGINAL_INDEX!; // Use the unique originalIndex
  }

  /**
   * A trackBy function for ngFor loops over Quarter items.
   * Uses the quarter"s key for efficient change tracking.
   *
   * @param index The current index in the loop.
   * @param quarter The Quarter object.
   * @returns The unique key of the quarter.
   */
  protected trackByQuarterKey(index: number, quarter: Quarter): string {
    return QUARTER.KEY!; // Use the unique key
  }

  private updateFormControl() {
    THIS.FORM_CONTROL.SET_VALUE(THIS.TARIFF_DATA);
    THIS.FORM_CONTROL.MARK_AS_DIRTY();
  }

  private createQuartersForYear(year: number): Quarter[] {
    const quarters: Quarter[] = [];
    for (let i = 1; i <= 4; i++) {
      const [startDate, endDate] = THIS.GET_QUARTER_DATES(year, i);
      const quarterKey = `y${year}_q${i}`;
      const newQuarter: Quarter = { // Explicitly create the object
        quarter: i,
        dailySchedule: [], // Initialize as empty array
        formattedDateRange: DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(startDate, endDate, THIS.TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")),
        key: quarterKey,
      };

      // No need to call updateFilteredSchedules here, as dailySchedule is empty.
      // It will be called in parseInitialData for existing data, or in addTimeRange for new schedules.
      QUARTERS.PUSH(newQuarter);
    }
    return quarters;
  }

  private createDailySchedule(tariff: string, from: string, to: string, originalIndex: number): DailySchedule {
    return { tariff, from, to, originalIndex };
  }

  private parseInitialData() {
    const rawValue = THIS.FORM_CONTROL.VALUE;

    if (ARRAY.IS_ARRAY(rawValue) && RAW_VALUE.EVERY(item => typeof item === "object" && item !== null && "year" in item && "tariffs" in item && "quarters" in item)) {
      THIS.TARIFF_DATA = rawValue;
    } else {
      CONSOLE.WARN("Initial form control value is not in the expected tariffData format. Initializing with default year.");
      THIS.TARIFF_DATA = [];
    }

    if (THIS.TARIFF_DATA.LENGTH === 0) {
      THIS.ADD_YEAR(); // This path ensures quarters are created with `dailySchedule` and `key`
    } else {
      THIS.TARIFF_DATA.FOR_EACH(yearData => {
        YEAR_DATA.QUARTERS = YEAR_DATA.QUARTERS || []; // Ensure quarters array exists

        // Use a map to rebuild quarters to ensure no nulls/undefineds and consistency
        YEAR_DATA.QUARTERS = YEAR_DATA.QUARTERS.MAP(quarter => {
          // If a quarter from rawValue is null/undefined, replace it with a default empty one
          if (!quarter) {
            CONSOLE.WARN(`Found null/undefined quarter for year ${YEAR_DATA.YEAR}. Replacing with default.`);
            // Create a default quarter if the parsed one is invalid
            const defaultQuarterNum = 1; // Or try to infer if possible, but 1 is safe fallback
            const [startDate, endDate] = THIS.GET_QUARTER_DATES(YEAR_DATA.YEAR, defaultQuarterNum);
            return {
              quarter: defaultQuarterNum,
              dailySchedule: [],
              formattedDateRange: DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(startDate, endDate, THIS.TRANSLATE.INSTANT("GENERAL.DATE_FORMAT")),
              key: `y${YEAR_DATA.YEAR}_q${defaultQuarterNum}`,
            };
          }

          // Ensure key is set
          QUARTER.KEY = `y${YEAR_DATA.YEAR}_q${QUARTER.QUARTER}`;

          // Ensure formattedDateRange is set
          const [startDate, endDate] = THIS.GET_QUARTER_DATES(YEAR_DATA.YEAR, QUARTER.QUARTER);
          QUARTER.FORMATTED_DATE_RANGE = DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(startDate, endDate, THIS.TRANSLATE.INSTANT("GENERAL.DATE_FORMAT"));

          // Ensure dailySchedule array exists
          QUARTER.DAILY_SCHEDULE = QUARTER.DAILY_SCHEDULE || [];
          QUARTER.DAILY_SCHEDULE.FOR_EACH((schedule, index) => {
            if (SCHEDULE.ORIGINAL_INDEX === undefined || SCHEDULE.ORIGINAL_INDEX === null) {
              SCHEDULE.ORIGINAL_INDEX = index;
            }
          });

          // !!! Crucial: Filter schedules once after parsing/initialization !!!
          THIS.UPDATE_FILTERED_SCHEDULES(quarter); // Filter immediately after populating dailySchedule

          return quarter; // Return the processed quarter
        });
      });
    }
    THIS.INITIALIZE_FORM(); // Initialize expanded quarters states
  }
  private initializeForm() {
    THIS.TARIFF_DATA.FOR_EACH(yearData => {
      YEAR_DATA.QUARTERS.FOR_EACH(q => {
        // Use the pre-calculated key for initialization
        if (Q.KEY) { // Defensive check
          THIS.EXPANDED_QUARTERS[Q.KEY] = false;
        }
      });
    });
  }

  /**
   * Helper method to update the pre-filtered schedule arrays for a given quarter.
   * Call this whenever QUARTER.DAILY_SCHEDULE is modified (add/remove).
   * @param quarter The quarter object whose schedules need to be updated.
   */
  private updateFilteredSchedules(quarter: Quarter) {
    // Ensure dailySchedule is an array before filtering
    const schedulesToFilter = QUARTER.DAILY_SCHEDULE || [];
    QUARTER.LOW_SCHEDULES = SCHEDULES_TO_FILTER.FILTER(s => S.TARIFF === "low");
    QUARTER.HIGH_SCHEDULES = SCHEDULES_TO_FILTER.FILTER(s => S.TARIFF === "high");
  }

}
