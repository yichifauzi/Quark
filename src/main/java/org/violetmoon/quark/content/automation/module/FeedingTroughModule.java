package org.violetmoon.quark.content.automation.module;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.content.automation.block.FeedingTroughBlock;
import org.violetmoon.quark.content.automation.block.be.FeedingTroughBlockEntity;
import org.violetmoon.quark.mixin.mixins.accessor.AccessorTemptingSensor;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.event.play.entity.ZEntityJoinLevel;
import org.violetmoon.zeta.event.play.entity.living.ZBabyEntitySpawn;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.util.Hint;

import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * @author WireSegal
 * Created at 9:48 AM on 9/20/19.
 */
@ZetaLoadModule(category = "automation")
public class FeedingTroughModule extends ZetaModule {

    //using a ResourceKey because they're interned, and Holder.Reference#is leverages this for a very efficient implementation
    private static final ResourceKey<PoiType> FEEDING_TROUGH_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Quark.asResource("feeding_trough"));
    private static final GameProfile DUMMY_PROFILE = new GameProfile(UUID.randomUUID(), "[FeedingTrough]");

    public static BlockEntityType<FeedingTroughBlockEntity> blockEntityType;
    @Hint
    Block feeding_trough;

    @Config(description = "How long, in game ticks, between animals being able to eat from the trough")
    @Config.Min(1)
    public static int cooldown = 30;

    @Config(description = "The maximum amount of animals allowed around the trough's range for an animal to enter love mode")
    public static int maxAnimals = 32;

    @Config(description = "The chance (between 0 and 1) for an animal to enter love mode when eating from the trough")
    @Config.Min(value = 0.0, exclusive = true)
    @Config.Max(1.0)
    public static double loveChance = 0.333333333;

    @Config
    public static double range = 10;

    @Config(description = "Chance that an animal decides to look for a through. Closer it is to 1 the more performance it will take. Decreasing will make animals take longer to find one")
    public static double lookChance = 0.01;

    private static final WeakHashMap<Animal, TroughPointer> NEARBY_TROUGH_CACHE = new WeakHashMap<>();

    private static final ThreadLocal<Boolean> breedingOccurred = ThreadLocal.withInitial(() -> false);

    @PlayEvent
    public void onBreed(ZBabyEntitySpawn.Lowest event) {
        if (event.getCausedByPlayer() == null && event.getParentA().level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))
            breedingOccurred.set(true);
    }

    @PlayEvent
    public void onOrbSpawn(ZEntityJoinLevel event) {
        if (event.getEntity() instanceof ExperienceOrb && breedingOccurred.get()) {
            event.setCanceled(true);
            breedingOccurred.remove();
        }
    }

    // Both TempingSensor and TemptGoal work by keeping track of a nearby player who is holding food.
    // The Feeding Trough causes mobs to pathfind to it by injecting a fakeplayer into these AI goals, who stands at the
    // location of the Trough and holds food they like.

    // The "realPlayer" parameter represents a real player located by existing TemptingSensor/TemptGoal code.
    // If there is a real player, and they are holding food, we don't swap them for a fakeplayer, so that animals path to
    // real players before they consider pathing to the Trough.
    // We now only call these if a valid realPlayer is not there, hence why we don't need that parameter anymore

    public static @Nullable Player modifyTemptingSensor(TemptingSensor sensor, Animal animal, ServerLevel level) {
        return modifyTempt(level, animal, ((AccessorTemptingSensor) sensor).quark$getTemptations());
    }

    public static @Nullable Player modifyTemptGoal(TemptGoal goal, Animal animal, ServerLevel level) {
        return modifyTempt(level, animal, goal.items);
    }

    private static @Nullable Player modifyTempt(ServerLevel level, Animal animal, Ingredient temptations) {
        //early-exit conditions
        if (!Quark.ZETA.modules.isEnabled(FeedingTroughModule.class) ||
                !animal.canFallInLove() ||
                animal.getAge() != 0
        ) {
            return null;
        }

        //do we already know about a nearby trough?
        NEARBY_TROUGH_CACHE.entrySet().removeIf(p -> !p.getValue().valid(p.getKey()));

        TroughPointer pointer = NEARBY_TROUGH_CACHE.get(animal);

        //There's no cached trough nearby.
        //Randomize whether we actually look for a new trough, to hopefully not eat all the tick time.
        if (pointer == null && level.random.nextFloat() <= lookChance) {
            pointer = TroughPointer.find(level, animal, temptations);
            if (pointer != null){
                NEARBY_TROUGH_CACHE.put(animal, pointer);
            }
        }

        //did we find one?
        if (pointer != null) {
            pointer.tryEatingOrTickCooldown(animal);

            if (!pointer.isOnCooldown()) {

                //if the animal can see it, direct the animal to this trough's fakeplayer
                BlockPos location = pointer.pos;
                Vec3 eyesPos = animal.position().add(0, animal.getEyeHeight(), 0);
                Vec3 targetPos = new Vec3(location.getX(), location.getY(), location.getZ()).add(0.5, 0.0625, 0.5);
                BlockHitResult ray = level.clip(new ClipContext(eyesPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, animal));
                if (ray.getType() == HitResult.Type.BLOCK && ray.getBlockPos().equals(location)) {
                    return pointer.foodHolder;
                }
            }
        }

        return null;
    }


    @LoadEvent
    public final void register(ZRegister event) {
        feeding_trough = new FeedingTroughBlock("feeding_trough", this,
                Block.Properties.of().mapColor(MapColor.WOOD).ignitedByLava().strength(0.6F).sound(SoundType.WOOD));

        blockEntityType = BlockEntityType.Builder.of(FeedingTroughBlockEntity::new, feeding_trough).build(null);
        event.getRegistry().register(blockEntityType, "feeding_trough", Registries.BLOCK_ENTITY_TYPE);

        PoiType feedingTroughPoi = new PoiType(ImmutableSet.copyOf(feeding_trough.getStateDefinition().getPossibleStates()), 1, 32);
        event.getRegistry().register(feedingTroughPoi, FEEDING_TROUGH_POI_KEY.location(), Registries.POINT_OF_INTEREST_TYPE);
    }

    private static final class TroughPointer {
        private final BlockPos pos;
        private final FakePlayer foodHolder;
        private final Ingredient temptations;
        private int eatCooldown = 0; //Ideally cooldown should be per entity... Assuming troughs don't change much this is fine
        private int giveUpCooldown = 20 * 20; //max seconds till we give up

        private TroughPointer(BlockPos pos, FakePlayer player, Ingredient temptations) {
            this.pos = pos;
            this.foodHolder = player;
            this.temptations = temptations;
        }

        // This is a bit ugly. 0 = new pointer, 1 = end of life, other = ticking cooldown
        // Once a through is found and an animal is fed, its considered valid until cooldown runs out.
        // Then its invalidated so animals can find possibly closer ones
        boolean valid(Animal animal) {
            if (eatCooldown == 1){
                return false;
            }
            if (giveUpCooldown <= 0){
                return false;
            }
            if (eatCooldown != 0) return true;
            if (animal.isRemoved() || !animal.isAlive() || foodHolder.level() != animal.level() || pos.distSqr(animal.blockPosition()) > range * range) {
                    return false;
            }
            //check if it has food and tile is valid
            if(animal.level().getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough){
                //this should be called in tick, but we save one tile call by doing this...
                trough.updateFoodHolder(animal, temptations, foodHolder);
                //if it still has food
                return !foodHolder.getMainHandItem().isEmpty();
            }
            return false;
        }

        void tryEatingOrTickCooldown(Animal animal) {
            giveUpCooldown--;
            if (eatCooldown == 0) {
                //I wish this could be made smaller. Vanilla AI will not keep animals too close to a player holding food
                float feedDistance = 1.5f;
                if (pos.distToCenterSqr(animal.position()) < (feedDistance * feedDistance)) {
                    if (animal.level().getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough) {
                        switch (trough.tryFeedingAnimal(animal)) {
                            case FED -> eatCooldown = cooldown; // just fed. set normal cooldown
                            case SECS -> eatCooldown = 1; // remove immediately, as it will use animal own love cooldown for feeding again
                        }
                    }
                }
            } else eatCooldown--;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TroughPointer) obj;
            return Objects.equals(this.pos, that.pos) &&
                    Objects.equals(this.foodHolder, that.foodHolder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, foodHolder);
        }

        // If animal cant eat.
        // Pointer won't be erased
        // until cooldown is 0. Maybe would have been better with entity nbt data like it was so it persisted
        public boolean isOnCooldown() {
            return eatCooldown != 0;
        }


        @Nullable
        static TroughPointer find(ServerLevel level, Animal animal, Ingredient temptations) {
            // this is an expensive part
            BlockPos position = animal.getOnPos();
            var opt = level.getPoiManager().findClosest(
                    holder -> holder.is(FEEDING_TROUGH_POI_KEY), p -> p.distSqr(position) <= range * range,
                    position, (int) range, PoiManager.Occupancy.ANY);
            if (opt.isPresent()) {
                BlockPos pos = opt.get();

                if (level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough) {
                    //only returns if it has the right food
                    FakePlayer foodHolder = FakePlayerFactory.get(level, DUMMY_PROFILE);
                    if (foodHolder != null) {
                        trough.updateFoodHolder(animal, temptations, foodHolder);
                        // if it has a food item
                        if (!foodHolder.getMainHandItem().isEmpty()) {
                            return new TroughPointer(pos, foodHolder, temptations);
                        }
                    }
                    return null;
                }
            }
            return null;
        }
    }
}
