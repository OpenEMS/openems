package io.openems.edge.core.utility;

/**
 * This Interface holds the Method of Simply checking a String input if it contains only number / is only a number.
 */
public interface ContainsOnlyNumbers {

    String REG_EX_VALID_NUMBER_CHECK = "[-+]?([0-9]*[.][0-9]+|[0-9]+)";

    /**
     * Small helper method to determine if a String contains only numbers with decimals.
     *
     * @param value the String that will be checked, if it's only containing numbers.
     * @return the result oft the match with the regex that describes only numbers.
     */
    static boolean containsOnlyValidNumbers(String value) {
        return value.matches(REG_EX_VALID_NUMBER_CHECK);
    }

}
