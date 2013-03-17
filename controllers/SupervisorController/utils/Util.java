package utils;

import java.nio.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: annapawlicka
 * Date: 20/12/2012
 * Time: 23:11
 * Utilities is a collection of methods that implement statistical/mathematical functions.
 */

public class Util {

    /**
     * Normal probability density function
     * Y = normpdf(X,mu,sigma) computes the pdf at each of the values in X using
     * the normal distribution with mean mu and standard deviation sigma.
     * X, mu, and sigma can be vectors, matrices, or multidimensional arrays that all have the same size.
     * @param x
     * @param mean
     * @param sd
     * @return
     */
    public static double normPdf(double x, double mean, double sd) {
        double y = (1 / (sd * Math.sqrt(2 * Math.PI))) * Math.exp(-Math.pow(x - mean, 2) / (2 * sd * sd));
        //System.out.println("normPDF: "+y);
        return y;
    }

    /**
     * Product of array elements
     * @param a
     * @return
     */
    public static double prod(double[] a) {
        double p = 1;
        for (int i = 0; i < a.length; i++)
            if (a[i] != 0)
                p *= a[i];

        return p;
    }

    /**
     * Copy one array into another
     * @param a
     * @return
     */
    public static float[] copy(float[] a) {
        float[] b = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    /* Sum of array elements */
    public static double sum(double in[]) {
        double sum = 0;
        for (int i = 0; i < in.length; i++)
            sum += in[i];

        return sum;
    }

    /**
     * Modulus after division
     * M = mod(X,Y) if Y ~= 0, returns X - n.*Y where n = floor(X./Y).
     * If Y is not an integer and the quotient X./Y is within roundoff error of an integer, then n is that integer.
     * @param x
     * @param y
     * @return
     */
    public static double mod(double x, double y)
    {
        double result = x % y;
        if (result < 0)
        {
            result += y;
        }
        return result;
    }

    /**
     * Create an empty binary string of given length
     * @param x
     * @return
     */
    public static String getEmptyBinaryString(int x){

        String str = "0";
        for(int i=0; i<x-1; i++){
           str += "0";
        }
        return str;
    }

    /**
     * Computes the arithmetic mean of a set of values. Uses the definitional formula: mean = sum(x_i) / n
     * @param values    An array of doubles
     * @return          Arithmetic mean (double)
     */
    public static double mean(double[] values){
        double sum = 0.0;
        for(int i=0; i<values.length; i++){
            sum+=values[i];
        }
        return (sum) / values.length;
    }

    /**
     * Makes a three's rule to normalize a value in a 0..1 interval
     *
     * @param min   lowest saturation point. Values below this will return 0
     * @param max   highest saturation point. Values above this will return 1
     * @param value
     * @return      a double value between 0 and 1
     * @throws      Exception
     */
    public static double normalize(double min, double max, double value) throws Exception {
        return normalize(min, max, value, 0, 1);
    }

    /**
     * Makes a three's rule to normalize a value in a 0..1 interval
     *
     * @param min   lowest saturation point. Values below this will return 0
     * @param max   highest saturation point. Values above this will return 1
     * @param value
     *
     * @param start beginning of the normalization interval
     * @param end   ending of the normalization interval
     *
     * @return      a double value between start and end
     * @throws      Exception
     *
     */
    public static double normalize(double min, double max, double value,
                                double start, double end) throws Exception {
        if (max <= min) throw new Exception("Min cannot be higher than max. Values entered are not valid.");
        if (end <= start) throw new Exception("End cannot be higher than start. Values entered are not valid.");
        if (value >= max) return end;
        if (value <= min) return start;

        double i1 = max - min;
        double i2 = end - start;
        double y = (value - min) * i2 / i1;
        return y + start;

    }

    /**
     * float2Byte method - writes floats to byte array
     * @param inData    float array
     * @return          byte array
     */
    public static byte[] float2Byte(float[] inData) {
        int j=0;
        int length=inData.length;
        byte[] outData=new byte[length*4];
        for (int i=0;i<length;i++) {
            int data=Float.floatToIntBits(inData[i]);
            outData[j++]=(byte)(data>>>24);
            outData[j++]=(byte)(data>>>16);
            outData[j++]=(byte)(data>>>8);
            outData[j++]=(byte)(data>>>0);
        }
        return outData;
    }

    /**
     * bytearray2float method - converts bytes into a float
     * @param b     byte array
     * @return      float value
     */
    public static float bytearray2float(byte[] b) {
        ByteBuffer buf = ByteBuffer.wrap(b);
        return buf.getFloat();
    }

    /**
     * doubleToByteArray - converts float array into a byte array
     * @param inData
     * @return
     */
    public static byte[] doubleToByteArray(double[] inData) {
        int j=0;
        int length=inData.length;
        byte[] outData = new byte[length*8];
        for (int i=0;i<length;i++) {
            int data=(int)Double.doubleToLongBits(inData[i]);
            outData[j++]=(byte)(data>>>24);
            outData[j++]=(byte)(data>>>16);
            outData[j++]=(byte)(data>>>8);
            outData[j++]=(byte)(data>>>0);
        }
        return outData;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

}