// ======================================================================
//  SOPA DE LETRAS - PROYECTO COMPLETO (Java Swing, IntelliJ IDEA Community)
//  El archivo se debe llamar sopadeletras.java (igual que la clase)
//
//  CÓMO LEER LOS COMENTARIOS:
//   - Antes de cada método hay un cuadro con: QUÉ HACE / RECIBE (entrada) / DEVUELVE (salida).
//   - Dentro del código, cada if / else / for / while tiene su explicación.
//   - "void" significa que el método NO devuelve nada, solo hace una acción.
//
//  FLUJO DEL JUEGO:
//   Menú (nombre) -> Nivel 1 (3 min) -> Nivel 2 (5 min) -> Nivel 3 (5 min)
//   -> pregunta "¿aceptas un nuevo reto?" -> el jugador ELIGE su carrera
//   -> Nivel 4 extra (3 min) -> ranking.
// ======================================================================

import javax.sound.sampled.AudioFormat;          // Formato del sonido (calidad, canales...)
import javax.sound.sampled.AudioSystem;          // Acceso a las bocinas del computador
import javax.sound.sampled.Clip;                 // Reproduce sonidos cortos (clic, efectos)
import javax.sound.sampled.LineEvent;            // Avisa cuando un sonido terminó
import javax.sound.sampled.SourceDataLine;       // Reproduce sonido continuo (la música)
import javax.swing.*;                            // Ventanas, botones, paneles, tablas...
import javax.swing.table.DefaultTableModel;      // Modelo (datos) de la tabla del ranking
import java.awt.*;                               // Colores, fuentes, dibujo
import java.awt.event.MouseAdapter;              // Para detectar lo que hace el mouse
import java.awt.event.MouseEvent;                // Datos del mouse (posición, botón)
import java.awt.geom.AffineTransform;            // Para girar los confetis y las verduras al dibujarlas
import java.awt.geom.GeneralPath;                // Para dibujar formas libres (chorro de sopa, zanahoria)
import java.nio.charset.StandardCharsets;        // Para guardar tildes bien en el archivo
import java.nio.file.Files;                      // Leer/escribir archivos
import java.nio.file.Path;                       // Ruta de un archivo
import java.nio.file.Paths;                      // Crear rutas
import java.util.ArrayList;                      // Listas que crecen solas
import java.util.Collections;                    // Para mezclar (shuffle)
import java.util.Random;                         // Números al azar
import java.util.function.Supplier;              // Para pasar "una tarea" a la ventana de carga
import java.io.ByteArrayOutputStream;            // Para leer la respuesta de Firebase
import java.io.InputStream;                      // Para leer la respuesta de Firebase
import java.io.OutputStream;                     // Para enviar datos a Firebase
import java.net.HttpURLConnection;               // Conexión HTTP con Firebase
import java.net.URL;                             // Dirección de Firebase
import java.net.URLEncoder;                      // Para escribir bien nombres con espacios o tildes en la dirección
import java.util.regex.Matcher;                  // Para leer el JSON que devuelve Firebase
import java.util.regex.Pattern;                  // Para leer el JSON que devuelve Firebase

public class sopadeletras extends JFrame {       // Nuestra clase ES una ventana (JFrame)

    /**
     * QUÉ HACE: es el punto de entrada, lo primero que Java ejecuta.
     * RECIBE: args (argumentos de consola, aquí no se usan).
     * DEVUELVE: nada (void).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {           // Pide a Swing abrir la ventana de forma segura
            sopadeletras ventana = new sopadeletras();   // Crea la ventana
            ventana.setVisible(true);                    // La hace visible
            ventana.campoNombre.requestFocusInWindow();  // Deja el cursor listo en la caja del nombre
        });
    }

    //  1) DATOS FIJOS: BANCOS DE PALABRAS (sin tildes, y largas)
    //     Se evitan palabras cortas como "FE" porque aparecían
    //     repetidas por casualidad dentro de las letras al azar.
    private final String[] BANCO_TECNOLOGICO = {"PYTHON", "SERVIDOR", "TECLADO", "PANTALLA", "INTERNET", "ROUTER",
            "SOFTWARE", "MONITOR", "CODIGO", "JAVA", "DATOS", "WEBCAM"};
    private final String[] BANCO_FUTBOL = {"ARQUERO", "DELANTERO", "CANCHA", "ARBITRO", "PENAL", "CORNER",
            "DEFENSA", "MUNDIAL", "CAMPEON", "ESTADIO", "FUTBOL", "BALON"};
    private final String[] BANCO_OTROS = {"ORTODOXO"," FISCALIZACION","COHERENTE","BANCALIZACION","INSTRUMENTACION",
            "ONOMATOPEYA","HISTORIA","CONSTELACIONES", "PRIVATIZACION",};

    // Carreras que el jugador puede ELEGIR en el reto extra (puedes agregar o quitar líneas)
    private final String[] NOMBRES_CARRERAS = {"Ing. Sistemas", "Ing. Redes y Telecomunicaciones", "Filosofía",
            "Nutrición", "Fisioterapia", "Marketing", "Contabilidad", "Administración", "Finanzas",
            "Psicopedagogía", "Derecho", "Medicina", "Enfermería", "Arquitectura"};

    // Palabras largas de cada carrera (en el MISMO orden que la lista de arriba)
    private final String[][] BANCOS_CARRERAS = {
            {"PROGRAMACION", "ALGORITMO", "COMPILADOR", "CIBERSEGURIDAD", "SERVIDOR", "FRAMEWORK", "DEPURACION", "ARQUITECTURA", "BASEDEDATOS", "VIRTUALIZACION"},
            {"ENRUTADOR", "CONMUTADOR", "FIBRAOPTICA", "ANTENA", "PROTOCOLO", "TELEFONIA", "SATELITE", "CABLEADO", "INALAMBRICO", "LATENCIA"},
            {"METAFISICA", "EPISTEMOLOGIA", "ARISTOTELES", "PENSAMIENTO", "DIALECTICA", "RACIONALISMO", "ONTOLOGIA", "SOCRATES", "ESTETICA", "FENOMENOLOGIA"},
            {"PROTEINAS", "VITAMINAS", "CARBOHIDRATOS", "METABOLISMO", "HIDRATACION", "NUTRIENTES", "CALORIAS", "OBESIDAD", "MINERALES", "ALIMENTACION"},
            {"REHABILITACION", "KINESIOLOGIA", "ARTICULACION", "ELECTROTERAPIA", "MASAJE", "ESTIRAMIENTO", "POSTURA", "COLUMNA", "MOVILIDAD", "FLEXIBILIDAD"},
            {"PUBLICIDAD", "ESTRATEGIA", "SEGMENTACION", "MERCADOTECNIA", "CONSUMIDOR", "FIDELIZACION", "PROMOCION", "COMPETENCIA", "BRANDING", "TENDENCIAS"},
            {"AUDITORIA", "PRESUPUESTO", "IMPUESTOS", "INVENTARIO", "DEPRECIACION", "CONTABILIDAD", "PATRIMONIO", "BALANCE", "TRIBUTARIO", "LIQUIDEZ"},
            {"GERENCIA", "PLANIFICACION", "LIDERAZGO", "ORGANIZACION", "EFICIENCIA", "PRODUCTIVIDAD", "LOGISTICA", "DIRECCION", "EMPRESA", "CONTROL"},
            {"INVERSION", "INFLACION", "RENTABILIDAD", "DIVIDENDOS", "FINANCIAMIENTO", "COTIZACION", "PRESTAMO", "CAPITAL", "ACCIONES", "PORTAFOLIO"},
            {"APRENDIZAJE", "PEDAGOGIA", "COGNITIVO", "INCLUSION", "MOTIVACION", "DIAGNOSTICO", "ORIENTACION", "DESARROLLO", "PSICOLOGIA", "DIDACTICA"},
            {"CONSTITUCION", "JURISPRUDENCIA", "LEGISLACION", "SENTENCIA", "TRIBUNAL", "CONTRATO", "PROCESAL", "ABOGADO", "DEMANDA", "JUSTICIA"},
            {"CARDIOLOGIA","ERITROPOYETINA","HOMOCISTENIA","TRICUSPIDE" ,"MITRAL","TOXICOLOGIA", "PEDIATRIA", "CIRUGIA", "ANATOMIA", "FARMACOLOGIA", "PATOLOGIA", "NEUROLOGIA", "TRAUMATOLOGIA", "ONCOLOGIA", "URGENCIAS"},
            {"VACUNACION", "CURACION", "QUIROFANO", "PACIENTE", "HIGIENE", "MEDICACION", "HOSPITAL", "EMERGENCIA", "ASISTENCIA", "BIOSEGURIDAD"},
            {"ESTRUCTURA", "URBANISMO", "FACHADA", "CIMIENTOS", "MAQUETA", "DISENO", "CONSTRUCCION", "PAISAJISMO", "VOLUMETRIA", "ILUMINACION"}
    };

    // Las 8 direcciones posibles de una palabra. Cada par es {cambio de fila, cambio de columna}.
    // Ejemplo {0,1} = avanza hacia la derecha; {1,1} = diagonal hacia abajo-derecha; {0,-1} = al revés.
    private final int[][] DIRECCIONES = {{0, 1}, {1, 0}, {1, 1}, {0, -1}, {-1, 0}, {-1, -1}, {-1, 1}, {1, -1}};

    // Colores para pintar cada palabra encontrada (la 1ª roja, la 2ª azul, etc.)
    private final Color[] PALETA_COLORES = {
            new Color(255, 102, 102), new Color(102, 178, 255), new Color(102, 220, 102),
            new Color(255, 178, 102), new Color(255, 153, 255), new Color(178, 102, 255),
            new Color(102, 230, 210), new Color(190, 190, 190), new Color(240, 220, 90),
            new Color(150, 200, 120), new Color(230, 130, 170), new Color(120, 160, 230)
    };

    private final String ARCHIVO_RANKING = "ranking_sopa.txt";   // Copia local del ranking (respaldo si no hay internet)

    // ----- FIREBASE (Realtime Database) -----
    // Pega aquí la URL de TU base de datos (Firebase > Realtime Database > Datos; sale arriba).
    // Ejemplo: "https://sopa-de-letras-12345-default-rtdb.firebaseio.com"
    // Mientras tenga "TU-PROYECTO", el juego usa solo el archivo local.
    private final String FIREBASE_URL = "https://sopadeletra-e3981-default-rtdb.firebaseio.com";
    // Opcional: secreto/token si tus reglas NO son públicas. Déjalo "" si usas reglas abiertas.
    private final String FIREBASE_AUTH = "";
    private final int NIVELES_BASE = 3;                          // Cantidad de niveles antes del reto extra

    // ============================================================
    //  2) CLASES AUXILIARES (moldes para guardar varios datos juntos)
    // ============================================================

    /** Una "ronda" = un tablero con su categoría, tamaño y tiempo límite. */
    private static class Ronda {
        String categoria;   // Ej: "Vida"
        String etiqueta;    // Ej: "Nivel 3"
        int tamano;         // Tamaño del tablero (10 = 10x10)
        int cantidad;       // Cuántas palabras hay que encontrar
        int segundos;       // Tiempo límite en segundos
        String[] banco;     // Palabras entre las que se elige

        /** RECIBE los 6 datos de la ronda y los guarda. No devuelve nada (es un constructor). */
        Ronda(String categoria, String etiqueta, int tamano, int cantidad, int segundos, String[] banco) {
            this.categoria = categoria;   // "this." = el dato de ESTE objeto
            this.etiqueta = etiqueta;
            this.tamano = tamano;
            this.cantidad = cantidad;
            this.segundos = segundos;
            this.banco = banco;
        }
    }

    /** Una palabra que el jugador ya encontró: dónde empieza, dónde termina y su color. */
    private static class PalabraEncontrada {
        String palabra;
        int filaIni, colIni, filaFin, colFin;   // Coordenadas de la primera y la última letra
        Color color;

        /** RECIBE la palabra, sus coordenadas y su color. No devuelve nada. */
        PalabraEncontrada(String palabra, int fi, int ci, int ff, int cf, Color color) {
            this.palabra = palabra;
            this.filaIni = fi;
            this.colIni = ci;
            this.filaFin = ff;
            this.colFin = cf;
            this.color = color;
        }
    }

    /** Una fila del ranking: nombre, cuántas rondas completó y su tiempo total. */
    private static class Registro {
        String nombre;
        int rondas;
        int segundos;

        /** RECIBE nombre, rondas y segundos. No devuelve nada. */
        Registro(String nombre, int rondas, int segundos) {
            this.nombre = nombre;
            this.rondas = rondas;
            this.segundos = segundos;
        }
    }

    /** Un confeti: un papelito de color que cae girando. */
    private static class Particula {
        double x, y, vx, vy, rot, vrot;   // Posición, velocidad y giro
        int ancho, alto;                  // Tamaño del papelito
        Color color;                      // Color
    }

    //  3) SONIDO (todo generado por el programa, sin archivos)
    //     Un sonido digital es una lista de números (bytes) que
    //     describen una onda. Aquí calculamos esas ondas con matemática.

    private static class Sonido {
        private static volatile boolean musicaActiva = true;      // ¿La música está encendida?
        private static volatile boolean hiloCorriendo = false;    // ¿Ya hay un hilo tocando música?
        private static final AudioFormat FORMATO = new AudioFormat(44100f, 16, 1, true, false); // Calidad CD, 1 canal
        private static final Random AZAR = new Random();          // Para el "ruido" del efecto de cierre

        /**
         * QUÉ HACE: crea una nota pura (onda senoidal) que se apaga suavemente.
         * RECIBE: frecuencia en Hz (más alto = más agudo), duración en milisegundos y volumen (0 a 1).
         * DEVUELVE: un arreglo de bytes con el sonido listo para reproducir.
         */
        private static byte[] generarTono(double frecuencia, int milisegundos, double volumen) {
            int muestras = (int) (44100 * milisegundos / 1000.0);          // Cantidad de "puntitos" del sonido
            byte[] datos = new byte[muestras * 2];                         // Cada muestra usa 2 bytes
            for (int i = 0; i < muestras; i++) {                           // Repite una vez por cada muestra
                double t = i / 44100.0;                                    // Tiempo (segundos) de esta muestra
                double envolvente = Math.exp(-3.0 * i / muestras);         // Hace que el sonido se apague poco a poco
                if (i < 200) envolvente *= i / 200.0;                      // Si es el inicio: sube suave (evita chasquidos)
                short valor = (short) (Math.sin(2 * Math.PI * frecuencia * t) * volumen * envolvente * 32767);
                datos[2 * i] = (byte) (valor & 0xFF);                      // Guarda la parte baja del número
                datos[2 * i + 1] = (byte) ((valor >> 8) & 0xFF);           // Guarda la parte alta del número
            }
            return datos;                                                  // Devuelve el sonido creado
        }

        /**
         * QUÉ HACE: crea una nota "más rica" (con armónicos, suena más a instrumento) para la música.
         * RECIBE: frecuencia (Hz), duración (ms) y volumen (0 a 1).
         * DEVUELVE: bytes del sonido.
         */
        private static byte[] generarNotaMusical(double frecuencia, int milisegundos, double volumen) {
            int muestras = (int) (44100 * milisegundos / 1000.0);
            byte[] datos = new byte[muestras * 2];
            for (int i = 0; i < muestras; i++) {
                double t = i / 44100.0;
                double envolvente = Math.exp(-2.2 * i / muestras);         // Se apaga más lento que el tono simple
                if (i < 150) envolvente *= i / 150.0;                      // Entrada suave
                double onda = Math.sin(2 * Math.PI * frecuencia * t)            // Nota base
                        + 0.4 * Math.sin(2 * Math.PI * frecuencia * 2 * t)      // + 2° armónico (octava)
                        + 0.15 * Math.sin(2 * Math.PI * frecuencia * 3 * t);    // + 3° armónico
                short valor = (short) (onda / 1.55 * volumen * envolvente * 32767);
                datos[2 * i] = (byte) (valor & 0xFF);
                datos[2 * i + 1] = (byte) ((valor >> 8) & 0xFF);
            }
            return datos;
        }

        /**
         * QUÉ HACE: crea el efecto de "cierre / zip / swoosh": un barrido de agudo creciente con un poco de ruido.
         * RECIBE: frecuencia inicial, frecuencia final, duración (ms) y volumen.
         * DEVUELVE: bytes del sonido.
         */
        private static byte[] generarBarrido(double f1, double f2, int milisegundos, double volumen) {
            int muestras = (int) (44100 * milisegundos / 1000.0);
            byte[] datos = new byte[muestras * 2];
            double fase = 0;                                               // Posición actual dentro de la onda
            for (int i = 0; i < muestras; i++) {
                double progreso = (double) i / muestras;                   // Va de 0 (inicio) a 1 (final)
                double f = f1 + (f2 - f1) * progreso * progreso;           // La frecuencia sube cada vez más rápido
                fase += 2 * Math.PI * f / 44100.0;                         // Avanzamos la onda según la frecuencia actual
                double envolvente = Math.sin(Math.PI * progreso);          // Sube y baja suave (como una campana)
                double ruido = (AZAR.nextDouble() * 2 - 1) * 0.35;         // Ruido = el "shhh" del cierre
                short valor = (short) ((Math.sin(fase) * 0.65 + ruido) * volumen * envolvente * 32767);
                datos[2 * i] = (byte) (valor & 0xFF);
                datos[2 * i + 1] = (byte) ((valor >> 8) & 0xFF);
            }
            return datos;
        }

        /**
         * QUÉ HACE: pega dos sonidos uno detrás del otro.
         * RECIBE: sonido a y sonido b.   DEVUELVE: un solo sonido (a seguido de b).
         */
        private static byte[] unir(byte[] a, byte[] b) {
            byte[] r = new byte[a.length + b.length];                      // Arreglo del tamaño de los dos juntos
            System.arraycopy(a, 0, r, 0, a.length);                        // Copia a al inicio
            System.arraycopy(b, 0, r, a.length, b.length);                 // Copia b a continuación
            return r;
        }

        /**
         * QUÉ HACE: mezcla dos sonidos para que suenen AL MISMO TIEMPO (melodía + bajo).
         * RECIBE: dos sonidos del mismo largo.   DEVUELVE: el sonido mezclado.
         */
        private static byte[] mezclar(byte[] a, byte[] b) {
            byte[] r = new byte[a.length];
            for (int i = 0; i + 1 < a.length; i += 2) {                    // Recorre de 2 en 2 (cada muestra = 2 bytes)
                int va = (short) ((a[i] & 0xFF) | (a[i + 1] << 8));        // Reconstruye el número de a
                int vb = (short) ((b[i] & 0xFF) | (b[i + 1] << 8));        // Reconstruye el número de b
                int suma = va + vb;                                        // Mezclar = sumar las ondas
                if (suma > 32767) suma = 32767;                            // Si se pasa del máximo, lo recortamos
                else if (suma < -32768) suma = -32768;                     // Si baja del mínimo, lo recortamos
                r[i] = (byte) (suma & 0xFF);
                r[i + 1] = (byte) ((suma >> 8) & 0xFF);
            }
            return r;
        }

        /**
         * QUÉ HACE: reproduce un sonido corto UNA sola vez.
         * RECIBE: los bytes del sonido.   DEVUELVE: nada (void).
         */
        private static void reproducir(byte[] datos) {
            try {                                                          // "try" = intenta; si falla, salta al catch
                Clip clip = AudioSystem.getClip();                         // Pide un reproductor corto
                clip.open(FORMATO, datos, 0, datos.length);                // Le carga el sonido
                clip.addLineListener(ev -> {                               // Cuando pase algo con el reproductor...
                    if (ev.getType() == LineEvent.Type.STOP) clip.close(); // ...si terminó (STOP), lo libera
                });
                clip.start();                                              // ¡Suena!
            } catch (Exception e) {
                // Si el computador no tiene audio, no pasa nada: el juego sigue sin sonido
            }
        }

        /**
         * QUÉ HACE: crea el sonido de un líquido cayendo (ruido suave que ondula + burbujitas).
         * RECIBE: duración en milisegundos y volumen (0 a 1).   DEVUELVE: bytes del sonido.
         */
        private static byte[] generarLiquido(int milisegundos, double volumen) {
            int muestras = (int) (44100 * milisegundos / 1000.0);
            byte[] datos = new byte[muestras * 2];
            double suave = 0;                                              // Guarda el ruido "suavizado" (suena a agua)
            for (int i = 0; i < muestras; i++) {
                double t = i / 44100.0;
                double ruido = AZAR.nextDouble() * 2 - 1;                  // Ruido blanco (número al azar entre -1 y 1)
                suave = suave * 0.92 + ruido * 0.08;                       // Mezcla con lo anterior: quita lo agudo y áspero
                double oleaje = 0.6 + 0.4 * Math.sin(2 * Math.PI * 7 * t); // El volumen sube y baja 7 veces por segundo
                double burbuja = Math.sin(2 * Math.PI * (300 + 200 * Math.sin(2 * Math.PI * 5 * t)) * t) * 0.15; // "blub blub"
                double envolvente = Math.sin(Math.PI * i / muestras);      // Empieza y termina suave
                double onda = (suave * 3.0 * oleaje + burbuja) * volumen * envolvente;
                onda = Math.max(-1, Math.min(1, onda));                    // Limita el valor entre -1 y 1 (evita distorsión)
                short valor = (short) (onda * 32767);
                datos[2 * i] = (byte) (valor & 0xFF);
                datos[2 * i + 1] = (byte) ((valor >> 8) & 0xFF);
            }
            return datos;
        }

        /** Efecto de la sopa cayendo al plato (dura 1.5 segundos). No recibe ni devuelve nada. */
        static void efectoLiquido() {
            reproducir(generarLiquido(1500, 0.5));
        }

        /** Efecto "cling" de porcelana cuando el plato termina de formarse. No recibe ni devuelve nada. */
        static void efectoPlato() {
            reproducir(unir(generarTono(2600, 90, 0.4), generarTono(3100, 140, 0.35)));
        }

        /** El "clic" del reloj. No recibe nada ni devuelve nada. */
        static void clic() {
            reproducir(generarTono(1800, 45, 0.6));
        }

        /** EFECTO AL ENCONTRAR UNA PALABRA: "TRASHH" (barrido de cierre) + un "ding" al final. */
        static void efectoPalabraEncontrada() {
            reproducir(unir(generarBarrido(300, 3200, 240, 0.7), generarTono(1318.5, 170, 0.5)));
        }

        /** Sonido de victoria: 4 notas subiendo (Do, Mi, Sol, Do agudo). */
        static void fanfarria() {
            double[] notas = {523.25, 659.25, 783.99, 1046.50};
            byte[] todo = new byte[0];                                     // Empieza vacío
            for (double n : notas) {                                       // Por cada nota de la lista...
                todo = unir(todo, generarTono(n, 180, 0.5));               // ...la pegamos al final
            }
            reproducir(todo);
        }

        /** Sonido de "se acabó el tiempo": una nota grave y larga. */
        static void sonidoFin() {
            reproducir(generarTono(160, 900, 0.7));
        }

        /**
         * QUÉ HACE: enciende la música de fondo (una melodía con bajo que se repite sin parar).
         * RECIBE/DEVUELVE: nada. Usa un "hilo" aparte para no congelar la ventana.
         */
        static void iniciarMusica() {
            if (hiloCorriendo) return;                                     // Si ya hay música sonando, no hace nada
            hiloCorriendo = true;
            Thread hilo = new Thread(() -> {                               // Hilo = tarea que corre en paralelo
                SourceDataLine linea = null;
                try {
                    linea = AudioSystem.getSourceDataLine(FORMATO);        // Salida de audio continua
                    linea.open(FORMATO);
                    linea.start();
                    // Melodía: 32 pasos. El 0 significa silencio.
                    double[] melodia = {
                            659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25, 0,
                            587.33, 698.46, 880.00, 698.46, 587.33, 523.25, 493.88, 0,
                            659.25, 783.99, 1046.50, 987.77, 880.00, 783.99, 659.25, 587.33,
                            523.25, 659.25, 783.99, 0, 523.25, 0, 0, 0};
                    // Bajo: una nota grave cada 4 pasos (acompañamiento)
                    double[] bajo = {
                            130.81, 0, 0, 0, 130.81, 0, 0, 0, 146.83, 0, 0, 0, 98.00, 0, 0, 0,
                            130.81, 0, 0, 0, 164.81, 0, 0, 0, 110.00, 0, 0, 0, 98.00, 0, 0, 0};
                    int paso = 0;                                          // En qué nota vamos
                    int ms = 190;                                          // Duración de cada paso (velocidad)
                    int largo = (int) (44100 * ms / 1000.0) * 2;           // Tamaño en bytes de un paso
                    while (musicaActiva) {                                 // Repite mientras la música esté ENCENDIDA
                        double fm = melodia[paso % melodia.length];        // Nota de melodía de este paso
                        double fb = bajo[paso % bajo.length];              // Nota de bajo de este paso
                        byte[] voz1 = (fm == 0) ? new byte[largo] : generarNotaMusical(fm, ms, 0.13); // Si es 0: silencio
                        byte[] voz2 = (fb == 0) ? new byte[largo] : generarNotaMusical(fb, ms * 3, 0.16);
                        byte[] voz2Corta = new byte[largo];                // El bajo dura más, tomamos solo lo que cabe
                        System.arraycopy(voz2, 0, voz2Corta, 0, Math.min(largo, voz2.length));
                        byte[] paso1 = mezclar(voz1, voz2Corta);           // Melodía + bajo juntos
                        linea.write(paso1, 0, paso1.length);               // Enviamos el sonido a las bocinas
                        paso++;                                            // Pasamos a la siguiente nota
                    }
                } catch (Exception e) {
                    // Si no hay audio disponible, se ignora
                } finally {                                                // "finally" se ejecuta SIEMPRE al terminar
                    if (linea != null) {                                   // Si se llegó a abrir la salida de audio...
                        linea.stop();
                        linea.close();                                     // ...la cerramos
                    }
                    hiloCorriendo = false;
                }
            });
            hilo.setDaemon(true);                                          // Se cierra solo al cerrar el programa
            hilo.start();                                                  // Arranca el hilo
        }

        /** Apaga la música si estaba encendida y la enciende si estaba apagada. */
        static void alternarMusica() {
            musicaActiva = !musicaActiva;                                  // "!" invierte: true -> false, false -> true
            if (musicaActiva) iniciarMusica();                             // Si quedó encendida, la arrancamos
        }

        /** DEVUELVE true si la música está encendida, false si está apagada. */
        static boolean estaEncendida() {
            return musicaActiva;
        }
    }

    // ============================================================
    //  4) VARIABLES DEL JUEGO (cambian mientras se juega)
    // ============================================================
    private ArrayList<Ronda> rondas;                     // Rondas de la partida (empieza con 3; el reto extra agrega la 4ª)
    private int rondaActual;                             // Índice de la ronda que se juega (0, 1, 2, 3)
    private int tiempoTotalPartida;                      // Suma de tiempos de las rondas completadas
    private int tamano;                                  // Tamaño del tablero actual
    private int tiempoRestante;                          // Segundos que quedan
    private boolean juegoActivo = false;                 // ¿Se puede jugar ahora mismo? (false durante letreros)
    private boolean tableroVisible = false;              // ¿Se muestran las letras? (se ocultan en la intro)
    private String nombreJugador = "";                   // Nombre del participante
    private ArrayList<String> palabrasDelJuego;          // Palabras a buscar en esta ronda
    private ArrayList<PalabraEncontrada> encontradas;    // Palabras ya encontradas (en orden)
    private char[][] matrizLetras;                       // La tabla de letras (filas x columnas)
    private Random ran = new Random();                   // Generador de azar

    // Componentes visuales
    private JPanel panelMenu;                            // Pantalla del menú
    private JPanel panelJuego;                           // Pantalla del juego
    private TableroPanel tablero;                        // El tablero donde se dibujan las letras
    private DefaultListModel<String> modeloLista;        // Contenido de la lista de palabras
    private JList<String> listaPalabrasUI;               // Lista visual de palabras a buscar
    private JPanel panelEncontradas;                     // Franja de abajo con las palabras halladas
    private JLabel lblProgreso;                          // Texto "Encontradas: 2 / 6"
    private JLabel lblReloj;                             // RELOJ: siempre visible abajo a la derecha
    private JTextField campoNombre;                      // Donde se escribe el nombre
    private Overlay overlay;                             // Capa de letreros y confeti
    private Timer relojJuego;                            // Cronómetro que "late" cada segundo
    private String mensajeRecord = "";                   // Mensaje de récord personal (se muestra al terminar)

    // ============================================================
    //  5) CONSTRUCTOR
    // ============================================================

    /** QUÉ HACE: prepara la ventana la primera vez. No recibe ni devuelve nada. */
    public sopadeletras() {
        setTitle("Sopa de Letras Interactiva");                  // Título de la ventana
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize(); // Tamaño de la pantalla del computador
        setSize(Math.min(1100, pantalla.width - 40), Math.min(850, pantalla.height - 60)); // Grande, pero sin pasarse de la pantalla
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);          // Al cerrar la ventana termina el programa
        setLocationRelativeTo(null);                             // Centra la ventana en la pantalla

        overlay = new Overlay();                                 // Creamos la capa de letreros/confeti
        setGlassPane(overlay);                                   // La ponemos por ENCIMA de todo (como un vidrio)
        overlay.setVisible(false);                               // Al inicio está oculta

        inicializarMenu();                                       // Armamos el menú
        setContentPane(panelMenu);                               // Mostramos el menú
        Sonido.iniciarMusica();                                  // Arranca la música de fondo
    }
    //  6) MENÚ PRINCIPAL
    // ============================================================

    /**
     * QUÉ HACE: construye el menú con fondo verde y verduras dibujadas, la caja del nombre y los botones.
     *           Si el jugador presiona ENTER dentro de la caja del nombre, entra directo al juego
     *           (es más rápido que mover el mouse hasta el botón).
     * RECIBE/DEVUELVE: nada.
     */
    private void inicializarMenu() {
        panelMenu = new FondoMenu();                             // Panel especial con fondo verde y verduras
        panelMenu.setLayout(new BorderLayout());                 // Layout simple para poner el contenido al centro

        JPanel contenido = new JPanel(new GridLayout(7, 1, 10, 10)); // 7 filas con todo el contenido del menú
        contenido.setOpaque(false);                              // Transparente: así se ve la tarjeta blanca de atrás
        contenido.setBorder(BorderFactory.createEmptyBorder(125, 190, 125, 190)); // Márgenes (dejan ver las verduras)

        JLabel titulo = new JLabel("SOPA DE LETRAS", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 36));
        titulo.setForeground(new Color(20, 100, 40));            // Verde oscuro
        contenido.add(titulo);

        JLabel info = new JLabel("<html><center>Nivel 1: 3 minutos. Niveles 2 y 3: 5 minutos.<br>"
                + "Al terminar puedes aceptar un reto extra de tu carrera (3 minutos).</center></html>",
                SwingConstants.CENTER);
        info.setFont(new Font("Arial", Font.PLAIN, 14));
        contenido.add(info);

        JLabel lblNombre = new JLabel("Escribe tu nombre y presiona ENTER:", SwingConstants.CENTER);
        lblNombre.setFont(new Font("Arial", Font.BOLD, 16));
        contenido.add(lblNombre);

        campoNombre = new JTextField();                          // Caja de texto para el nombre
        campoNombre.setFont(new Font("Arial", Font.PLAIN, 20));
        campoNombre.setHorizontalAlignment(SwingConstants.CENTER);
        contenido.add(campoNombre);

        JButton btnJugar = new JButton("JUGAR");
        btnJugar.setFont(new Font("Arial", Font.BOLD, 20));
        btnJugar.setBackground(new Color(46, 160, 70));          // Botón verde
        btnJugar.setForeground(Color.WHITE);
        btnJugar.addActionListener(e -> {                        // Qué pasa al hacer clic en JUGAR (o al presionar ENTER)
            String nombre = campoNombre.getText().trim().replace(";", ""); // Lee el nombre y quita ';' (lo usa el archivo)
            if (nombre.isEmpty()) {                              // Si no escribió nada...
                JOptionPane.showMessageDialog(this, "Escribe tu nombre para participar.");
                return;                                          // ...avisa y NO empieza el juego
            }
            nombreJugador = nombre;                              // Guardamos el nombre
            iniciarPartida();                                    // ¡A jugar!
        });
        contenido.add(btnJugar);

        // ENTER dentro de la caja de texto = lo mismo que hacer clic en JUGAR
        campoNombre.addActionListener(e -> btnJugar.doClick());

        JButton btnMejorar = new JButton("MEJORAR TIEMPO (elegir mi nombre)");
        btnMejorar.setFont(new Font("Arial", Font.BOLD, 16));
        btnMejorar.setBackground(new Color(245, 150, 40));       // Botón naranja
        btnMejorar.setForeground(Color.WHITE);
        btnMejorar.addActionListener(e -> mejorarTiempo());      // Abre la lista de participantes
        contenido.add(btnMejorar);

        JPanel fila = new JPanel(new GridLayout(1, 2, 10, 0));   // Dos botones lado a lado
        fila.setOpaque(false);
        JButton btnRanking = new JButton("VER RANKING");
        btnRanking.setFont(new Font("Arial", Font.PLAIN, 16));
        btnRanking.addActionListener(e -> mostrarRanking());     // Al hacer clic muestra el ranking
        fila.add(btnRanking);
        fila.add(crearBotonMusica());
        contenido.add(fila);

        panelMenu.add(contenido, BorderLayout.CENTER);           // El contenido va en el centro del fondo verde
    }

    /**
     * QUÉ HACE: botón "MEJORAR TIEMPO". Muestra la lista de participantes que ya están en el ranking,
     *           el jugador elige su nombre y vuelve a jugar para superar su marca.
     *           Si lo logra, su resultado en el ranking se reemplaza por el mejor (no se duplica).
     * RECIBE/DEVUELVE: nada.
     */
    private void mejorarTiempo() {
        ArrayList<Registro> lista = cargarRankingConMensaje("Cargando participantes...");  // Participantes guardados
        if (lista.isEmpty()) {                                   // Si todavía no hay nadie en el ranking...
            JOptionPane.showMessageDialog(this, "Todavía no hay participantes. ¡Juega primero para entrar al ranking!");
            return;                                              // ...avisa y sale
        }
        ordenarRanking(lista);                                   // Los mejores primero

        int elegido = dialogoRanking("MEJORAR TIEMPO", "Elige tu nombre y supera tu marca", lista, lista.size(), true);
        if (elegido >= 0) {                                      // Si eligió un nombre y presionó JUGAR...
            nombreJugador = lista.get(elegido).nombre;           // ...usamos ese nombre
            campoNombre.setText(nombreJugador);
            iniciarPartida();                                    // ...y empieza a jugar
        }                                                        // (si cancela, no pasa nada)
    }

    //  FONDO TEMÁTICO DEL JUEGO: cambia según la categoría
    //  (degradado de color + iconos de la categoría repetidos)

    /** Datos visuales de una categoría: dos colores del degradado y los iconos (códigos Unicode). */
    private static class Tema {
        Color c1, c2;      // Colores del degradado (arriba-izquierda -> abajo-derecha)
        int[] iconos;      // Iconos de la categoría (emojis)
        Tema(Color c1, Color c2, int... iconos) { this.c1 = c1; this.c2 = c2; this.iconos = iconos; }
    }

    /** QUÉ HACE: devuelve el tema (colores + iconos) de una categoría. RECIBE: el nombre. */
    private static Tema temaDe(String categoria) {
        switch (categoria) {
            case "Tecnológico":    return new Tema(new Color(20, 70, 150), new Color(70, 150, 220), 0x1F4BB, 0x1F5A5, 0x2328, 0x1F5B1, 0x1F4E1, 0x1F4BE, 0x1F4F1);
            case "Fútbol":         return new Tema(new Color(20, 100, 50), new Color(90, 175, 80), 0x26BD, 0x1F3C6, 0x1F945, 0x1F45F, 0x1F3DF);
            case "Vida":           return new Tema(new Color(170, 60, 60), new Color(58, 253, 0), 0x2764, 0x1F331, 0x1F30D, 0x2600, 0x1F338, 0x1F54A);
            case "Ing. Sistemas":  return new Tema(new Color(50, 50, 140), new Color(100, 120, 210), 0x1F4BB, 0x1F9E9, 0x2699, 0x1F512, 0x1F4BE);
            case "Ing. Redes y Telecomunicaciones":
                return new Tema(new Color(0, 100, 110), new Color(60, 175, 180), 0x1F4E1, 0x1F310, 0x1F4F6, 0x1F50C, 0x1F6F0);
            case "Filosofía":      return new Tema(new Color(90, 60, 110), new Color(170, 130, 180), 0x1F989, 0x1F4DC, 0x1F4AD, 0x1F3DB, 0x2753);
            case "Nutrición":      return new Tema(new Color(200, 100, 20), new Color(240, 180, 70), 0x1F34E, 0x1F951, 0x1F955, 0x1F347, 0x1F957);
            case "Fisioterapia":   return new Tema(new Color(0, 120, 150), new Color(90, 195, 210), 0x1F3C3, 0x1F4AA, 0x1F9B4, 0x2764, 0x1F9D8);
            case "Marketing":      return new Tema(new Color(160, 40, 120), new Color(235, 110, 170), 0x1F4E2, 0x1F4C8, 0x1F3AF, 0x1F4A1, 0x1F6D2);
            case "Contabilidad":   return new Tema(new Color(30, 90, 60), new Color(100, 160, 110), 0x1F9EE, 0x1F4B0, 0x1F4CA, 0x1F9FE, 0x1F4B5);
            case "Administración": return new Tema(new Color(60, 80, 100), new Color(130, 155, 180), 0x1F4CB, 0x1F4C5, 0x1F4BC, 0x1F454, 0x1F4C1);
            case "Finanzas":       return new Tema(new Color(20, 90, 80), new Color(190, 160, 50), 0x1F4B2, 0x1F4C8, 0x1F3E6, 0x1F4B3, 0x1F4B0);
            case "Psicopedagogía": return new Tema(new Color(110, 60, 160), new Color(180, 130, 220), 0x1F9E0, 0x1F4DA, 0x270F, 0x1F9E9, 0x1F393);
            case "Derecho":        return new Tema(new Color(110, 40, 40), new Color(190, 120, 90), 0x2696, 0x1F528, 0x1F4DC, 0x1F3DB, 0x1F4D5);
            case "Medicina":       return new Tema(new Color(170, 30, 50), new Color(235, 110, 120), 0x2695, 0x1F48A, 0x1F489, 0x1F9EC, 0x2764);
            case "Enfermería":     return new Tema(new Color(20, 130, 130), new Color(240, 140, 160), 0x1F489, 0x1F321, 0x2695, 0x1F3E5, 0x2764);
            case "Arquitectura":   return new Tema(new Color(90, 90, 100), new Color(215, 150, 80), 0x1F3DB, 0x1F3E0, 0x1F4D0, 0x1F3D7, 0x1F4CF);
            default:               return new Tema(new Color(40, 90, 150), new Color(110, 170, 220), 0x2B50, 0x1F4A1, 0x1F4DA);
        }
    }

    /** QUÉ HACE: elige una fuente del sistema que sepa dibujar emojis (o null si ninguna puede). */
    private static Font fuenteEmoji(int tam, int codigo) {
        String texto = new String(Character.toChars(codigo));
        String[] nombres = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI Symbol", "Dialog"};
        for (String n : nombres) {
            Font f = new Font(n, Font.PLAIN, tam);
            if (f.canDisplayUpTo(texto) == -1) return f;
        }
        return null;
    }

    /** QUÉ HACE: dibuja el patrón de iconos de la categoría repartidos por toda el área. */
    private static void dibujarPatronIconos(Graphics2D g2, Tema t, int w, int h, int alfa) {
        int paso = 92, k = 0;
        for (int fila = 0, y = 30; y < h + paso; y += paso, fila++) {
            for (int x = (fila % 2 == 0 ? 25 : 25 + paso / 2); x < w + paso; x += paso, k++) {
                int codigo = t.iconos[(k * 3 + fila) % t.iconos.length];
                Font f = fuenteEmoji(36 + (k % 3) * 6, codigo);
                if (f == null) continue;
                AffineTransform viejo = g2.getTransform();
                g2.translate(x, y);
                g2.rotate(Math.toRadians(((k * 37) % 50) - 25));   // Giro distinto para cada icono
                g2.setFont(f);
                g2.setColor(new Color(255, 255, 255, alfa));
                g2.drawString(new String(Character.toChars(codigo)), -18, 14);
                g2.setTransform(viejo);
            }
        }
    }

    /** QUÉ HACE: dibuja una verdura/fideos sencillos con formas. tipo 0..5, (x,y) centro, s tamaño. */
    private static void dibujarVerdura(Graphics2D g2, int tipo, double x, double y, double s, double rot) {
        AffineTransform viejo = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(rot);
        switch (tipo) {
            case 0: { // Zanahoria
                GeneralPath c = new GeneralPath();
                c.moveTo(-s * 0.2, -s * 0.3); c.lineTo(s * 0.2, -s * 0.3); c.lineTo(0, s * 0.55); c.closePath();
                g2.setColor(new Color(240, 130, 30)); g2.fill(c);
                g2.setColor(new Color(60, 160, 60));
                g2.fillOval((int) (-s * 0.22), (int) (-s * 0.55), (int) (s * 0.16), (int) (s * 0.28));
                g2.fillOval((int) (-s * 0.07), (int) (-s * 0.62), (int) (s * 0.16), (int) (s * 0.32));
                g2.fillOval((int) (s * 0.07), (int) (-s * 0.55), (int) (s * 0.16), (int) (s * 0.28));
                break;
            }
            case 1: { // Tomate
                g2.setColor(new Color(220, 60, 50));
                g2.fillOval((int) (-s * 0.32), (int) (-s * 0.28), (int) (s * 0.64), (int) (s * 0.58));
                g2.setColor(new Color(50, 150, 60));
                g2.fillOval((int) (-s * 0.2), (int) (-s * 0.36), (int) (s * 0.4), (int) (s * 0.16));
                g2.fillRect((int) (-s * 0.03), (int) (-s * 0.5), (int) (s * 0.06), (int) (s * 0.18));
                break;
            }
            case 2: { // Fideos
                g2.setColor(new Color(250, 225, 130));
                g2.setStroke(new BasicStroke((float) (s * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = -1; i <= 1; i++) {
                    GeneralPath f = new GeneralPath();
                    f.moveTo(-s * 0.4, i * s * 0.18);
                    f.curveTo(-s * 0.2, i * s * 0.18 - s * 0.25, 0, i * s * 0.18 + s * 0.25, s * 0.2, i * s * 0.18);
                    f.curveTo(s * 0.3, i * s * 0.18 - s * 0.12, s * 0.35, i * s * 0.18 + s * 0.1, s * 0.42, i * s * 0.18);
                    g2.draw(f);
                }
                break;
            }
            case 3: { // Vaina de arvejas
                g2.setColor(new Color(90, 180, 70));
                g2.fillRoundRect((int) (-s * 0.45), (int) (-s * 0.15), (int) (s * 0.9), (int) (s * 0.3), (int) (s * 0.3), (int) (s * 0.3));
                g2.setColor(new Color(150, 220, 100));
                for (int i = -1; i <= 1; i++) g2.fillOval((int) (i * s * 0.26 - s * 0.1), (int) (-s * 0.1), (int) (s * 0.2), (int) (s * 0.2));
                break;
            }
            case 4: { // Brócoli
                g2.setColor(new Color(170, 210, 120));
                g2.fillRect((int) (-s * 0.08), 0, (int) (s * 0.16), (int) (s * 0.4));
                g2.setColor(new Color(50, 140, 60));
                g2.fillOval((int) (-s * 0.34), (int) (-s * 0.2), (int) (s * 0.36), (int) (s * 0.36));
                g2.fillOval((int) (-s * 0.02), (int) (-s * 0.2), (int) (s * 0.36), (int) (s * 0.36));
                g2.fillOval((int) (-s * 0.18), (int) (-s * 0.4), (int) (s * 0.36), (int) (s * 0.36));
                break;
            }
            default: { // Hongo
                g2.setColor(new Color(245, 235, 215));
                g2.fillRoundRect((int) (-s * 0.1), 0, (int) (s * 0.2), (int) (s * 0.38), 6, 6);
                g2.setColor(new Color(200, 90, 60));
                g2.fillArc((int) (-s * 0.38), (int) (-s * 0.35), (int) (s * 0.76), (int) (s * 0.7), 0, 180);
                break;
            }
        }
        g2.setTransform(viejo);
    }

    /** QUÉ HACE: pinta el mantel de la mesa (color de la categoría suavizado) con verduras y fideos. */
    private static void dibujarMantelComida(Graphics2D g2, Tema t, int w, int h) {
        Color a = mezclar(t.c1, Color.WHITE, 0.55f), b = mezclar(t.c2, Color.WHITE, 0.55f);
        g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
        g2.fillRect(0, 0, w, h);
        Composite viejo = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        int paso = 84, k = 0;
        for (int fila = 0, y = 30; y < h + paso; y += paso, fila++) {
            for (int x = (fila % 2 == 0 ? 25 : 25 + paso / 2); x < w + paso; x += paso, k++) {
                dibujarVerdura(g2, (k + fila) % 6, x, y, 46, Math.toRadians(((k * 53) % 70) - 35));
            }
        }
        g2.setComposite(viejo);
    }

    /** QUÉ HACE: mezcla dos colores. RECIBE: los colores y cuánto del segundo (0..1). */
    private static Color mezclar(Color c1, Color c2, float f) {
        return new Color((int) (c1.getRed() * (1 - f) + c2.getRed() * f),
                (int) (c1.getGreen() * (1 - f) + c2.getGreen() * f),
                (int) (c1.getBlue() * (1 - f) + c2.getBlue() * f));
    }

    /** Panel de fondo de la pantalla de juego: degradado + iconos de la categoría. */
    private static class FondoTema extends JPanel {
        private final Tema tema;
        FondoTema(String categoria) {
            super(new BorderLayout(10, 10));
            this.tema = temaDe(categoria);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, tema.c1, getWidth(), getHeight(), tema.c2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            dibujarPatronIconos(g2, tema, getWidth(), getHeight(), 70);
            g2.dispose();
        }
    }
    //  FONDO DEL MENÚ: degradado verde + verduras dibujadas con código
    //  (no usa imágenes: cada verdura se dibuja con óvalos y formas)
    private class FondoMenu extends JPanel {

        /** QUÉ HACE: Swing la llama sola cuando hay que dibujar el menú. RECIBE: el "pincel" g. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(30, 130, 50), w, h, new Color(150, 225, 120))); // Fondo verde degradado
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 255, 255, 130));          // Marco blanco fino
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(14, 14, w - 28, h - 28, 40, 40);

            int k = 0;                                           // Contador para variar el tipo de verdura
            for (int x = 55; x < w - 40; x += 100) {             // Fila de arriba y de abajo
                dibujarVerdura(g2, k++, x, 52);
                dibujarVerdura(g2, k++, x + 30, h - 52);
            }
            for (int y = 150; y < h - 110; y += 100) {           // Columna izquierda y derecha
                dibujarVerdura(g2, k++, 52, y);
                dibujarVerdura(g2, k++, w - 52, y);
            }

            g2.setColor(new Color(255, 255, 255, 225));          // Tarjeta blanca donde va el contenido
            g2.fillRoundRect(150, 100, w - 300, h - 200, 50, 50);
            g2.setColor(new Color(20, 100, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(150, 100, w - 300, h - 200, 50, 50);
            g2.dispose();
        }

        /**
         * QUÉ HACE: dibuja UNA verdura en una posición, ligeramente girada.
         * RECIBE: pincel, número (decide cuál verdura: 0 tomate, 1 zanahoria, 2 brócoli, 3 berenjena, 4 pimiento),
         *         y la posición central (cx, cy).   DEVUELVE: nada.
         */
        private void dibujarVerdura(Graphics2D g2, int numero, int cx, int cy) {
            AffineTransform original = g2.getTransform();        // Guardamos la posición normal del pincel
            g2.translate(cx, cy);                                // Movemos el "origen" al centro de la verdura
            g2.rotate(Math.toRadians((numero * 37) % 50 - 25));  // La giramos un poco (distinto ángulo cada vez)
            switch (numero % 5) {                                // Según el número, dibujamos una verdura distinta
                case 0:                                          // TOMATE
                    g2.setColor(new Color(220, 40, 40));
                    g2.fillOval(-30, -26, 60, 56);
                    g2.setColor(new Color(255, 255, 255, 110));  // Brillo
                    g2.fillOval(-18, -16, 16, 10);
                    g2.setColor(new Color(40, 150, 50));         // Hojitas verdes arriba
                    for (int k = 0; k < 5; k++) {
                        AffineTransform o = g2.getTransform();
                        g2.rotate(k * 2 * Math.PI / 5, 0, -24);
                        g2.fillOval(-4, -36, 8, 16);
                        g2.setTransform(o);
                    }
                    break;
                case 1:                                          // ZANAHORIA
                    g2.setColor(new Color(240, 130, 30));
                    GeneralPath z = new GeneralPath();           // Triángulo apuntando hacia abajo
                    z.moveTo(-14, -30);
                    z.lineTo(14, -30);
                    z.lineTo(0, 40);
                    z.closePath();
                    g2.fill(z);
                    g2.setColor(new Color(200, 100, 20));        // Rayitas de la zanahoria
                    g2.drawLine(-8, -15, 8, -15);
                    g2.drawLine(-6, 0, 6, 0);
                    g2.drawLine(-3, 15, 3, 15);
                    g2.setColor(new Color(40, 160, 60));         // Hojas
                    g2.fillOval(-14, -54, 10, 26);
                    g2.fillOval(-5, -60, 10, 30);
                    g2.fillOval(4, -54, 10, 26);
                    break;
                case 2:                                          // BRÓCOLI
                    g2.setColor(new Color(150, 200, 100));       // Tallo
                    g2.fillRoundRect(-8, 5, 16, 30, 8, 8);
                    g2.setColor(new Color(30, 120, 50));         // Copa (varios círculos)
                    g2.fillOval(-30, -22, 28, 28);
                    g2.fillOval(2, -22, 28, 28);
                    g2.fillOval(-16, -34, 32, 32);
                    g2.fillOval(-22, -8, 44, 24);
                    g2.setColor(new Color(90, 180, 90, 170));   // Brillo
                    g2.fillOval(-10, -26, 10, 10);
                    break;
                case 3:                                          // BERENJENA
                    g2.setColor(new Color(100, 40, 130));
                    g2.fillOval(-18, -20, 36, 62);
                    g2.setColor(new Color(255, 255, 255, 90));   // Brillo
                    g2.fillOval(-10, -8, 7, 22);
                    g2.setColor(new Color(40, 150, 60));         // Gorrito verde
                    g2.fillOval(-16, -26, 32, 16);
                    g2.fillRect(-3, -38, 6, 14);
                    break;
                default:                                         // PIMIENTO VERDE
                    g2.setColor(new Color(60, 170, 60));
                    g2.fillOval(-28, -22, 30, 50);
                    g2.fillOval(-2, -22, 30, 50);
                    g2.fillOval(-16, -20, 32, 52);
                    g2.setColor(new Color(255, 255, 255, 90));   // Brillo
                    g2.fillOval(-18, -8, 6, 20);
                    g2.setColor(new Color(30, 110, 40));         // Palito
                    g2.fillRect(-3, -34, 6, 14);
                    break;
            }
            g2.setTransform(original);                           // Devolvemos el pincel a su posición normal
        }
    }

    /**
     * QUÉ HACE: crea el botón que enciende/apaga la música.
     * RECIBE: nada.   DEVUELVE: el botón (JButton) ya configurado.
     */
    private JButton crearBotonMusica() {
        // Operador "? :" = if resumido: si está encendida pone un texto, si no, el otro
        JButton btn = new JButton(Sonido.estaEncendida() ? "Música: ENCENDIDA" : "Música: APAGADA");
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        btn.addActionListener(e -> {                             // Al hacer clic...
            Sonido.alternarMusica();                             // ...cambia el estado de la música
            btn.setText(Sonido.estaEncendida() ? "Música: ENCENDIDA" : "Música: APAGADA"); // ...y el texto
        });
        return btn;
    }

    // ============================================================
    //  7) RONDAS Y FLUJO DE LA PARTIDA
    // ============================================================

    /**
     * QUÉ HACE: crea los 3 niveles base (el reto extra se agrega después, cuando el jugador lo elige).
     * RECIBE: nada.   DEVUELVE: la lista de rondas (ArrayList de Ronda).
     */
    private ArrayList<Ronda> crearRondas() {
        ArrayList<Ronda> lista = new ArrayList<>();
        lista.add(new Ronda("Tecnológico", "Nivel 1", 10, 6, 180, BANCO_TECNOLOGICO)); // 10x10, 6 palabras, 3 min
        lista.add(new Ronda("Fútbol", "Nivel 2", 12, 8, 300, BANCO_FUTBOL));           // 12x12, 8 palabras, 5 min
        lista.add(new Ronda("Vida", "Nivel 3", 15, 10, 300, BANCO_OTROS));              // 15x15, 10 palabras, 5 min
        return lista;
    }

    /** QUÉ HACE: empieza una partida nueva desde el nivel 1. No recibe ni devuelve nada. */
    private void iniciarPartida() {
        rondas = crearRondas();          // Los 3 niveles
        rondaActual = 0;                 // Empezamos en el primero
        tiempoTotalPartida = 0;          // Tiempo acumulado en cero
        iniciarRonda();
    }

    /** QUÉ HACE: prepara y arranca la ronda actual (tablero, pantalla y letrero de categoría). */
    private void iniciarRonda() {
        Ronda r = rondas.get(rondaActual);       // Tomamos los datos de la ronda actual
        tamano = r.tamano;
        tiempoRestante = r.segundos;
        juegoActivo = false;                     // Todavía no se puede jugar (sale el letrero)
        tableroVisible = false;                  // Y las letras se esconden hasta que termine el letrero

        palabrasDelJuego = elegirPalabras(r);    // Elegimos las palabras de esta ronda
        encontradas = new ArrayList<>();         // Aún no ha encontrado ninguna
        armarMatrizLogica();                     // Armamos el tablero

        modeloLista = new DefaultListModel<>();  // Llenamos la lista lateral
        for (String p : palabrasDelJuego) modeloLista.addElement(p);

        construirPantallaJuego(r);               // Dibujamos la pantalla
        setContentPane(panelJuego);
        revalidate();                            // Reacomoda los componentes
        repaint();                               // Redibuja

        // Letrero con confeti; cuando termina, se ejecuta comenzarCuentaRegresiva()
        overlay.mostrar("CATEGORÍA: " + r.categoria.toUpperCase(),
                new String[]{r.etiqueta,
                        "Encuentra " + palabrasDelJuego.size() + " palabras en " + formatear(r.segundos),
                        "Pueden estar en diagonal y al revés"},
                new Color(30, 70, 140), 2400, true,
                () -> tablero.iniciarAnimacion(() -> comenzarCuentaRegresiva())); // Letrero -> cae la sopa -> corre el tiempo
    }

    /** QUÉ HACE: se llama cuando el letrero desaparece; ahora sí corre el tiempo. */
    private void comenzarCuentaRegresiva() {
        tableroVisible = true;                   // Mostramos las letras
        juegoActivo = true;                      // Se permite arrastrar el mouse
        tablero.repaint();
        actualizarReloj();                       // Muestra el tiempo completo en el reloj
        if (relojJuego != null) relojJuego.stop();   // Si había un cronómetro viejo, lo paramos
        relojJuego = new Timer(1000, e -> tick());   // Cada 1000 ms (1 segundo) ejecuta tick()
        relojJuego.start();
    }

    /** QUÉ HACE: se ejecuta CADA SEGUNDO: baja el tiempo, actualiza el reloj y suena el clic. */
    private void tick() {
        tiempoRestante--;                                    // Baja un segundo
        actualizarReloj();                                   // Refresca el reloj de la pantalla
        if (tiempoRestante <= 30 && tiempoRestante > 0) {    // Si faltan 30 segundos o menos...
            Sonido.clic();                                   // ...suena el "clic" del reloj
        }
        if (tiempoRestante <= 0) {                           // Si el tiempo llegó a cero...
            tiempoAgotado();                                 // ...se acabó la partida
        }
    }

    /**
     * QUÉ HACE: dibuja el tiempo en el reloj (abajo a la derecha). Normal = azul; últimos 30 s = rojo parpadeante.
     * RECIBE/DEVUELVE: nada (usa la variable tiempoRestante).
     */
    private void actualizarReloj() {
        lblReloj.setText("<html><center>TIEMPO<br><span style='font-size:30px'>"
                + formatear(Math.max(0, tiempoRestante)) + "</span></center></html>");
        if (tiempoRestante <= 30) {                          // Zona de peligro: últimos 30 segundos
            lblReloj.setForeground(new Color(200, 0, 0));    // Letras rojas
            lblReloj.setBorder(BorderFactory.createLineBorder(new Color(200, 0, 0), 4));
            // Parpadeo: si el segundo es par, fondo blanco; si es impar, fondo rosado
            lblReloj.setBackground(tiempoRestante % 2 == 0 ? Color.WHITE : new Color(255, 200, 200));
        } else {                                             // Todavía hay tiempo de sobra
            lblReloj.setForeground(new Color(20, 60, 140));  // Letras azules
            lblReloj.setBorder(BorderFactory.createLineBorder(new Color(20, 60, 140), 3));
            lblReloj.setBackground(new Color(230, 240, 255));
        }
    }

    /** QUÉ HACE: el jugador encontró TODAS las palabras: suma tiempo, celebra y pasa al siguiente paso. */
    private void rondaCompletada() {
        relojJuego.stop();                                   // Paramos el cronómetro
        juegoActivo = false;
        Ronda r = rondas.get(rondaActual);
        int usado = r.segundos - tiempoRestante;             // Cuánto tardó en esta ronda
        tiempoTotalPartida += usado;                         // Se suma al total
        rondaActual++;                                       // Pasamos a la siguiente ronda
        boolean hayMas = rondaActual < rondas.size();        // ¿Hay otra ronda ya preparada en la lista?

        StringBuilder frase = new StringBuilder();           // Armamos la frase con las palabras halladas
        for (PalabraEncontrada pe : encontradas) frase.append(pe.palabra).append("  ");

        ArrayList<String> lineas = new ArrayList<>();
        lineas.add("Tiempo en que lo lograste: " + formatear(usado));
        lineas.add("Frase formada con tus palabras:");
        lineas.addAll(partirEnLineas(frase.toString().trim(), 55));

        Sonido.fanfarria();                                  // Sonido de victoria
        overlay.mostrar("¡CATEGORÍA " + r.categoria.toUpperCase() + " COMPLETADA!",
                lineas.toArray(new String[0]), new Color(30, 120, 60), 5000, true, () -> {
                    if (hayMas) {                            // Si quedan rondas en la lista...
                        iniciarRonda();                      // ...pasa DIRECTO a la siguiente
                    } else if (rondaActual == NIVELES_BASE) {// Si NO quedan, pero acaba de terminar los 3 niveles...
                        ofrecerRetoExtra();                  // ...le preguntamos si quiere el reto extra
                    } else {                                 // Si terminó el reto extra (nivel 4)...
                        finalizarPartida();                  // ...se acaba el juego
                    }
                });
    }

    /**
     * QUÉ HACE: pregunta "¿quieres aceptar un nuevo reto?". Si dice que sí, deja ELEGIR la carrera
     *           y agrega el nivel 4 (3 minutos). Si dice que no, termina la partida.
     */
    private void ofrecerRetoExtra() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¡Completaste los 3 niveles!\n\n¿Quieres aceptar un NUEVO RETO?\n"
                        + "Es una sopa de letras de la carrera que tú elijas, por 3 minutos.",
                "Nuevo reto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {           // Si hizo clic en "Sí"...
            int carrera = elegirCarrera();                   // ...le mostramos las categorías para elegir
            if (carrera >= 0) {                              // Si realmente eligió una (no cerró la ventana)...
                rondas.add(new Ronda(NOMBRES_CARRERAS[carrera], "Nivel 4 - Reto extra", 15, 8, 180,
                        BANCOS_CARRERAS[carrera]));          // ...agregamos el nivel 4 con las palabras de esa carrera
                iniciarRonda();                              // ...y lo empezamos
            } else {                                         // Si cerró la ventana sin elegir...
                finalizarPartida();                          // ...terminamos la partida
            }
        } else {                                             // Si respondió "No"...
            finalizarPartida();                              // ...terminamos la partida
        }
    }

    /**
     * QUÉ HACE: muestra una ventana con un botón por cada carrera para que el jugador elija su categoría.
     * RECIBE: nada.
     * DEVUELVE: la posición (0, 1, 2...) de la carrera elegida, o -1 si cerró la ventana sin elegir.
     */
    private int elegirCarrera() {
        final int[] elegida = {-1};                          // Caja para guardar la elección (-1 = ninguna)
        JDialog dialogo = new JDialog(this, "Elige la categoría de tu carrera", true); // true = bloquea el resto
        dialogo.setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("¿De qué carrera quieres tu sopa de letras?", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 10, 0, 10));
        dialogo.add(titulo, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));   // 2 columnas, filas automáticas
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        for (int i = 0; i < NOMBRES_CARRERAS.length; i++) {      // Un botón por cada carrera
            final int indice = i;                                // Copia fija de i (Java lo exige dentro del botón)
            JButton b = new JButton(NOMBRES_CARRERAS[i]);
            b.setFont(new Font("Arial", Font.PLAIN, 15));
            b.addActionListener(e -> {                           // Al hacer clic en la carrera...
                elegida[0] = indice;                             // ...guardamos cuál fue
                dialogo.dispose();                               // ...y cerramos la ventana
            });
            panel.add(b);
        }
        dialogo.add(panel, BorderLayout.CENTER);
        dialogo.pack();                                          // Ajusta el tamaño al contenido
        dialogo.setLocationRelativeTo(this);                     // La centra sobre la ventana principal
        dialogo.setVisible(true);                                // La muestra y ESPERA aquí hasta que se cierre
        return elegida[0];                                       // Devuelve la elección
    }

    /** QUÉ HACE: se acabó el tiempo: guarda lo logrado y muestra el ranking. */
    private void tiempoAgotado() {
        relojJuego.stop();
        juegoActivo = false;
        Sonido.sonidoFin();
        registrarResultado();                                    // Guarda lo que alcanzó en el ranking
        overlay.mostrar("¡SE ACABÓ EL TIEMPO!",
                lineasFinales(),
                new Color(150, 30, 30), 3500, false, () -> {
                    mostrarRanking();
                    volverAlMenu();
                });
    }

    /** QUÉ HACE: cierra la partida con un letrero de felicitación, muestra el ranking y vuelve al menú. */
    private void finalizarPartida() {
        registrarResultado();
        overlay.mostrar("¡FELICIDADES, " + nombreJugador.toUpperCase() + "!",
                lineasFinales(),
                new Color(160, 110, 0), 4500, true, () -> {
                    mostrarRanking();
                    volverAlMenu();
                });
    }

    /** QUÉ HACE: regresa a la pantalla del menú y detiene el cronómetro. */
    private void volverAlMenu() {
        if (relojJuego != null) relojJuego.stop();               // Si hay cronómetro, lo paramos
        if (tablero != null) tablero.detenerAnimacion();         // Si la sopa estaba cayendo, la detenemos
        juegoActivo = false;
        setContentPane(panelMenu);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> campoNombre.requestFocusInWindow()); // Cursor listo para escribir el nombre
    }

    /**
     * QUÉ HACE: elige las palabras de la ronda al azar, sin repetir y sin que una esté dentro de otra.
     * RECIBE: la ronda (r) con su banco de palabras.
     * DEVUELVE: lista de palabras elegidas en MAYÚSCULAS.
     */
    private ArrayList<String> elegirPalabras(Ronda r) {
        ArrayList<String> mezcladas = new ArrayList<>();
        Collections.addAll(mezcladas, r.banco);                  // Copiamos el banco
        Collections.shuffle(mezcladas);                          // Lo mezclamos al azar
        ArrayList<String> elegidas = new ArrayList<>();
        for (String w : mezcladas) {                             // Revisamos las palabras una por una
            if (elegidas.size() >= r.cantidad) break;            // Si ya hay suficientes, dejamos de buscar
            boolean choca = false;
            for (String c : elegidas) {                          // Comparamos con las ya elegidas
                if (w.contains(c) || c.contains(w)) choca = true;// Si una está dentro de otra, hay conflicto
            }
            if (!choca) elegidas.add(w.toUpperCase());           // Si no hay conflicto, la agregamos
        }
        return elegidas;
    }

    /**
     * QUÉ HACE: arma la tabla: coloca las palabras, rellena con letras al azar y verifica que
     *           CADA palabra aparezca UNA SOLA VEZ (así no hay palabras repetidas).
     * RECIBE/DEVUELVE: nada (deja el resultado en matrizLetras).
     */
    private void armarMatrizLogica() {
        boolean ok = false;                                      // ¿Quedó bien el tablero?
        int intentos = 0;                                        // Contador de seguridad
        do {                                                     // "do-while": hace esto al menos una vez
            matrizLetras = new char[tamano][tamano];
            for (int i = 0; i < tamano; i++)                     // Recorre filas
                for (int j = 0; j < tamano; j++)                 // Recorre columnas
                    matrizLetras[i][j] = ' ';                    // Casilla vacía

            boolean todoColocado = true;
            for (String palabra : palabrasDelJuego) {            // Intentamos colocar cada palabra
                if (!colocarPalabra(palabra)) {                  // Si una NO cupo...
                    todoColocado = false;
                    break;                                       // ...cortamos y volvemos a empezar
                }
            }

            if (todoColocado) {                                  // Si todas cupieron, rellenamos
                char[][] base = new char[tamano][tamano];        // Copia con solo las palabras puestas
                for (int i = 0; i < tamano; i++) base[i] = matrizLetras[i].clone();

                for (int k = 0; k < 50 && !ok; k++) {            // Probamos hasta 50 rellenos distintos
                    for (int i = 0; i < tamano; i++) {
                        for (int j = 0; j < tamano; j++) {
                            // Si la casilla estaba vacía pone letra al azar; si no, deja la letra de la palabra
                            matrizLetras[i][j] = (base[i][j] == ' ') ? (char) ('A' + ran.nextInt(26)) : base[i][j];
                        }
                    }
                    ok = todasSonUnicas();                       // ¿Ninguna palabra quedó repetida?
                }
            }
            intentos++;
        } while (!ok && intentos < 500);                         // Repite si falló (máximo 500 veces)
    }

    /**
     * QUÉ HACE: revisa que cada palabra del juego aparezca exactamente 1 vez en el tablero.
     * RECIBE: nada.   DEVUELVE: true si todas son únicas, false si alguna se repite o no está.
     */
    private boolean todasSonUnicas() {
        for (String palabra : palabrasDelJuego) {
            if (contarOcurrencias(palabra) != 1) return false;   // Si alguna no aparece exactamente 1 vez: falla
        }
        return true;                                             // Todas pasaron la prueba
    }

    /**
     * QUÉ HACE: cuenta cuántas veces aparece una palabra en el tablero (en las 8 direcciones).
     * RECIBE: la palabra a buscar.   DEVUELVE: cantidad de veces que aparece (un número entero).
     */
    private int contarOcurrencias(String palabra) {
        int total = 0;
        for (int i = 0; i < tamano; i++) {                       // Cada casilla como posible inicio (fila)
            for (int j = 0; j < tamano; j++) {                   // (columna)
                for (int[] d : DIRECCIONES) {                    // Cada una de las 8 direcciones
                    boolean coincide = true;
                    for (int k = 0; k < palabra.length(); k++) { // Comparamos letra por letra
                        int ni = i + d[0] * k;                   // Fila de la k-ésima letra
                        int nj = j + d[1] * k;                   // Columna de la k-ésima letra
                        // Si se sale del tablero o la letra no coincide, esta posición no sirve
                        if (ni < 0 || ni >= tamano || nj < 0 || nj >= tamano || matrizLetras[ni][nj] != palabra.charAt(k)) {
                            coincide = false;
                            break;
                        }
                    }
                    if (coincide) total++;                       // Si todas las letras coincidieron: +1 aparición
                }
            }
        }
        return total;
    }

    /**
     * QUÉ HACE: intenta poner UNA palabra en el tablero, en una dirección y lugar al azar.
     * RECIBE: la palabra.   DEVUELVE: true si logró colocarla, false si no encontró lugar.
     */
    private boolean colocarPalabra(String palabra) {
        int largo = palabra.length();
        for (int intento = 0; intento < 300; intento++) {        // Probamos hasta 300 lugares al azar
            int[] dir = DIRECCIONES[ran.nextInt(DIRECCIONES.length)]; // Dirección al azar
            int fila = ran.nextInt(tamano);                      // Inicio al azar
            int col = ran.nextInt(tamano);
            int filaFinal = fila + dir[0] * (largo - 1);         // Dónde caería la última letra
            int colFinal = col + dir[1] * (largo - 1);
            if (filaFinal < 0 || filaFinal >= tamano || colFinal < 0 || colFinal >= tamano) {
                continue;                                        // Se sale del tablero: salta a probar otro lugar
            }

            boolean puede = true;
            for (int i = 0; i < largo; i++) {                    // Revisamos casilla por casilla
                char actual = matrizLetras[fila + dir[0] * i][col + dir[1] * i];
                if (actual != ' ' && actual != palabra.charAt(i)) { // Si hay otra letra distinta, choca
                    puede = false;
                    break;
                }
            }
            if (puede) {                                         // Si cabe, la escribimos
                for (int i = 0; i < largo; i++) {
                    matrizLetras[fila + dir[0] * i][col + dir[1] * i] = palabra.charAt(i);
                }
                return true;                                     // Listo: colocada
            }
        }
        return false;                                            // No se pudo en 300 intentos
    }

    // ============================================================
    //  9) PANTALLA DEL JUEGO
    // ============================================================

    /**
     * QUÉ HACE: construye la pantalla de juego (título, tablero, lista, palabras halladas y RELOJ).
     * RECIBE: la ronda actual (para mostrar categoría y nivel).   DEVUELVE: nada.
     */
    private void construirPantallaJuego(Ronda r) {
        panelJuego = new FondoTema(r.categoria);   // Fondo según la categoría
        panelJuego.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ----- ARRIBA: nivel, categoría y jugador -----
        JLabel lblInfo = new JLabel(r.etiqueta + "  |  Categoría: " + r.categoria + "  |  Jugador: " + nombreJugador,
                SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.BOLD, 17));
        lblInfo.setForeground(Color.WHITE);
        panelJuego.add(lblInfo, BorderLayout.NORTH);

        // ----- CENTRO: tablero -----
        tablero = new TableroPanel();
        tablero.tema = temaDe(r.categoria);                      // Mantel con verduras del color de la categoría
        panelJuego.add(tablero, BorderLayout.CENTER);

        // ----- DERECHA: lista de palabras + botones -----
        JPanel panelDerecho = new JPanel(new BorderLayout(5, 5));
        panelDerecho.setPreferredSize(new Dimension(220, 0));
        panelDerecho.setOpaque(false);

        JLabel lblLista = new JLabel("Palabras a buscar:");
        lblLista.setFont(new Font("Arial", Font.BOLD, 14));
        lblLista.setForeground(Color.WHITE);
        panelDerecho.add(lblLista, BorderLayout.NORTH);

        listaPalabrasUI = new JList<>(modeloLista);
        listaPalabrasUI.setFont(new Font("Arial", Font.BOLD, 14));
        listaPalabrasUI.setFixedCellHeight(26);
        listaPalabrasUI.setCellRenderer(new DefaultListCellRenderer() {   // Define cómo se dibuja cada renglón
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel etiqueta = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
                String palabra = (String) value;                 // Palabra de este renglón
                etiqueta.setOpaque(true);
                etiqueta.setBackground(Color.WHITE);             // Fondo blanco por defecto
                for (PalabraEncontrada pe : encontradas) {       // ¿Esta palabra ya fue encontrada?
                    if (pe.palabra.equals(palabra)) {            // Sí: se pinta de SU color y con un visto
                        etiqueta.setBackground(pe.color);
                        etiqueta.setText("✔ " + palabra);
                    }
                }
                return etiqueta;
            }
        });
        panelDerecho.add(new JScrollPane(listaPalabrasUI), BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(2, 1, 5, 5));
        botones.setOpaque(false);
        botones.add(crearBotonMusica());
        JButton btnVolver = new JButton("Abandonar (Menú)");
        btnVolver.addActionListener(e -> {
            if (relojJuego != null) relojJuego.stop();           // Paramos el cronómetro
            registrarResultado();                                // Guardamos lo que alcanzó
            volverAlMenu();
        });
        botones.add(btnVolver);
        panelDerecho.add(botones, BorderLayout.SOUTH);
        panelJuego.add(panelDerecho, BorderLayout.EAST);

        // ----- ABAJO: palabras encontradas (izquierda) + RELOJ (derecha) -----
        JPanel panelAbajo = new JPanel(new BorderLayout(10, 5));
        panelAbajo.setOpaque(false);
        lblProgreso = new JLabel("");
        lblProgreso.setFont(new Font("Arial", Font.BOLD, 14));
        lblProgreso.setForeground(Color.WHITE);
        panelAbajo.add(lblProgreso, BorderLayout.NORTH);

        panelEncontradas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)) {   // Caja blanca translúcida
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panelEncontradas.setOpaque(false);
        panelEncontradas.setBorder(BorderFactory.createTitledBorder("Palabras encontradas"));
        panelEncontradas.setPreferredSize(new Dimension(0, 95));
        panelAbajo.add(panelEncontradas, BorderLayout.CENTER);

        lblReloj = new JLabel("", SwingConstants.CENTER);        // El reloj SIEMPRE se ve
        lblReloj.setFont(new Font("Arial", Font.BOLD, 16));
        lblReloj.setOpaque(true);
        lblReloj.setPreferredSize(new Dimension(190, 95));
        panelAbajo.add(lblReloj, BorderLayout.EAST);
        panelJuego.add(panelAbajo, BorderLayout.SOUTH);

        actualizarPanelEncontradas();
        actualizarReloj();
    }

    /** QUÉ HACE: refresca la franja de palabras halladas, el contador y la lista de la derecha. */
    private void actualizarPanelEncontradas() {
        panelEncontradas.removeAll();                            // Borra las fichas anteriores
        for (PalabraEncontrada pe : encontradas) {               // Una "ficha" de color por palabra encontrada
            JLabel ficha = new JLabel(pe.palabra);
            ficha.setOpaque(true);
            ficha.setBackground(pe.color);                       // Mismo color que en el tablero
            ficha.setFont(new Font("Arial", Font.BOLD, 14));
            ficha.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            panelEncontradas.add(ficha);
        }
        lblProgreso.setText("Encontradas: " + encontradas.size() + " / " + palabrasDelJuego.size()
                + "      (Arrastra el mouse desde la primera hasta la última letra)");
        panelEncontradas.revalidate();
        panelEncontradas.repaint();
        listaPalabrasUI.repaint();
    }

    // ============================================================
    //  10) TABLERO: dibuja letras y maneja el arrastre del mouse
    // ============================================================
    private class TableroPanel extends JPanel {

        private int selIniFila = -1, selIniCol = -1;   // Dónde empezó el arrastre (-1 = nada seleccionado)
        private int selFinFila = -1, selFinCol = -1;   // Dónde está el mouse ahora mismo
        private Tema tema = temaDe("");                // Colores del mantel (según la categoría)
        private java.awt.image.BufferedImage cacheMantel;   // Mantel ya dibujado (para no repetirlo en cada cuadro)

        // ----- Datos de la animación de la sopa -----
        private boolean animando = false;              // ¿Se está reproduciendo la animación?
        private long inicioAnim;                       // Momento (en nanosegundos) en que empezó
        private double[][] retraso;                    // Cuánto espera cada letra antes de empezar a caer
        private Timer timerAnim;                       // Cronómetro de la animación (se repite cada 16 ms)
        private final double DURACION_ANIM = 3.5;      // Duración total en segundos

        /** QUÉ HACE: configura el panel y activa la detección del mouse (presionar, arrastrar, soltar). */
        TableroPanel() {
            setBackground(Color.WHITE);
            MouseAdapter controlador = new MouseAdapter() {

                /** Se ejecuta al PRESIONAR el botón del mouse: marca la primera letra. */
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!juegoActivo) return;                    // Si no se puede jugar (letrero), ignora
                    selIniFila = filaEn(e.getY());               // Fila donde presionó
                    selIniCol = colEn(e.getX());                 // Columna donde presionó
                    selFinFila = selIniFila;                     // Al inicio, el final = el inicio
                    selFinCol = selIniCol;
                    repaint();
                }

                /** Se ejecuta mientras ARRASTRA con el botón apretado: estira la línea amarilla. */
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!juegoActivo || selIniFila == -1) return;// Si no hay selección en curso, ignora
                    int f = filaEn(e.getY());                    // Fila actual del mouse
                    int c = colEn(e.getX());                     // Columna actual del mouse
                    int df = f - selIniFila;                     // Diferencia de filas respecto al inicio
                    int dc = c - selIniCol;                      // Diferencia de columnas respecto al inicio
                    // Solo acepta líneas rectas: misma fila, misma columna o diagonal perfecta
                    if (df == 0 || dc == 0 || Math.abs(df) == Math.abs(dc)) {
                        selFinFila = f;
                        selFinCol = c;
                        repaint();
                    }
                }

                /** Se ejecuta al SOLTAR el mouse: revisa si lo arrastrado es una palabra. */
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!juegoActivo) return;
                    verificarSeleccion();
                    selIniFila = selIniCol = selFinFila = selFinCol = -1; // Borra la selección
                    repaint();
                }
            };
            addMouseListener(controlador);                       // Activa presionar/soltar
            addMouseMotionListener(controlador);                 // Activa arrastrar
        }

        // Medidas del tablero (se adaptan al tamaño de la ventana)
        /** DEVUELVE el tamaño en píxeles de cada casilla cuadrada. */
        private int lado() { return (int) (Math.min(getWidth(), getHeight()) * 0.63 / tamano); } // La sopa ocupa el 63% (cabe dentro del plato redondo)
        /** DEVUELVE el margen izquierdo para centrar el tablero. */
        private int origenX() { return (getWidth() - lado() * tamano) / 2; }
        /** DEVUELVE el margen superior para centrar el tablero. */
        private int origenY() { return (getHeight() - lado() * tamano) / 2; }
        /** RECIBE la posición Y del mouse; DEVUELVE el número de fila (limitado entre 0 y tamano-1). */
        private int filaEn(int y) { return Math.max(0, Math.min(tamano - 1, (y - origenY()) / Math.max(1, lado()))); }
        /** RECIBE la posición X del mouse; DEVUELVE el número de columna (limitado entre 0 y tamano-1). */
        private int colEn(int x) { return Math.max(0, Math.min(tamano - 1, (x - origenX()) / Math.max(1, lado()))); }

        /**
         * QUÉ HACE: lee las letras de la línea arrastrada y comprueba si forman una palabra de la lista.
         *           Si la forma: la guarda con su color, suena el efecto "TRASHH" y actualiza la pantalla.
         * RECIBE/DEVUELVE: nada.
         */
        private void verificarSeleccion() {
            if (selIniFila == -1) return;                        // Si no había selección, no hay nada que revisar
            int df = selFinFila - selIniFila;
            int dc = selFinCol - selIniCol;
            int pasos = Math.max(Math.abs(df), Math.abs(dc));    // Cantidad de casillas que avanzó la línea
            int pasoFila = Integer.signum(df);                   // Dirección en fila: -1, 0 o 1
            int pasoCol = Integer.signum(dc);                    // Dirección en columna: -1, 0 o 1

            StringBuilder sb = new StringBuilder();              // Aquí armamos el texto leído
            for (int i = 0; i <= pasos; i++) {                   // Recorremos la línea casilla por casilla
                sb.append(matrizLetras[selIniFila + pasoFila * i][selIniCol + pasoCol * i]);
            }
            String derecho = sb.toString();                      // Texto leído de inicio a fin
            String alReves = sb.reverse().toString();            // Texto leído al revés

            for (String palabra : palabrasDelJuego) {            // Comparamos con cada palabra del juego
                if (yaEncontrada(palabra)) continue;             // Si ya estaba encontrada, la saltamos
                if (palabra.equals(derecho) || palabra.equals(alReves)) { // ¿Coincide al derecho o al revés?
                    Color color = PALETA_COLORES[encontradas.size() % PALETA_COLORES.length]; // Siguiente color
                    encontradas.add(new PalabraEncontrada(palabra, selIniFila, selIniCol, selFinFila, selFinCol, color));
                    actualizarPanelEncontradas();
                    if (encontradas.size() == palabrasDelJuego.size()) { // ¿Ya encontró TODAS?
                        SwingUtilities.invokeLater(() -> rondaCompletada()); // Termina la ronda (ahí suena la victoria)
                    } else {                                     // Si todavía faltan palabras...
                        Sonido.efectoPalabraEncontrada();        // ...suena el efecto "TRASHH" del cierre
                    }
                    return;                                      // Ya se resolvió, no seguimos buscando
                }
            }
        }

        /** RECIBE una palabra; DEVUELVE true si ya está en la lista de encontradas. */
        private boolean yaEncontrada(String palabra) {
            for (PalabraEncontrada pe : encontradas) if (pe.palabra.equals(palabra)) return true;
            return false;
        }

        /** QUÉ HACE: dibuja una "píldora" de color sobre una línea de letras. RECIBE: pincel, coordenadas y color. */
        private void dibujarLinea(Graphics2D g2, int fi, int ci, int ff, int cf, Color color) {
            int l = lado();
            int x1 = origenX() + ci * l + l / 2, y1 = origenY() + fi * l + l / 2;   // Centro de la casilla inicial
            int x2 = origenX() + cf * l + l / 2, y2 = origenY() + ff * l + l / 2;   // Centro de la casilla final
            g2.setColor(color);
            g2.setStroke(new BasicStroke(l * 0.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Línea gruesa redondeada
            g2.drawLine(x1, y1, x2, y2);
        }

        // ================= ANIMACIÓN DE LA SOPA =================

        /**
         * QUÉ HACE: arranca la animación: cae un chorro de sopa, se forma el caldo, caen las letras
         *           una por una hasta formar la sopa de letras y al final aparece el plato.
         * RECIBE: alTerminar = acción a ejecutar cuando la animación acabe (aquí: empezar a correr el tiempo).
         * DEVUELVE: nada.
         */
        void iniciarAnimacion(Runnable alTerminar) {
            animando = true;                                     // Activamos la animación
            tableroVisible = false;                              // Las letras "fijas" todavía no se muestran
            retraso = new double[tamano][tamano];                // Una espera para cada casilla
            for (int i = 0; i < tamano; i++) {
                for (int j = 0; j < tamano; j++) {
                    retraso[i][j] = 0.9 + ran.nextDouble() * 1.4;// Cada letra empieza a caer en un momento distinto
                }
            }
            inicioAnim = System.nanoTime();                      // Anotamos la hora de inicio
            Sonido.efectoLiquido();                              // Suena la sopa cayendo
            if (timerAnim != null) timerAnim.stop();             // Si había una animación vieja, la paramos
            timerAnim = new Timer(16, e -> {                     // Cada 16 ms (≈60 cuadros por segundo)
                double t = (System.nanoTime() - inicioAnim) / 1e9;   // Segundos transcurridos
                if (t >= DURACION_ANIM) {                        // Si ya terminó la animación...
                    timerAnim.stop();
                    animando = false;
                    tableroVisible = true;                       // ...mostramos la sopa final con el plato
                    repaint();
                    Sonido.efectoPlato();                        // "cling" del plato
                    alTerminar.run();                            // ...y ejecutamos lo que seguía (empezar el tiempo)
                } else {                                         // Si todavía no termina...
                    repaint();                                   // ...solo redibujamos el siguiente cuadro
                }
            });
            timerAnim.start();
        }

        /** QUÉ HACE: detiene la animación (por ejemplo si el jugador abandona). RECIBE/DEVUELVE: nada. */
        void detenerAnimacion() {
            if (timerAnim != null) timerAnim.stop();
            animando = false;
        }

        /** RECIBE un número; DEVUELVE ese número limitado entre 0 y 1 (si es menor a 0 da 0, si es mayor a 1 da 1). */
        private double limitar(double v) { return Math.max(0, Math.min(1, v)); }

        /** RECIBE un avance de 0 a 1; DEVUELVE el avance "suavizado" (empieza rápido y frena al final). */
        private double suavizar(double p) { return 1 - Math.pow(1 - p, 3); }

        /**
         * QUÉ HACE: dibuja el plato: sombra, borde blanco, aro y el caldo dorado del centro.
         * RECIBE: pincel, transparencia del borde (0 = invisible, 1 = visible) y
         *         escala del caldo (0 = sin caldo, 1 = caldo completo).
         * DEVUELVE: nada.
         */
        private void dibujarPlato(Graphics2D g2, float alphaBorde, double escalaCaldo) {
            int cx = getWidth() / 2, cy = getHeight() / 2;       // Centro del panel
            int m = Math.min(getWidth(), getHeight());           // Diámetro del plato
            if (alphaBorde > 0) {                                // Si el borde ya empezó a aparecer...
                int a = (int) (255 * alphaBorde);                // ...transparencia en escala 0-255
                g2.setColor(new Color(0, 0, 0, (int) (70 * alphaBorde)));
                g2.fillOval(cx - m / 2 + 6, cy - m / 2 + 10, m, m);          // Sombra del plato
                g2.setPaint(new GradientPaint(cx - m / 2, cy - m / 2, new Color(255, 255, 255, a),
                        cx + m / 2, cy + m / 2, new Color(205, 208, 212, a))); // Borde blanco con degradado
                g2.fillOval(cx - m / 2, cy - m / 2, m, m);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(190, 195, 200, a));
                g2.drawOval(cx - m / 2, cy - m / 2, m, m);                    // Contorno
                int r2 = (int) (m * 0.47);
                g2.setColor(new Color(215, 218, 222, a));
                g2.drawOval(cx - r2, cy - r2, 2 * r2, 2 * r2);               // Aro interior del plato
            }
            int radio = (int) (m * 0.45 * escalaCaldo);          // Tamaño actual del caldo
            if (radio > 2) {                                     // Solo lo dibujamos si ya tiene tamaño visible
                g2.setPaint(new RadialGradientPaint(cx, cy, radio, new float[]{0f, 1f},
                        new Color[]{new Color(255, 243, 200), new Color(245, 205, 120)})); // Claro al centro, dorado en la orilla
                g2.fillOval(cx - radio, cy - radio, 2 * radio, 2 * radio);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(220, 170, 80));
                g2.drawOval(cx - radio, cy - radio, 2 * radio, 2 * radio);
            }
        }

        /**
         * QUÉ HACE: dibuja el chorro de sopa que cae desde arriba hasta el centro, con gotitas.
         * RECIBE: pincel, segundos transcurridos (t) y el centro del plato (cx, cy).   DEVUELVE: nada.
         */
        private void dibujarChorro(Graphics2D g2, double t, int cx, int cy) {
            double cabeza = cy * Math.min(1, t / 0.6);           // Punta de abajo del chorro (va bajando)
            double cola = 0;                                     // Parte de arriba del chorro
            if (t > 1.1) cola = cy * Math.min(1, (t - 1.1) / 0.5); // Después de 1.1 s el chorro se "corta" y sube la cola
            if (cola >= cy) return;                              // Si ya se cortó del todo, no hay nada que dibujar

            GeneralPath chorro = new GeneralPath();              // Línea que sigue el chorro
            boolean primero = true;
            for (double y = cola; y <= cabeza; y += 6) {         // Un punto cada 6 píxeles
                double x = cx + Math.sin(y * 0.06 + t * 12) * 4; // Ondula de lado a lado (efecto líquido)
                if (primero) {                                   // El primer punto solo posiciona el pincel
                    chorro.moveTo(x, y);
                    primero = false;
                } else {                                         // Los demás puntos se unen con una línea
                    chorro.lineTo(x, y);
                }
            }
            g2.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(240, 175, 60));                // Chorro dorado
            g2.draw(chorro);
            g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 225, 140));               // Brillo claro en el medio
            g2.draw(chorro);

            if (t < 1.5) {                                       // Gotitas que saltan mientras cae el chorro
                g2.setColor(new Color(240, 175, 60));
                for (int k = 0; k < 8; k++) {
                    double gy = (t * 320 + k * 61) % Math.max(1, cy);   // Altura de la gota (se repite)
                    int gx = (int) (cx + Math.sin(k * 2.3) * 32);       // Posición lateral de la gota
                    g2.fillOval(gx, (int) gy, 7, 9);
                }
            }
        }

        /**
         * QUÉ HACE: dibuja UN cuadro de la animación: caldo que se expande, chorro, letras cayendo y plato que aparece.
         * RECIBE: el pincel.   DEVUELVE: nada.
         */
        private void dibujarAnimacion(Graphics2D g2) {
            double t = (System.nanoTime() - inicioAnim) / 1e9;   // Segundos desde que empezó
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            double escala = suavizar(limitar((t - 0.5) / 1.2));  // El caldo crece entre 0.5 s y 1.7 s
            float alphaBorde = (float) limitar((t - 2.9) / 0.5); // El borde del plato aparece entre 2.9 s y 3.4 s
            dibujarPlato(g2, alphaBorde, escala);
            if (t < 1.7) dibujarChorro(g2, t, cx, cy);           // El chorro solo existe al principio

            int l = lado(), ox = origenX(), oy = origenY();
            g2.setFont(new Font("Arial", Font.BOLD, Math.max(10, (int) (l * 0.5))));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.BLACK);
            for (int i = 0; i < tamano; i++) {
                for (int j = 0; j < tamano; j++) {
                    double p = (t - retraso[i][j]) / 0.6;        // Avance de la caída de esta letra (0 a 1)
                    if (p < 0) continue;                         // Todavía no le toca caer: no se dibuja
                    if (p > 1) p = 1;                            // Ya llegó: se queda en su casilla
                    double yFin = oy + i * l;                    // Altura final (su casilla)
                    double yIni = -2.0 * l;                      // Altura inicial (arriba, fuera de pantalla)
                    double y;
                    if (p < 0.75) {                              // Primera parte: cae acelerando
                        double q = p / 0.75;
                        y = yIni + (yFin - yIni) * q * q;
                    } else {                                     // Segunda parte: pequeño rebote al caer en la sopa
                        double q = (p - 0.75) / 0.25;
                        y = yFin - Math.sin(q * Math.PI) * l * 0.25;
                    }
                    String letra = String.valueOf(matrizLetras[i][j]);
                    int tx = ox + j * l + (l - fm.stringWidth(letra)) / 2;
                    int ty = (int) y + (l + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(letra, tx, ty);
                }
            }
        }

        /** QUÉ HACE: Swing la llama solo cada vez que hay que dibujar el tablero. RECIBE: el "pincel" g. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);                             // Limpia el fondo
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (cacheMantel == null || cacheMantel.getWidth() != getWidth() || cacheMantel.getHeight() != getHeight()) {
                cacheMantel = new java.awt.image.BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()),
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D gm = cacheMantel.createGraphics();
                gm.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                dibujarMantelComida(gm, tema, getWidth(), getHeight());   // Mantel con verduras y fideos
                gm.dispose();
            }
            g2.drawImage(cacheMantel, 0, 0, null);

            if (animando) {                                      // Si la sopa está cayendo...
                dibujarAnimacion(g2);                            // ...dibujamos el cuadro de la animación
                return;                                          // ...y no dibujamos nada más
            }
            if (!tableroVisible) return;                         // Antes de la animación solo se ve el mantel

            dibujarPlato(g2, 1f, 1.0);                           // Plato completo con su caldo
            int l = lado(), ox = origenX(), oy = origenY();

            for (PalabraEncontrada pe : encontradas) {           // Palabras halladas (con su color)
                dibujarLinea(g2, pe.filaIni, pe.colIni, pe.filaFin, pe.colFin,
                        new Color(pe.color.getRed(), pe.color.getGreen(), pe.color.getBlue(), 190));
            }
            if (selIniFila != -1) {                              // Selección actual (amarilla)
                dibujarLinea(g2, selIniFila, selIniCol, selFinFila, selFinCol, new Color(255, 230, 0, 170));
            }

            g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Arial", Font.BOLD, Math.max(10, (int) (l * 0.5)))); // Letra proporcional a la casilla
            FontMetrics fm = g2.getFontMetrics();                // Medidas del texto para centrarlo
            for (int i = 0; i < tamano; i++) {
                for (int j = 0; j < tamano; j++) {
                    g2.setColor(new Color(120, 90, 30, 40));     // Cuadrícula muy suave (sobre el caldo)
                    g2.drawRect(ox + j * l, oy + i * l, l, l);
                    String letra = String.valueOf(matrizLetras[i][j]);
                    int tx = ox + j * l + (l - fm.stringWidth(letra)) / 2;
                    int ty = oy + i * l + (l + fm.getAscent() - fm.getDescent()) / 2;
                    g2.setColor(Color.BLACK);
                    g2.drawString(letra, tx, ty);
                }
            }
        }
    }

    // ============================================================
    //  11) OVERLAY: letrero grande con confeti por encima de todo
    // ============================================================
    private class Overlay extends JComponent {
        private String titulo = "";
        private String[] lineas = new String[0];
        private Color colorBanner = Color.BLUE;
        private ArrayList<Particula> confeti = new ArrayList<>();
        private Timer animacion;                 // Cronómetro de la animación (30 ms)
        private long finMs;                      // Momento en que debe desaparecer
        private Runnable alTerminar;             // Acción a ejecutar cuando desaparezca

        /** QUÉ HACE: prepara la capa y hace que absorba los clics (así no se toca el tablero de abajo). */
        Overlay() {
            setOpaque(false);
            MouseAdapter bloqueo = new MouseAdapter() { };       // Escucha el mouse pero no hace nada
            addMouseListener(bloqueo);
            addMouseMotionListener(bloqueo);
        }

        /**
         * QUÉ HACE: muestra el letrero (y confeti si se pide) y luego ejecuta una acción.
         * RECIBE: título, líneas de texto, color del letrero, duración en milisegundos,
         *         si lleva confeti (true/false) y la acción a ejecutar al terminar.
         * DEVUELVE: nada.
         */
        void mostrar(String titulo, String[] lineas, Color color, int duracionMs, boolean conConfeti, Runnable alTerminar) {
            this.titulo = titulo;
            this.lineas = lineas;
            this.colorBanner = color;
            this.alTerminar = alTerminar;
            this.finMs = System.currentTimeMillis() + duracionMs; // Hora actual + duración = hora de fin
            confeti.clear();
            if (conConfeti) {                                    // Si se pidió confeti, creamos 180 papelitos
                int w = Math.max(getWidth(), 1000);
                for (int i = 0; i < 180; i++) {
                    Particula p = new Particula();
                    p.x = ran.nextInt(w);                        // Posición horizontal al azar
                    p.y = -ran.nextInt(600);                     // Empiezan a distintas alturas (arriba, fuera de pantalla)
                    p.vx = ran.nextDouble() * 2 - 1;             // Se mueven un poco de lado
                    p.vy = 2 + ran.nextDouble() * 4;             // Caen a distinta velocidad
                    p.rot = ran.nextDouble() * 6;
                    p.vrot = ran.nextDouble() * 0.4 - 0.2;
                    p.ancho = 6 + ran.nextInt(8);
                    p.alto = 10 + ran.nextInt(10);
                    p.color = new Color(ran.nextInt(256), ran.nextInt(256), ran.nextInt(256));
                    confeti.add(p);
                }
            }
            setVisible(true);
            if (animacion != null) animacion.stop();             // Si había una animación vieja, la paramos
            animacion = new Timer(30, e -> {                     // Cada 30 ms (≈33 cuadros por segundo)
                int h = Math.max(getHeight(), 700);
                int w = Math.max(getWidth(), 1000);
                for (Particula p : confeti) {                    // Movemos cada papelito
                    p.x += p.vx;
                    p.y += p.vy;
                    p.rot += p.vrot;
                    if (p.y > h) {                               // Si salió por abajo...
                        p.y = -10;                               // ...vuelve a caer desde arriba
                        p.x = ran.nextInt(w);
                    }
                }
                repaint();
                if (System.currentTimeMillis() >= this.finMs) {  // Si ya pasó el tiempo del letrero...
                    animacion.stop();
                    setVisible(false);                           // ...lo ocultamos
                    confeti.clear();
                    if (this.alTerminar != null) this.alTerminar.run(); // ...y ejecutamos la acción pendiente
                }
            });
            animacion.start();
        }

        /** QUÉ HACE: dibuja fondo oscuro, letrero y confeti. RECIBE: el "pincel" g. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(new Color(0, 0, 0, 130));                // Fondo negro semitransparente
            g2.fillRect(0, 0, w, h);

            int alto = 120 + lineas.length * 34;                 // Alto del letrero según cuántas líneas tenga
            int ancho = (int) (w * 0.8);                         // 80% del ancho de la ventana
            int x = (w - ancho) / 2, y = (h - alto) / 2;         // Centrado
            g2.setColor(colorBanner);
            g2.fillRoundRect(x, y, ancho, alto, 40, 40);         // Rectángulo redondeado
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(x, y, ancho, alto, 40, 40);         // Borde blanco

            int tam = 40;                                        // Título: empieza grande...
            g2.setFont(new Font("Arial", Font.BOLD, tam));
            while (g2.getFontMetrics().stringWidth(titulo) > ancho - 40 && tam > 14) { // ...y se achica mientras no quepa
                tam -= 2;
                g2.setFont(new Font("Arial", Font.BOLD, tam));
            }
            g2.drawString(titulo, x + (ancho - g2.getFontMetrics().stringWidth(titulo)) / 2, y + 65);

            g2.setFont(new Font("Arial", Font.PLAIN, 22));       // Líneas de texto
            for (int i = 0; i < lineas.length; i++) {
                int tw = g2.getFontMetrics().stringWidth(lineas[i]);
                g2.drawString(lineas[i], x + (ancho - tw) / 2, y + 110 + i * 34);
            }

            for (Particula p : confeti) {                        // Confeti por encima de todo
                AffineTransform original = g2.getTransform();    // Guardamos la posición normal del pincel
                g2.rotate(p.rot, p.x, p.y);                      // Giramos el pincel
                g2.setColor(p.color);
                g2.fillRect((int) p.x, (int) p.y, p.ancho, p.alto);
                g2.setTransform(original);                       // Devolvemos el pincel a su posición normal
            }
            g2.dispose();
        }
    }

    // ============================================================
    //  12) RANKING (se guarda en Firebase + copia en ranking_sopa.txt)
    // ============================================================

    /**
     * QUÉ HACE: guarda el resultado del jugador en el archivo (solo si completó al menos una ronda).
     *           Si el nombre ya existía, SOLO reemplaza su marca si esta vez lo hizo mejor
     *           (más rondas, o las mismas rondas en menos tiempo). Así cada nombre aparece una sola vez.
     *           Deja en mensajeRecord un texto para mostrar al jugador.
     * RECIBE/DEVUELVE: nada (usa nombreJugador, rondaActual y tiempoTotalPartida).
     */
    private void registrarResultado() {
        mensajeRecord = "";                                      // Empezamos sin mensaje
        if (rondaActual <= 0) return;                            // Si no completó ninguna ronda, no se guarda
        ArrayList<Registro> lista = cargarRankingConMensaje("Guardando tu resultado...");  // Leemos lo que ya había
        Registro anterior = buscarRegistro(lista, nombreJugador);// ¿Ya existía este nombre?
        int antRondas = (anterior != null) ? anterior.rondas : 0;       // Marca anterior (rondas)
        int antSegundos = (anterior != null) ? anterior.segundos : 0;   // Marca anterior (segundos)

        boolean cambio = agregarMejor(lista, new Registro(nombreJugador, rondaActual, tiempoTotalPartida));
        if (anterior != null && cambio) {                        // Ya existía y esta vez lo hizo mejor
            mensajeRecord = "¡Mejoraste tu marca! (antes: " + formatear(antSegundos) + ")";
        } else if (anterior != null) {                           // Ya existía y NO lo superó
            mensajeRecord = "Tu mejor marca sigue siendo " + formatear(antSegundos) + " (" + antRondas + " rondas)";
        }                                                        // Si era un nombre nuevo, no hay mensaje especial

        guardarRankingLocal(lista);                              // Copia de respaldo en el archivo

        final Registro mio = buscarRegistro(lista, nombreJugador);
        if (cambio && mio != null && firebaseActivo()) {         // Nombre nuevo o marca mejorada: se sube a Firebase
            new Thread(() -> {                                   // En segundo plano para no congelar el juego
                try {
                    guardarEnFirebase(mio);
                } catch (Exception e) {
                    System.err.println("No se pudo guardar en Firebase: " + e.getMessage());
                }
            }).start();
        }
    }

    /** QUÉ HACE: escribe toda la lista en el archivo local (respaldo). RECIBE: la lista. */
    private void guardarRankingLocal(ArrayList<Registro> lista) {
        try {
            ArrayList<String> texto = new ArrayList<>();
            for (Registro r : lista) texto.add(r.nombre + ";" + r.rondas + ";" + r.segundos); // Una línea por jugador
            Files.write(Paths.get(ARCHIVO_RANKING), texto, StandardCharsets.UTF_8);          // Escribimos el archivo
        } catch (Exception e) {
            // Si no se puede guardar, el juego sigue igual
        }
    }

    // ------------------------------------------------------------
    //  FIREBASE: se usa la API REST (no necesita librerías extra)
    //  Estructura en la base de datos:  ranking / <nombre> / { nombre, rondas, segundos }
    // ------------------------------------------------------------

    /** DEVUELVE: true si ya pusiste tu URL de Firebase en FIREBASE_URL. */
    private boolean firebaseActivo() {
        return FIREBASE_URL != null && !FIREBASE_URL.isEmpty() && !FIREBASE_URL.contains("TU-PROYECTO");
    }

    /**
     * QUÉ HACE: envía una petición a Firebase y devuelve la respuesta (texto JSON).
     * RECIBE: método ("GET" o "PUT"), ruta (ej: "/ranking") y el cuerpo JSON (null si no hay).
     * LANZA: Exception si hay error de red o Firebase rechaza la petición (ej: reglas).
     */
    private String firebaseSolicitud(String metodo, String ruta, String cuerpo) throws Exception {
        String direccion = FIREBASE_URL + ruta + ".json" + (FIREBASE_AUTH.isEmpty() ? "" : "?auth=" + FIREBASE_AUTH);
        HttpURLConnection con = (HttpURLConnection) new URL(direccion).openConnection();
        con.setConnectTimeout(4000);                             // Máximo 4 s para conectar
        con.setReadTimeout(4000);                                // Máximo 4 s para recibir
        con.setRequestMethod(metodo);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (cuerpo != null) {
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(cuerpo.getBytes(StandardCharsets.UTF_8));
            }
        }
        int codigo = con.getResponseCode();
        InputStream is = (codigo >= 200 && codigo < 300) ? con.getInputStream() : con.getErrorStream();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (is != null) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) salida.write(buffer, 0, n);
            is.close();
        }
        String texto = new String(salida.toByteArray(), StandardCharsets.UTF_8);
        con.disconnect();
        if (codigo < 200 || codigo >= 300) throw new Exception("HTTP " + codigo + " " + texto);
        return texto;
    }

    /** QUÉ HACE: convierte un nombre en una clave válida de Firebase (sin . $ # [ ] / y en minúsculas). */
    private String claveFirebase(String nombre) {
        String c = nombre.trim().toLowerCase().replaceAll("[.$#\\[\\]/\\p{Cntrl}]", "_");
        return c.isEmpty() ? "_" : c;
    }

    /** QUÉ HACE: sube (o reemplaza) el registro de UN jugador en ranking/<nombre>. */
    private void guardarEnFirebase(Registro r) throws Exception {
        String clave = URLEncoder.encode(claveFirebase(r.nombre), "UTF-8").replace("+", "%20");
        String json = "{\"nombre\":\"" + escaparJson(r.nombre) + "\",\"rondas\":" + r.rondas
                + ",\"segundos\":" + r.segundos + "}";
        firebaseSolicitud("PUT", "/ranking/" + clave, json);
    }

    /** QUÉ HACE: lee todo el ranking de Firebase. LANZA Exception si no hay conexión. */
    private ArrayList<Registro> cargarDeFirebase() throws Exception {
        String json = firebaseSolicitud("GET", "/ranking", null);
        ArrayList<Registro> lista = new ArrayList<>();
        // Firebase devuelve las claves en orden alfabético: nombre, rondas, segundos
        Pattern patron = Pattern.compile("\\{\\s*\"nombre\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"rondas\"\\s*:\\s*(\\d+)\\s*,\\s*\"segundos\"\\s*:\\s*(\\d+)\\s*\\}");
        Matcher m = patron.matcher(json);
        while (m.find()) {
            agregarMejor(lista, new Registro(desescaparJson(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
        }
        return lista;
    }

    /** Escapa comillas, barras y saltos de línea para poder meter un texto dentro de un JSON. */
    private String escaparJson(String t) {
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c < 32) sb.append(' ');
            else sb.append(c);
        }
        return sb.toString();
    }

    /** Hace lo contrario de escaparJson (también entiende los códigos unicode). */
    private String desescaparJson(String t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\\' && i + 1 < t.length()) {
                char d = t.charAt(++i);
                if (d == 'u' && i + 4 < t.length()) {
                    sb.append((char) Integer.parseInt(t.substring(i + 1, i + 5), 16));
                    i += 4;
                } else if (d == 'n') sb.append('\n');
                else sb.append(d);                               // \" \\ \/ ...
            } else sb.append(c);
        }
        return sb.toString();
    }

    /**
     * QUÉ HACE: busca un nombre dentro de la lista (sin importar mayúsculas o minúsculas).
     * RECIBE: la lista y el nombre.   DEVUELVE: el Registro encontrado, o null si no existe.
     */
    private Registro buscarRegistro(ArrayList<Registro> lista, String nombre) {
        for (Registro r : lista) {
            if (r.nombre.equalsIgnoreCase(nombre)) return r;     // Encontrado
        }
        return null;                                             // No está en la lista
    }

    /**
     * QUÉ HACE: agrega un resultado a la lista conservando solo la MEJOR marca de cada nombre.
     * RECIBE: la lista y el nuevo registro.
     * DEVUELVE: true si la lista cambió (nombre nuevo o marca mejorada), false si no superó la marca anterior.
     */
    private boolean agregarMejor(ArrayList<Registro> lista, Registro nuevo) {
        Registro existente = buscarRegistro(lista, nuevo.nombre);
        if (existente == null) {                                 // Si el nombre no existía...
            lista.add(nuevo);                                    // ...lo agregamos
            return true;
        }
        // Es mejor si completó MÁS rondas, o las mismas rondas pero en MENOS tiempo
        boolean mejora = nuevo.rondas > existente.rondas
                || (nuevo.rondas == existente.rondas && nuevo.segundos < existente.segundos);
        if (mejora) {                                            // Si es mejor, reemplazamos la marca vieja
            existente.rondas = nuevo.rondas;
            existente.segundos = nuevo.segundos;
        }                                                        // Si no es mejor, no tocamos nada
        return mejora;
    }

    /** QUÉ HACE: ordena la lista: más rondas primero; si empatan, menos tiempo primero. RECIBE: la lista. DEVUELVE: nada. */
    private void ordenarRanking(ArrayList<Registro> lista) {
        lista.sort((a, b) -> a.rondas != b.rondas ? b.rondas - a.rondas : a.segundos - b.segundos);
    }

    /**
     * QUÉ HACE: arma las líneas del letrero final (rondas, tiempo total y el mensaje de récord si existe).
     * RECIBE: nada.   DEVUELVE: arreglo de textos, uno por línea.
     */
    private String[] lineasFinales() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Rondas completadas: " + rondaActual);
        l.add("Tiempo total: " + formatear(tiempoTotalPartida));
        if (!mensajeRecord.isEmpty()) l.add(mensajeRecord);      // Solo si hay mensaje de récord
        return l.toArray(new String[0]);
    }

    /**
     * QUÉ HACE: lee el ranking guardado en el archivo.
     * RECIBE: nada.   DEVUELVE: lista de Registros (vacía si el archivo no existe).
     */
    private ArrayList<Registro> cargarRanking() {
        if (firebaseActivo()) {
            try {
                ArrayList<Registro> nube = cargarDeFirebase();   // Ranking oficial: está en Firebase
                if (nube.isEmpty()) {                            // Firebase vacío: subimos lo que haya en el archivo local
                    ArrayList<Registro> local = cargarRankingLocal();
                    for (Registro r : local) guardarEnFirebase(r);
                    return local;
                }
                guardarRankingLocal(nube);                       // Actualizamos la copia local
                return nube;
            } catch (Exception e) {
                System.err.println("Firebase no disponible, se usa el archivo local: " + e.getMessage());
            }
        }
        return cargarRankingLocal();                             // Sin internet o sin configurar: archivo local
    }

    /**
     * QUÉ HACE: lee el ranking mostrando una ventanita de "Cargando..." mientras se consulta Firebase.
     *           Si Firebase no está configurado (solo archivo local) no muestra nada porque es instantáneo.
     * RECIBE: el texto a mostrar.   DEVUELVE: la lista de Registros.
     */
    private ArrayList<Registro> cargarRankingConMensaje(String mensaje) {
        if (!firebaseActivo()) return cargarRanking();           // Sin Firebase: no hace falta esperar
        ArrayList<Registro> r = conCargando(mensaje, () -> cargarRanking());
        return (r != null) ? r : new ArrayList<>();
    }

    /**
     * QUÉ HACE: ejecuta una tarea lenta (internet) en segundo plano y, mientras tanto, muestra una
     *           ventana con el mensaje y una barra animada. Cuando la tarea termina, la ventana se cierra
     *           sola y se devuelve el resultado (el juego no se congela).
     * RECIBE: el mensaje y la tarea.   DEVUELVE: lo que devuelva la tarea (null si falló).
     */
    private <T> T conCargando(String mensaje, Supplier<T> tarea) {
        final JDialog dialogo = new JDialog(this, "Cargando", true);   // true = bloquea la ventana de atrás
        dialogo.setUndecorated(true);
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 100, 60), 3),
                BorderFactory.createEmptyBorder(22, 36, 22, 36)));
        panel.setBackground(new Color(250, 250, 245));
        JLabel texto = new JLabel(mensaje, SwingConstants.CENTER);
        texto.setFont(new Font("Arial", Font.BOLD, 17));
        panel.add(texto, BorderLayout.CENTER);
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);                            // Barra que se mueve sin porcentaje
        barra.setPreferredSize(new Dimension(260, 14));
        panel.add(barra, BorderLayout.SOUTH);
        dialogo.setContentPane(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);

        SwingWorker<T, Void> trabajador = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                long inicio = System.currentTimeMillis();
                T resultado = null;
                try {
                    resultado = tarea.get();                     // Aquí se consulta Firebase
                } catch (Exception e) {
                    // Si falla, se devuelve null
                }
                long falta = 400 - (System.currentTimeMillis() - inicio);   // Se ve al menos 0,4 s (no parpadea)
                if (falta > 0) {
                    try { Thread.sleep(falta); } catch (InterruptedException ie) { /* nada */ }
                }
                return resultado;
            }
            @Override
            protected void done() {
                dialogo.dispose();                               // Termina: cierra la ventana de carga
            }
        };
        trabajador.execute();
        dialogo.setVisible(true);                                // Muestra la ventana y espera aquí hasta que se cierre
        try {
            return trabajador.get();
        } catch (Exception e) {
            return null;
        }
    }

    /** QUÉ HACE: lee el ranking del archivo local (respaldo). DEVUELVE: lista de Registros. */
    private ArrayList<Registro> cargarRankingLocal() {
        ArrayList<Registro> lista = new ArrayList<>();
        try {
            Path ruta = Paths.get(ARCHIVO_RANKING);
            if (Files.exists(ruta)) {                            // Solo si el archivo ya existe
                for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                    String[] p = linea.split(";");               // Separa "nombre;rondas;segundos"
                    if (p.length == 3) {                         // Si la línea tiene los 3 datos...
                        agregarMejor(lista, new Registro(p[0], Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()))); // Si hay nombres repetidos, deja el mejor
                    }                                            // (si no, se ignora esa línea dañada)
                }
            }
        } catch (Exception e) {
            // Archivo dañado: se ignora
        }
        return lista;
    }

    /** QUÉ HACE: crea un botón redondeado de color (cambia al pasar el mouse y al presionarlo). */
    private JButton botonColor(String texto, final Color color) {
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed() ? color.darker() : (getModel().isRollover() ? color.brighter() : color);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(color.darker());
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g2.dispose();
                super.paintComponent(g);                         // Dibuja el texto encima
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFont(new Font("Arial", Font.BOLD, 16));
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(170, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * QUÉ HACE: muestra la tabla del ranking en una ventana colorida (mismos colores del menú).
     *           Sirve para dos cosas: solo VER el ranking, o ELEGIR un nombre (Mejorar tiempo).
     * RECIBE: título, subtítulo, la lista ya ordenada, máximo de filas y si el jugador puede elegir una fila.
     * DEVUELVE: la posición elegida en la lista, o -1 si solo miró o canceló.
     */
    private int dialogoRanking(String titulo, String subtitulo, ArrayList<Registro> lista, int maxFilas, final boolean elegir) {
        final int[] resultado = {-1};
        final JDialog dialogo = new JDialog(this, titulo, true);

        // ----- Fondo verde degradado con marco blanco (igual que el menú) -----
        JPanel fondo = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 130, 50), getWidth(), getHeight(), new Color(150, 225, 120)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 130));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 30, 30);
                g2.dispose();
            }
        };
        fondo.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        // ----- Título -----
        JPanel cabecera = new JPanel(new GridLayout(2, 1));
        cabecera.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 32));
        lblTitulo.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel(subtitulo, SwingConstants.CENTER);
        lblSub.setFont(new Font("Arial", Font.BOLD, 15));
        lblSub.setForeground(new Color(255, 245, 190));
        cabecera.add(lblTitulo);
        cabecera.add(lblSub);
        fondo.add(cabecera, BorderLayout.NORTH);

        // ----- Tabla -----
        String[] columnas = {"Puesto", "Nombre", "Rondas completadas", "Tiempo total"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int col) { return false; }   // Solo lectura
        };
        for (int i = 0; i < Math.min(maxFilas, lista.size()); i++) {
            Registro r = lista.get(i);
            modelo.addRow(new Object[]{(i + 1) + "°", r.nombre, r.rondas, formatear(r.segundos)});
        }
        final JTable tabla = new JTable(modelo);
        tabla.setRowHeight(34);
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(235, 215, 160));
        tabla.setRowSelectionAllowed(elegir);                    // Solo se puede marcar una fila al elegir nombre
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFocusable(elegir);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(110);

        // Cabecera verde con letras blancas
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tabla.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Arial", Font.BOLD, 15));
                l.setOpaque(true);
                l.setBackground(new Color(30, 120, 55));
                l.setForeground(Color.WHITE);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 1, new Color(245, 150, 40)));
                return l;
            }
        });

        // Filas: 1° dorado, 2° plateado, 3° bronce, el resto crema alternado; la elegida en naranja
        javax.swing.table.DefaultTableCellRenderer celda = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Arial", row < 3 ? Font.BOLD : Font.PLAIN, 17));
                Color color;
                if (row == 0) color = new Color(255, 215, 90);          // Oro
                else if (row == 1) color = new Color(222, 229, 240);    // Plata
                else if (row == 2) color = new Color(240, 181, 130);    // Bronce
                else color = (row % 2 == 0) ? new Color(255, 250, 235) : new Color(255, 238, 200);
                if (sel && elegir) {
                    l.setBackground(new Color(245, 150, 40));           // Fila elegida: naranja
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(color);
                    l.setForeground(new Color(40, 40, 40));
                }
                return l;
            }
        };
        for (int c = 0; c < tabla.getColumnCount(); c++) tabla.getColumnModel().getColumn(c).setCellRenderer(celda);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(20, 100, 40), 3));
        scroll.getViewport().setBackground(new Color(255, 250, 235));
        fondo.add(scroll, BorderLayout.CENTER);

        // ----- Botones -----
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        botones.setOpaque(false);
        if (elegir) {
            JButton btnJugar = botonColor("JUGAR", new Color(245, 150, 40));      // Naranja (como el menú)
            JButton btnCancelar = botonColor("CANCELAR", new Color(200, 70, 60));
            btnJugar.addActionListener(e -> {
                resultado[0] = tabla.getSelectedRow();
                dialogo.dispose();
            });
            btnCancelar.addActionListener(e -> dialogo.dispose());
            botones.add(btnJugar);
            botones.add(btnCancelar);
            dialogo.getRootPane().setDefaultButton(btnJugar);
            tabla.addMouseListener(new MouseAdapter() {          // Doble clic sobre un nombre = JUGAR
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                        resultado[0] = tabla.getSelectedRow();
                        dialogo.dispose();
                    }
                }
            });
            if (modelo.getRowCount() > 0) tabla.setRowSelectionInterval(0, 0);   // Deja el primero marcado
        } else {
            JButton btnCerrar = botonColor("CERRAR", new Color(245, 150, 40));
            btnCerrar.addActionListener(e -> dialogo.dispose());
            botones.add(btnCerrar);
            dialogo.getRootPane().setDefaultButton(btnCerrar);
        }
        fondo.add(botones, BorderLayout.SOUTH);

        // ESC cierra la ventana
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialogo.setContentPane(fondo);
        dialogo.setSize(680, 560);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);                                // Se muestra y ESPERA aquí hasta que se cierre
        return resultado[0];
    }

    /** QUÉ HACE: muestra la tabla del ranking (más rondas primero; si empatan, menos tiempo primero). */
    private void mostrarRanking() {
        ArrayList<Registro> lista = cargarRankingConMensaje("Cargando ranking...");
        ordenarRanking(lista);                                   // Más rondas primero; si empatan, menos tiempo

        dialogoRanking("RANKING", "Los mejores jugadores de la sopa", lista, 20, false);
    }

    // ============================================================
    //  13) FUNCIONES ÚTILES
    // ============================================================

    /**
     * QUÉ HACE: convierte segundos a formato mm:ss.
     * RECIBE: segundos (ej: 75).   DEVUELVE: texto (ej: "01:15").
     */
    private String formatear(int segundos) {
        return String.format("%02d:%02d", segundos / 60, segundos % 60); // "/" = minutos, "%" = segundos sobrantes
    }

    /**
     * QUÉ HACE: parte un texto largo en varias líneas para que quepa en el letrero.
     * RECIBE: el texto y el máximo de letras por línea.   DEVUELVE: lista de líneas.
     */
    private ArrayList<String> partirEnLineas(String texto, int max) {
        ArrayList<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {             // Separa el texto por espacios
            if (actual.length() + palabra.length() + 1 > max && actual.length() > 0) {
                lineas.add(actual.toString());                   // Si ya no cabe, cerramos esta línea...
                actual = new StringBuilder();                    // ...y empezamos una nueva
            }
            if (actual.length() > 0) actual.append("  ");        // Espacio entre palabras (si no es la primera)
            actual.append(palabra);
        }
        if (actual.length() > 0) lineas.add(actual.toString());  // Agregamos la última línea
        return lineas;
    }
}