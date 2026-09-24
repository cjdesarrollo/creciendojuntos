# Azure Functions - Node.js (Modelo v4)

Este proyecto contiene la estructura base para desarrollar **Azure Functions** con Node.js utilizando el modelo de programación v4 de `@azure/functions`.

## 📁 Estructura del proyecto

- [`host.json`](file:///c:/Users/walte/Escritorio/Personal/Creciendo_Juntos/Azure_Backups/azure_backups/host.json): Configuración del runtime de Azure Functions.
- [`local.settings.json`](file:///c:/Users/walte/Escritorio/Personal/Creciendo_Juntos/Azure_Backups/azure_backups/local.settings.json): Configuración de variables de entorno y conexión local (no subir a repositorio).
- [`src/functions/httpTrigger.js`](file:///c:/Users/walte/Escritorio/Personal/Creciendo_Juntos/Azure_Backups/azure_backups/src/functions/httpTrigger.js): Función HTTP Trigger de muestra.
- [`package.json`](file:///c:/Users/walte/Escritorio/Personal/Creciendo_Juntos/Azure_Backups/azure_backups/package.json): Gestión de paquetes de Node.js.

## 🚀 Requisitos e Instalación

1. Instalar dependencias del proyecto:
   ```bash
   npm install
   ```

2. (Opcional) Instalar Azure Functions Core Tools globalmente si deseas probar las funciones en local:
   ```bash
   npm install -g azure-functions-core-tools@4 --unsafe-perm true
   ```

## 🛠️ Ejecución local

Para iniciar el entorno local de funciones:
```bash
npm start
```
O directamente con el CLI:
```bash
func start
```

Una vez en ejecución, la función estará disponible en:
`http://localhost:7071/api/httpTrigger?name=TuNombre`
