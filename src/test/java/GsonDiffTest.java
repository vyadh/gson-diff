import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GsonDiffTest {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Test
    void noDifferencesResultsInEmptyDocument() {
        var data1 = gson.toJsonTree(Map.of("key1", "value1", "key2", 42));
        var data2 = gson.toJsonTree(Map.of("key1", "value1", "key2", 42));

        var result = GsonDiff.diff(data1, data2);

        assertThat(result.isJsonObject()).isTrue();
        assertThat(result.getAsJsonObject().isEmpty()).isTrue();
    }

    @Test
    void returnOtherObjectWhenOneIsNull() {
        var data = gson.toJsonTree(Map.of("key", "value"));

        assertThat(GsonDiff.diff(data, null)).isSameAs(data);
        assertThat(GsonDiff.diff(null, data)).isSameAs(data);
    }

    @Test
    void returnSecondWhenFirstIsDifferentClass() {
        var data1 = new JsonObject();
        var data2 = new JsonPrimitive("after");

        var result = GsonDiff.diff(data1, data2);

        assertThat(result).isSameAs(data2);
    }

    @Test
    void differenceWhenPrimitive() {
        var data1 = new JsonPrimitive("before");
        var data2 = new JsonPrimitive("after");

        var result = GsonDiff.diff(data1, data2);

        assertThat(result).isEqualTo(new JsonPrimitive("after"));
    }

    @Test
    void returnBasicDifferencesInValues() {
        var data1 = gson.toJsonTree(Map.of("string", "x", "int", 42, "boolean", true));
        var data2 = gson.toJsonTree(Map.of("string", "y", "int", 43, "boolean", false));

        var result = GsonDiff.diff(data1, data2).getAsJsonObject().asMap();

        assertThat(result).containsOnly(
                Map.entry("string", new JsonPrimitive("y")),
                Map.entry("int", new JsonPrimitive(43)),
                Map.entry("boolean", new JsonPrimitive(false))
        );
    }

    @Test
    void returnAddedEntries() {
        var data1 = gson.toJsonTree(Map.of("key1", "one"));
        var data2 = gson.toJsonTree(Map.of("key1", "one", "key2", "two"));

        var result = GsonDiff.diff(data1, data2).getAsJsonObject().asMap();

        assertThat(result).containsOnly(
                Map.entry("key2", new JsonPrimitive("two"))
        );
    }

    @Test
    void returnNullElementForRemovedEntries() {
        var data1 = gson.toJsonTree(Map.of("key1", "one", "key2", "two"));
        var data2 = gson.toJsonTree(Map.of("key1", "one"));

        var result = GsonDiff.diff(data1, data2).getAsJsonObject().asMap();

        assertThat(result).containsOnly(
                Map.entry("key2", JsonNull.INSTANCE)
        );
    }

    @Test
    void returnNullElementForNullValues() {
        var data1 = Map.of("key1", "one", "key2", "two");
        var data2 = new HashMap<>();
        data2.put("key1", "one");
        data2.put("key2", null);

        var result = GsonDiff.diff(gson.toJsonTree(data1), gson.toJsonTree(data2)).getAsJsonObject().asMap();

        assertThat(result).containsOnly(
                Map.entry("key2", JsonNull.INSTANCE)
        );
    }

    @Test
    void returnDifferencesInSubtree() {
        var data1 = Map.of("key", "value", "subtree", Map.of(
                "same", "one",
                "changed", "two",
                "removed", "three"
        ));
        var data2 = Map.of("key", "value", "subtree", Map.of(
                "same", "one",
                "changed", "2",
                "added", "4"
        ));

        var result = GsonDiff.diff(gson.toJsonTree(data1), gson.toJsonTree(data2));

        assertThat(result).isEqualTo(gson.toJsonTree(Map.of(
                "subtree", gson.toJsonTree(Map.of(
                        "changed", new JsonPrimitive("2"),
                        "removed", JsonNull.INSTANCE,
                        "added", new JsonPrimitive("4")
                ))
        )));
    }

    @Test
    void returnDifferenceInSerialisedObjects() {
        var data1 = new Data("one", false, 42,
                new Subtree(null, 11, true));
        var data2 = new Data("two", true, 43,
                new Subtree("value", 22, null));

        var result = GsonDiff.diff(data1, data2);

        assertThat(result).isEqualTo(gson.toJsonTree(Map.of(
                "string", "two",
                "number", 43,
                "bool", true,
                "subtree", gson.toJsonTree(Map.of(
                        "changed", new JsonPrimitive(22),
                        "removed", JsonNull.INSTANCE,
                        "added", new JsonPrimitive("value")
                ))
        )));
    }

    public record Data(String string, boolean bool, int number, Subtree subtree) {}
    public record Subtree(String added, Integer changed, Boolean removed) {}

}
