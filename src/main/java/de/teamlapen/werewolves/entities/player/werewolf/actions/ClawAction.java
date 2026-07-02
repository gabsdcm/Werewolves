package de.teamlapen.werewolves.entities.player.werewolf.actions;

import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import de.teamlapen.vampirism.util.RegUtil;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.api.entities.player.action.IActionCooldownMenu;
import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfClawSlot;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import de.teamlapen.werewolves.items.WerewolfClawItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClawAction extends DefaultWerewolfAction implements ILastingAction<IWerewolfPlayer>, IActionCooldownMenu {

    @Override
    public boolean isEnabled() {
        return WerewolvesConfig.BALANCE.SKILLS.claw_enabled.get();
    }

    @Override
    public boolean canBeUsedBy(@NotNull IWerewolfPlayer werewolf) {
        if (!werewolf.getForm().isTransformed()) {
            return false;
        }
        WerewolfPlayer player = (WerewolfPlayer) werewolf;
        if (player.getClawSlot().getStack().getItem() instanceof WerewolfClawItem) {
            return true;
        }
        Inventory inventory = player.getRepresentingPlayer().getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.getItem(i).getItem() instanceof WerewolfClawItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean activate(IWerewolfPlayer werewolf, ActivationContext context) {
        WerewolfPlayer player = (WerewolfPlayer) werewolf;
        WerewolfClawSlot slot = player.getClawSlot();
        Inventory inventory = player.getRepresentingPlayer().getInventory();

        // auto-equip: move the best available claw from the inventory into the slot
        int index = findBestClawIndex(inventory, slot.getStack());
        if (index >= 0) {
            ItemStack inventoryClaw = inventory.getItem(index);
            ItemStack previous = slot.getStack();
            slot.setStack(inventoryClaw);
            inventory.setItem(index, previous);
        }

        if (!(slot.getStack().getItem() instanceof WerewolfClawItem)) {
            return false;
        }
        slot.setActive(true);
        applyModifiers(player);
        player.checkToolDamage(player.getRepresentingPlayer().getMainHandItem(),
                player.getRepresentingPlayer().getMainHandItem(), true);
        player.syncClawSlot();
        return true;
    }

    @Override
    public void onDeactivated(IWerewolfPlayer werewolf) {
        WerewolfPlayer player = (WerewolfPlayer) werewolf;
        removeModifiers(player);
        player.getClawSlot().setActive(false);
        player.checkToolDamage(player.getRepresentingPlayer().getMainHandItem(),
                player.getRepresentingPlayer().getMainHandItem(), true);
        player.syncClawSlot();
    }

    @Override
    public void onReActivated(IWerewolfPlayer werewolf) {
        applyModifiers((WerewolfPlayer) werewolf);
    }

    @Override
    public void onActivatedClient(IWerewolfPlayer werewolf) {
    }

    @Override
    public boolean onUpdate(@NotNull IWerewolfPlayer werewolf) {
        return !werewolf.getForm().isTransformed();
    }

    @Override
    public int getDuration(IWerewolfPlayer werewolf) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getCooldown(IWerewolfPlayer werewolf) {
        return WerewolvesConfig.BALANCE.SKILLS.claw_cooldown.get() * 20;
    }

    @Override
    public boolean showHudCooldown(Player player) {
        return true;
    }

    @Override
    public boolean showHudDuration(Player player) {
        return false;
    }

    private void applyModifiers(WerewolfPlayer player) {
        IItemWithTier.TIER tier = getTier(player);
        if (tier == null) {
            return;
        }
        AttributeInstance damage = player.asEntity().getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = player.asEntity().getAttribute(Attributes.ATTACK_SPEED);
        if (damage != null) {
            damage.removeModifier(damageId());
            damage.addTransientModifier(
                    new AttributeModifier(damageId(), getAttackDamage(tier), AttributeModifier.Operation.ADD_VALUE));
        }
        if (speed != null) {
            speed.removeModifier(speedId());
            speed.addTransientModifier(
                    new AttributeModifier(speedId(), getAttackSpeed(tier), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeModifiers(WerewolfPlayer player) {
        AttributeInstance damage = player.asEntity().getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = player.asEntity().getAttribute(Attributes.ATTACK_SPEED);
        if (damage != null) {
            damage.removeModifier(damageId());
        }
        if (speed != null) {
            speed.removeModifier(speedId());
        }
    }

    @Nullable
    private IItemWithTier.TIER getTier(WerewolfPlayer player) {
        if (player.getClawSlot().getStack().getItem() instanceof WerewolfClawItem claw) {
            return claw.getVampirismTier();
        }
        return null;
    }

    private ResourceLocation damageId;
    private ResourceLocation speedId;

    private ResourceLocation damageId() {
        if (this.damageId == null) {
            this.damageId = RegUtil.id(this);
        }
        return this.damageId;
    }

    private ResourceLocation speedId() {
        if (this.speedId == null) {
            this.speedId = RegUtil.id(this).withSuffix("_speed");
        }
        return this.speedId;
    }

    private static double getAttackDamage(IItemWithTier.TIER tier) {
        return switch (tier) {
            case NORMAL -> WerewolvesConfig.BALANCE.SKILLS.claw_normal_attack_damage.get();
            case ENHANCED -> WerewolvesConfig.BALANCE.SKILLS.claw_enhanced_attack_damage.get();
            case ULTIMATE -> WerewolvesConfig.BALANCE.SKILLS.claw_ultimate_attack_damage.get();
        };
    }

    private static double getAttackSpeed(IItemWithTier.TIER tier) {
        return switch (tier) {
            case NORMAL -> WerewolvesConfig.BALANCE.SKILLS.claw_normal_attack_speed.get();
            case ENHANCED -> WerewolvesConfig.BALANCE.SKILLS.claw_enhanced_attack_speed.get();
            case ULTIMATE -> WerewolvesConfig.BALANCE.SKILLS.claw_ultimate_attack_speed.get();
        };
    }

    private static int findBestClawIndex(Inventory inventory, ItemStack current) {
        int bestRank = current.getItem() instanceof WerewolfClawItem c ? c.getVampirismTier().ordinal() : -1;
        int bestIndex = -1;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.getItem(i).getItem() instanceof WerewolfClawItem c) {
                int rank = c.getVampirismTier().ordinal();
                if (rank > bestRank) {
                    bestRank = rank;
                    bestIndex = i;
                }
            }
        }
        return bestIndex;
    }
}
