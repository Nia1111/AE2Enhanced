package com.github.aeddddd.ae2enhanced.guide.loader;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 front-matter 解析器（对齐 GuideME Frontmatter，但不引入 YAML 库）.
 * 仅支持固定结构：
 * <pre>
 * ---
 * navigation:
 *   title: xxx        # 必填
 *   parent: xxx.md    # 可选
 *   position: 10      # 可选整数
 *   icon: item_id     # 可选
 * item_ids: [a, b]    # 可选字符串列表
 * ---
 * </pre>
 */
public final class FrontMatterParser {

    private FrontMatterParser() {}

    public static final class FrontMatter {
        public String title;
        public String parent;
        public int position;
        public String icon;
        public final List<String> itemIds = new ArrayList<>();

        public boolean hasNavigation() {
            return title != null && !title.isEmpty();
        }
    }

    /**
     * 解析结果：front-matter + 去掉 front-matter 后的正文.
     */
    public static final class ParseResult {
        public final FrontMatter frontMatter;
        public final String body;

        ParseResult(FrontMatter frontMatter, String body) {
            this.frontMatter = frontMatter;
            this.body = body;
        }
    }

    /**
     * 若内容以 --- 开头则解析 front-matter，否则返回空 front-matter + 原文.
     */
    public static ParseResult parse(String content) {
        FrontMatter fm = new FrontMatter();
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.startsWith("---\n")) {
            return new ParseResult(fm, normalized);
        }
        int end = normalized.indexOf("\n---", 4);
        if (end < 0) {
            return new ParseResult(fm, normalized);
        }
        // \n--- 后必须是行尾（\n 或文档结束）
        int afterEnd = end + 4;
        if (afterEnd < normalized.length() && normalized.charAt(afterEnd) != '\n') {
            return new ParseResult(fm, normalized);
        }
        String yaml = normalized.substring(4, end);
        String body = afterEnd < normalized.length() ? normalized.substring(afterEnd + 1) : "";

        parseLines(fm, yaml.split("\n"));
        return new ParseResult(fm, body);
    }

    private static void parseLines(FrontMatter fm, String[] lines) {
        boolean inNavigation = false;
        for (String rawLine : lines) {
            if (rawLine.trim().isEmpty()) continue;
            boolean indented = rawLine.startsWith("  ") || rawLine.startsWith("\t");
            String line = rawLine.trim();
            if (!indented) {
                inNavigation = false;
                if (line.equals("navigation:")) {
                    inNavigation = true;
                } else if (line.startsWith("item_ids:")) {
                    parseStringList(line.substring("item_ids:".length()).trim(), fm.itemIds);
                }
            } else if (inNavigation) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                switch (key) {
                    case "title":
                        fm.title = value;
                        break;
                    case "parent":
                        fm.parent = value;
                        break;
                    case "position":
                        try {
                            fm.position = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                            // 非法 position 按 0 处理
                        }
                        break;
                    case "icon":
                        fm.icon = value;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 解析 [a, b, c] 或行内逗号分隔的字符串列表.
     */
    private static void parseStringList(String value, List<String> out) {
        String v = value;
        if (v.startsWith("[") && v.endsWith("]")) {
            v = v.substring(1, v.length() - 1);
        }
        for (String part : v.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
    }
}
