package dev.hugoservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CloudCategories {
    public static final int SLOTS = 12;
    private static final Pattern ID = Pattern.compile("^[a-z][a-z0-9]{1,15}$");
    private static final Pattern ICON = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final Pattern PATH = Pattern.compile("^assets/[a-z0-9_./$-]+$");
    public static final Set<String> BUILT_IN = Set.of(
            "crystal", "anchor", "armor", "fire", "sky", "obsidian", "sword", "pickaxe", "glowstone",
            "gap", "pearl", "hotbar", "axe", "totem", "stone", "deepslate", "snow", "grass",
            "inventory", "cosmetics", "extra", "upload", "cloud", "mypack");
    private static volatile List<Def> defs = List.of();

    private CloudCategories() {
    }

    public static List<Def> all() {
        return defs;
    }

    public static Def get(String id) {
        for (Def d : defs) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    public static Def forGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        if (groupId.equals("spears")) {
            return get("spear");
        }
        return get(groupId);
    }

    public static boolean known(String category) {
        return BUILT_IN.contains(category) || CloudCategories.get(category) != null;
    }

    public static boolean read(JsonObject root) {
        ArrayList<Def> parsed = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        HashSet<Integer> slots = new HashSet<>();
        for (String key : new String[]{"categories", "adminCategories"}) {
            if (!root.has(key) || !root.get(key).isJsonArray()) {
                continue;
            }
            for (JsonElement e : root.getAsJsonArray(key)) {
                try {
                    Def d = CloudCategories.parse(e.getAsJsonObject(), key.equals("adminCategories"));
                    if (d == null || !ids.add(d.id()) || !slots.add(d.slot())) {
                        continue;
                    }
                    parsed.add(d);
                } catch (RuntimeException ignored) {
                }
            }
        }
        parsed.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        boolean changed = !CloudCategories.ids(parsed).equals(CloudCategories.ids(defs));
        defs = List.copyOf(parsed);
        return changed;
    }

    public static void readJson(String json) {
        CloudCategories.read(JsonParser.parseString(json).getAsJsonObject());
    }

    private static List<String> ids(List<Def> list) {
        ArrayList<String> out = new ArrayList<>();
        for (Def d : list) {
            out.add(d.slot() + ":" + d.id() + ":" + d.name() + ":" + d.icon());
        }
        return out;
    }

    private static Def parse(JsonObject o, boolean admin) {
        String id = CloudCategories.str(o, "id");
        String name = CloudCategories.str(o, "name");
        int slot = o.has("slot") ? o.get("slot").getAsInt() : -1;
        if (id == null || !ID.matcher(id).matches() || BUILT_IN.contains(id) || slot < 1 || slot > 12
                || name == null || name.isBlank() || name.length() > 20) {
            return null;
        }
        String icon = CloudCategories.str(o, "icon");
        if (icon == null || !ICON.matcher(icon).matches()) {
            icon = "minecraft:paper";
        }
        JsonArray files = o.has("files") && o.get("files").isJsonArray() ? o.getAsJsonArray("files") : new JsonArray();
        ArrayList<Rule> rules = new ArrayList<>();
        for (JsonElement f : files) {
            if (rules.size() >= 32 || !f.isJsonObject()) {
                break;
            }
            String match = CloudCategories.str(f.getAsJsonObject(), "match");
            String to = CloudCategories.str(f.getAsJsonObject(), "to");
            if (match == null || to == null || match.length() > 300 || !PATH.matcher(to).matches() || to.contains("..")) {
                continue;
            }
            rules.add(new Rule(Pattern.compile(match), to));
        }
        if (rules.isEmpty()) {
            return null;
        }
        String detect = CloudCategories.str(o, "detect");
        String type = "item";
        String top = null;
        String side = null;
        double[] crop = null;
        if (o.has("preview") && o.get("preview").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("preview");
            type = "block".equals(CloudCategories.str(p, "type")) ? "block" : "item";
            top = CloudCategories.str(p, type.equals("block") ? "top" : "texture");
            side = CloudCategories.str(p, "side");
            if (p.has("crop") && p.get("crop").isJsonArray() && p.getAsJsonArray("crop").size() == 4) {
                JsonArray c = p.getAsJsonArray("crop");
                double[] r = new double[]{c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble(), c.get(3).getAsDouble()};
                if (r[0] >= 0.0 && r[1] >= 0.0 && r[2] > 0.0 && r[3] > 0.0 && r[0] + r[2] <= 1.0001 && r[1] + r[3] <= 1.0001) {
                    crop = r;
                }
            }
        }
        return new Def(id, slot, name.trim(), icon, List.copyOf(rules), detect, type, CloudCategories.clean(top), CloudCategories.clean(side), crop, admin);
    }

    private static String clean(String path) {
        return path != null && PATH.matcher(path.toLowerCase(Locale.ROOT)).matches() && !path.contains("..")
                ? path.toLowerCase(Locale.ROOT) : null;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    public static String target(String lowerPath, String category) {
        Def d = CloudCategories.get(category);
        if (d == null) {
            return null;
        }
        for (Rule r : d.rules()) {
            Matcher m = r.match().matcher(lowerPath);
            if (!m.matches()) {
                continue;
            }
            try {
                String to = m.replaceFirst(r.to());
                if (!PATH.matcher(to).matches() || to.contains("..")) {
                    continue;
                }
                return to;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public static String toGroupPath(String assetsPath) {
        String p = assetsPath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String prefix = "assets/minecraft/";
        if (p.startsWith(prefix)) {
            p = p.substring(prefix.length());
        }
        return p;
    }

    public static List<String> wantedFiles(Def def) {
        LinkedHashSet<String> files = new LinkedHashSet<>();
        if (def.previewTop() != null) {
            files.add(toGroupPath(def.previewTop()));
        }
        if (def.previewSide() != null) {
            files.add(toGroupPath(def.previewSide()));
        }
        for (Rule rule : def.rules()) {
            for (String path : expandRule(rule)) {
                if (path != null && !path.contains("$") && !path.contains("..") && !path.isBlank()) {
                    files.add(path);
                }
            }
        }
        return List.copyOf(files);
    }

    static List<String> expandRule(Rule rule) {
        String to = rule.to();
        if (!to.contains("$")) {
            return List.of(toGroupPath(to));
        }
        List<String> samples = finiteStrings(rule.match().pattern(), 64);
        if (samples.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String sample : samples) {
            Matcher m = rule.match().matcher(sample);
            if (!m.matches()) {
                continue;
            }
            try {
                String dest = m.replaceFirst(to);
                if (PATH.matcher(dest).matches() && !dest.contains("$") && !dest.contains("..")) {
                    out.add(toGroupPath(dest));
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(out);
    }

    static List<String> finiteStrings(String regex, int cap) {
        String p = regex;
        if (p.startsWith("^")) {
            p = p.substring(1);
        }
        if (p.endsWith("$") && (p.length() < 2 || p.charAt(p.length() - 2) != '\\')) {
            p = p.substring(0, p.length() - 1);
        }
        ArrayList<String> out = new ArrayList<>();
        if (!expandRegex(p, 0, "", out, cap)) {
            return List.of();
        }
        return out;
    }

    private static boolean expandRegex(String p, int i, String cur, List<String> out, int cap) {
        if (out.size() > cap) {
            return false;
        }
        if (i >= p.length()) {
            out.add(cur);
            return true;
        }
        char c = p.charAt(i);
        if (c == '^' || c == '$') {
            return expandRegex(p, i + 1, cur, out, cap);
        }
        if (c == '\\' && i + 1 < p.length()) {
            char n = p.charAt(i + 1);
            String lit = n == 'Q' ? null : String.valueOf(unescape(n));
            if (lit == null) {
                return false;
            }
            boolean opt = i + 2 < p.length() && p.charAt(i + 2) == '?';
            int next = opt ? i + 3 : i + 2;
            if (opt && !expandRegex(p, next, cur, out, cap)) {
                return false;
            }
            return expandRegex(p, next, cur + lit, out, cap);
        }
        if (c == '(') {
            int end = matchingParen(p, i);
            if (end < 0) {
                return false;
            }
            String inner = p.substring(i + 1, end);
            if (inner.startsWith("?:")) {
                inner = inner.substring(2);
            } else if (inner.startsWith("?=" ) || inner.startsWith("?!") || inner.startsWith("?<")) {
                return false;
            }
            boolean optional = end + 1 < p.length() && p.charAt(end + 1) == '?';
            int next = optional ? end + 2 : end + 1;
            ArrayList<String> pieces = new ArrayList<>();
            for (String alt : splitTopAlts(inner)) {
                ArrayList<String> sub = new ArrayList<>();
                if (!expandRegex(alt, 0, "", sub, cap)) {
                    return false;
                }
                pieces.addAll(sub);
            }
            if (optional) {
                pieces.add("");
            }
            for (String piece : pieces) {
                if (!expandRegex(p, next, cur + piece, out, cap)) {
                    return false;
                }
            }
            return true;
        }
        if (c == '[') {
            int end = p.indexOf(']', i);
            if (end < 0) {
                return false;
            }
            int next = end + 1;
            if (next < p.length() && (p.charAt(next) == '+' || p.charAt(next) == '*' || p.charAt(next) == '{')) {
                return false;
            }
            boolean optional = next < p.length() && p.charAt(next) == '?';
            if (optional) {
                next++;
            }
            List<Character> chars = classChars(p.substring(i + 1, end));
            if (chars.isEmpty() || chars.size() > 16) {
                return false;
            }
            if (optional && !expandRegex(p, next, cur, out, cap)) {
                return false;
            }
            for (char ch : chars) {
                if (!expandRegex(p, next, cur + ch, out, cap)) {
                    return false;
                }
            }
            return true;
        }
        if (c == '.' || c == '+' || c == '*' || c == '|' || c == '?' || c == '{') {
            return false;
        }
        boolean optional = i + 1 < p.length() && p.charAt(i + 1) == '?';
        int next = optional ? i + 2 : i + 1;
        if (optional && !expandRegex(p, next, cur, out, cap)) {
            return false;
        }
        return expandRegex(p, next, cur + c, out, cap);
    }

    private static char unescape(char n) {
        return switch (n) {
            case 'n' -> '\n';
            case 't' -> '\t';
            default -> n;
        };
    }

    private static int matchingParen(String p, int open) {
        int depth = 0;
        for (int i = open; i < p.length(); ++i) {
            char c = p.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitTopAlts(String inner) {
        ArrayList<String> alts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inner.length(); ++i) {
            char c = inner.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == '|' && depth == 0) {
                alts.add(inner.substring(start, i));
                start = i + 1;
            }
        }
        alts.add(inner.substring(start));
        return alts;
    }

    private static List<Character> classChars(String body) {
        if (body.startsWith("^")) {
            return List.of();
        }
        ArrayList<Character> chars = new ArrayList<>();
        for (int i = 0; i < body.length(); ++i) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                chars.add(unescape(body.charAt(i + 1)));
                i++;
                continue;
            }
            if (i + 2 < body.length() && body.charAt(i + 1) == '-' ) {
                char a = c;
                char b = body.charAt(i + 2);
                if (b < a || b - a > 15) {
                    return List.of();
                }
                for (char ch = a; ch <= b; ++ch) {
                    chars.add(ch);
                }
                i += 2;
                continue;
            }
            chars.add(c);
        }
        return chars;
    }

    public record Def(String id, int slot, String name, String icon, List<Rule> rules, String detect,
                      String previewType, String previewTop, String previewSide, double[] crop, boolean admin) {
    }

    public record Rule(Pattern match, String to) {
    }
}
