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
  templateUrl: "./formly-custom-tariff-table.html",
  standalone: false,
  styleUrls: ["./tariff-table.scss"],
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
    this.parseInitialData();
    this.initializeForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.props) {
      // Re-parse data if props change, ensuring new data is processed
      this.parseInitialData();
    }
  }

  protected addYear() {
    const lastYear = this.tariffData[this.tariffData.length - 1]?.year || this.currentYear - 1;
    const newYear = lastYear + 1;

    this.tariffData.push({
      year: newYear,
      tariffs: { low: 0, standard: 0, high: 0 },
      quarters: this.createQuartersForYear(newYear),
    });

    // After adding a new year, ensure all its schedules (even if empty initially) are filtered
    this.tariffData[this.tariffData.length - 1].quarters.forEach(q => this.updateFilteredSchedules(q));

    this.updateFormControl();
  }

  protected removeYear(yearIndex: number) {
    if (this.tariffData.length > 1) {
      this.tariffData.splice(yearIndex, 1);
      this.updateFormControl();
    }
  }

  /**
   * Toggles the visibility state of a quarter's daily schedule section using its pre-calculated key.
   *
   * @param quarterKey The unique key for the quarter (e.g., "y2024_q1").
   */
  protected toggleQuarterVisibilityByKey(quarterKey: string) {
    this.expandedQuarters[quarterKey] = !this.expandedQuarters[quarterKey];
  }

  /**
   * Checks if a quarter's daily schedule section is expanded using its pre-calculated key.
   *
   * @param quarterKey The unique key for the quarter (e.g., "y2024_q1").
   * @returns True if the quarter is expanded, false otherwise.
   */
  protected isQuarterExpandedByKey(quarterKey: string): boolean {
    return !!this.expandedQuarters[quarterKey];
  }

  protected updateTariffPrice(yearIndex: number, tariffKey: keyof YearData["tariffs"], value: string | number) {

    // Convert to number and ensure non-negative
    const numericValue = Math.max(0, Number(value));

    this.tariffData[yearIndex].tariffs[tariffKey] = numericValue;
    this.updateFormControl();
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
    const yearData = this.tariffData[yearIndex];
    if (!yearData) {
      console.error(`Attempted to remove time range from non-existent year: Index ${yearIndex}`);
      this.service.toast("Error: Year data not found for removal.", "danger");
      return;
    }

    const quarterData = yearData.quarters[quarterIndex];
    if (!quarterData) {
      console.error(`Attempted to remove time range from non-existent quarter: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      this.service.toast("Error: Quarter data not found for removal.", "danger");
      return;
    }

    const dailyScheduleArray = quarterData.dailySchedule;
    if (!dailyScheduleArray || !Array.isArray(dailyScheduleArray)) {
      console.error(`Attempted to remove time range from missing or invalid daily schedule array: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      this.service.toast("Error: Daily schedule not found or is invalid.", "danger");
      return;
    }

    // Check if the scheduleIndex is valid before splicing
    if (scheduleIndex >= 0 && scheduleIndex < dailyScheduleArray.length) {
      dailyScheduleArray.splice(scheduleIndex, 1);
      this.updateFilteredSchedules(quarterData);
      this.updateFormControl();
    } else {
      console.error(`Attempted to remove time range with invalid schedule index: Index ${scheduleIndex}, Array Length ${dailyScheduleArray.length}`);
      this.service.toast("Error: Invalid time slot index for removal.", "danger");
    }
  }

  protected updateTimeRange(
    yearIndex: number,
    quarterIndex: number,
    scheduleIndex: number,
    field: "from" | "to",
    value: string
  ) {
    const quarter = this.tariffData[yearIndex]?.quarters[quarterIndex];
    if (!quarter || !quarter.dailySchedule) {
      console.error(`Cannot update time range: Quarter or dailySchedule missing. Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      this.service.toast("Error: Could not update time slot.", "danger");
      return;
    }

    const scheduleToUpdate = quarter.dailySchedule[scheduleIndex];
    if (scheduleToUpdate) {
      scheduleToUpdate[field] = value;
      // No need to re-filter here, as only the content of an existing schedule changed, not its tariff type or addition/removal
      this.updateFormControl();
    } else {
      console.error(`Cannot update time range: Schedule not found at index ${scheduleIndex}.`);
      this.service.toast("Error: Time slot not found for update.", "danger");
    }
  }

  protected isTimeRangeInvalid(schedule: DailySchedule): boolean {
    // Ensuring direct value check as pipe converts it to HH:mm string
    const fromTime = schedule.from.includes("T") ? schedule.from.substring(11, 16) : schedule.from;
    const toTime = schedule.to.includes("T") ? schedule.to.substring(11, 16) : schedule.to;

    if (fromTime && toTime && fromTime.includes(":") && toTime.includes(":")) {
      const inValid = (fromTime >= toTime) && (toTime !== "00:00");
      if (inValid) {
        this.service.toast(this.translate.instant("FORMLY_FORM.TARIFF_TABLE.ERROR_MESSAGES.INVALID_TIME_INPUT"), "danger", 5000);
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
    return schedules.filter(s => s.tariff === tariff);
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
    const quarter = this.tariffData[yearIndex]?.quarters[quarterIndex];

    if (quarter) {
      if (!quarter.dailySchedule) {
        quarter.dailySchedule = [];
      }

      quarter.dailySchedule.push(
        this.createDailySchedule(
          tariff,
          this.translate.instant("General.FROM"),
          this.translate.instant("General.TO"),
          quarter.dailySchedule.length // Assign originalIndex as current length
        )
      );
      this.updateFilteredSchedules(quarter);
      this.updateFormControl();
    } else {
      console.error(`Attempted to add time range to non-existent quarter: Year Index ${yearIndex}, Quarter Index ${quarterIndex}`);
      this.service.toast("Error: Could not find quarter", "danger", 3000);
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
    return schedule.originalIndex!; // Use the unique originalIndex
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
    return quarter.key!; // Use the unique key
  }

  // removes Negative inputs
  protected sanitizeInput(event: CustomEvent) {
    const input = event.target as HTMLInputElement;
    const value = input.value;

    // Remove negative signs and non-numeric characters
    const sanitized = value.replace(/[^\d.]/g, "");

    // Update value if modified
    if (sanitized !== value) {
      input.value = sanitized;
    }
  }

  private updateFormControl() {
    this.formControl.setValue(this.tariffData);
    this.formControl.markAsDirty();
  }

  private createQuartersForYear(year: number): Quarter[] {
    const quarters: Quarter[] = [];
    for (let i = 1; i <= 4; i++) {
      const [startDate, endDate] = this.getQuarterDates(year, i);
      const quarterKey = `y${year}_q${i}`;
      const newQuarter: Quarter = { // Explicitly create the object
        quarter: i,
        dailySchedule: [], // Initialize as empty array
        formattedDateRange: DateUtils.formatQuarterDateRange(startDate, endDate, this.translate.instant("General.dateFormat")),
        key: quarterKey,
      };

      // No need to call updateFilteredSchedules here, as dailySchedule is empty.
      // It will be called in parseInitialData for existing data, or in addTimeRange for new schedules.
      quarters.push(newQuarter);
    }
    return quarters;
  }

  private createDailySchedule(tariff: string, from: string, to: string, originalIndex: number): DailySchedule {
    return { tariff, from, to, originalIndex };
  }

  private parseInitialData() {
    const rawValue = this.formControl.value;

    if (Array.isArray(rawValue) && rawValue.every(item => typeof item === "object" && item !== null && "year" in item && "tariffs" in item && "quarters" in item)) {
      this.tariffData = rawValue;
    } else {
      console.warn("Initial form control value is not in the expected tariffData format. Initializing with default year.");
      this.tariffData = [];
    }

    if (this.tariffData.length === 0) {
      this.addYear(); // This path ensures quarters are created with `dailySchedule` and `key`
    } else {
      this.tariffData.forEach(yearData => {
        yearData.quarters = yearData.quarters || []; // Ensure quarters array exists

        // Use a map to rebuild quarters to ensure no nulls/undefineds and consistency
        yearData.quarters = yearData.quarters.map(quarter => {
          // If a quarter from rawValue is null/undefined, replace it with a default empty one
          if (!quarter) {
            console.warn(`Found null/undefined quarter for year ${yearData.year}. Replacing with default.`);
            // Create a default quarter if the parsed one is invalid
            const defaultQuarterNum = 1; // Or try to infer if possible, but 1 is safe fallback
            const [startDate, endDate] = this.getQuarterDates(yearData.year, defaultQuarterNum);
            return {
              quarter: defaultQuarterNum,
              dailySchedule: [],
              formattedDateRange: DateUtils.formatQuarterDateRange(startDate, endDate, this.translate.instant("General.dateFormat")),
              key: `y${yearData.year}_q${defaultQuarterNum}`,
            };
          }

          // Ensure key is set
          quarter.key = `y${yearData.year}_q${quarter.quarter}`;

          // Ensure formattedDateRange is set
          const [startDate, endDate] = this.getQuarterDates(yearData.year, quarter.quarter);
          quarter.formattedDateRange = DateUtils.formatQuarterDateRange(startDate, endDate, this.translate.instant("General.dateFormat"));

          // Ensure dailySchedule array exists
          quarter.dailySchedule = quarter.dailySchedule || [];
          quarter.dailySchedule.forEach((schedule, index) => {
            if (schedule.originalIndex === undefined || schedule.originalIndex === null) {
              schedule.originalIndex = index;
            }
          });

          // !!! Crucial: Filter schedules once after parsing/initialization !!!
          this.updateFilteredSchedules(quarter); // Filter immediately after populating dailySchedule

          return quarter; // Return the processed quarter
        });
      });
    }
    this.initializeForm(); // Initialize expanded quarters states
  }
  private initializeForm() {
    this.tariffData.forEach(yearData => {
      yearData.quarters.forEach(q => {
        // Use the pre-calculated key for initialization
        if (q.key) { // Defensive check
          this.expandedQuarters[q.key] = false;
        }
      });
    });
  }

  /**
   * Helper method to update the pre-filtered schedule arrays for a given quarter.
   * Call this whenever quarter.dailySchedule is modified (add/remove).
   * @param quarter The quarter object whose schedules need to be updated.
   */
  private updateFilteredSchedules(quarter: Quarter) {
    // Ensure dailySchedule is an array before filtering
    const schedulesToFilter = quarter.dailySchedule || [];
    quarter.lowSchedules = schedulesToFilter.filter(s => s.tariff === "low");
    quarter.highSchedules = schedulesToFilter.filter(s => s.tariff === "high");
  }

}
