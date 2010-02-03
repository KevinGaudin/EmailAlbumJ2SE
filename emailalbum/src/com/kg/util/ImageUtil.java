/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kg.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author gaudin
 */
public class ImageUtil {

    public final static String jpeg = "jpeg";
    public final static String jpg = "jpg";
    public final static String gif = "gif";
    public final static String tiff = "tiff";
    public final static String tif = "tif";

    /**
     * Resize an image. Keeps image proportions.
     * 
     * @param img
     *            the source image
     * @param newW
     *            the new width
     * @param newH
     *            the new height
     * 
     * @return A new BufferedImage containing the resized image. The original proportions are preserved.
     */
    public static BufferedImage resize(BufferedImage img, Dimension dim) {
        double newW = dim.getWidth();
        double newH = dim.getHeight();        
        if (img != null && (newH != 0 && newW != 0)) {
            int w = img.getWidth();
            int h = img.getHeight();
            float imageFactor = (float) w / (float) h;
            float newImageFactor = (float) newW / (float) newH;

            // Calculates wich dimension should be used to preserve image proportions.
            if (newImageFactor <= imageFactor) {
                newH = (int) (newW / imageFactor);
            } else if (newImageFactor > imageFactor) {
                newW = (int) (newH * imageFactor);
            }
            
            // Don't zoom too much
            double zoom = newH / h;
            if(zoom > 1.25) {
                newH = 1.25 * h;
                newW = 1.25 * w;
            }

            BufferedImage dimg = new BufferedImage((int) newW, (int) newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dimg.createGraphics();
            // Best image quality, worse performance.
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(img, 0, 0, (int) newW, (int) newH, 0, 0, w, h, null);
            g.dispose();
            return dimg;
        }
        return img;
    }
    /*
     * Get the extension of a file.
     */

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
    
    public static FileFilter getJpegFilter() {
        return new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                String extension = ImageUtil.getExtension(f);
                if (extension != null) {
                    if (extension.equalsIgnoreCase(ImageUtil.jpeg) ||
                            extension.equalsIgnoreCase(ImageUtil.jpg)) {
                        return true;
                    } else {
                        return false;
                    }
                }

                return false;
            }

            public String getDescription() {
                return "Jpeg Pictures";
            }
        };
    } 
}
