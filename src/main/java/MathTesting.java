import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
public class MathTesting {
    public static void main (String[] args) throws IOException {
        byte[] bitmap = ReadBytes("src/main/Photos/output.bmp");
        byte[] targeted_bitmap = new byte[bitmap.length-54];
        System.arraycopy(bitmap, 54, targeted_bitmap, 0, bitmap.length-54);
        float[] ready_vector = new float[targeted_bitmap.length/3];
        //вычисляем интенсивность
        for (int pixel_index = 0; pixel_index < ready_vector.length; pixel_index++){
            ready_vector[pixel_index] = ((float) (targeted_bitmap[pixel_index*3] * 0.0721))+ //BLUE
                    ((float) (targeted_bitmap[pixel_index*3+1] * 0.7154))+ //GREEN
                    ((float) (targeted_bitmap[pixel_index*3+2] * 0.2125)); //RED
        }
        try(FileWriter writer = new FileWriter("src/main/NumericData/source.txt", false))
        {
            for (int i = 0; i < ready_vector.length; i++){
                writer.write(String.valueOf(ready_vector[i]));
                writer.append('\n');
            }
            writer.flush();}
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
        try(FileWriter writer2 = new FileWriter("src/main/NumericData/source1.txt", false))
        {
            for (int i = 1; i < ready_vector.length; i++){
                writer2.write(String.valueOf(ready_vector[i]));
                writer2.append('\n');
            }
            writer2.write(String.valueOf(ready_vector[ready_vector.length-1]));
            writer2.flush();
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    public static byte[] ReadBytes (String source) throws IOException {
        File input_file = new File(source);
        return Files.readAllBytes(input_file.toPath());
    }

}
