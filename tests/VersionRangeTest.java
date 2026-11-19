import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Тесты выбора нижней границы диапазона версий.
 */
class VersionRangeTest {

    @Test
    void plainVersionIsItsOwnLowerBound() {
        assertEquals("4.3.0", VersionRange.lowerBound("4.3.0"));
    }

    @Test
    void openRangeGivesLowerBound() {
        assertEquals("13.0.1", VersionRange.lowerBound("[13.0.1, )"));
    }

    @Test
    void closedRangeGivesLowerBound() {
        assertEquals("1.0", VersionRange.lowerBound("[1.0, 2.0)"));
    }

    @Test
    void exactVersionInBrackets() {
        assertEquals("1.5.0", VersionRange.lowerBound("[1.5.0]"));
    }

    @Test
    void exclusiveLowerBoundIsTreatedAsInclusive() {
        assertEquals("1.0", VersionRange.lowerBound("(1.0, )"));
    }

    @Test
    void prereleaseVersionIsKept() {
        assertEquals("2.0.0-beta.1", VersionRange.lowerBound("[2.0.0-beta.1, )"));
    }

    @Test
    void rangeWithoutLowerBoundGivesEmpty() {
        assertEquals("", VersionRange.lowerBound("(, 2.0]"));
    }

    @Test
    void emptyAndStrangeValuesGiveEmpty() {
        assertEquals("", VersionRange.lowerBound(""));
        assertEquals("", VersionRange.lowerBound("   "));
        assertEquals("", VersionRange.lowerBound("1.0.*"));
        assertEquals("", VersionRange.lowerBound("abc"));
    }
}
