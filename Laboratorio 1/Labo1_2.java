import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

public class Labo1_2 {

    static final String ARCHIVO = "matriz_100k.bin";
    static final String ARCHIVO_INFO = "info_carga.txt";
    static final String ARCHIVO_DATOS = "datos_cargados.txt";
    static final int HEADER_SIZE = 16;

    static long rows;
    static long cols;
    static int bytesPorFila;   // bytes empacados de datos por fila
    static long rowStride;     // = bytesPorFila + 1 (datos + separador)

    public static void main(String[] args) throws IOException {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            System.out.println("Primero ejecuta Labo1ED para generar la matriz.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            leerHeader(raf);

            System.out.println("Matriz: " + rows + " x " + cols);
            System.out.printf("Tamano en disco: %.2f GB%n", archivo.length() / 1e9);

            Scanner sc = new Scanner(System.in);
            int opcion;
            do {
                opcion = mostrarMenu(sc);
                switch (opcion) {
                    case 1 -> leerFila(raf, sc);
                    case 2 -> leerColumna(raf, sc);
                    case 3 -> leerIntervaloFilas(raf, sc);
                    case 4 -> leerIntervaloColumnas(raf, sc);
                    case 5 -> leerDato(raf, sc);
                    case 6 -> leerChunk(raf, sc);
                    case 0 -> System.out.println("Saliendo...");
                    default -> System.out.println("Opcion invalida.");
                }
            } while (opcion != 0);

            sc.close();
        }
    }

    static void leerHeader(RandomAccessFile raf) throws IOException {
        byte[] header = new byte[HEADER_SIZE];
        raf.readFully(header);
        ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        rows = bb.getLong();
        cols = bb.getLong();
        bytesPorFila = (int) ((cols + 7) / 8);
        rowStride = bytesPorFila + 1;
    }

    static int mostrarMenu(Scanner sc) {
        System.out.println("\n--- Menu de lectura ---");
        System.out.println("1. Leer una fila completa");
        System.out.println("2. Leer una columna completa");
        System.out.println("3. Leer un intervalo de filas");
        System.out.println("4. Leer un intervalo de columnas");
        System.out.println("5. Leer un dato especifico (fila, columna)");
        System.out.println("6. Leer un chunk (intervalo de filas x intervalo de columnas)");
        System.out.println("0. Salir");
        return (int) pedirLong(sc, "Elige una opcion: ", 0, 6);
    }

    // ---------- operaciones de lectura ----------
    // Cada operacion arma una matriz 2D (datos[fila][columna]) con lo que carga en RAM,
    // y luego llama a registrarCarga(...) para actualizar los dos archivos de salida.

    static void leerFila(RandomAccessFile raf, Scanner sc) throws IOException {
        long fila = pedirLong(sc, "Numero de fila (0 a " + (rows - 1) + "): ", 0, rows - 1);

        byte[][] datos = { leerBitsDeFila(raf, fila, 0, cols - 1) };
        registrarCarga("Leer una fila completa", fila, fila, 0, cols - 1, datos);
    }

    static void leerColumna(RandomAccessFile raf, Scanner sc) throws IOException {
        long col = pedirLong(sc, "Numero de columna (0 a " + (cols - 1) + "): ", 0, cols - 1);

        System.out.println("Leyendo columna " + col + " (" + rows + " valores)...");
        System.out.println("(el acceso por columna requiere 1 lectura por fila, ya que los datos estan ordenados por filas)");

        byte[][] datos = new byte[(int) rows][1];
        for (long f = 0; f < rows; f++) {
            datos[(int) f][0] = leerBitsDeFila(raf, f, col, col)[0];
        }

        registrarCarga("Leer una columna completa", 0, rows - 1, col, col, datos);
    }

    static void leerIntervaloFilas(RandomAccessFile raf, Scanner sc) throws IOException {
        long f1 = pedirLong(sc, "Fila inicial (0 a " + (rows - 1) + "): ", 0, rows - 1);
        long f2 = pedirLong(sc, "Fila final (" + f1 + " a " + (rows - 1) + "): ", f1, rows - 1);

        byte[][] datos = new byte[(int) (f2 - f1 + 1)][];
        for (long f = f1; f <= f2; f++) {
            datos[(int) (f - f1)] = leerBitsDeFila(raf, f, 0, cols - 1);
        }

        registrarCarga("Leer un intervalo de filas", f1, f2, 0, cols - 1, datos);
    }

    static void leerIntervaloColumnas(RandomAccessFile raf, Scanner sc) throws IOException {
        long c1 = pedirLong(sc, "Columna inicial (0 a " + (cols - 1) + "): ", 0, cols - 1);
        long c2 = pedirLong(sc, "Columna final (" + c1 + " a " + (cols - 1) + "): ", c1, cols - 1);

        System.out.println("Leyendo columnas [" + c1 + ", " + c2 + "] para las " + rows + " filas...");
        System.out.println("(el acceso por columnas es mas lento: requiere 1 lectura por fila)");

        byte[][] datos = new byte[(int) rows][];
        for (long f = 0; f < rows; f++) {
            datos[(int) f] = leerBitsDeFila(raf, f, c1, c2);
        }

        registrarCarga("Leer un intervalo de columnas", 0, rows - 1, c1, c2, datos);
    }

    static void leerDato(RandomAccessFile raf, Scanner sc) throws IOException {
        long fila = pedirLong(sc, "Fila (0 a " + (rows - 1) + "): ", 0, rows - 1);
        long col = pedirLong(sc, "Columna (0 a " + (cols - 1) + "): ", 0, cols - 1);

        byte valor = leerBitsDeFila(raf, fila, col, col)[0];
        byte[][] datos = {{ valor }};

        registrarCarga("Leer un dato especifico", fila, fila, col, col, datos);
        System.out.println("Dato en [" + fila + "][" + col + "] = " + valor);
    }

    static void leerChunk(RandomAccessFile raf, Scanner sc) throws IOException {
        long f1 = pedirLong(sc, "Fila inicial (0 a " + (rows - 1) + "): ", 0, rows - 1);
        long f2 = pedirLong(sc, "Fila final (" + f1 + " a " + (rows - 1) + "): ", f1, rows - 1);
        long c1 = pedirLong(sc, "Columna inicial (0 a " + (cols - 1) + "): ", 0, cols - 1);
        long c2 = pedirLong(sc, "Columna final (" + c1 + " a " + (cols - 1) + "): ", c1, cols - 1);

        byte[][] datos = new byte[(int) (f2 - f1 + 1)][];
        for (long f = f1; f <= f2; f++) {
            datos[(int) (f - f1)] = leerBitsDeFila(raf, f, c1, c2);
        }

        registrarCarga("Leer un chunk (filas x columnas)", f1, f2, c1, c2, datos);
    }

    // ---------- lectura y desempacado de bits ----------

    /**
     * Lee los datos de la fila indicada, entre las columnas c1 y c2 (ambas incluidas),
     * y devuelve un arreglo con los valores 0/1 ya desempacados (equivalente a np.unpackbits).
     */
    static byte[] leerBitsDeFila(RandomAccessFile raf, long fila, long c1, long c2) throws IOException {
        int byteInicio = (int) (c1 / 8);
        int byteFin = (int) (c2 / 8);
        int nBytesLeer = byteFin - byteInicio + 1;

        byte[] bloque = new byte[nBytesLeer];
        raf.seek(HEADER_SIZE + fila * rowStride + byteInicio);
        raf.readFully(bloque);

        int ancho = (int) (c2 - c1 + 1);
        byte[] resultado = new byte[ancho];
        for (int i = 0; i < ancho; i++) {
            long col = c1 + i;
            int byteIdxRelativo = (int) (col / 8) - byteInicio;
            int bitIdx = 7 - (int) (col % 8); // bit-order 'big', igual que np.packbits/unpackbits
            resultado[i] = (byte) ((bloque[byteIdxRelativo] >> bitIdx) & 1);
        }
        return resultado;
    }

    // ---------- generacion de los 2 archivos de salida ----------

    /**
     * Actualiza (sobrescribe) los dos archivos de salida cada vez que se carga algo nuevo en RAM:
     * - ARCHIVO_INFO: resumen de la operacion (cuantas filas/columnas y cuales).
     * - ARCHIVO_DATOS: el contenido completo cargado en esta consulta.
     */
    static void registrarCarga(String operacion, long f1, long f2, long c1, long c2, byte[][] datos) throws IOException {
        long nFilas = f2 - f1 + 1;
        long nCols = c2 - c1 + 1;

        try (PrintWriter info = new PrintWriter(new FileWriter(ARCHIVO_INFO, false))) {
            info.println("Operacion: " + operacion);
            info.println();
            info.println("Filas cargadas: " + nFilas + " (indices " + f1 + " a " + f2 + ")");
            info.println("Columnas cargadas: " + nCols + " (indices " + c1 + " a " + c2 + ")");
            info.println("Total de valores en RAM: " + (nFilas * nCols));
        }

        try (PrintWriter datosOut = new PrintWriter(new FileWriter(ARCHIVO_DATOS, false))) {
            for (byte[] fila : datos) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < fila.length; i++) {
                    sb.append(fila[i]);
                    if (i < fila.length - 1) sb.append(" ");
                }
                datosOut.println(sb);
            }
        }

        System.out.println("Listo. " + nFilas + " fila(s) x " + nCols + " columna(s) cargadas en RAM.");
        System.out.println("Detalle en: " + ARCHIVO_INFO + " | Datos en: " + ARCHIVO_DATOS);
    }

    // ---------- utilidades ----------

    static long pedirLong(Scanner sc, String mensaje, long min, long max) {
        while (true) {
            System.out.print(mensaje);
            String entrada = sc.nextLine().trim();
            try {
                long valor = Long.parseLong(entrada);
                if (valor < min || valor > max) {
                    System.out.println("Debe estar entre " + min + " y " + max + ".");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("Ingresa un numero entero valido.");
            }
        }
    }
}