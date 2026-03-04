package com.github.leopoko.solclassic.client;

import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 食事記録の本のカスタムGUI画面。
 * プレイヤーの食事履歴をアイテムアイコン付きで表示する。
 */
public class FoodHistoryBookScreen extends Screen {

    // バニラの本テクスチャ
    private static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/gui/book.png");
    // テクスチャサイズ
    private static final int BOOK_WIDTH = 146;
    private static final int BOOK_HEIGHT = 180;
    // 1ページあたりの表示アイテム数
    private static final int ITEMS_PER_PAGE = 6;
    // エントリテキストの縮小倍率
    private static final float TEXT_SCALE = 0.75f;
    // エントリの描画開始オフセット
    private static final int ENTRY_START_Y = 30;
    private static final int ENTRY_HEIGHT = 18;
    private static final int CONTENT_LEFT = 18;

    private final Player player;
    private final List<FoodEntry> foodEntries;
    private int currentPage = 0;
    private int totalPages = 1;

    private Button nextButton;
    private Button prevButton;

    public FoodHistoryBookScreen(Player player) {
        super(Component.translatable("gui.food_history_book.title"));
        this.player = player;
        this.foodEntries = buildFoodEntries();
        this.totalPages = Math.max(1, (int) Math.ceil((double) foodEntries.size() / ITEMS_PER_PAGE));
    }

    /**
     * 画面を開くための静的メソッド
     */
    public static void open(Player player) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new FoodHistoryBookScreen(player));
    }

    /**
     * 食事履歴からユニークな食べ物のエントリリストを構築する。
     * 最近食べた順にソートし、各食べ物の回数と減衰率を計算する。
     */
    private List<FoodEntry> buildFoodEntries() {
        LinkedList<ItemStack> history = FoodHistoryHolder.INSTANCE.getClientFoodHistory(player);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        // ユニークな食べ物をカウント（出現順を保持）
        Map<Item, Integer> countMap = new LinkedHashMap<>();
        // 最近食べた順にするため、逆順で走査
        Iterator<ItemStack> descIter = history.descendingIterator();
        while (descIter.hasNext()) {
            Item item = descIter.next().getItem();
            countMap.merge(item, 1, Integer::sum);
        }

        List<FoodEntry> entries = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : countMap.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();
            ItemStack stack = new ItemStack(item);
            float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
            int percent = (int) (multiplier * 100f);
            entries.add(new FoodEntry(stack, count, percent));
        }

        return entries;
    }

    @Override
    protected void init() {
        int bookLeft = (this.width - BOOK_WIDTH) / 2;
        int bookTop = (this.height - BOOK_HEIGHT) / 2;

        // ページ送りボタン（バニラの本と同じ位置）
        int buttonY = bookTop + BOOK_HEIGHT - 25;

        nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateButtons();
            }
        }).bounds(bookLeft + BOOK_WIDTH - 43, buttonY, 20, 12).build());

        prevButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        }).bounds(bookLeft + 18, buttonY, 20, 12).build());

        // 閉じるボタン
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.onClose();
        }).bounds((this.width - 100) / 2, bookTop + BOOK_HEIGHT + 5, 100, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        nextButton.visible = currentPage < totalPages - 1;
        prevButton.visible = currentPage > 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int bookLeft = (this.width - BOOK_WIDTH) / 2;
        int bookTop = (this.height - BOOK_HEIGHT) / 2;

        // 本の背景を描画
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(BOOK_LOCATION, bookLeft, bookTop, 20, 1, BOOK_WIDTH, BOOK_HEIGHT);

        // タイトルを描画
        Component title = Component.translatable("gui.food_history_book.title")
                .withStyle(Style.EMPTY.withBold(true));
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, bookLeft + (BOOK_WIDTH - titleWidth) / 2, bookTop + 12, 0x000000, false);

        if (foodEntries.isEmpty()) {
            // 履歴が空の場合
            Component empty = Component.translatable("gui.food_history_book.empty");
            int emptyWidth = this.font.width(empty);
            graphics.drawString(this.font, empty, bookLeft + (BOOK_WIDTH - emptyWidth) / 2, bookTop + ENTRY_START_Y + 20, 0x666666, false);
        } else {
            // サマリーを描画
            Component summary = Component.translatable("gui.food_history_book.summary",
                    String.valueOf(foodEntries.size()));
            int summaryWidth = this.font.width(summary);
            graphics.drawString(this.font, summary, bookLeft + (BOOK_WIDTH - summaryWidth) / 2, bookTop + 24, 0x666666, false);

            // 食べ物エントリを描画
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, foodEntries.size());

            for (int i = startIndex; i < endIndex; i++) {
                FoodEntry entry = foodEntries.get(i);
                int entryY = bookTop + ENTRY_START_Y + 14 + (i - startIndex) * ENTRY_HEIGHT;
                int entryX = bookLeft + CONTENT_LEFT;

                // アイテムアイコン（16x16）
                graphics.renderItem(entry.stack, entryX, entryY);

                // テキスト部分を縮小描画
                float invScale = 1.0f / TEXT_SCALE;
                int textX = entryX + 18;
                int textY = entryY + 5;

                graphics.pose().pushPose();
                graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

                // 食べ物名（省略対応）
                String name = entry.stack.getHoverName().getString();
                int maxNameWidth = (int) (54 * invScale);
                if (this.font.width(name) > maxNameWidth) {
                    name = this.font.plainSubstrByWidth(name, maxNameWidth - this.font.width("..")) + "..";
                }
                graphics.drawString(this.font, name, (int) (textX * invScale), (int) (textY * invScale), 0x000000, false);

                // 回数
                String times = "\u00d7" + entry.count;
                graphics.drawString(this.font, times, (int) ((textX + 56) * invScale), (int) (textY * invScale), 0x555555, false);

                // 減衰率（色分け: 緑=高い、黄色=中、赤=低い）
                String percent = entry.multiplierPercent + "%";
                int color = getMultiplierColor(entry.multiplierPercent);
                graphics.drawString(this.font, percent, (int) ((textX + 76) * invScale), (int) (textY * invScale), color, false);

                graphics.pose().popPose();
            }
        }

        // ページ番号を描画
        if (totalPages > 1) {
            Component pageText = Component.translatable("gui.food_history_book.page",
                    String.valueOf(currentPage + 1), String.valueOf(totalPages));
            int pageWidth = this.font.width(pageText);
            graphics.drawString(this.font, pageText, bookLeft + (BOOK_WIDTH - pageWidth) / 2,
                    bookTop + BOOK_HEIGHT - 28, 0x999999, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 減衰率に応じた色を返す。
     * 100-75%: 緑、74-25%: 黄、24-0%: 赤
     */
    private int getMultiplierColor(int percent) {
        if (percent >= 75) {
            return 0x006600; // 緑
        } else if (percent >= 25) {
            return 0x886600; // 黄
        } else {
            return 0xAA0000; // 赤
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 食べ物エントリのデータクラス
     */
    private static class FoodEntry {
        final ItemStack stack;
        final int count;
        final int multiplierPercent;

        FoodEntry(ItemStack stack, int count, int multiplierPercent) {
            this.stack = stack;
            this.count = count;
            this.multiplierPercent = multiplierPercent;
        }
    }
}
