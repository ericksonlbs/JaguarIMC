package br.usp.each.saeg.jaguar.example;

/**
 * A very simple code used as motivational example
 *
 */
public class IMC
{
    public static int imc(double peso, double altura)
    {        
        double imc;
        int nivel;
        imc = peso/(altura*altura);
        if (imc < 18.5)
        {
            nivel = 0;
        }
        else if (imc < 25)
        {
            nivel = 1;
        }
        else if (imc < 30)
        {
            nivel = 2;
        }
        else if (imc < 35)
        {
            nivel = 3;
        }
        else if (imc > 40)
        {
            nivel = 4;
        }
        else 
        {
            nivel = 5;
        }
        return nivel;
    }
}