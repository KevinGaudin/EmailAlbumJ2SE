package com.kg.emailalbum.creator.ui;

import com.kg.util.ImageUtil;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

public class ImagePreview extends JComponent
        implements PropertyChangeListener {
    private ResourceBundle bundle = java.util.ResourceBundle.getBundle(this.getClass().getName());
    
    ImageIcon[] thumbnails = null;
    Rectangle[] commentIconsPositions = null;
    Map comments = new HashMap();
    File[] files = null;
    int nbcols = 1;
    int nbrows = 1;
    private Map cache = new HashMap();
    private ImageIcon iconEditComment = null;
    private ImageIcon iconAddComment = null;
    private ImageIcon iconDeleteComment = null;
    private ImageIcon iconComment = null;

    public ImagePreview(final JFileChooser fc) {
        setPreferredSize(new Dimension(500, 500));
        fc.addPropertyChangeListener(this);

        iconEditComment = new ImageIcon(getClass().getResource("/com/kg/emailalbum/creator/ui/comment_edit.gif"));
        iconAddComment = new ImageIcon(getClass().getResource("/com/kg/emailalbum/creator/ui/comment_add.gif"));
        iconDeleteComment = new ImageIcon(getClass().getResource("/com/kg/emailalbum/creator/ui/comment_delete.gif"));
        iconComment = new ImageIcon(getClass().getResource("/com/kg/emailalbum/creator/ui/comment.gif"));

        this.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent arg0) {
                int x = arg0.getX();
                int y = arg0.getY();
                int colwidth = getWidth() / nbcols;
                int colnum = x / colwidth;
                System.out.println("Col : " + colnum);

                int rowheight = getHeight() / nbrows;
                int rownum = y / rowheight;
                System.out.println("Row : " + rownum);

                int filenumber = nbcols * rownum + colnum;
                System.out.println("Fichier : " + files[filenumber].getName());


                if (commentIconsPositions[filenumber].contains(x, y)) {
                    getUserComment(filenumber);
                } else {
                    File[] newSelection = new File[files.length - 1];
                    int j = 0;
                    for (int i = 0; i < files.length; i++) {
                        if (i != filenumber) {
                            newSelection[j] = files[i];
                            j++;
                        }
                    }
                    fc.setSelectedFiles(newSelection);
                }
            }

            public void mousePressed(MouseEvent arg0) {
            }

            public void mouseReleased(MouseEvent arg0) {
            }

            public void mouseEntered(MouseEvent arg0) {
            }

            public void mouseExited(MouseEvent arg0) {
            }
        });
    }

    private void getUserComment(int fileIndex) {
        String oldCaption = (String) comments.get(files[fileIndex]);

        String newCaption = (String) JOptionPane.showInputDialog(
                null,
                bundle.getString("input.caption"), oldCaption);
        
        if(newCaption == null) return;
        
        newCaption = newCaption.trim();
        if (newCaption.length() == 0) {
            if(oldCaption != null)
            {
                comments.remove(files[fileIndex]);
                repaint();
            }
        } else {
            if(oldCaption == null || !oldCaption.equals(newCaption))
            {
                comments.put(files[fileIndex], newCaption);
                repaint();
            }
        }
    }

    public void loadImages() {
        if (files == null || files.length == 0) {
            return;
        }
        thumbnails = new ImageIcon[files.length];
        commentIconsPositions = new Rectangle[files.length];
        nbcols = (int) Math.sqrt(files.length);
        nbrows = (int) Math.ceil((float) files.length / (float) nbcols);
        int iconWidth = getWidth() / nbcols - 2;
        int iconHeight = getHeight() / nbrows - 2;
        BufferedImage tmpIcon = null;
        for (int i = 0; i < files.length; i++) {
            try {
                tmpIcon = loadImage(files[i]);
                if (tmpIcon.getWidth() > iconWidth || tmpIcon.getHeight() > iconHeight) {
                    thumbnails[i] = new ImageIcon(ImageUtil.resize(tmpIcon, new Dimension(iconWidth, iconHeight)), files[i].getName());
                } else {
                    thumbnails[i] = new ImageIcon(tmpIcon, files[i].getName());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            paintComponent(getGraphics());
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
            files = (File[]) (((JFileChooser) e.getSource()).getSelectedFiles());
            if (isShowing()) {
                loadImages();
            //repaint();
            }
        }
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, getWidth(), getHeight());
        if (thumbnails == null || thumbnails.length == 0) {
            loadImages();
        }
        if (thumbnails != null) {
            for (int i = 0; i < thumbnails.length; i++) {
                if (thumbnails[i] != null) {
                    int colWidth = (getWidth() / nbcols);
                    int x = (i % nbcols) * colWidth + colWidth / 2 - thumbnails[i].getIconWidth() / 2;
                    int rowHeight = getHeight() / nbrows;
                    int y = (i / nbcols) * rowHeight + rowHeight / 2 - thumbnails[i].getIconHeight() / 2;

                    g2.setPaintMode();
                    thumbnails[i].paintIcon(this, g2, x, y);
                    g2.setColor(new Color(0, 0, 0, 200));
                    g2.fillRect(x, y, thumbnails[i].getIconWidth(), 13);
                    g2.setColor(Color.white);
                    Map fontAttr = new HashMap();
                    fontAttr.put(TextAttribute.FAMILY, "Dialog");
                    fontAttr.put(TextAttribute.SIZE, new Float(10));
                    fontAttr.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
                    g2.setFont(Font.getFont(fontAttr));
                    g2.drawString(thumbnails[i].getDescription(), x + 2, y + 11);

                    ImageIcon iconCaption = iconEditComment;
                    if (comments.get(files[i]) != null) {
                        iconCaption = iconComment;
                    }
                    Rectangle commentRect = new Rectangle(x + thumbnails[i].getIconWidth() - iconCaption.getIconWidth(),
                            y + thumbnails[i].getIconHeight() - iconCaption.getIconHeight(),
                            iconCaption.getIconWidth(),
                            iconCaption.getIconHeight());
                    g2.drawImage(iconCaption.getImage(),
                            commentRect.x,
                            commentRect.y,
                            this);
                    commentIconsPositions[i] = commentRect;
                }

            }
        }
    }

    private BufferedImage loadImage(File file) throws IOException {
        BufferedImage image = (BufferedImage) cache.get(file);
        if (image == null) {
            image = ImageUtil.resize(ImageIO.read(file), getSize());
            System.gc();
            cache.put(file, image);
        }
        return image;
    }
}
