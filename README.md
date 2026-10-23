# Cobble Party HUD

Mod **solo de cliente** para Minecraft **1.21.1 (Fabric y NeoForge)** con **Cobblemon 1.7.3 o posterior** (probado hasta 1.8.2).

- Mueve y escala el HUD de party de Cobblemon (los seis retratos) a cualquier parte de la pantalla.
- Pantalla "Mover y escalar HUD" (tecla **Y**, Mod Menu en Fabric o el botón "Config" de la lista de mods en NeoForge): arrastrar con el ratón, rueda para el tamaño (×0.5–×2) y flechas para afinar.
- Se pega al punto más cercano de la pantalla (esquinas, centro de cada borde o centro), así se adapta a otra resolución o escala de interfaz.
- Forma **vertical** (como Cobblemon) u **horizontal** (los seis en fila, con los avisos de nivel arriba o abajo).
- **Alineación** con menos de 6 Pokémon: izquierda/centro/derecha en horizontal y arriba/centro/abajo en vertical. Con centro o derecha/abajo, los Pokémon se juntan dentro del hueco de 6 y sus avisos (nivel, "+N EXP", evolución) van con ellos.
- **Colores** (botón "Colores…" del editor): un color por ranura o el mismo para todas, con 12 predefinidos, barras R/G/B y código hex, más la opacidad de las ranuras (30–100 %). El Pokémon seleccionado (↑/↓) puede ir en automático (el color de su ranura, más claro) o con un color fijo propio que se mueve con la selección. Solo se tiñe el panel de la ranura; el retrato, el texto, las barras y los avisos no cambian.
- Pegado a la derecha, los slots se dibujan **en espejo** para que su lado plano quede contra el borde.
- Botón para desactivarlo: el HUD vuelve a su sitio original sin perder tu posición.
- En la pantalla de colocación se ven también los HUD de Cobble Hunt Log y Cobble EV Tracker, si están instalados, para colocarlos juntos.
- Compatible con el ajuste de escala del HUD de party que hace el mod del servidor Diosesmon.

Solo cambia dónde y a qué tamaño se dibuja lo que Cobblemon ya muestra; no añade información.

- Fabric: Fabric Loader ≥ 0.16.0, Fabric API y Cobblemon ≥ 1.7.3. Recomendado: Mod Menu.
- NeoForge: NeoForge 21.1.x y Cobblemon ≥ 1.7.3 (que a su vez necesita Kotlin for Forge).
- Licencia: MIT. Pokémon es una marca de Nintendo/Game Freak/The Pokémon Company.

## Compilar
```
.\gradlew build
```
Salen dos jars: `fabric/build/libs/cobblepartyhud-fabric-<versión>.jar` y `neoforge/build/libs/cobblepartyhud-neoforge-<versión>.jar`.

Estructura multi-loader (estilo MultiLoader-Template, sin Architectury API): `common/` tiene todo el HUD, los mixins y el editor; `fabric/` y `neoforge/` solo el arranque y la integración con cada loader.
