package com.github.aeddddd.ae2enhanced.client.specialcrafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.I18n;

import appeng.api.storage.data.IAEItemStack;

import com.github.aeddddd.ae2enhanced.specialcrafting.SpecialPlanInfo;

/**
 * 特殊计划显示文案构建（1.12.2 移植,Component 适配为 String + § 格式码）.
 */
public final class SpecialPlanTooltip {

    private SpecialPlanTooltip() {
    }

    /**
     * 行内描述（列表每行数量区）:发配轮次/调用次数.灰色,大数字 K/M/G/T/P 缩写.
     */
    public static String descriptionLine(SpecialPlanInfo.Entry entry) {
        if (entry.kind == SpecialPlanInfo.KIND_SELF_DUP) {
            return "\u00a77" + I18n.format("gui.ae2enhanced.special_plan.dup_desc", compact(entry.totalCrafts));
        }
        return "\u00a77" + I18n.format("gui.ae2enhanced.special_plan.rounds_desc", compact(entry.rounds));
    }

    /**
     * 普通处理样板的行内描述:调用次数 + 按当前 CPU 协处理器估算的发配轮次.
     *
     * @param pushesPerRound 每拍推送预算（1 + 协处理器数）
     */
    public static String normalDescriptionLine(long calls, long pushesPerRound) {
        long rounds = Math.max(1, (calls + pushesPerRound - 1) / pushesPerRound);
        return "\u00a77" + I18n.format("gui.ae2enhanced.special_plan.normal_calls", compact(calls),
                compact(rounds));
    }

    /**
     * 悬停详情:完整结构信息.
     */
    public static List<String> tooltipLines(IAEItemStack key, SpecialPlanInfo.Entry entry) {
        List<String> lines = new ArrayList<>();
        if (entry.kind == SpecialPlanInfo.KIND_SELF_DUP) {
            lines.add("\u00a76" + I18n.format("gui.ae2enhanced.special_plan.dup_header"));
            lines.add(I18n.format("gui.ae2enhanced.special_plan.dup_per_craft",
                    format(key, entry.perRoundConsume), format(key, entry.perRoundProduce),
                    format(key, entry.perRoundProduce - entry.perRoundConsume)));
            lines.add(I18n.format("gui.ae2enhanced.special_plan.dup_total", compact(entry.totalCrafts),
                    format(key, entry.initialExtract)));
        } else {
            lines.add("\u00a76" + I18n.format("gui.ae2enhanced.special_plan.cycle_header",
                    compact(entry.rounds)));
            lines.add(I18n.format("gui.ae2enhanced.special_plan.cycle_per_round",
                    format(key, entry.perRoundConsume), format(key, entry.perRoundProduce)));
            if (entry.initialExtract > 0) {
                lines.add(I18n.format("gui.ae2enhanced.special_plan.initial_extract",
                        format(key, entry.initialExtract)));
            }
        }
        return lines;
    }

    private static String format(IAEItemStack key, long amount) {
        return amount + " " + key.getDefinition().getDisplayName();
    }

    /** 大数字缩写（K/M/G/T/P). */
    public static String compact(long number) {
        if (number < 1000) {
            return Long.toString(number);
        }
        String[] suffixes = { "K", "M", "G", "T", "P" };
        double value = number;
        int idx = -1;
        while (value >= 1000 && idx < suffixes.length - 1) {
            value /= 1000;
            idx++;
        }
        String text = value >= 100 ? Long.toString(Math.round(value))
                : Double.toString(Math.floor(value * 10) / 10);
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text + suffixes[idx];
    }
}
