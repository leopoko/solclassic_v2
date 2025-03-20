package com.github.leopoko.solclassic.forge.mixin;

import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class DisableConflictingMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("DisableConflictingMixinPlugin");

    @Override
    public void onLoad(String mixinPackage) {
        // 初期化処理（必要なら）
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // ここで、無効化したい mixin の完全修飾名を指定します
        if ("net.lyof.phantasm.mixin.LivingEntityMixin".equals(mixinClassName)) {
            if (ModList.get().isLoaded("endsphantasm")) {
                LOGGER.info("Skipping mixin: " + mixinClassName + " for target class: " + targetClassName);
                return false;
            }
        }
        LOGGER.info("Applying mixin: " + mixinClassName + " for target class: " + targetClassName);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 必要に応じて実装
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}