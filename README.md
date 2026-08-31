# Sortea V1.1

Aplicación Android nativa para generar boletas de sorteo en PDF.

## V1.1
- Menú principal renovado con información de la versión y funciones principales.
- Icono oficial de Sortea integrado en la aplicación.
- Pantalla "Acerca de" actualizada a V1.1.0.
- Tema visual refinado con una identidad verde/dorada coherente con el icono.
- Generación de boletas, vista previa y configuración de tamaño conservadas.
- Hasta 499 boletas por generación.
- Números 001–999 sin repetir.
- Distribución automática en hoja Letter según el tamaño configurado.
- Los PDF se guardan en la carpeta `Sortea` del almacenamiento del dispositivo.

## Plantilla oficial
La boleta PDF y la vista previa utilizan directamente la plantilla suministrada para el proyecto (`app/src/main/res/drawable-nodpi/boleta_template.png`). Los datos variables se superponen sobre las áreas correspondientes.

## Compatibilidad
- Kotlin + Jetpack Compose + Material 3.
- compileSdk 35 / targetSdk 35.
- Java 17.
- Generación mediante `assembleDebug`.

## GitHub Actions
**El workflow existente no fue modificado.** Continúa ejecutando tests, `assembleDebug`, verificando `app-debug.apk` y publicando el artifact con la configuración que ya tenía el proyecto.
