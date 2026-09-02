import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Labo1ED {

    static final long ROWS = 100000;
    static final long COLS = 100000;
    static final String ARCHIVO = "matriz_100k.bin";

    static final byte VALOR_SEPARADOR = 0; // el byte 0x00 se reserva exclusivamente como separador de fila
    static final int HEADER_SIZE = 16;     // 2 x int64 (rows, cols)

    public static void main(String[] args) {
        generarMatriz();
    }

    static void generarMatriz() {
        File archivo = new File(ARCHIVO);

        System.out.println("Generando matriz " + ROWS + " x " + COLS + "...");
        System.out.println("Se guardara en: " + archivo.getAbsolutePath() + "\n");

        long inicio = System.currentTimeMillis();

        // bytes que ocupa una fila empacada (8 datos por byte, redondeado hacia arriba)
        int bytesPorFila = (int) ((COLS + 7) / 8);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(archivo), 1 << 20)) {

            // header: dimensiones como int64 little-endian
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            header.putLong(ROWS);
            header.putLong(COLS);
            out.write(header.array());

            // como todo dato vale 1, cada byte empacado queda 11111111 (0xFF): se arma una sola vez y se reutiliza
            byte[] filaEmpacada = new byte[bytesPorFila];
            Arrays.fill(filaEmpacada, (byte) 0xFF);

            for (long f = 0; f < ROWS; f++) {
                out.write(filaEmpacada);
                out.write(VALOR_SEPARADOR); // separador al final de cada fila

                if (f % 5000 == 0) {
                    System.out.println("  fila " + f + " / " + ROWS);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al generar la matriz", e);
        }

        double segundos = (System.currentTimeMillis() - inicio) / 1000.0;
        System.out.printf("%nListo en %.1f s%n", segundos);
        System.out.printf("Tamano en disco: %.2f GB%n", archivo.length() / 1e9);
    }
}