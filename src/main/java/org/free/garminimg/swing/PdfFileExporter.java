/*
 * JGarminImgParser - A java library to parse .IMG Garmin map files.
 *
 * Copyright (C) 2007 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.free.garminimg.swing;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfFileExporter extends FileExporter
{
    private Graphics2D g2;

    private Document document;

    private PageSizeItem pageSize;

    private int dpi=100;

    private static JPanel configurationPanel=null;

    private static FileExporterConfigListener itemListener=null;

    private static PageSizeItem pageSizes[]={
            new PageSizeItem("A3 landscape", PageSize.A3.rotate()),
            new PageSizeItem("A3", PageSize.A3),
            new PageSizeItem("A4 landscape", PageSize.A4.rotate()),
            new PageSizeItem("A4", PageSize.A4),
            new PageSizeItem("A5 landscape", PageSize.A5.rotate()),
            new PageSizeItem("A5", PageSize.A5),
            new PageSizeItem("Letter landscape", PageSize.LETTER.rotate()),
            new PageSizeItem("Letter", PageSize.LETTER),
            new PageSizeItem("Legal letter landscape", PageSize.LEGAL.rotate()),
            new PageSizeItem("Legal letter", PageSize.LEGAL),
    };

    private PdfWriter writer;

    private PdfContentByte cb;

    private PdfTemplate tp;

    public String getExtension()
    {
        return "pdf";
    }

    public void setup(File selectedFile, int width, int height) throws IOException
    {
        document=new Document(pageSize.rectangle);
        try
        {
            writer=PdfWriter.getInstance(document, new FileOutputStream(selectedFile));
        }
        catch(DocumentException e)
        {
            throw new IOException(e.getMessage());
        }
        document.open();
        cb=writer.getDirectContent();
        tp=cb.createTemplate(pageSize.rectangle.width(), pageSize.rectangle.height());
        g2=tp.createGraphics(pageSize.rectangle.width(), pageSize.rectangle.height());
        g2.scale(pageSize.rectangle.width()/width, pageSize.rectangle.height()/height);
    }

    public Graphics2D getG2()
    {
        return g2;
    }

    public void finishSave() throws IOException
    {
        g2.dispose();
        cb.addTemplate(tp, 0, 0);
        document.close();
    }

    public JPanel getConfigurationPanel(FileExporterConfigListener itemListener)
    {
        PdfFileExporter.itemListener=itemListener;
        if(configurationPanel==null)
            configurationPanel=createConfigurationPanel();
        sizeChanged();
        return configurationPanel;
    }

    private JPanel createConfigurationPanel()
    {
        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();

        c.gridx=0;
        c.gridy=0;
        c.weightx=0;
        c.weighty=0;
        c.gridwidth=GridBagConstraints.REMAINDER;
        panel.add(new JLabel("Page size"), c);

        c.gridy++;
        JComboBox pageSizeCombo=new JComboBox(pageSizes);
        panel.add(pageSizeCombo, c);
        pageSize=pageSizes[2];
        pageSizeCombo.setSelectedItem(pageSize);
        pageSizeCombo.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                pageSize=(PageSizeItem)e.getItem();
                sizeChanged();
            }
        });

        c.gridx=0;
        c.gridy++;
        c.weightx=1.0;
        c.fill=GridBagConstraints.HORIZONTAL;
        final JSlider dpiSlider=new JSlider(JSlider.HORIZONTAL, 20, 300, dpi);
        dpiSlider.setMajorTickSpacing(40);
        dpiSlider.setMinorTickSpacing(20);
        dpiSlider.setPaintTicks(true);
        dpiSlider.setPaintTrack(false);
        dpiSlider.setSnapToTicks(true);
        dpiSlider.setPaintLabels(true);
        panel.add(dpiSlider, c);
        dpiSlider.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                dpi=((dpiSlider.getValue()+10)/20)*20;
                sizeChanged();
            }
        });

        return panel;
    }

    protected void sizeChanged()
    {
        float width=pageSize.rectangle.width()*dpi/72;
        float height=pageSize.rectangle.height()*dpi/72;
        itemListener.exportSizeChanged((int)width, (int)height);
    }

    public String toString()
    {
        return "PDF";
    }

    private static class PageSizeItem
    {
        private String txt;

        private Rectangle rectangle;

        public PageSizeItem(String txt, Rectangle rectangle)
        {
            this.txt=txt;
            this.rectangle=rectangle;
        }

        public String toString()
        {
            return txt;
        }
    }

    public float getFontSize()
    {
        return super.getFontSize()*dpi/100.0f;
    }
}
