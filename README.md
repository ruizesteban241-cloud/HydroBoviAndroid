# HydroBovi - App Android para HC-05

## Cómo compilar y obtener el APK:

### Opción 1: Android Studio (Recomendado)

1. Abre **Android Studio**
2. File → Open → Selecciona la carpeta `HydroBoviAndroid`
3. Espera a que sincronice Gradle (puede tardar 5-10 min la primera vez)
4. Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfiere el APK al celular e instálalo

### Opción 2: Línea de comandos

```bash
cd HydroBoviAndroid
./gradlew assembleDebug
```

APK generado en: `app/build/outputs/apk/debug/app-debug.apk`

---

## Cómo usar la app:

1. **Vincula el HC-05** desde Configuración Bluetooth del celular (PIN: 1234)
2. Abre la app HydroBovi
3. Acepta los permisos de Bluetooth
4. Toca **🔵 Conectar**
5. Selecciona **HC-05** de la lista
6. Verás los datos en tiempo real:
   - Estado del tanque (Vacío/Llenando/Lleno) con colores
   - Estado de cada flotador
   - Caudal en L/m
   - Estado de la válvula

---

## Formato esperado del PIC:

El PIC debe enviar por Bluetooth cada 500ms:

```
Estado:LLENANDO Bajo:ALTO Alto:BAJO Valvula:ACTIVA Caudal:1.2L/m
```

---

## Requisitos:

- Android 5.0+ (API 21)
- Bluetooth Classic (HC-05 funciona)
- Permisos de ubicación (requerido por Android para Bluetooth)
