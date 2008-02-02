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

import org.free.garminimg.ImgFilesBag;
import org.free.garminimg.utils.ImgConstants;
import org.free.garminimg.utils.MapDrawer;
import org.free.garminimg.utils.MapTransformer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class ExportDialog<COORD> extends JDialog implements FileExporterConfigListener
{
    private MapTransformer<COORD> selectionTransformer;

    private MapTransformer<COORD> sampleTransformer;

    private JPanel filler;

    private MapPanel<COORD> selectionMapPanel;

    private MapControlPanel<COORD> selectionControlPanel;

    private int width=100;

    private int height=100;

    private MapPanel<COORD> mapSamplePanel;

    private File file;

    private FileExporter fileExporter;

    private JLabel fileLabel;

    private JComboBox formatsSel;

    private JButton generate;

    private static final Color LABEL_COLOR=Color.BLACK;

    private static final Color LABEL_BACKGROUND_COLOR=new Color(255, 255, 255, 128);

    public ExportDialog(Frame owner, ImgFilesBag map, MapTransformer transformer)
    {
        super(owner, "Export map setup");
        this.file=null;
        selectionTransformer=transformer.clone();
        sampleTransformer=transformer.clone();

        setSize(800, 600);

        setLayout(new GridBagLayout());

        GridBagConstraints c=new GridBagConstraints();

        selectionMapPanel=new ExportMapPanel<COORD>(map, selectionTransformer);
        selectionControlPanel=new MapControlPanel<COORD>(selectionMapPanel);
        filler=new JPanel(new GridBagLayout());
        filler.setBorder(BorderFactory.createTitledBorder("Zone selection"));
        filler.add(selectionControlPanel);
        c.gridx=0;
        c.gridy=0;
        c.gridheight=GridBagConstraints.REMAINDER;
        c.weightx=1.0;
        c.weighty=1.0;
        c.anchor=GridBagConstraints.FIRST_LINE_START;
        c.fill=GridBagConstraints.BOTH;
        add(filler, c);
        filler.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                updateSelectionPanelSize();
                synchronizeSampleTransformer();
            }
        });

        JPanel filePanel=createFilePanel();
        c.gridx++;
        c.weightx=0.2;
        c.weighty=0.0001;
        c.gridheight=1;
        c.insets=new Insets(2, 2, 2, 2);
        add(filePanel, c);

        JPanel formatPanel=createFormatPanel();
        formatPanel.setBorder(BorderFactory.createTitledBorder("Output setup"));
        c.gridy++;
        add(formatPanel, c);

        JPanel qualityPanel=createQualityPanel(map);
        qualityPanel.setBorder(BorderFactory.createTitledBorder("Quality sample"));
        c.gridy++;
        c.weighty=1.0;
        add(qualityPanel, c);

        JPanel buttonsPanel=createButtonsPanel();
        c.gridy++;
        c.weighty=0.0001;
        c.fill=0;
        c.anchor=GridBagConstraints.CENTER;
        add(buttonsPanel, c);

        validate();
    }

    public void reset()
    {
        file=null;
        fileLabel.setText("Choose a filename...");
    }

    private JPanel createFilePanel()
    {
        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();

        c.gridx=0;
        c.gridy=0;
        c.weightx=1;
        c.weighty=0;
        c.fill=GridBagConstraints.HORIZONTAL;
        fileLabel=new JLabel("Choose a filename...");
        panel.add(fileLabel, c);

        c.gridx++;
        c.weightx=0;
        JButton changeFile=new JButton("Choose");
        panel.add(changeFile, c);
        changeFile.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                chooseFile();
            }
        });

        return panel;
    }

    private void chooseFile()
    {
        JFileChooser chooser=new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                return FileExporter.getExporterForFile(f)!=null;
            }

            @Override
            public String getDescription()
            {
                return "Any";
            }
        });
        if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)
        {
            final File selectedFile=chooser.getSelectedFile();

            final FileExporter exporter=FileExporter.getExporterForFile(selectedFile);
            if(exporter!=null)
            {
                file=selectedFile;
                formatsSel.setSelectedItem(exporter);

            }
            else
            {
                file=new File(selectedFile.getAbsolutePath()+"."+fileExporter.getExtension());
            }
            fileLabel.setText(file.getName());
            generate.setEnabled(true);
        }
    }

    private JPanel createButtonsPanel()
    {
        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();

        JButton fullMap=new JButton("Full map");
        c.insets=new Insets(2, 2, 2, 2);
        c.gridx=0;
        c.gridy=0;
        c.weightx=1;
        c.weighty=0;
        c.fill=GridBagConstraints.HORIZONTAL;
        panel.add(fullMap, c);
        fullMap.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectionMapPanel.showAllMap();
            }
        });

        generate=new JButton("Generate");
        generate.setEnabled(false);
        c.gridy++;
        panel.add(generate, c);
        generate.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                generateFile();
            }
        });

        JButton close=new JButton("Close");
        c.gridy++;
        panel.add(close, c);
        close.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        return panel;
    }

    private void generateFile()
    {
        MapTransformer<COORD> generateTransformer=selectionTransformer.clone();
        generateTransformer.changeDimensions(width, height);
        try
        {
            selectionMapPanel.saveMapAs(file, fileExporter, generateTransformer);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void synchronizeSampleTransformer()
    {
        if(mapSamplePanel!=null)
        {
            sampleTransformer.setFrom(selectionTransformer);
            sampleTransformer.changeDimensions(width, height);
            sampleTransformer.setDimensions(mapSamplePanel.getWidth(), mapSamplePanel.getHeight());
            mapSamplePanel.transformerChangedManually();
        }
    }

    private JPanel createQualityPanel(ImgFilesBag map)
    {
        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();

        c.gridx=0;
        c.gridy=0;
        c.weightx=1;
        c.weighty=0;
        c.fill=GridBagConstraints.HORIZONTAL;

        mapSamplePanel=new ExportMapPanel<COORD>(map, sampleTransformer);
        //c.gridy++;
        c.weighty=1;
        c.fill=GridBagConstraints.BOTH;
        panel.add(mapSamplePanel, c);

        return panel;
    }

    private JPanel createFormatPanel()
    {
        final JPanel panel=new JPanel(new GridBagLayout());
        final GridBagConstraints c=new GridBagConstraints();

        formatsSel=new JComboBox();
        for(int i=0; i<FileExporter.exporters.length; i++)
        {
            FileExporter exporter=FileExporter.exporters[i];
            formatsSel.addItem(exporter);
        }
        c.fill=GridBagConstraints.HORIZONTAL;
        c.gridx=0;
        c.gridy=0;
        c.ipadx=2;
        c.anchor=GridBagConstraints.FIRST_LINE_START;
        formatsSel.setAlignmentX(0.0f);
        panel.add(formatsSel, c);

        c.gridy++;
        c.weightx=1.0;
        formatsSel.addItemListener(new ItemListener()
        {
            private JPanel formatConfigs=null;

            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange()==ItemEvent.SELECTED)
                {
                    if(formatConfigs!=null)
                        panel.remove(formatConfigs);
                    fileExporter=((FileExporter)e.getItem());
                    formatConfigs=fileExporter.getConfigurationPanel(ExportDialog.this);
                    panel.add(formatConfigs, c);
                    validate();
                    updateSelectionPanelSize();
                    synchronizeSampleTransformer();
                    if(file!=null)
                    {
                        final String old=file.getAbsolutePath();
                        file=new File(old.substring(0, old.length()-4)+"."+fileExporter.getExtension());
                        fileLabel.setText(file.getName());
                    }
                }
            }
        });
        formatsSel.setSelectedIndex(1);
        formatsSel.setSelectedIndex(0);

        return panel;
    }

    public void exportSizeChanged(int width, int height)
    {
        if(width!=this.width || height!=this.height)
        {
            boolean ratioChanged=Math.abs((double)height/width-(double)this.height/this.width)>0.001;
            this.width=width;
            this.height=height;
            if(ratioChanged)
                updateSelectionPanelSize();
            synchronizeSampleTransformer();
        }
    }

    private void updateSelectionPanelSize()
    {
        Insets margin=filler.getInsets();
        int fillerWidth=filler.getWidth()-margin.left-margin.right;
        int fillerHeight=filler.getHeight()-margin.top-margin.bottom;

        if(fillerWidth<=0 || fillerHeight<=0) return;  //weird swing behavior...

        double ratio=(double)height/width;
        double fillerRatio=(double)fillerHeight/fillerWidth;
        if(fillerRatio>=ratio)
        {   //height bigger
            int newHeight=(int)(fillerWidth*ratio);
            selectionMapPanel.setSize(fillerWidth, newHeight);
            selectionControlPanel.setBounds(margin.left, margin.top+(fillerHeight-newHeight)/2, fillerWidth, newHeight);
        }
        else
        {   //width bigger
            int newWidth=(int)(fillerHeight/ratio);
            selectionMapPanel.setSize(newWidth, fillerHeight);
            selectionControlPanel.setBounds(margin.left+(fillerWidth-newWidth)/2, margin.top, newWidth, fillerHeight);
        }
    }

    private class ExportMapDrawer extends MapDrawer
    {
        public ExportMapDrawer(MapConfig config, Graphics2D g2, float fontSize)
        {
            super(config, g2, ExportDialog.this, fontSize, LABEL_COLOR, LABEL_BACKGROUND_COLOR);
        }

        protected ImgConstants.PolygonDescription setPolygonStyle(int type)
        {
            ImgConstants.PolygonDescription result=super.setPolygonStyle(type);
            if(type==ImgConstants.BACKGROUND || type==ImgConstants.DEFINITION_AREA)
            {
                //we don't want a yellowish background in printed maps.
                g2.setPaint(Color.WHITE);
                g2.setColor(Color.WHITE);
            }
            return result;
        }
    }

    private class ExportMapPanel<COORD> extends MapPanel<COORD>
    {
        public ExportMapPanel(ImgFilesBag map, MapTransformer<COORD> transformer)
        {
            super(map, transformer);
        }

        protected void transformerChanged()
        {
            super.transformerChanged();
            synchronizeSampleTransformer();
        }

        public MapDrawer createMapDrawer(MapConfig workConfig, Graphics2D g2, float fontSize, int poiThreshold)
        {
            return new ExportMapDrawer(workConfig, g2, fontSize);
        }
    }
}
