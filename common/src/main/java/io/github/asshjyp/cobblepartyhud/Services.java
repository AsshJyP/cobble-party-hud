package io.github.asshjyp.cobblepartyhud;

import io.github.asshjyp.cobblepartyhud.platform.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Lo que depende del loader, cargado con ServiceLoader (como en MultiLoader-Template): cada loader declara su
 * implementación en META-INF/services/io.github.asshjyp.cobblepartyhud.platform.IPlatformHelper.
 */
public final class Services {
	public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

	private Services() {}

	private static <T> T load(Class<T> type) {
		return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
				.orElseThrow(() -> new IllegalStateException("No hay implementación de " + type.getName()));
	}
}
