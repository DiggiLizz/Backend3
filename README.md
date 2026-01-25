Sistema de Migración de Datos Financieros - Proyecto Backend III
Lilian Zapata - 25/01/2026


1. Descripción General
Este proyecto consiste en la implementación de una solución de procesamiento por lotes (Batch) para la modernización de los sistemas de "Finanzas ABC".
El sistema migra datos desde estructuras legacy (archivos CSV) hacia una base de datos Oracle, aplicando transformaciones de negocio y políticas de escalamiento.

2. Tecnologías Utilizadas
- Java 21
- Spring Boot 3.3.2
 -Spring Batch 5
- Sql developer
- Maven

3. Arquitectura y Componentes
El sistema se organiza en paquetes según su responsabilidad funcional:
  - Jobs: Definición y flujo de los procesos de negocio.
  - Processors: Lógica de validación, detección de anomalías y cálculos financieros.
  - Listeners: Monitoreo y trazabilidad de la ejecución del Job.
  - Support: Manejo de excepciones y mapeo de datos.
  - Tasklets: Generación de reportes de salida en formato de texto plano.

4. Criterios de la Semana 3
   
4.1. Escalamiento y Rendimiento
Se ha implementado una política de procesamiento paralelo mediante el uso de ThreadPoolTaskExecutor. Esto permite la ejecución de Steps de manera multihilo 
(Multi-threading), optimizando el uso de recursos y reduciendo el tiempo de procesamiento.

Configuración del Pool: CorePoolSize: 4, MaxPoolSize: 8.

Identificación: Los hilos se identifican en los logs con los prefijos DailyJob-, InterestThread- y Annual-.

4.2. Resiliencia y Tolerancia a Fallos (Skip Policy)
Se configuraron políticas de omisión de registros para garantizar la continuidad del servicio ante datos mal formados. 
Los límites de tolerancia se definieron según la criticidad de la información:
  - Movimientos Diarios: Límite de 10 omisiones.
  - Cálculo de Intereses: Límite de 100 omisiones.
  - Estados Anuales: Límite de 500 omisiones.

4.3. Monitoreo y Trazabilidad
Se integró un JobLoggerListener que registra formalmente en la consola el ID de ejecución, el nombre del Job y el estado final (COMPLETED/FAILED),
cumpliendo con los estándares de auditoría requeridos.

5. Instrucciones de Ejecución
Para iniciar los procesos de forma independiente desde la terminal, ejecute:
- Migración Diaria: mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=dailyMovementsJob"
- Intereses Trimestrales: mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=quarterlyInterestJob"
- Resumen Anual: mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=annualStatementsJob"

6. Evidencias de Funcionamiento
El sistema genera las siguientes evidencias tras su ejecución exitosa (respaldadas en documento word, guardado en zip):
- Logs de consola: Detalle de hilos en ejecución y reportes del Listener.
- Tablas sql developer: Persistencia de datos en DAILY_TRANSACTIONS, INTEREST_RESULTS y ANNUAL_STATEMENTS.
- Reportes TXT: Archivos de resumen generados por los Tasklets de cierre.
