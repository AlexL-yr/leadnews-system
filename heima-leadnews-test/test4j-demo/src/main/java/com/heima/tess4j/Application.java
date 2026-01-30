package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.io.File;

public class Application {
    /**
     * recognize words in image
     * @param args
     */
    public static void main(String[] args) throws TesseractException {

        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("E:\\app-webForleadnews\\testdata");

        tesseract.setLanguage("chi_sim+eng");

        File file = new File("C:\\Users\\LuYiR\\Desktop\\9bd427d8faca05687c261d7426ae8be2.png");
        String result = tesseract.doOCR(file);

        System.out.println("the result is "+ result.replaceAll("\\r|\\n","-"));
    }
}
