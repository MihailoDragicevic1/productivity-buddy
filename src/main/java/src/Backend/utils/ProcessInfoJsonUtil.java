package src.Backend.utils;

import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessInfoJsonUtil {

    public static class JsonProcessInfo {
        public String originalName;
        public String aliasName;
        public String category;
        public boolean isTrackingFreezed;
        public long totalTimeSeconds;
    }

    public static void createEmptyJsonFile(Path path) throws IOException {
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        Files.writeString(path, "{\n  \"processes\": []\n}");
    }

    public static List<JsonProcessInfo> parseJson(String json) {
        List<JsonProcessInfo> list = new ArrayList<>();

        Pattern objectPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(json);

        while (objectMatcher.find()) {
            String objectText = objectMatcher.group(1);

            if (!objectText.contains("originalName")) {
                continue;
            }

            JsonProcessInfo info = new JsonProcessInfo();

            info.originalName = extractStringValue(objectText, "originalName");
            info.aliasName = extractStringValue(objectText, "aliasName");
            info.category = extractStringValue(objectText, "category");
            info.isTrackingFreezed = extractBooleanValue(objectText, "isTrackingFreezed");
            info.totalTimeSeconds = extractLongValue(objectText, "totalTimeSeconds");

            if (info.aliasName == null || info.aliasName.isBlank()) {
                info.aliasName = info.originalName;
            }

            if (info.category == null || info.category.isBlank()) {
                info.category = "uncategorized";
            }

            list.add(info);
        }

        return list;
    }

    public static void writeJson(Path path, List<JsonProcessInfo> infos) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"processes\": [\n");

        for (int i = 0; i < infos.size(); i++) {
            JsonProcessInfo info = infos.get(i);

            sb.append("    {\n");
            sb.append("      \"originalName\": \"").append(escapeJson(info.originalName)).append("\",\n");
            sb.append("      \"aliasName\": \"").append(escapeJson(info.aliasName)).append("\",\n");
            sb.append("      \"category\": \"").append(escapeJson(info.category)).append("\",\n");
            sb.append("      \"isTrackingFreezed\": ").append(info.isTrackingFreezed).append(",\n");
            sb.append("      \"totalTimeSeconds\": ").append(info.totalTimeSeconds).append("\n");
            sb.append("    }");

            if (i < infos.size() - 1) {
                sb.append(",");
            }

            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        Files.writeString(path, sb.toString());
    }

    public static List<JsonProcessInfo> mergeProcessesByName(ConcurrentHashMap<ProcessKey, ProcessData> mapToWrite) {
        ConcurrentHashMap<String, JsonProcessInfo> merged = new ConcurrentHashMap<>();

        for (ProcessKey key : mapToWrite.keySet()) {
            ProcessData data = mapToWrite.get(key);

            if (data == null) {
                continue;
            }

            upsertProcessInfo(merged, data);
        }

        return new ArrayList<>(merged.values());
    }

    public static void upsertProcessInfo(List<JsonProcessInfo> infos, ProcessData data) {
        if (infos == null || data == null) {
            return;
        }

        String originalName = data.getName();
        if (originalName == null || originalName.isBlank()) {
            return;
        }

        JsonProcessInfo target = null;

        for (JsonProcessInfo info : infos) {
            if (info.originalName != null && info.originalName.equalsIgnoreCase(originalName)) {
                target = info;
                break;
            }
        }

        if (target == null) {
            target = new JsonProcessInfo();
            target.originalName = originalName;
            target.aliasName = (data.getAlias() == null || data.getAlias().isBlank()) ? originalName : data.getAlias();
            target.category = (data.getCategory() == null || data.getCategory().isBlank()) ? "uncategorized" : data.getCategory();
            target.isTrackingFreezed = data.isTrackingFrozen();
            target.totalTimeSeconds = data.getTotalTimeSeconds();
            infos.add(target);
            return;
        }

        if (data.getAlias() != null && !data.getAlias().isBlank()) {
            target.aliasName = data.getAlias();
        }

        if (data.getCategory() != null && !data.getCategory().isBlank()) {
            target.category = data.getCategory();
        }

        target.isTrackingFreezed = data.isTrackingFrozen();

        if (data.getTotalTimeSeconds() > target.totalTimeSeconds) {
            target.totalTimeSeconds = data.getTotalTimeSeconds();
        }
    }

    private static void upsertProcessInfo(ConcurrentHashMap<String, JsonProcessInfo> merged, ProcessData data) {
        String originalName = data.getName();
        if (originalName == null || originalName.isBlank()) {
            return;
        }

        JsonProcessInfo existing = merged.get(originalName);

        if (existing == null) {
            JsonProcessInfo info = new JsonProcessInfo();
            info.originalName = originalName;
            info.aliasName = (data.getAlias() == null || data.getAlias().isBlank()) ? originalName : data.getAlias();
            info.category = (data.getCategory() == null || data.getCategory().isBlank()) ? "uncategorized" : data.getCategory();
            info.isTrackingFreezed = data.isTrackingFrozen();
            info.totalTimeSeconds = data.getTotalTimeSeconds();

            merged.put(originalName, info);
        } else {
            if (data.getAlias() != null && !data.getAlias().isBlank()) {
                existing.aliasName = data.getAlias();
            }

            if (data.getCategory() != null && !data.getCategory().isBlank()) {
                existing.category = data.getCategory();
            }

            existing.isTrackingFreezed = data.isTrackingFrozen();

            if (data.getTotalTimeSeconds() > existing.totalTimeSeconds) {
                existing.totalTimeSeconds = data.getTotalTimeSeconds();
            }
        }
    }

    private static String extractStringValue(String text, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private static boolean extractBooleanValue(String text, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(true|false)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }

        return false;
    }

    private static long extractLongValue(String text, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return 0;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}