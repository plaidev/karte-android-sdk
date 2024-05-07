package io.karte.android.variables;
import org.jetbrains.annotations.NotNull;

public interface VariablesPredicate<T> {
    boolean test(@NotNull T key);
}
