# A&H Logística · despacho y conductores

Aplicación web instalable con dos espacios separados: **Torre de control** (`/control`) y **Conductor** (`/conductor`). Está hecha para una operación pequeña en Aguascalientes: una cuenta de logística y dos cuentas de conductor. No requiere Firebase ni Google Cloud.

## Incluye

- Emisión, asignación, aceptación, inicio, entrega y cancelación de órdenes.
- Mapa OpenStreetMap con la última ubicación de los conductores.
- El conductor elige cuándo compartir ubicación; se envía cada 8 segundos y la torre consulta cambios cada 7 segundos.
- La ubicación se guarda como un solo punto por conductor, se elimina al detener el seguimiento o cerrar sesión y no se crea historial de recorridos. Si el teléfono pierde conexión, la última posición se borra tras 12 horas sin actualización.
- La pantalla muestra las 100 órdenes más recientes para mantener ligera la sincronización; las órdenes anteriores siguen en la base.
- Inicio de sesión privado con tres cuentas internas. No hay registro público de usuarios.
- La base D1 guarda cuentas, órdenes y el punto actual de cada conductor.

La ubicación requiere permiso del navegador y una dirección HTTPS. Para este MVP, el conductor debe mantener la app abierta durante el servicio; los navegadores pueden suspender el GPS cuando la app queda en segundo plano o se bloquea el teléfono. Si se pierde la conexión, la torre muestra el último punto como desactualizado.

## Publicar en el plan gratuito de Cloudflare

Se necesita una cuenta de Cloudflare con **Workers Free** y acceso al navegador para autorizar Wrangler mediante OAuth. La publicación usa el subdominio gratuito `workers.dev`; no hace falta comprar dominio. No pongas contraseñas ni secretos en GitHub, en este archivo o en el chat.

También se requiere Node.js y npm instalados en la computadora desde donde se publica.

Desde esta carpeta, ejecuta:

```powershell
npx wrangler@latest login
npx wrangler@latest d1 create ah-logistica
```

Wrangler mostrará un `database_id`. Pégalo en `database_id` dentro de `wrangler.jsonc` y después aplica el esquema:

```powershell
npx wrangler@latest d1 migrations apply ah-logistica --remote
npx wrangler@latest deploy
```

El primer despliegue creará el Worker. Luego carga estos secretos con Wrangler; cada comando solicita el valor sin guardarlo en el repositorio:

```powershell
npx wrangler@latest secret put SESSION_SECRET
npx wrangler@latest secret put PASSWORD_PEPPER
npx wrangler@latest secret put SETUP_CODE
```

Genera los tres valores aleatorios y distintos con un administrador de contraseñas. Para `SETUP_CODE`, conserva el valor temporal hasta completar el primer acceso. Wrangler publicará automáticamente una nueva versión al guardar cada secreto.

Abre la URL `https://ah-logistica-operacion.<tu-subdominio>.workers.dev` que indique Wrangler. La primera pantalla permite crear las cuentas privadas. Los usuarios son `control1`, `conductor1` y `conductor2`; elige contraseñas de al menos 12 caracteres y conserva las claves en un administrador de contraseñas. El formulario de instalación deja de estar disponible cuando existen esas tres cuentas.

## Operación

1. Logística inicia sesión en `/control`, crea la orden y elige al conductor.
2. El conductor inicia sesión en `/conductor`, acepta la orden y activa **Compartir ubicación**.
3. En ruta, el conductor marca el inicio del recorrido y después la entrega. Puede apagar el GPS al finalizar.
4. Logística ve los estados y la última ubicación en el mapa.

El MVP no calcula tarifas, no cobra, no factura, no geocodifica direcciones ni ofrece navegación giro a giro. Los datos de origen y destino se capturan como texto. OpenStreetMap aporta las teselas del mapa y requiere conexión a internet.

## Costos y límites

Workers Free incluye hasta 100,000 solicitudes por día y D1 Free hasta 5 millones de filas leídas, 100,000 escritas por día y 5 GB de almacenamiento total. Para tres personas, la sincronización ligera está dentro de esas cuotas en un uso normal. Si se rebasa una cuota gratuita, Cloudflare rechaza solicitudes de ese servicio hasta que se reinicie la cuota; no cambies a un plan de pago para esta aplicación.

- [Workers: precios y cuotas](https://developers.cloudflare.com/workers/platform/pricing/)
- [D1: precios y cuotas](https://developers.cloudflare.com/d1/platform/pricing/)
- [Wrangler: secretos](https://developers.cloudflare.com/workers/configuration/secrets/)
