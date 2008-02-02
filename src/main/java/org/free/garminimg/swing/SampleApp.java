/*
 * JGarminImgParser - A java library to parse .IMG Garmin map files.
 *
 * Copyright (C) 2006 Patrick Valsecchi
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

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

public class SampleApp extends JFrame
{
    private static final long serialVersionUID=-3781560455797537921L;

    private JPanel jContentPane=null;

    private JMenuBar jJMenuBar=null;

    private JMenu fileMenu=null;

    private JMenuItem exitMenuItem=null;

    private JMenuItem openMenuItem=null;

    private JMenuItem exportMenuItem=null;

    private JMenu searchMenu=null;

    private JMenuItem searchMenuItem=null;

    private MapPanel<Point2D.Double> mapPanel;

    private ExportDialog dialog=null;

    /**
     * This is the default constructor
     */
    public SampleApp()
    {
        super();
        initialize();
    }

    /**
     * This method initializes this
     */
    private void initialize()
    {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setJMenuBar(getJJMenuBar());
        this.setSize(700, 500);
        this.setContentPane(getJContentPane());
        this.setTitle("Sample map display");
    }

    /**
     * This method initializes jContentPane
     */
    private JPanel getJContentPane()
    {
        if(jContentPane==null)
        {
            jContentPane=new MapControlPanel<Point2D.Double>(getMapPanel());
        }
        return jContentPane;
    }

    private MapPanel<Point2D.Double> getMapPanel()
    {
        if(mapPanel==null)
            mapPanel=new MapPanel<Point2D.Double>(new NullConverter(), 0);
        return mapPanel;
    }

    /**
     * This method initializes jJMenuBar
     */
    private JMenuBar getJJMenuBar()
    {
        if(jJMenuBar==null)
        {
            jJMenuBar=new JMenuBar();
            jJMenuBar.add(getFileMenu());
            jJMenuBar.add(getSearchMenu());
        }
        return jJMenuBar;
    }

    /**
     * This method initializes jMenu
     */
    private JMenu getFileMenu()
    {
        if(fileMenu==null)
        {
            fileMenu=new JMenu();
            fileMenu.setText("File");
            fileMenu.add(getOpenMenuItem());
            fileMenu.add(getExportMenuItem());
            fileMenu.add(getExitMenuItem());
        }
        return fileMenu;
    }

    private JMenu getSearchMenu()
    {
        if(searchMenu==null)
        {
            searchMenu=new JMenu();
            searchMenu.setText("Search");
            searchMenu.add(getSearchMenuItem());
        }
        return searchMenu;
    }

    /**
     * This method initializes jMenuItem
     */
    private JMenuItem getExitMenuItem()
    {
        if(exitMenuItem==null)
        {
            exitMenuItem=new JMenuItem();
            exitMenuItem.setText("Exit");
            exitMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    System.exit(0);
                }
            });
        }
        return exitMenuItem;
    }

    private JMenuItem getOpenMenuItem()
    {
        if(openMenuItem==null)
        {
            openMenuItem=new JMenuItem();
            openMenuItem.setText("Open");
            openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK, true));
            openMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    openAction();
                }
            });
        }
        return openMenuItem;
    }

    private JMenuItem getExportMenuItem()
    {
        if(exportMenuItem==null)
        {
            exportMenuItem=new JMenuItem();
            exportMenuItem.setText("Export");
            exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK, true));
            exportMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    saveAsAction();
                }
            });
            exportMenuItem.setEnabled(false);
        }
        return exportMenuItem;
    }

    private JMenuItem getSearchMenuItem()
    {
        if(searchMenuItem==null)
        {
            searchMenuItem=new JMenuItem();
            searchMenuItem.setText("Search");
            searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK, true));
            searchMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    searchAction();
                }
            });
        }
        return searchMenuItem;
    }

    private void searchAction()
    {
        SearchPanel panel=new SearchPanel(this, mapPanel);
        panel.setVisible(true);
    }

    protected void openAction()
    {
        JFileChooser chooser=new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".img");
            }

            @Override
            public String getDescription()
            {
                return "IMG or dir";
            }
        });
        if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)
        {
            File file=chooser.getSelectedFile();
            open(file);
        }
    }

    private void open(File file)
    {
        try
        {
            long milliStart=System.currentTimeMillis();

            mapPanel.clearMaps();
            mapPanel.addMapLocation(file);
            mapPanel.showAllMap();

            long milliEnd=System.currentTimeMillis();
            System.out.println("Time to open [ms]: "+(milliEnd-milliStart));

            exportMenuItem.setEnabled(true);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    protected void saveAsAction()
    {
        if(dialog==null)
            dialog=new ExportDialog(this, mapPanel.getMap(), mapPanel.getTransformer());
        else
            dialog.reset();
        dialog.setVisible(true);
    }

    /**
     * Launches this application
     */
    public static void main(String[] args)
    {
        SampleApp application=new SampleApp();
        application.setVisible(true);
        if(args.length>0)
        {
            File file=new File(args[0]);
            if(file.exists())
            {
                application.open(file);
            }
        }
    }
}
