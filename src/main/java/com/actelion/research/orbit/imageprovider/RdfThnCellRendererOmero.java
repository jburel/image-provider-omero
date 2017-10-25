/*
 *     Orbit, a versatile image analysis software for biological image-based quantification.
 *     Copyright (C) 2009 - 2017  Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.actelion.research.orbit.imageprovider;

import com.actelion.research.orbit.beans.RawDataFile;
import com.actelion.research.orbit.gui.IFileListCellRenderer;
import com.actelion.research.orbit.utils.RawUtilsCommon;

import javax.media.jai.RenderedOp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class RdfThnCellRendererOmero extends JLabel implements IFileListCellRenderer {

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("dd.MM.yyyy");
        }
    };
    public static final Map<Integer, ImageIcon> iconHash = new ConcurrentHashMap<>();

    private final long maxHashSize = 500;
    private int maxTitleLength = 30;
    private int iconWidth = 200;
    protected JList list = null;
    protected ImageIcon dummyThn = new ImageIcon(new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB));
    // private static int numThnThreads = Runtime.getRuntime().availableProcessors();
    // private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private ImageProviderOmero imageProviderOmero;

    protected final AtomicLong timeOut = new AtomicLong(0L);

    public RdfThnCellRendererOmero(ImageProviderOmero imageProviderOmero) {
        this.imageProviderOmero = imageProviderOmero;
        dummyThn.getImage().getGraphics().setColor(Color.darkGray);
        dummyThn.getImage().getGraphics().fillRect(0, 0, dummyThn.getIconWidth(), dummyThn.getIconHeight());
    }

    public void refreshThnWorker() {
        iconHash.clear();
        timeOut.set(System.currentTimeMillis());
    }

    @Override
    public void clearIconHash() {
        RdfThnCellRendererOmero.iconHash.clear();
    }


    public Component getListCellRendererComponent(
            JList list,
            Object value,            // value to display
            int index,               // cell index
            boolean isSelected,      // is the cell selected
            boolean cellHasFocus)    // the list and the cell have the focus
    {

        this.list = list;
        String s = value.toString();
        String toolTip = value.toString();
        boolean hasLinkedChannels = false;
        if (value instanceof RawDataFile) {
            RawDataFile rdf = (RawDataFile) value;
            hasLinkedChannels = rdf.isFlagBit(RawDataFile.Flag_HAS_LINKED_CHANNELS);
            s = rdf.getFileName()
            //	+" ("+String.format("%1$.2f", fSize)+" kb)"+
            //		" ["+dateFormat.format(rdf.getReferenceDate())+"]"
            ;

            toolTip = rdf.getFileName()
                    + " (" + RawUtilsCommon.formatFileSize(rdf.getFileSize()) + ") " +
                    " [" + dateFormat.get().format(rdf.getReferenceDate()) + "]"
            ;

            // Icon
            ImageIcon icon = dummyThn;
            Integer fileKey = rdf.getRawDataFileId();
            if (RdfThnCellRendererOmero.iconHash.containsKey(fileKey)) {
                icon = RdfThnCellRendererOmero.iconHash.get(fileKey);
            } else {
                try {
                    if (RdfThnCellRendererOmero.iconHash.size() <= maxHashSize) {
                        RdfThnCellRendererOmero.iconHash.put(fileKey, dummyThn);
                        executor.execute(new ThnRunny(fileKey));
                    }
                } catch (Throwable re) {
                    // can happen while shutting down the executor, but no problem
                }

            }
            if (icon != null) {
                int w = iconWidth;
                if (w <= 0) w = iconWidth;
                //int h = (int)((w/(double)icon.getIconWidth()) * ((double)icon.getIconHeight()/icon.getIconWidth()));
                int h = (int) (w * 0.75d);
                if (h > 0) {
                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    int iw = w;
                    int ih = h;
                    if (icon.getIconWidth() > icon.getIconHeight()) {  // adjust icon height
                        ih = (int) (w * ((double) icon.getIconHeight() / icon.getIconWidth()));
                    } else {    // adjust icon width
                        iw = (int) (h * ((double) icon.getIconWidth() / icon.getIconHeight()));
                    }
                    int iOffsX = 0;
                    int iOffsY = 0;
                    if (iw < w) {
                        iOffsX = (w - iw) / 2;
                    }
                    if (ih < h) {
                        iOffsY = (h - ih) / 2;
                    }
                    img.getGraphics().setColor(Color.darkGray);
                    img.getGraphics().fillRect(0, 0, w, h);
                    img.getGraphics().drawImage(icon.getImage(), iOffsX, iOffsY, iw, ih, this);

                    ImageIcon scaledIcon = new ImageIcon(img);

                    //ImageIcon scaledIcon = new ImageIcon(icon.getImage().getScaledInstance(w , -1, Image.SCALE_FAST));
                    setIcon(scaledIcon);
                    //setPreferredSize(new Dimension(this.getWidth(), scaledIcon.getImage().getHeight(list)));
                    setPreferredSize(new Dimension(getWidth(), /*scaledIcon.getIconHeight()*/h + 25));
                    //setSize(new Dimension(w,w));
                    //setMinimumSize(new Dimension(w,w));
                    //setMaximumSize(new Dimension(w,w));
                }
            } else setIcon(null);

        }

        if (s.length() > maxTitleLength) {
            //s = s.substring(0,maxTitleLength)+"...";
            s = "<html><body><font size=\"2\">" + s + "</font></body></html>";
        }
        setText(s);
        setToolTipText(toolTip);
        setBorder(new EmptyBorder(3, 3, 3, 3));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            if (hasLinkedChannels) setBackground(Color.blue);
        } else {
            //setBackground(list.getBackground());
            setBackground(Color.darkGray);
            setForeground(list.getForeground());
            if (hasLinkedChannels) setBackground(Color.green);
        }
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setHorizontalTextPosition(JLabel.CENTER);
        setVerticalTextPosition(JLabel.BOTTOM);
        //setHorizontalAlignment(JLabel.LEFT);

        setOpaque(true);
        return this;
    }


    public int getIconWidth() {
        return iconWidth;
    }


    public void setIconWidth(int iconWidth) {
        this.iconWidth = iconWidth;
        //firePropertyChange("icon", null, iconWidth);
    }


    public ExecutorService getExecutor() {
        return executor;
    }


    public void close() {
        timeOut.set(System.currentTimeMillis());
        getExecutor().shutdown();
    }


    public class ThnRunny implements Runnable {

        private Integer fileKey = null;
        private long startTime = System.currentTimeMillis();

        public ThnRunny(Integer fileKey) {
            this.fileKey = fileKey;
        }

        public void run() {
            try {

                if (Thread.interrupted()) return;
                if (timeOut.get() > startTime) return;

                //ImageIcon icon = TiffConverter.getImageIcon(fileKey, 0, 0);
                RawDataFile rdf = new RawDataFile();
                rdf.setRawDataFileId(fileKey);
                RenderedImage img = imageProviderOmero.getThumbnail(rdf);
                ImageIcon icon = new ImageIcon(RenderedOp.wrapRenderedImage(img).getAsBufferedImage());

                if ((RdfThnCellRendererOmero.iconHash != null) && (icon != null)) {
                    RdfThnCellRendererOmero.iconHash.put(fileKey, icon);
                    //setPreferredSize(new Dimension(getWidth(),icon.getImage().getHeight(list)));
                    //list.revalidate();
                    list.repaint();
                }
            } catch (Throwable t) {
                //can happen (e.g. outofmemory) but no problem
                //System.out.println("icon error: "+t);
                //t.printStackTrace();
            }
        }
    }

}
