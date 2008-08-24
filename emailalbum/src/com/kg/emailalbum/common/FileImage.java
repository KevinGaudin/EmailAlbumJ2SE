/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kg.emailalbum.common;

import java.awt.image.BufferedImage;

/**
 *
 * @author gaudin
 */
public class FileImage {
    private BufferedImage image = null;
    private String fileName = null;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
