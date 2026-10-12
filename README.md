# Cobble Party HUD

Mod **solo de cliente** para Minecraft **1.21.1 (Fabric)** con **Cobblemon 1.7.x**.

- Mueve y escala el HUD de party de Cobblemon (los seis retratos) a cualquier parte de la pantalla.
- Pantalla "Mover y escalar HUD" (tecla **Y** o Mod Menu): arrastrar con el ratón, rueda para el tamaño (×0.5–×2) y flechas para afinar.
- Se pega al punto más cercano de la pantalla (esquinas, centro de cada borde o centro), así se adapta a otra resolución o escala de interfaz.
- Forma **vertical** (como Cobblemon) u **horizontal** (los seis en fila, con los avisos de nivel arriba o abajo).
- Pegado a la derecha, los slots se dibujan **en espejo** para que su lado plano quede contra el borde.
- Botón para desactivarlo: el HUD vuelve a su sitio original sin perder tu posición.
- En la pantalla de colocación se ven también los HUD de Cobble Hunt Log y Cobble EV Tracker, si están instalados, para colocarlos juntos.
- Compatible con el ajuste de escala del HUD de party que hace el mod del servidor Diosesmon.

Solo cambia dónde y a qué tamaño se dibuja lo que Cobblemon ya muestra; no añade información.

- Requiere: Fabric Loader ≥ 0.16.0, Fabric API y Cobblemon ≥ 1.7.0. Recomendado: Mod Menu.
- Licencia: MIT. Pokémon es una marca de Nintendo/Game Freak/The Pokémon Company.

## Compilar
```
.\gradlew build
```
El `.jar` queda en `build/libs/`.
