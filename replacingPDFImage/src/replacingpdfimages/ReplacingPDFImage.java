/*
 *   Here is the code to replace all images in PDF files with a red image,
 *   using the iText library for Java.
 *
 *  This code is property of Rocchina Romano.
 *
 *  Copyright Rocchina Romano 2020
 *
 *  class ReplacingPDFImage (main class)
 */
package replacingpdfimages;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PRIndirectReference;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author Rocchina
 */
public class ReplacingPDFImage {

    //red image's path:
    private static final String redImage = "./src/replacingpdfimages/red.jpg";
    
    //Input PDF's path:
    private static final String input = "./src/replacingpdfimages/input.pdf";
   
    //Output PDF's path
    private static final String output = "./src/replacingpdfimages/output.pdf";
    
    //Temporary files's path
    private static final String pathTemp = "./src/replacingpdfimages/temp/";
    
    //Support pdf file
    private static String supportPDFfile = input;
    
    private static int numPdf = 1;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Replacing all images in PDF files with a red image
        try {
            int numImages = countsPDFimages();
            System.out.println("The PDF has " + numImages + " images!");
            
            if(numImages == 0){
                //The PDF file hasn't images
                System.out.println("The PDF doesn't contains images!");
            }else{
                //The PDF file contains images, then we replace the PDF's images
                //with a red image
                replaceImages();
            }
        } catch (IOException | DocumentException ex) {
            System.err.println(ex.getMessage());
        }
        //At the end, we empty the temporary files folder.
        emptyFolder(pathTemp);
    }

    private static int countsPDFimages() throws IOException {
        //Check if the PDF contains images
        PdfReader reader = new PdfReader(input);
        System.out.println("The PDF has " + reader.getNumberOfPages() 
                + " pages.");
        int count = 0;
        for (int i = 1; i <= reader.getXrefSize(); i++) {
            PdfObject obj = reader.getPdfObject(i);
            if(obj != null && obj.isStream()){
                PRStream stream = (PRStream)obj;
                PdfObject type = stream.get(PdfName.SUBTYPE);
                if(type != null && type.toString().equals(PdfName.IMAGE.toString())){
                    //It is an image, then we increase the "count" variable
                    count++;
                }
            }
        }
        reader.close();
        return count;
    }

    private static void replaceImages() throws IOException, DocumentException {
        PdfReader reader = new PdfReader(input);
        int numPag = reader.getNumberOfPages();
        reader.close();
        
        for(int i = numPag; i >= 1; i--){
                System.out.println("Current page: " + i);
                replace(i);
                System.out.println("REPLACEMENT MADE.");
        }
        
    }

   private static void replace(int pag) {
        try {
            String supportPDF;
            if(pag == 1){
                //last iteration
                supportPDF = output;
            }else{
                supportPDF = pathTemp + "out_" + numPdf + ".pdf";
            }
            PdfReader reader = new PdfReader(supportPDFfile);
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(supportPDF));
            PdfWriter writer = stamper.getWriter();
            writer.setCompressionLevel(0);
            PdfDictionary pg = reader.getPageN(pag);
            PdfDictionary res = (PdfDictionary)PdfReader.getPdfObject(pg.get(PdfName.RESOURCES));
            PdfDictionary xobj = (PdfDictionary)PdfReader.getPdfObject(res.get(PdfName.XOBJECT));
            if(xobj != null){
                //I get all the xobject keys, to trace the PDF images
                int keys = xobj.getKeys().size();
                Set<PdfName> setKey = xobj.getKeys();
                System.out.println(setKey.toString());
                int i = 0;
                String[] resImage = new String[keys];
                for(PdfName name : setKey){
                    resImage[i] = name.toString();
                    i++;
                }
                
                for(int j = 0; j <= resImage.length-1; j++){
                    String s = resImage[j];
                    for(PdfName name : setKey){
                        String sPdf = name.toString();
                        if(s.equals(sPdf)){
                            PdfObject obj = xobj.get(name);
                            if(obj.isIndirect()){
                                PdfDictionary tg = (PdfDictionary)PdfReader.getPdfObject(obj);
                                PdfName type = (PdfName)PdfReader.getPdfObject(tg.get(PdfName.SUBTYPE));
                                if(PdfName.IMAGE.equals(type)){
                                    PdfReader.killIndirect(obj);
                                    Image img = Image.getInstance(redImage);
                                    Image imageMask = img.getImageMask();
                                    if(imageMask != null){
                                        writer.addDirectImageSimple(imageMask);
                                    }
                                    writer.addDirectImageSimple(img, (PRIndirectReference)obj);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            stamper.close();
            reader.close();
            supportPDFfile = supportPDF;
            numPdf++;
        } catch (IOException | DocumentException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void emptyFolder(String pathTemp) {
        File directory = new File(pathTemp);
        File[] files = directory.listFiles();
        for(File file : files){
            file.delete();
        }
    }
}