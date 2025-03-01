package youyihj.zenutils.api.cotx.brackets;

import com.teamacronymcoders.base.registrysystem.ItemRegistry;
import com.teamacronymcoders.contenttweaker.ContentTweaker;
import com.teamacronymcoders.contenttweaker.modules.vanilla.items.ItemContent;
import com.teamacronymcoders.contenttweaker.modules.vanilla.items.ItemRepresentation;
import com.teamacronymcoders.contenttweaker.modules.vanilla.items.food.ItemContentFood;
import com.teamacronymcoders.contenttweaker.modules.vanilla.items.food.ItemFoodRepresentation;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.BracketHandler;
import crafttweaker.zenscript.IBracketHandler;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.compiler.IEnvironmentGlobal;
import stanhebben.zenscript.parser.Token;
import stanhebben.zenscript.symbols.IZenSymbol;
import stanhebben.zenscript.type.natives.IJavaMethod;
import youyihj.zenutils.Reference;
import youyihj.zenutils.api.cotx.annotation.ExpandContentTweakerEntry;
import youyihj.zenutils.api.zenscript.SidedZenRegister;
import youyihj.zenutils.impl.util.ReflectUtils;
import youyihj.zenutils.impl.zenscript.ExpressionCallStaticThenCastWithStringArg;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author youyihj
 */
@SidedZenRegister(modDeps = Reference.MOD_COT)
@BracketHandler(priority = 100)
public class BracketHandlerCoTItem implements IBracketHandler {
    private static final IJavaMethod representationGetter = CraftTweakerAPI.getJavaMethod(BracketHandlerCoTItem.class, "getItemRepresentation", String.class);

    @Override
    public IZenSymbol resolve(IEnvironmentGlobal environment, List<Token> tokens) {
        if (tokens.size() > 2) {
            if (tokens.get(0).getValue().equals("cotItem") && tokens.get(1).getValue().equals(":")) {
                return find(environment, tokens);
            }
        }
        return null;
    }

    public static ItemRepresentation getItemRepresentation(String name) {
        Item item = getItem(name);
        Class<? extends Item> itemClass = item.getClass();
        try {
            if (itemClass == ItemContent.class) {
                return ((ItemRepresentation) ReflectUtils.removePrivate(ItemContent.class, "itemRepresentation").get(item));
            } else if (itemClass == ItemContentFood.class) {
                return ((ItemFoodRepresentation) ReflectUtils.removePrivate(ItemContentFood.class, "itemRepresentation").get(item));
            } else if (itemClass.isAnnotationPresent(ExpandContentTweakerEntry.class)) {
                for (Method method : itemClass.getMethods()) {
                    if (method.isAnnotationPresent(ExpandContentTweakerEntry.RepresentationGetter.class)) {
                        return ((ItemRepresentation) method.invoke(item));
                    }
                }
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            CraftTweakerAPI.logError(null, e);
        }
        return null;
    }

    private static Item getItem(String name) {
        Item item = ContentTweaker.instance.getRegistry(ItemRegistry.class, "ITEM")
                .get(new ResourceLocation(ContentTweaker.MOD_ID, name));
        if (item instanceof ItemContent) {
            LateGetContentLookup.addItem(((ItemContent) item));
        }
        return item;
    }

    private IZenSymbol find(IEnvironmentGlobal environment, List<Token> tokens) {
        String name = tokens.get(2).getValue();
        ItemRepresentation itemRepresentation = getItemRepresentation(name);
        if (itemRepresentation == null) {
            return null;
        }
        return position -> new ExpressionCallStaticThenCastWithStringArg(position, environment, representationGetter, itemRepresentation.getClass(), name);
    }

    @Override
    public String getRegexMatchingString() {
        return "cotItem:.*";
    }

    @Override
    public Class<?> getReturnedClass() {
        return ItemRepresentation.class;
    }
}
