package br.usp.each.saeg.jaguar.example;

/**
 * A very simple code used as motivational example
 *
 */
public class Avg
{
    public static float avg(float[] array)
    {
        int i = 0;
        float avg = 0;
        float sum = 0;
        float length = array.length;
        while(i < length)
        {
            sum += array[i];
            i++;
        }
        
        if(length > 0)
        {
            avg = (sum / length);
        }
        return avg;        
    }
}