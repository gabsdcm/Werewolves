package de.teamlapen.werewolves.items;

import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.items.IFactionExclusiveItem;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WerewolfClawItem extends Item implements IFactionExclusiveItem, IItemWithTier {

    private final @NotNull TIER tier;

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
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public TIER getVampirismTier() {
        return this.tier;
    }
}
