/*
 *   Here is the code to replace images in PDF, in Java. 
 *   It will replace all the images in pdf with a red image. 
 *
 *  This code is property of Rocchina Romano.
 *
 *  Copyright Rocchina Romano 2020
 *
 *  class SostituisciImmaginiPDF (main class)
 */
package sostituisciimmaginipdf;

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
public class SostituisciImmaginiPDF {

    //percorso immagine sostitutiva:
    private static final String red = "./src/sostituisciimmaginipdf/red.jpg";
    
    //percorso PDF di input:
    private static final String input = "./src/sostituisciimmaginipdf/input.pdf";
   
    
    
    //percorso PDF di output:
    private static final String output = "./src/sostituisciimmaginipdf/output.pdf";
    
    //path dei file temp
    private static final String pathTemp = "./src/sostituisciimmaginipdf/temp/";
    
    //pdf di appoggio
    private static String appoggio = input;
    
    private static int numPdf = 1;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //sostituisce tutte le immagini del PDF con un'immagine rossa
        try {
            int numImmagini = contaImmaginiPDF();
            System.out.println("Il PDF ha " + numImmagini + " immagini!");
            //Se il pdf ha immagini, procediamo con l'estrazione delle immagini
            //e la sostituzione 
            if(numImmagini == 0){
                System.out.println("Il PDF non contiene immagini!");
            }else{
                //procediamo con la sostituzione delle immagini
                sostituisciImmagini();
            }
        } catch (IOException | DocumentException ex) {
            System.err.println(ex.getMessage());
        }
        //Alla fine svuotare la cartella dei file temporanei
        svuotaCartella(pathTemp);
    }

    private static int contaImmaginiPDF() throws IOException {
        //Verifica se il PDF contiene o meno immagini
        PdfReader reader = new PdfReader(input);
        System.out.println("Il PDF ha " + reader.getNumberOfPages() 
                + " pagine.");
        int conta = 0;
        for (int i = 1; i <= reader.getXrefSize(); i++) {
            PdfObject obj = reader.getPdfObject(i);
            if(obj != null && obj.isStream()){
                PRStream stream = (PRStream)obj;
                PdfObject type = stream.get(PdfName.SUBTYPE);
                if(type != null && type.toString().equals(PdfName.IMAGE.toString())){
                    //E' un'immagine, incrementiamo il contatore
                    conta++;
                }
            }
        }
        reader.close();
        return conta;
    }

    private static void sostituisciImmagini() throws IOException, DocumentException {
        PdfReader reader = new PdfReader(input);
        int numeroPag = reader.getNumberOfPages();
        reader.close();
        
        for(int i = numeroPag; i >= 1; i--){
                System.out.println("Pagina esaminata: " + i);
                sostituisci(i);
                System.out.println("SOSTITUZIONE EFFETTUATA.");
        }
        
    }

   private static void sostituisci(int pag) {
        try {
            String pdfAppoggio;
            if(pag == 1){
                //ultima iterazione
                pdfAppoggio = output;
            }else{
                pdfAppoggio = pathTemp + "out_" + numPdf + ".pdf";
            }
            PdfReader reader = new PdfReader(appoggio);
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(pdfAppoggio));
            PdfWriter writer = stamper.getWriter();
            writer.setCompressionLevel(0);
            PdfDictionary pg = reader.getPageN(pag);
            PdfDictionary res = (PdfDictionary)PdfReader.getPdfObject(pg.get(PdfName.RESOURCES));
            PdfDictionary xobj = (PdfDictionary)PdfReader.getPdfObject(res.get(PdfName.XOBJECT));
            if(xobj != null){
                int keys = xobj.getKeys().size();
                Set<PdfName> setKey = xobj.getKeys();
                System.out.println(setKey.toString());
                int i = 0;
                String[] resImage = new String[keys];
                for(PdfName name : setKey){
                    resImage[i] = name.toString();
                    i++;
                }
                    
                //Esamino l'array resImage
                //scorro il flusso setKey
                //cerco l'oggetto di setKey che ha quel nome e lo sostituisco
                
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
                                    Image img = Image.getInstance(red);
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
            appoggio = pdfAppoggio;
            numPdf++;
        } catch (IOException | DocumentException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void svuotaCartella(String pathTemp) {
        File directory = new File(pathTemp);
        File[] files = directory.listFiles();
        for(File file : files){
            file.delete();
        }
    }
    
}