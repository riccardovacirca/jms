package dev.jms.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un Handler per essere eseguito in modalità non-blocking.
 *
 * Gli handler annotati con @Async:
 * - Non bloccano l'IO thread durante la lettura del body
 * - Vengono eseguiti su un executor dedicato
 * - Scrivono la risposta in modo non-blocking
 *
 * Usare solo per handler con operazioni lente (query pesanti, elaborazioni lunghe).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Async {
}
