package anonymous.group;

import java.util.BitSet;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;

/**
 * Utility methods.
 * 
 */
public class Utils {

    /**
     * Converts a string to proper case, e.g. test to Test.
     * @param s
     * @return 
     */
    public static String toProperCase(String s) {
        if (s == null) {
            return null;
        }

        if (s.length() == 0) {
            return s;
        }

        if (s.length() == 1) {
            return s.toUpperCase();
        }

        return s.substring(0, 1).toUpperCase()
                + s.substring(1).toLowerCase();
    }

    //taken from https://stackoverflow.com/a/27995662/8540029
    public static List<boolean[]> bool(int n) {
        return IntStream.range(0, (int) Math.pow(2, n))
                .mapToObj(i -> new long[]{i})
                .map(BitSet::valueOf)
                .map(bs -> bitSetToArray(bs, n))
                .collect(toList());
    }

    private static boolean[] bitSetToArray(BitSet bs, int width) {
        boolean[] result = new boolean[width];
        bs.stream().forEach(i -> result[i] = true);
        return result;
    }
}
