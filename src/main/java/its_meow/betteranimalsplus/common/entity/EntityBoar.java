package its_meow.betteranimalsplus.common.entity;

import java.util.Set;

import javax.annotation.Nullable;

import its_meow.betteranimalsplus.util.HeadTypes;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class EntityBoar extends EntityAnimalWithSelectiveTypes implements IMob {

    public EntityBoar(World worldIn) {
        super(worldIn);
        this.setSize(0.9F, 0.9F);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        if (!this.isChild() && this.getEntityWorld().getDifficulty() != EnumDifficulty.PEACEFUL) {
            this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.2D, false));
        }
        this.tasks.addTask(3, new EntityAIMate(this, 1.0D));
        this.tasks.addTask(5, new EntityAIFollowParent(this, 1.1D));
        // Eats grass at priority 6
        this.tasks.addTask(6, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        if (!this.isChild() && this.getEntityWorld().getDifficulty() != EnumDifficulty.PEACEFUL) {
            this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, true, new Class[0]));
            this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityAnimal>(this, EntityAnimal.class, 90, true, true, (@Nullable Entity in) -> in instanceof EntityChicken || in instanceof EntityPheasant || (in instanceof EntityAnimal && ((EntityAnimal) in).isChild() && !(in instanceof EntityBoar || in instanceof EntityPig))));
            this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityLivingBase>(this, EntityLivingBase.class, 50, true, true, (@Nullable Entity in) -> (in instanceof EntityAnimal && !(in instanceof EntityBoar || in instanceof EntityPig)) || in instanceof EntityPlayer));
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.38D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.5D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PIG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block blockIn) {
        this.playSound(SoundEvents.ENTITY_PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!world.isRemote && !this.isChild()) {
            if (this.rand.nextInt(12) == 0) {
                ItemStack stack = new ItemStack(HeadTypes.BOARHEAD.getItem(this.getTypeNumber()));
                this.entityDropItem(stack, 0.5F);
            }
        }
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LootTableList.ENTITIES_PIG;
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        Vec3d pos = this.getPositionVector();
        Vec3d targetPos = entityIn.getPositionVector();
        ((EntityLivingBase) entityIn).knockBack(entityIn, 0.8F, pos.x - targetPos.x, pos.z - targetPos.z);

        // Vanilla attack code for mobs

        float f = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        int i = 0;

        if (entityIn instanceof EntityLivingBase) {
            f += EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((EntityLivingBase) entityIn).getCreatureAttribute());
            i += EnchantmentHelper.getKnockbackModifier(this);
        }

        boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), f);

        if (flag) {
            if (i > 0 && entityIn instanceof EntityLivingBase) {
                ((EntityLivingBase) entityIn).knockBack(this, i * 0.5F, MathHelper.sin(this.rotationYaw * 0.017453292F), (-MathHelper.cos(this.rotationYaw * 0.017453292F)));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }

            int j = EnchantmentHelper.getFireAspectModifier(this);

            if (j > 0) {
                entityIn.setFire(j * 4);
            }

            if (entityIn instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entityIn;
                ItemStack itemstack = this.getHeldItemMainhand();
                ItemStack itemstack1 = entityplayer.isHandActive() ? entityplayer.getActiveItemStack() : ItemStack.EMPTY;

                if (!itemstack.isEmpty() && !itemstack1.isEmpty() && itemstack.getItem().canDisableShield(itemstack, itemstack1, entityplayer, this) && itemstack1.getItem().isShield(itemstack1, entityplayer)) {
                    float f1 = 0.25F + EnchantmentHelper.getEfficiencyModifier(this) * 0.05F;

                    if (this.rand.nextFloat() < f1) {
                        entityplayer.getCooldownTracker().setCooldown(itemstack1.getItem(), 100);
                        this.world.setEntityState(entityplayer, (byte) 30);
                    }
                }
            }

            this.applyEnchantments(this, entityIn);
        }

        return flag;
    }

    /**
     * Called when a lightning bolt hits the entity.
     */
    @Override
    public void onStruckByLightning(EntityLightningBolt lightningBolt) {
        if (!this.world.isRemote && !this.isDead) {
            EntityPigZombie entitypigzombie = new EntityPigZombie(this.world);
            entitypigzombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            entitypigzombie.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            entitypigzombie.setNoAI(this.isAIDisabled());

            if (this.hasCustomName()) {
                entitypigzombie.setCustomNameTag(this.getCustomNameTag());
                entitypigzombie.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
            }

            this.world.spawnEntity(entitypigzombie);
            this.setDead();
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if(this.world.isBlockLoaded(this.getPosition())) {
            if(net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this)) {
                if (this.rand.nextInt(200) == 0) {
                    Block block = this.world.getBlockState(this.getPosition()).getBlock();
                    if (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS) {
                        this.world.setBlockToAir(this.getPosition());
                        this.setInLove(null);
                    }
                }
            }
        }
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        if (ageable instanceof EntityBoar) {
            EntityBoar boar = new EntityBoar(this.world);
            boar.setType(this.getTypeNumber());
            return boar;
        } else if (ageable instanceof EntityPig) {
            EntityPig pig = new EntityPig(this.world);
            EntityBoar boar = new EntityBoar(this.world);
            boar.setType(this.getTypeNumber());
            return this.rand.nextBoolean() ? pig : boar;
        } else {
            return null;
        }
    }

    @Override
    public boolean canMateWith(EntityAnimal otherAnimal) {
        if (otherAnimal != this) {
            if (otherAnimal instanceof EntityBoar || otherAnimal instanceof EntityPig) {
                if (otherAnimal.isInLove() && this.isInLove()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.CARROT || stack.getItem() == Items.GOLDEN_CARROT;
    }

    @Override
    public int getVariantMax() {
        return 4;
    }

    @Override
    protected IVariantTypes getBaseChild() {
        return null;
    }

    @Override
    protected int[] getTypesFor(Set<BiomeDictionary.Type> types) {
        if(types.contains(Type.FOREST) && !types.contains(Type.CONIFEROUS)) {
            return new int[] {1, 2, 3};
        } else if(types.contains(Type.CONIFEROUS) && !types.contains(Type.SNOWY)) {
            return new int[] {1, 2, 3};
        } else if(types.contains(Type.CONIFEROUS) && types.contains(Type.SNOWY)) {
            return new int[] {1, 4};
        } else if(types.contains(Type.SNOWY) && !types.contains(Type.CONIFEROUS)) { 
            return new int[] {4};
        } else if(types.contains(Type.SAVANNA) || types.contains(Type.PLAINS)) {
            return new int[] {1, 2, 3};
        } else {
            return new int[] {1, 2, 3, 4};
        }
    }

    @Override
    protected String getContainerName() {
        return "boar";
    }

}
