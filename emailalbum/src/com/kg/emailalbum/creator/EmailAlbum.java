/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kg.emailalbum.creator;

import com.kg.emailalbum.common.FileImage;
import com.kg.emailalbum.creator.ui.ImagePreview;
import com.kg.util.ImageUtil;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

/**
 *
 * @author gaudin
 */
public class EmailAlbum {

    private static final Dimension IMAGE_SIZE = new Dimension(1024, 768);
    private static final String PICTURES_ARCHIVE_PATH = "com/kg/emailalbum/viewer/pictures/";
    private static final String[] VIEWER_FILES = {"com/kg/emailalbum/viewer/ui/EmailAlbum.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$1.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$2.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$3.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$4.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$5.class",
        "com/kg/emailalbum/viewer/ui/EmailAlbum$6.class",
        "com/kg/util/ImageUtil.class",
        "com/kg/util/ImageUtil$1.class"
    };
    private static final String CONTENT_FILE = "com/kg/emailalbum/viewer/pictures/content";
    private JarOutputStream archive = null;
    private StringBuffer contentFile = new StringBuffer();

    public EmailAlbum(File[] files) {

        for (int i = 0; i < files.length; i++) {
            File file = files[i];


            if (file.exists() && file.isFile()) {
                FileImage picture = new FileImage();
                picture.setFileName(file.getName());
                try {
                    BufferedImage pic = ImageIO.read(file);
                    if (pic != null) {
                        if (pic.getWidth() > IMAGE_SIZE.getWidth() || pic.getHeight() > IMAGE_SIZE.getHeight()) {
                            pic = ImageUtil.resize(pic, IMAGE_SIZE);
                        }

                        picture.setImage(pic);
                        addToArchive(picture);
                        contentFile.append(picture.getFileName()).append("\n");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (archive != null) {
            try {
                writeContentFile();
                archive.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (archive != null) {
            archive.close();
        }
    }

    private void addToArchive(FileImage picture) throws IOException {
        getArchive().putNextEntry(new ZipEntry(PICTURES_ARCHIVE_PATH + picture.getFileName()));
        ImageIO.write(picture.getImage(), "jpeg", getArchive());
        getArchive().closeEntry();
    }

    private JarOutputStream getArchive() throws IOException {
        if (archive == null) {
            Calendar today = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmm");
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, com.kg.emailalbum.viewer.ui.EmailAlbum.class.getName());
            archive = new JarOutputStream(new FileOutputStream("./" + df.format(today.getTime()) + "-album.jar"), manifest);
            for (int i = 0; i < VIEWER_FILES.length; i++) {
                ZipEntry entry = new ZipEntry(VIEWER_FILES[i]);
                archive.putNextEntry(entry);
                InputStream viewerIS = getClass().getResourceAsStream("/" + VIEWER_FILES[i]);
                byte[] viewer = new byte[viewerIS.available()];
                int bytesread = 0;
                while (viewerIS.available() > 0) {
                    bytesread = viewerIS.read(viewer, bytesread, viewerIS.available());
                }
                archive.write(viewer);
                archive.closeEntry();
            }

        }
        return archive;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFileChooser fileSelector = null;
        if (args.length > 0) {
            System.out.println("Using base directory : " + args[0]);
            fileSelector = new JFileChooser(args[0]);
        } else {
            fileSelector = new JFileChooser();
        }
        fileSelector.setMultiSelectionEnabled(true);
        fileSelector.addChoosableFileFilter(ImageUtil.getJpegFilter());
        
        fileSelector.setAccessory(new ImagePreview(fileSelector));
        
        int returnVal = fileSelector.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            new EmailAlbum(fileSelector.getSelectedFiles());
        }
    }

    private void writeContentFile() throws IOException {
        getArchive().putNextEntry(new ZipEntry(CONTENT_FILE));
        getArchive().write(contentFile.toString().getBytes());
        getArchive().closeEntry();
    }
}
