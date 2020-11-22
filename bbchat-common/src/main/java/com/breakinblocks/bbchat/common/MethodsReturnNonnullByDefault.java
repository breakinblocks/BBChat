package com.breakinblocks.bbchat.common;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be applied to a package, class or method to indicate that
 * the method return values in that element are nonnull by default unless there is:
 * <ul>
 * <li>An explicit nullness annotation
 * <li>The method overrides a method in a superclass (in which case the
 * annotation of the corresponding parameter in the superclass applies)
 * </ul>
 *
 * @see Nonnull
 * @see ParametersAreNonnullByDefault
 */
@Documented
@Nonnull
@TypeQualifierDefault(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodsReturnNonnullByDefault {
}
