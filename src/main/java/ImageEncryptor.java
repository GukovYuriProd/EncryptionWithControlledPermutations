import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class ImageEncryptor {

    private static int BLOCK_SIZE = 20;

    //BGR - порядок кодирования битов
    //формула вычисления интенсивности пикселя по стандарту BT-709 Y = 0.2125·R + 0.7154·G + 0.0721·B
    public static void main(String[] Args) throws IOException {
        EncryptImageBySPBlock("src/main/Photos/picture.bmp", "src/main/Photos/output.bmp");
    }


    public static void EncryptImageBySPBlock(String source, String destination) throws IOException {
        byte[] bitmap = ReadBytes(source); //чтение
        bitmap = encrypted_bitmap(bitmap); //шифрование
        WriteBytesToFile(destination, bitmap); //запись
    }

    public static void WriteBytesToFile(String source, byte[] bytes) throws IOException {
        File output_file = new File(source);
        BufferedOutputStream output_stream = new BufferedOutputStream(new FileOutputStream(output_file));
        output_stream.write(bytes);
        output_stream.flush();
        output_stream.close();
    }

    public static byte[] ReadBytes(String source) throws IOException {
        File input_file = new File(source);
        return Files.readAllBytes(input_file.toPath());
    }

    public static byte[] encrypted_bitmap(byte[] bitmap) {
        int pixels_count = bitmap.length;
        byte[] output_bitmap = new byte[pixels_count];
        //заполняем шапку файла
        System.arraycopy(bitmap, 0, output_bitmap, 0, 54);
        //то есть это 30 байтов, кодирующие 2 последовательных пикселя
        int data_block = 30; //ДЛИНА БЛОКА ДАННЫХ
        for (int index = 54; index < pixels_count; index += data_block) {
            //здесь на каждой итерации обрабатываем 30 последовательных байтов в значимой массе байтов
            //и заполняем их биты в общий битовый блок bits на 240 разрядов
            byte[] bits = new byte[data_block * 8]; //блок данных
            for (int byte_index = 0; byte_index < data_block; byte_index++) { //читаю по порядку байты одного блока
                for (int bit = byte_index * 8 + 7; bit >= byte_index * 8; bit--) { //читаю биты одного байта из блока
                    if ((bitmap[index + byte_index] % 2 == 1) || (bitmap[index + byte_index] % 2 == -1)) bits[bit] = 1;
                    bitmap[index + byte_index] = (byte) (bitmap[index + byte_index] >> 1);
                }
            }
            //обработка блока данных S и P блоками
            bits = BlockEncryptor(bits);
            //далее происходит запись в выходную карту байтов
            for (int i = index; i < index + data_block; i++) {
                int pre_byte = 0;
                for (int local_bit = (i - index) * 8; local_bit < (i - index) * 8 + 8; local_bit++) {
                    pre_byte += bits[local_bit] * Math.pow(2, (7 - (local_bit - ((i - index) * 8))));
                }
                output_bitmap[i] = (byte) pre_byte;
            }
        }
        return output_bitmap;

    }

    public static byte[] BlockEncryptor(byte[] block) {
        byte[] output = block;
        int rounds = 9;
        for (int index = 0; index < rounds; index++) {
            output = EncruptWithControlPermutation(output);
        }
        return output;
    }

    public static byte[] EncruptWithControlPermutation(byte[] block) { //Блок шифрования управляемой перестановкой
        byte[] output = new byte[block.length];
        int block_size = BLOCK_SIZE;
        int count_of_S_blocks = block.length / block_size;
        for (int S_index = 0; S_index < count_of_S_blocks; S_index++) {
            int sourcePosition = S_index * block_size;
            byte[] LeftPart = new byte[block_size / 2];
            byte[] RightPart = new byte[block_size / 2];
            System.arraycopy(block, sourcePosition, LeftPart, 0, block_size / 2);
            System.arraycopy(block, sourcePosition + block_size / 2, RightPart, 0, block_size / 2);

            PermuteFirstBySecond(LeftPart, RightPart); //Переставляем биты левого блока под управлением правого
            LeftPart = BlockXOR_LEFT(LeftPart, RightPart); //К левому блоку прибавляем по модулю два правый
            PermuteFirstBySecond(RightPart, LeftPart); //Переставляем биты правого блока под управлением левого
            RightPart = BlockXOR_LEFT(RightPart, LeftPart); //К левому блоку прибавляем по модулю два правый
            LeftPart = AddKey(LeftPart, new byte[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1}); //Добавление ключа к левому блоку
            RightPart = AddKey(RightPart, new byte[]{1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0}); //Добавление ключа к правому блоку

            System.arraycopy(LeftPart, 0, output, sourcePosition, block_size / 2);
            System.arraycopy(RightPart, 0, output, sourcePosition + block_size / 2, block_size / 2);
        }
        return output;

    }

    public static byte[] PermuteFirstBySecond(byte[] LeftPart, byte[] RightPart) {
        for (int i = 0; i < RightPart.length; i++) {
            byte buffer;
            if (RightPart[i] == 1) {
                Permutation permutation = Permutation.values()[i];
                buffer = LeftPart[permutation.a];
                LeftPart[permutation.a] = LeftPart[permutation.b];
                LeftPart[permutation.b] = LeftPart[buffer];
            }
        }
        //TODO Как то инкапсулировать это?
        //блок дополнительных сгенерированных перестановок
        byte[] genericPermutations = new byte[12];
        genericPermutations[0] = xor(RightPart[0], RightPart[1]);
        genericPermutations[1] = xor(RightPart[1], RightPart[2]);
        genericPermutations[2] = xor(RightPart[2], RightPart[3]);
        genericPermutations[3] = xor(RightPart[3], RightPart[4]);
        genericPermutations[4] = xor(RightPart[4], RightPart[5]);
        genericPermutations[5] = xor(RightPart[5], RightPart[6]);
        genericPermutations[6] = xor(RightPart[6], RightPart[7]);
        genericPermutations[7] = xor(RightPart[7], RightPart[8]);
        genericPermutations[8] = xor(RightPart[8], RightPart[9]);
        genericPermutations[9] = xor(RightPart[9], RightPart[1]);
        genericPermutations[10] = xor(RightPart[1], RightPart[8]);
        genericPermutations[11] = xor(RightPart[2], RightPart[7]);
        for (int i = 0; i < genericPermutations.length; i++) {
            byte buffer;
            if (genericPermutations[i] == 1) {
                Permutation permutation = Permutation.values()[i + BLOCK_SIZE/2];
                buffer = LeftPart[permutation.a];
                LeftPart[permutation.a] = LeftPart[permutation.b];
                LeftPart[permutation.b] = LeftPart[buffer];

            }
        }
        return LeftPart;
    }

    public static byte xor(byte A, byte B) {
        return (byte) (A == B ? 1 : 0);
    }

    public static byte[] BlockXOR_LEFT(byte[] FirstBlock, byte[] SecondBlock) {
        byte[] output = new byte[BLOCK_SIZE/2];
        for (int i = 0; i < output.length; i++) {
            output[i] = xor(FirstBlock[i], SecondBlock[i]);
        }
        return output;
    }

    public static byte[] AddKey(byte[] Block, byte[] key) {
        byte[] output = new byte[BLOCK_SIZE/2];
        for (int i = 0; i < output.length; i++) output[i] = xor(Block[i], key[i]);
        return output;
    }
}