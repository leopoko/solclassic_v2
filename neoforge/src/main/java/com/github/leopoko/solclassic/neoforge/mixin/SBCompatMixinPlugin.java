package com.github.leopoko.solclassic.neoforge.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sophisticated Backpacks互換Mixinの条件付きローダー。
 *
 * SophisticatedCoreがクラスパス上に存在する場合のみ、
 * SBFeedingUpgradeMixinを動的に登録する。
 *
 * getMixins()で動的に追加するため、solclassic-sb-compat.mixins.jsonの
 * "mixins"配列は空にしておくこと。
 */
public class SBCompatMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("SBCompatMixinPlugin");
    private static final String SB_FEEDING_CLASS = "net/p3pp3rf1y/sophisticatedcore/upgrades/feeding/FeedingUpgradeWrapper.class";

    @Override
    public void onLoad(String mixinPackage) {
        // 初期化不要
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    /**
     * SophisticatedCoreがクラスパス上に存在する場合のみSBFeedingUpgradeMixinを登録する。
     * これにより、SB未インストール時はMixinクラスがロードされず、
     * ターゲットクラス不在によるクラッシュを回避できる。
     */
    @Override
    public List<String> getMixins() {
        if (getClass().getClassLoader().getResource(SB_FEEDING_CLASS) != null) {
            LOGGER.info("[SoL Classic] SophisticatedCore detected, enabling SB Feeding Upgrade compatibility");
            return List.of("SBFeedingUpgradeMixin");
        }
        LOGGER.info("[SoL Classic] SophisticatedCore not found, skipping SB compatibility mixins");
        return Collections.emptyList();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
