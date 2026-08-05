package com.github.aeddddd.ae2enhanced.guide;

import java.util.Objects;

/**
 * 指南跳转目标 —— 页面 id + 可选页内锚点（对齐 GuideME PageAnchor）.
 */
public final class PageAnchor {

    private final String pageId;
    private final String anchor;

    public PageAnchor(String pageId, String anchor) {
        this.pageId = Objects.requireNonNull(pageId, "pageId");
        this.anchor = anchor;
    }

    public String getPageId() {
        return pageId;
    }

    public String getAnchor() {
        return anchor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageAnchor)) return false;
        PageAnchor other = (PageAnchor) o;
        return pageId.equals(other.pageId) && Objects.equals(anchor, other.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, anchor);
    }

    @Override
    public String toString() {
        return anchor == null ? pageId : pageId + "#" + anchor;
    }
}
