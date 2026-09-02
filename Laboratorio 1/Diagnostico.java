import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Diagnostico {
    public static void main(String[] args) throws Exception {
        File archivo = new File("matriz_100k.bin");
        System.out.println("Tamano exacto: " + archivo.length() + " bytes");
        System.out.println("Esperado:      1250100016 bytes");

        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            byte[] header = new byte[16];
            raf.readFully(header);
            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            System.out.println("rows (header): " + bb.getLong());
            System.out.println("cols (header): " + bb.getLong());

            // primeros 20 bytes de datos (deberian ser todos 0xFF)
            byte[] datos = new byte[20];
            raf.readFully(datos);
            System.out.print("Primeros 20 bytes de datos (deberian ser FF FF FF...): ");
            for (byte b : datos) System.out.printf("%02X ", b);
            System.out.println();

            // saltamos justo al final de la primera fila para ver el separador (deberia ser 00)
            raf.seek(16 + 12500);
            int separador = raf.read();
            System.out.printf("Byte separador tras fila 0 (deberia ser 00): %02X%n", separador);

            // primeros bytes de la fila 1 (deberian volver a ser FF)
            byte[] fila1 = new byte[10];
            raf.readFully(fila1);
            System.out.print("Primeros 10 bytes de la fila 1 (deberian ser FF): ");
            for (byte b : fila1) System.out.printf("%02X ", b);
            System.out.println();
        }
    }
}