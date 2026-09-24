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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class WerewolfClawItem extends Item implements IFactionLevelItem<IWerewolfPlayer>, IItemWithTier {

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

    @Override
    public @Nullable IFaction<?> getExclusiveFaction(@NotNull ItemStack stack) {
        return WReference.WEREWOLF_FACTION;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Item.TooltipContext pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        addTierInformation(pTooltipComponents);
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
}
