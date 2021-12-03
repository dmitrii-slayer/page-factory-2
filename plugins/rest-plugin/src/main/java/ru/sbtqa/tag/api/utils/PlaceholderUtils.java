package ru.sbtqa.tag.api.utils;

import com.google.common.reflect.TypeToken;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import ru.sbtqa.tag.api.EndpointEntry;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class PlaceholderUtils {

    private static final String QUOTE = "\"";

    private PlaceholderUtils() {
    }

    /**
     * Replace placeholders in string on parameters
     *
     * @param string replace placeholders in this string
     * @param parameters replace these parameters
     * @return string with replaced placeholders
     */
    public static String replaceTemplatePlaceholders(EndpointEntry entry, String string, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            String parameterName = parameter.getKey();
            Object parameterValue = parameter.getValue();

            if (isFieldExists(entry, parameterName)) {
                Field declaredField = FieldUtils.getAllFieldsList(entry.getClass())
                        .stream().filter(field -> field.getName().equals(parameterName))
                        .findFirst().orElseThrow(() -> new AutotestError("This error should never appear"));
                string = replacePlaceholder(string, declaredField, parameterName, parameterValue);
            } else {
                string = replacePlaceholder(string, null, parameterName, parameterValue);
            }
        }

        return string;
    }

    /**
     * Replace placeholders in string
     *
     * @param string replace placeholders in this string
     * @return string with replaced placeholders
     */
    public static String replaceTemplatePlaceholders(String string) {
        if (string.startsWith("${") && string.endsWith("}")) {
            String unquoteString = string.replaceAll("^\\$\\{", "").replaceAll("\\}$", "");
            String property = System.getProperty(unquoteString);
            return property != null ? property : string;
        }

        return string;
    }

    /**
     * Replace Json template placeholders in string on parameters
     *
     * @param jsonString replace placeholders in this json string
     * @param parameters replace these parameters
     * @return json string with replaced placeholders
     */
    public static String replaceJsonTemplatePlaceholders(EndpointEntry entry, String jsonString, Map<String, Object> parameters) {
        Set<Map.Entry<String, Object>> mandatoryValues = parameters.entrySet().stream()
                .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                .collect(Collectors.toSet());
        for (Map.Entry<String, Object> parameter : mandatoryValues) {
            String parameterName = parameter.getKey();
            Object parameterValue = parameter.getValue();

            if (isFieldExists(entry, parameterName)) {
                Field declaredField = FieldUtils.getAllFieldsList(entry.getClass())
                        .stream().filter(field -> field.getName().equals(parameterName))
                        .findFirst().orElseThrow(() -> new AutotestError("This error should never appear"));
                jsonString = replacePlaceholder(jsonString, declaredField, parameterName, parameterValue);
            } else {
                jsonString = replacePlaceholder(jsonString, null, parameterName, parameterValue);
            }

        }

        return jsonString;
    }

    private static boolean isFieldExists(EndpointEntry entry, String fieldName) {
        return FieldUtils.getAllFieldsList(entry.getClass()).stream()
                .anyMatch(field -> field.getName().equals(fieldName));
    }

    public static String removeOptionals(String jsonString, Map<String, Object> parameters) {
        Set<Map.Entry<String, Object>> optionals = parameters.entrySet().stream()
                .filter(stringObjectEntry -> stringObjectEntry.getValue() == null)
                .collect(Collectors.toSet());

        for (Map.Entry<String, Object> parameter : optionals) {
            String toRemoveRegex = "(\"[^\"]+\"\\s*:\\s*\"?\\$\\{" + parameter.getKey() + "}\"?\\s*,?)";
            jsonString = jsonString.replaceAll(toRemoveRegex, "");
        }

        String orphanCommaRegex = "(,)(\\s*})";
        return jsonString.replaceAll(orphanCommaRegex, "$2");
    }

    public static String removeEmptyObjects(String jsonString) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> data = new Gson().fromJson(jsonString, type);

        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonCleaner(data));
    }

    private static Map<String, Object> jsonCleaner(Map<String, Object> jsonData) {
        for (Iterator<Map.Entry<String, Object>> it = jsonData.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Object> entry = it.next();
            Object value = entry.getValue();
            if (value == null
                    || (value instanceof String && ((String) value).isEmpty())
                    || (value instanceof Map && ((Map<?, ?>) value).isEmpty())
                    || (value instanceof ArrayList && ((ArrayList<?>) value).isEmpty())) {
                it.remove();
            } else if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                entry.setValue(jsonCleaner((Map<String, Object>) value));
                if (entry.getValue() instanceof Map && ((Map<?, ?>) value).isEmpty()
                        || (value instanceof ArrayList && ((ArrayList<?>) value).isEmpty())) {
                    it.remove();
                }
            }
            if (value instanceof ArrayList) {
                List<?> list = ((ArrayList<?>) value).stream().filter(item -> {
                    if (item == null
                            || (item instanceof String && ((String) item).isEmpty())
                            || (item instanceof Map && ((Map<?, ?>) item).isEmpty())
                            || (item instanceof ArrayList && ((ArrayList<?>) item).isEmpty())) {
                        return false;
                    } else if (item instanceof Map && !((Map<?, ?>) item).isEmpty()) {
                        Map<String, Object> parsedArrayItem = jsonCleaner((Map<String, Object>) item);
                        if (parsedArrayItem instanceof Map && ((Map<?, ?>) parsedArrayItem).isEmpty()
                                || (parsedArrayItem instanceof ArrayList && ((ArrayList<?>) parsedArrayItem).isEmpty())) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                if (list.isEmpty()) {
                    it.remove();
                } else {
                    entry.setValue(list);
                }
            }
        }
        return jsonData;
    }

    /**
     * Replace placeholder in string on value
     *
     * @param string replace placeholders in this string
     * @param declaredField endpoint field to validate type
     * @param name placeholder body (without start and finish marks)
     * @param newValue replace placeholder on this mark
     * @return string with replaced placeholder
     */
    public static String replacePlaceholder(String string, Field declaredField, String name, Object newValue) {
        String value = String.valueOf(newValue);
        String[] nullables = new String[]{"null", QUOTE + "null" + QUOTE, QUOTE + QUOTE};

        String placeholder = Pattern.quote(createPlaceholder(name));
        if (!(newValue instanceof String) || (declaredField != null && declaredField.getType() != String.class)) {
            placeholder = QUOTE + placeholder + QUOTE;
        } else if (Arrays.asList(nullables).contains(newValue)) {
            placeholder = QUOTE + "?" + placeholder + QUOTE + "?";
        }

        return string.replaceAll(placeholder, value);
    }

    public static String createPlaceholder(String placeholderName) {
        return format("${%s}", placeholderName);
    }
}
