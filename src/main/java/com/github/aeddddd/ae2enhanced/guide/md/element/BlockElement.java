package com.github.aeddddd.ae2enhanced.guide.md.element;

import java.util.ArrayList;
import java.util.List;

/**
 * 块级元素 —— 页面的顶层结构（对齐 GuideME document/block）.
 */
public final class BlockElement {

    public enum Type {
        PARAGRAPH,
        HEADING,
        LIST_ITEM
    }

    private final Type type;
    private final int headingDepth;          // 仅 HEADING 有效（1-3）
    private final List<InlineElement> children;

    public BlockElement(Type type, int headingDepth) {
        this.type = type;
        this.headingDepth = headingDepth;
        this.children = new ArrayList<>();
    }

    public Type getType() {
        return type;
    }

    public int getHeadingDepth() {
        return headingDepth;
    }

    public List<InlineElement> getChildren() {
        return children;
    }

    /**
     * 提取纯文本（供搜索索引与锚点生成）.
     */
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        for (InlineElement el : children) {
            if (el.getText() != null) {
                sb.append(el.getText());
            } else if (el.getKind() == InlineElement.Kind.ITEM_LINK && el.getItemStack() != null) {
                sb.append(el.getItemStack().getDisplayName());
            }
        }
        return sb.toString();
    }
}
