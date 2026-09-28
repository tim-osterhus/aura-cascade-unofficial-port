package pixlepix.auracascade.data.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

public final class AuraCustomDataIngredient implements ICustomIngredient {
    private static final Codec<CustomData> NON_EMPTY_NBT_CODEC = CustomData.CODEC.validate(data ->
        data.isEmpty()
            ? DataResult.error(() -> "Custom-data ingredient requires a non-empty NBT predicate")
            : DataResult.success(data));

    public static final MapCodec<AuraCustomDataIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false)
            .fieldOf("items")
            .forGetter(AuraCustomDataIngredient::items),
        NON_EMPTY_NBT_CODEC.fieldOf("nbt").forGetter(AuraCustomDataIngredient::requiredData)
    ).apply(instance, AuraCustomDataIngredient::new));

    private final HolderSet<Item> items;
    private final CustomData requiredData;

    public AuraCustomDataIngredient(HolderSet<Item> items, CustomData requiredData) {
        this.items = items;
        this.requiredData = requiredData;
    }

    @Override
    public boolean test(ItemStack stack) {
        return items.contains(stack.getItemHolder())
            && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(requiredData.copyTag());
    }

    @Override
    public Stream<ItemStack> getItems() {
        return items.stream().map(item -> {
            ItemStack stack = new ItemStack(item.value());
            stack.set(DataComponents.CUSTOM_DATA, requiredData);
            return stack;
        });
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return AuraIngredientTypes.CUSTOM_DATA.get();
    }

    private HolderSet<Item> items() {
        return items;
    }

    private CustomData requiredData() {
        return requiredData;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AuraCustomDataIngredient that
            && items.equals(that.items)
            && requiredData.equals(that.requiredData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, requiredData);
    }
}
