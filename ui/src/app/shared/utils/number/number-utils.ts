/**
 * Helper functions for interacting with numbers.
 */
export namespace NumberUtils {

  /**
   * Parses a string value to a number.
   *
   * @param value the value
   * @returns the casted value if parsable, else null
   */
  export function parseNumberSafely(value: string | null): number | null {
    if (value == null || value == "") {
      return null;
    }
    const castedValue = Number.parseInt(value);
    if (castedValue == null || (Number.isFinite(castedValue) == false)) {
      return null;
    }

    return castedValue;
  }

  /**
   * Parses a string value to a number.
   *
   * @param value the value
   * @param orElse the orElse if value is not parsable
   * @returns the casted value if parsable, else null
   */
  export function parseNumberSafelyOrElse(value: string | null, orElse: number): number {
    if (value == null) {
      return orElse;
    }

    const castedValue = NumberUtils.parseNumberSafely(value);

    if (castedValue == null) {
      return orElse;
    }

    return castedValue;
  }

  /**
   * Subtracts values from each other - possibly null values
   *
   * @param values the values
   * @returns a number, if at least one value is not null, else null
   */
  export function subtractSafely(...values: (number | null)[]): number | null {
    return values
      .filter(value => value !== null && value !== undefined)
      .reduce((sum: number | null, curr) => {
        if (sum == null) {
          sum = curr;
        } else {
          sum -= curr;
        }

        return sum;
      }, null);
  }

  /**
   * Dividing values from each other - possibly null values
   *
   * @param values the values
   * @returns a number, if at least one value is not null, else null
   */
  export function divideSafely(...values: (number | null)[]): number | null {
    return values
      .filter(value => value !== null && value !== undefined)
      .reduce((sum: number | null, curr) => {
        // Dont divide through 0
        if (curr == 0) {
          return sum;
        }

        if (sum == null) {
          sum = curr;
        } else {
          sum /= curr;
        }

        return sum;
      }, null);
  }

  /**
   * Multiplying values with each other - possibly null values
   *
   * @param values the values
   * @returns a number, if at least one value is not null, else null
   */
  export function multiplySafely(...values: (number | null)[]): number | null {
    return values
      .filter(value => value !== null && value !== undefined)
      .reduce((sum: number | null, curr) => {
        if (sum == null) {
          sum = curr;
        } else {
          sum *= curr;
        }

        return sum;
      }, null);
  }
}
