package de.teamlapen.werewolves.items;

import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.items.IFactionLevelItem;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = WReference.MODID)
public class WerewolfClawItem extends Item implements IFactionLevelItem<IWerewolfPlayer>, IItemWithTier {

    private static final String CLAW_DATA_KEY = "werewolves_claw";
    private static final String SHARPNESS_KEY = "sharpness";
    private static final String DAMAGE_CAUSED_KEY = "damage_caused";

    private static final double INITIAL_DAMAGE_FRACTION = 0.7D;

    private final @NotNull TIER tier;
    private static final Map<TIER, Integer> TIER_LEVELS = Map.of(
        TIER.NORMAL, 6,
        TIER.ENHANCED, 10,
        TIER.ULTIMATE, 14
    );

    public WerewolfClawItem(@NotNull TIER tier) {
        super(new Properties().stacksTo(1));
        this.tier = tier;
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof WerewolfClawItem claw) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    Item.BASE_ATTACK_DAMAGE_ID, getTooltipAttackDamage(event.getItemStack()),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                Item.BASE_ATTACK_SPEED_ID, getAttackSpeedModifier(claw.tier),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }

    private static double getAttackDamage(@NotNull TIER tier) {
        return switch (tier) {
            case NORMAL -> 6.0D;
            case ENHANCED -> 10.0D;
            case ULTIMATE -> 13.0D;
        };
    }

    private static double getTooltipAttackDamage(@NotNull ItemStack stack) {
        if (!(stack.getItem() instanceof WerewolfClawItem claw)) {
            return 0.0D;
        }
        // The attribute event has no owner; the local player is the only one known (tooltips). Otherwise use the initial value.
        Player player = VampirismMod.proxy.getClientPlayer();
        int level = player == null ? 0 : WerewolfPlayer.getOpt(player)
                .map(werewolf -> werewolf.getClawLevelHandler().getLevelFor(claw.tier)).orElse(0);
        int maxLevel = player == null ? 0 : WerewolfPlayer.getOpt(player)
                .map(werewolf -> werewolf.getClawLevelHandler().getMaxLevel()).orElse(0);
        return calculateAttackDamage(stack, level, maxLevel);
    }

    public static double getMaxAttackDamage(@NotNull TIER tier) {
        return getAttackDamage(tier);
    }

    public static int getWearLimit(@NotNull TIER tier) {
        return switch (tier) {
            case NORMAL -> 300;
            case ENHANCED -> 600;
            case ULTIMATE -> 900;
        };
    }

    public static int getLootingLevel(@NotNull TIER tier) {
        return switch (tier) {
            case NORMAL -> 3;
            case ENHANCED -> 4;
            case ULTIMATE -> 5;
        };
    }

    public static double getDamageCaused(@NotNull ItemStack stack) {
        return getClawData(stack).getDouble(DAMAGE_CAUSED_KEY);
    }

    public static @NotNull Sharpness getSharpness(@NotNull ItemStack stack) {
        CompoundTag data = getClawData(stack);
        if (data.contains(SHARPNESS_KEY)) {
            try {
                return Sharpness.valueOf(data.getString(SHARPNESS_KEY));
                } catch (IllegalArgumentException ignored) {
            }
        }
        return Sharpness.SHARP;
    }

    public static double calculateAttackDamage(@NotNull ItemStack stack, int level, int maxLevel) {
        if (!(stack.getItem() instanceof WerewolfClawItem claw)) {
            return 0.0D;
        }
        double levelProgress = maxLevel <= 0 ? 0.0D : Math.min(1.0D, Math.max(0.0D, level / (double) maxLevel));
        double damage = getMaxAttackDamage(claw.tier) * (INITIAL_DAMAGE_FRACTION + (1.0D - INITIAL_DAMAGE_FRACTION) * levelProgress);
        return damage * getSharpness(stack).getDamageMultiplier();
    }

    public static boolean recordDamage(@NotNull ItemStack stack, double damage) {
        if (!(stack.getItem() instanceof WerewolfClawItem claw) || damage <= 0.0D) {
            return false;
        }

        CompoundTag data = getClawData(stack);
        double wearLimit = getWearLimit(claw.tier);
        double currentDamage = Math.min(wearLimit, getDamageCaused(stack));
        if (currentDamage >= wearLimit) {
            return false;
        }
        double newDamage = Math.min(wearLimit, currentDamage + damage);
        Sharpness newSharpness = sharpnessForDamage(newDamage, getWearLimit(claw.tier));
        if (Double.compare(currentDamage, newDamage) == 0
                && getSharpness(stack) == newSharpness) {
            return false;
        }
        data.putDouble(DAMAGE_CAUSED_KEY, newDamage);
        data.putString(SHARPNESS_KEY, newSharpness.name());
        saveClawData(stack, data);
        return true;
    }

    /**
     * Removes accumulated wear from the claw.
     *
     * @return true if the claw was worn and has been changed
     */
    public static boolean sharpen(@NotNull ItemStack stack, double wearRemoved) {
        if (!(stack.getItem() instanceof WerewolfClawItem claw) || wearRemoved <= 0.0D || !isWorn(stack)) {
            return false;
        }
        CompoundTag data = getClawData(stack);
        double newDamage = Math.max(0.0D, getDamageCaused(stack) - wearRemoved);
        data.putDouble(DAMAGE_CAUSED_KEY, newDamage);
        data.putString(SHARPNESS_KEY, sharpnessForDamage(newDamage, getWearLimit(claw.tier)).name());
        saveClawData(stack, data);
        return true;
    }

    public static boolean isWorn(@NotNull ItemStack stack) {
        return stack.getItem() instanceof WerewolfClawItem && getDamageCaused(stack) > 0.0D;
    }

    private static @NotNull Sharpness sharpnessForDamage(double damage, int limit) {
        double progress = limit <= 0 ? 1.0D : Math.min(1.0D, damage / limit);
        // Uniform thresholds: Sharp <25%, Worn >=25%, Used >=50%, Dull >=75%, Blunt >=100%.
        if (progress >= 1.0D) {
            return Sharpness.BLUNT;
        } else if (progress >= 0.75D) {
            return Sharpness.DULL;
        } else if (progress >= 0.50D) {
            return Sharpness.USED;
        } else if (progress >= 0.25D) {
            return Sharpness.WORN;
        }
        return Sharpness.SHARP;
    }

    private static @NotNull CompoundTag getClawData(@NotNull ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = customData.copyTag();
        return root.getCompound(CLAW_DATA_KEY);
    }

    private static void saveClawData(@NotNull ItemStack stack, @NotNull CompoundTag data) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(CLAW_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /**
     * Vanilla attack speed is 4.0, so -2.4 produces the conventional sword value of 1.6.
     */
    public static double getAttackSpeedModifier(@NotNull TIER tier) {
        return -2.4D;
    }

    @Override
    public @Nullable IFaction<?> getExclusiveFaction(@NotNull ItemStack stack) {
        return WReference.WEREWOLF_FACTION;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Item.TooltipContext pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        addTierInformation(pTooltipComponents);
        pTooltipComponents.add(getSharpness(pStack).getDisplayName());
        pTooltipComponents.add(Component.translatable("text.werewolves.claw_looting",
                Component.translatable("enchantment.level." + getLootingLevel(this.tier))).withStyle(ChatFormatting.GRAY));
        addFactionToolTips(pStack, pLevel, pTooltipComponents, pIsAdvanced, VampirismMod.proxy.getClientPlayer());
    }

    @Override
    public void addFactionToolTips(@NotNull ItemStack stack, @Nullable Item.TooltipContext context, @NotNull List<Component> tooltip, TooltipFlag flagIn, @Nullable Player player) {
        addOilDescTooltip(stack, context, tooltip, flagIn, player);

        IFaction<?> faction = getExclusiveFaction(stack);
        IFactionPlayerHandler playerHandler = player != null ? VampirismAPI.factionPlayerHandler(player) : null;
        ChatFormatting factionColor = ChatFormatting.GRAY;
        if (faction != null) {
            if (player != null) {
                factionColor = VampirismAPI.factionRegistry().getFaction(player) == faction ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
            }
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("text.vampirism.faction_exclusive", faction.getName().copy().withStyle(factionColor)));
        }

        int minLevel = getMinLevel(stack);
        if (minLevel > 1) {
            boolean canUse = playerHandler != null && playerHandler.isInFaction(faction) && playerHandler.getCurrentLevel() >= minLevel;
            tooltip.add(Component.literal(" ").append(Component.translatable("text.vampirism.required_level", String.valueOf(minLevel))).withStyle(canUse ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        }
    }

    @Override
    public int getMinLevel(@NotNull ItemStack stack) {
        return TIER_LEVELS.get(this.tier);
    }

    @Override
    public @Nullable ISkill<IWerewolfPlayer> getRequiredSkill(@NotNull ItemStack stack) {
        return null;
    }

    @Override
    public TIER getVampirismTier() {
        return this.tier;
    }

    public enum Sharpness {
        SHARP("sharp", 1.0D),
        WORN("worn", 0.97D),
        USED("used", 0.95D),
        DULL("dull", 0.9D),
        BLUNT("blunt", 0.85D);

        private final String translationKey;
        private final double damageMultiplier;

        Sharpness(String translationKey, double damageMultiplier) {
            this.translationKey = translationKey;
            this.damageMultiplier = damageMultiplier;
        }

        public double getDamageMultiplier() {
            return this.damageMultiplier;
        }

        public @NotNull Component getDisplayName() {
            return Component.translatable("text.werewolves.claw_sharpness." + this.translationKey);
        }
    }
}
