package me.illuminator3.yol

import org.bukkit.configuration.ConfigurationSection

import java.lang.reflect.Modifier.isStatic as isStatic
import java.util.Optional as Opt

object YamlObjectLoader
{
    fun <T> load(sect: ConfigurationSection, obj: T): T
    {
        val cls = obj!!::class.java

        for (field in cls.fields)
        {
            if (!isStatic(field.modifiers))
            {
                val idOpt = opt(field.getDeclaredAnnotation(Id::class.java))

                if (!idOpt.isPresent) continue

                val optionalOpt = opt(field.getDeclaredAnnotation(Optional::class.java))
                val defaultOpt = opt(field.getDeclaredAnnotation(Default::class.java))

                val isOptional  = optionalOpt.isPresent && optionalOpt.get().optional
                val id          = idOpt.get().id
                val default     = defaultOpt.map(Default::field).orElse(null)

                if (!sect.contains(id) && !isOptional)
                    error("Id missing in YAML: '$id'")

                var value = sect.get(id, null)

                if (value == null && default != null)
                {
                    val defaultField = cls.getDeclaredField(default)

                    defaultField.isAccessible = true

                    value = if (isStatic(defaultField.modifiers)) defaultField.get(null) else defaultField.get(obj)
                }

                field.isAccessible = true

                field.set(obj, value)
            }
        }

        return obj
    }

    private fun <T> opt(o: T): Opt<T> =
        Opt.ofNullable(o)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Id         (val id: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Optional   (val optional: Boolean)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Default    (val field: String)