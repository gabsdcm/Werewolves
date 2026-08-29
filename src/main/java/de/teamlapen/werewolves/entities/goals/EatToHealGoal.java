package de.teamlapen.werewolves.entities.goals;

import de.teamlapen.werewolves.config.WerewolvesConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EatToHealGoal<T extends Mob> extends Goal {

    private final T entity;
    private int cooldown;
    private float healAmount;
    @Nullable
    private Item eatenItem;
    private int eatenCount;

    public EatToHealGoal(T entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (this.entity.isUsingItem()) return false;
        if (this.entity.getHealth() >= this.entity.getMaxHealth() * WerewolvesConfig.BALANCE.MOBPROPS.werewolf_eat_health_threshold.get()) return false;
        return getOffhandFood() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.entity.isUsingItem() && this.entity.getUsedItemHand() == InteractionHand.OFF_HAND;
    }

    @Override
    public void start() {
        ItemStack stack = this.entity.getItemBySlot(EquipmentSlot.OFFHAND);
        FoodProperties properties = getOffhandFood();
        this.healAmount = properties == null ? 0 : properties.nutrition() / 2f;
        this.eatenItem = stack.getItem();
        this.eatenCount = stack.getCount();
        this.entity.startUsingItem(InteractionHand.OFF_HAND);
    }

    @Override
    public void stop() {
        ItemStack stack = this.entity.getItemBySlot(EquipmentSlot.OFFHAND);
        if (this.healAmount > 0 && this.eatenItem != null && (!stack.is(this.eatenItem) || stack.getCount() < this.eatenCount)) {
            this.entity.heal(this.healAmount);
        }
        this.entity.stopUsingItem();
        this.cooldown = WerewolvesConfig.BALANCE.MOBPROPS.werewolf_eat_cooldown.get();
        this.healAmount = 0;
        this.eatenItem = null;
        this.eatenCount = 0;
    }

    @Nullable
    private FoodProperties getOffhandFood() {
        return this.entity.getItemBySlot(EquipmentSlot.OFFHAND).getFoodProperties(this.entity);
    }
}
