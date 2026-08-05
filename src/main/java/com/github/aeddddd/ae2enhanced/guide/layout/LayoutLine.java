package com.github.aeddddd.ae2enhanced.guide.layout;

import com.github.aeddddd.ae2enhanced.guide.md.element.BlockElement;
import com.github.aeddddd.ae2enhanced.guide.md.element.InlineElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 一行排版结果 —— 若干原子（文本 run 或物品图标）+ 行高.
 * 每个原子回指源 InlineElement，供点击命中与 tooltip 悬停。
 */
public final class LayoutLine {

    /**
     * 行内原子：文本 run 或 16px 物品图标.
     */
    public static final class Atom {
        public final int x;                 // 行内 x 偏移
        public final int width;
        public final String text;           // 文本原子：显示文本（含 § 样式码由渲染层处理）
        public final boolean bold;
        public final InlineElement source;  // 源内联元素

        Atom(int x, int width, String text, boolean bold, InlineElement source) {
            this.x = x;
            this.width = width;
            this.text = text;
            this.bold = bold;
            this.source = source;
        }

        public boolean isIcon() {
            return source != null && source.getKind() == InlineElement.Kind.ITEM_LINK;
        }
    }

    public final int y;             // 行顶部 y（相对页面内容原点）
    public final int height;        // 行高 = max(文本行高, 图标高度)
    public final List<Atom> atoms = new ArrayList<>();
    public BlockElement.Type blockType = BlockElement.Type.PARAGRAPH; // 行所属块类型（渲染样式用）
    public int headingDepth;                                           // 仅标题行有效

    public LayoutLine(int y, int height) {
        this.y = y;
        this.height = height;
    }

    /**
     * 命中检测：返回 x 处的原子，未命中返回 null.
     */
    public Atom atomAt(int lineX) {
        for (Atom atom : atoms) {
            if (lineX >= atom.x && lineX < atom.x + atom.width) {
                return atom;
            }
        }
        return null;
    }
}
