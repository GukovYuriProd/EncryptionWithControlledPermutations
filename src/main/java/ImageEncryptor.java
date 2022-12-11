import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class ImageEncryptor {
    //BGR - порядок кодирования битов
    //формула вычисления интенсивности пикселя по стандарту BT-709 Y = 0.2125·R + 0.7154·G + 0.0721·B
    public static void main (String[] Args) throws IOException {
        EncryptImageBySPBlock("src/main/Photos/picture.bmp", "src/main/Photos/output.bmp");
    }
    public static void EncryptImageBySPBlock (String source, String destination) throws IOException{
        byte[] bitmap = ReadBytes(source); //чтение
        bitmap = encrypted_bitmap(bitmap); //шифрование
        WriteBytesToFile(destination, bitmap); //запись
    }
    public static void WriteBytesToFile (String source, byte[] bytes) throws IOException {
        File output_file = new File(source);
        BufferedOutputStream output_stream = new BufferedOutputStream(new FileOutputStream(output_file));
        output_stream.write(bytes);
        output_stream.flush();
        output_stream.close();
    }
    public static byte[] ReadBytes (String source) throws IOException {
        File input_file = new File(source);
        return Files.readAllBytes(input_file.toPath());
    }
    public static byte[] encrypted_bitmap (byte[] bitmap){
        int pixels_count = bitmap.length;
        byte[] output_bitmap = new byte[pixels_count];
        //заполняем шапку файла
        System.arraycopy(bitmap, 0, output_bitmap, 0, 54);
        //то есть это 30 байтов, кодирующие 2 последовательных пикселя
        int data_block = 30; //ДЛИНА БЛОКА ДАННЫХ
        for (int index = 54; index < pixels_count; index += data_block){
            //здесь на каждой итерации обрабатываем 30 последовательных байтов в значимой массе байтов
            //и заполняем их биты в общий битовый блок bits на 240 разрядов
            byte[] bits = new byte[data_block*8]; //блок данных
            for (int byte_index = 0; byte_index < data_block; byte_index++){ //читаю по порядку байты одного блока
                for (int bit = byte_index*8+7; bit >= byte_index*8; bit--){ //читаю биты одного байта из блока
                    if ((bitmap[index+byte_index]%2==1)||(bitmap[index+byte_index]%2==-1)) bits[bit] = 1;
                    bitmap[index+byte_index] = (byte) (bitmap[index+byte_index] >> 1);
                }
            }
            //обработка блока данных S и P блоками
            bits = BlockEncryptor(bits);
            //далее происходит запись в выходную карту байтов
            for (int i = index; i < index+data_block; i++) {
                int pre_byte = 0;
                for (int local_bit = (i-index)*8; local_bit < (i-index)*8 + 8; local_bit++){
                    pre_byte += bits[local_bit]*Math.pow(2,(7 - (local_bit-((i-index)*8))));
                }
                output_bitmap[i] = (byte) pre_byte;
            }
        }
        return output_bitmap;

    }
    public static byte[] BlockEncryptor(byte[] block){
        byte[] output = block;
        int rounds = 5;
        for (int index = 0; index < rounds; index++){
            output = EncruptWithControlPermutation(output);
        }
        return output;
    }
    public static byte[] EncruptWithControlPermutation(byte[] block){ //Блок шифрования управляемой перестановкой
        byte[] output = new byte[block.length];
        int block_size = 24;
        int count_of_S_blocks = block.length / block_size;
        for (int S_index = 0; S_index < count_of_S_blocks; S_index++){
            int sourcePosition = S_index*block_size;
            byte[] LeftPart = new byte[block_size/2];
            byte[] RightPart = new byte[block_size/2];
            System.arraycopy(block, sourcePosition, LeftPart, 0, block_size/2);
            System.arraycopy(block, sourcePosition+block_size/2, RightPart, 0, block_size/2);

            PermuteFirstBySecond(LeftPart, RightPart); //Переставляем биты левого блока под управлением правого
            LeftPart = BlockXOR_LEFT(LeftPart, RightPart); //К левому блоку прибавляем по модулю два правый
            PermuteFirstBySecond(RightPart, LeftPart); //Переставляем биты правого блока под управлением левого
            RightPart = BlockXOR_LEFT(RightPart, LeftPart); //К левому блоку прибавляем по модулю два правый
            LeftPart = AddKey(LeftPart, new byte[]{0,1,0,1,0,1,0,1,0,1,0,1}); //Добавление ключа к левому блоку
            RightPart = AddKey(RightPart, new byte[]{1,0,1,0,1,0,1,0,1,0,1,0}); //Добавление ключа к правому блоку

            System.arraycopy(LeftPart, 0, output, sourcePosition, block_size/2);
            System.arraycopy(RightPart, 0, output, sourcePosition+block_size/2, block_size/2);
        }
        return output;

    }
    public static byte[] PermuteFirstBySecond (byte[] LeftPart, byte[] RightPart){
        for (int i = 0; i < LeftPart.length; i++){
            byte buffer;
            if (RightPart[i] == 1){
                switch (i) {
                    case 0 -> {
                        buffer = LeftPart[0];
                        LeftPart[0] = LeftPart[2];
                        LeftPart[2] = LeftPart[buffer];
                    }
                    case 1 -> {
                        buffer = LeftPart[3];
                        LeftPart[3] = LeftPart[4];
                        LeftPart[4] = LeftPart[buffer];
                    }
                    case 2 -> {
                        buffer = LeftPart[5];
                        LeftPart[5] = LeftPart[7];
                        LeftPart[7] = LeftPart[buffer];
                    }
                    case 3 -> {
                        buffer = LeftPart[8];
                        LeftPart[8] = LeftPart[9];
                        LeftPart[9] = LeftPart[buffer];
                    }
                    case 4 -> {
                        buffer = LeftPart[10];
                        LeftPart[10] = LeftPart[11];
                        LeftPart[11] = LeftPart[buffer];
                    }
                    case 5 -> {
                        buffer = LeftPart[8];
                        LeftPart[8] = LeftPart[11];
                        LeftPart[11] = LeftPart[buffer];
                    }
                    case 6 -> {
                        buffer = LeftPart[4];
                        LeftPart[4] = LeftPart[6];
                        LeftPart[6] = LeftPart[buffer];
                    }
                    case 7 -> {
                        buffer = LeftPart[1];
                        LeftPart[1] = LeftPart[3];
                        LeftPart[3] = LeftPart[buffer];
                    }
                    case 8 -> {
                        buffer = LeftPart[0];
                        LeftPart[0] = LeftPart[1];
                        LeftPart[1] = LeftPart[buffer];
                    }
                    case 9 -> {
                        buffer = LeftPart[2];
                        LeftPart[2] = LeftPart[4];
                        LeftPart[4] = LeftPart[buffer];
                    }
                    case 10 -> {
                        buffer = LeftPart[5];
                        LeftPart[5] = LeftPart[8];
                        LeftPart[8] = LeftPart[buffer];
                    }
                    case 11 -> {
                        buffer = LeftPart[9];
                        LeftPart[9] = LeftPart[10];
                        LeftPart[10] = LeftPart[buffer];
                    }
                }
            }
        }
        //блок дополнительных сгенерированных перестановок
        byte[] genericPermutations = new byte[16];
        genericPermutations[0] = xor(RightPart[0], RightPart[1]);
        genericPermutations[1] = xor(RightPart[2], RightPart[3]);
        genericPermutations[2] = xor(RightPart[3], RightPart[4]);
        genericPermutations[3] = xor(RightPart[4], RightPart[5]);
        genericPermutations[4] = xor(RightPart[5], RightPart[6]);
        genericPermutations[5] = xor(RightPart[6], RightPart[7]);
        genericPermutations[6] = xor(RightPart[7], RightPart[8]);
        genericPermutations[7] = xor(RightPart[8], RightPart[9]);
        genericPermutations[8] = xor(RightPart[9], RightPart[10]);
        genericPermutations[9] = xor(RightPart[10], RightPart[11]);
        genericPermutations[10] = xor(RightPart[11], RightPart[5]);
        genericPermutations[11] = xor(RightPart[4], RightPart[1]);
        genericPermutations[12] = xor(RightPart[7], RightPart[2]);
        genericPermutations[13] = xor(RightPart[9], RightPart[3]);
        genericPermutations[14] = xor(RightPart[10], RightPart[5]);
        genericPermutations[15] = xor(RightPart[11], RightPart[4]);
        for (int i = 0; i < 16; i++){
            byte buffer;
            if (genericPermutations[i] == 1){
                switch (i) {
                    case 0 -> { //12
                        buffer = LeftPart[9];
                        LeftPart[9] = LeftPart[11];
                        LeftPart[11] = LeftPart[buffer];
                    }
                    case 1 -> { //13
                        buffer = LeftPart[6];
                        LeftPart[6] = LeftPart[8];
                        LeftPart[8] = LeftPart[buffer];
                    }
                    case 2 -> { //14
                        buffer = LeftPart[3];
                        LeftPart[3] = LeftPart[5];
                        LeftPart[5] = LeftPart[buffer];
                    }
                    case 3 -> { //15
                        buffer = LeftPart[1];
                        LeftPart[1] = LeftPart[2];
                        LeftPart[2] = LeftPart[buffer];
                    }
                    case 4 -> { //16
                        buffer = LeftPart[0];
                        LeftPart[0] = LeftPart[3];
                        LeftPart[3] = LeftPart[buffer];
                    }
                    case 5 -> { //17
                        buffer = LeftPart[4];
                        LeftPart[4] = LeftPart[5];
                        LeftPart[5] = LeftPart[buffer];
                    }
                    case 6 -> { //18
                        buffer = LeftPart[6];
                        LeftPart[6] = LeftPart[7];
                        LeftPart[7] = LeftPart[buffer];
                    }
                    case 7, 15 -> { //19, 27
                        buffer = LeftPart[8];
                        LeftPart[8] = LeftPart[10];
                        LeftPart[10] = LeftPart[buffer];
                    }
                    case 8 -> { //20
                        buffer = LeftPart[10];
                        LeftPart[10] = LeftPart[11];
                        LeftPart[11] = LeftPart[buffer];
                    }
                    case 9 -> { //21
                        buffer = LeftPart[7];
                        LeftPart[7] = LeftPart[9];
                        LeftPart[9] = LeftPart[buffer];
                    }
                    case 10 -> { //22
                        buffer = LeftPart[2];
                        LeftPart[2] = LeftPart[6];
                        LeftPart[6] = LeftPart[buffer];
                    }
                    case 11 -> { //23
                        buffer = LeftPart[0];
                        LeftPart[0] = LeftPart[1];
                        LeftPart[1] = LeftPart[buffer];
                    }
                    case 12 -> { //24
                        buffer = LeftPart[0];
                        LeftPart[0] = LeftPart[2];
                        LeftPart[2] = LeftPart[buffer];
                    }
                    case 13 -> { //25
                        buffer = LeftPart[3];
                        LeftPart[3] = LeftPart[4];
                        LeftPart[4] = LeftPart[buffer];
                    }
                    case 14 -> { //26
                        buffer = LeftPart[5];
                        LeftPart[5] = LeftPart[7];
                        LeftPart[7] = LeftPart[buffer];
                    }
                }
            }
        }
        return LeftPart;
    }
    public static byte xor (byte A, byte B){
        return (byte) (A == B ? 1 : 0);
    }
    public static byte[] BlockXOR_LEFT (byte[] FirstBlock, byte[] SecondBlock){
        byte[] output = new byte[12];
        for (int i = 0; i < 12; i++){
            output[i] = xor(FirstBlock[i], SecondBlock[i]);
        }
        return output;
    }
    public static byte[] AddKey (byte[] Block, byte[] key){
        byte[] output = new byte[12];
        for (int i = 0; i < 12; i++) output[i] = xor(Block[i], key[i]);
        return output;
    }
}