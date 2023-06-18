import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create a minimal JSON diff of two objects using Gson.
 */
public class GsonDiff {

    public static JsonElement diff(Object a, Object b) {
        var gson = new GsonBuilder().serializeNulls().create();
        return diff(gson.toJsonTree(a), gson.toJsonTree(b));
    }

    /**
     * The recursive difference between a and b. For example, when
     * `a = {"key0": "val", "key1": "abc", "key2": "xyz"}` and
     * `b = {"key0": "val", ""key1": "bcd", "key3": "123"}` the result will be:
     * `{"key1": "bcd", "key2": null, "key3": "123"}`.
     *
     * @param a the base object
     * @param b the object being compared to
     * @return the difference from the base object
     */
    public static JsonElement diff(JsonElement a, JsonElement b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (!a.getClass().equals(b.getClass())) {
            return b;
        }
        if (a.isJsonObject() && b.isJsonObject()) {
            return diffEntries(a.getAsJsonObject(), b.getAsJsonObject());
        }
        if (a.isJsonArray() && b.isJsonArray()) {
            throw new UnsupportedOperationException("JSON arrays not supported");
        }
        if (!b.equals(a)) {
            return b;
        }
        return new JsonObject();
    }

    private static JsonObject diffEntries(JsonObject a, JsonObject b) {
        var result = new JsonObject();

        var as = a.keySet();
        var bs = b.keySet();

        for (String key : unionKeys(as, bs)) {
            // Added
            if (!as.contains(key) && bs.contains(key)) {
                result.add(key, b.get(key));
            }
            // Removed
            else if (as.contains(key) && !bs.contains(key)) {
                result.add(key, JsonNull.INSTANCE);
            }
            // Changed
            else if (as.contains(key)) {
                JsonElement aValue = a.get(key);
                JsonElement bValue = b.get(key);
                if (!aValue.equals(bValue)) {
                    result.add(key, diff(aValue, bValue));
                }
            }
        }

        return result;
    }

    private static Set<String> unionKeys(Set<String> a, Set<String> b) {
        return Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet());
    }

}
