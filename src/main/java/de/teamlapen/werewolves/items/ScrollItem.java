package de.teamlapen.werewolves.items;

import de.teamlapen.werewolves.WerewolvesMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class ScrollItem extends Item {

    public ScrollItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            WerewolvesMod.proxy.displayScrollScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
