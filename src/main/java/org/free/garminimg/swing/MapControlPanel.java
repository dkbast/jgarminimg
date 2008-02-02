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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A swing panel that allows to control a MapPanel using the mouse.
 */
public class MapControlPanel<COORD> extends JPanel
{
    private static final long serialVersionUID=1624371546932123247L;

    protected final MapPanel<COORD> mapPanel;

    private MouseGestures previousMouseGestures=null;

    public MapControlPanel(MapPanel<COORD> mapPanel)
    {
        super();
        this.mapPanel=mapPanel;
        setupPanel();
        setupDefaultMouseGestures();
    }

    protected void setupPanel()
    {
        BorderLayout layout=new BorderLayout();
        setLayout(layout);

        JToolBar toolBar=new JToolBar(JToolBar.HORIZONTAL);
        fillButtonPanel(toolBar);

        add(toolBar, BorderLayout.PAGE_START);

        add(mapPanel, BorderLayout.CENTER);
    }

    protected void fillButtonPanel(JToolBar toolbar)
    {
        toolbar.add(new JLabel("zoom:"));
        JButton zoomAll=new JButton("all");
        zoomAll.setToolTipText("Zoom in order to show all the available map area");
        zoomAll.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.showAllMap();
            }
        });
        toolbar.add(zoomAll);

        JButton zoomIn=new JButton("+");
        zoomIn.setToolTipText("Zoom in");
        zoomIn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.zoom(1/1.5);
            }
        });
        toolbar.add(zoomIn);

        JButton zoomOut=new JButton("-");
        zoomOut.setToolTipText("Zoom out");
        zoomOut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.zoom(1.5);
            }
        });
        toolbar.add(zoomOut);

        toolbar.addSeparator();
        toolbar.add(new JLabel("labels:"));

        final JComboBox pointThreshold=new JComboBox(new String[]{"regions", "cities", "all"});
        final int[] thresholds=new int[]{0, 0x15, 0xFF};

        final JToggleButton showLineLabel=createToggle("lineLbl", "show/hide labels on lines");
        showLineLabel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.setShowLineLabel(showLineLabel.isSelected());
            }
        });
        toolbar.add(showLineLabel);

        final JToggleButton showPolygonLabel=createToggle("areaLbl", "show/hide labels on surfaces");
        showPolygonLabel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.setShowPolygonLabel(showPolygonLabel.isSelected());
            }
        });
        toolbar.add(showPolygonLabel);

        final JToggleButton showPointLabel=createToggle("poiLbl", "show/hide labels on points");
        showPointLabel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean selected=showPointLabel.isSelected();
                mapPanel.setShowPointLabel(selected);
                pointThreshold.setEnabled(selected);
            }
        });
        toolbar.add(showPointLabel);

        pointThreshold.setSelectedIndex(1);
        pointThreshold.setToolTipText("What kind of POI we want the labels");
        pointThreshold.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.setPoiThreshold(thresholds[pointThreshold.getSelectedIndex()]);
            }
        });
        toolbar.add(pointThreshold);

        toolbar.addSeparator();
        toolbar.add(new JLabel("details:"));
        final JSpinner details=new JSpinner(new SpinnerNumberModel(0, -5, 5, 1));
        details.setToolTipText("What detail level to use (bigger means more details)");
        details.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                mapPanel.setDetailLevel((Integer)details.getValue());
            }
        });
        toolbar.add(details);
        toolbar.addSeparator();

        final JToggleButton shading=createToggle("shading", "enable/disable the relief shading of topo maps");
        shading.setSelected(false);
        shading.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mapPanel.setShading(shading.isSelected());
            }
        });
        toolbar.add(shading);

        toolbar.add(new Box.Filler(new Dimension(0, 0), new Dimension(Short.MAX_VALUE, 0),
                                   new Dimension(Short.MAX_VALUE, 0)));
    }

    private JToggleButton createToggle(String prefix, String tooltip)
    {
        final JToggleButton result=new JToggleButton();
        result.setSelectedIcon(new ImageIcon(MapControlPanel.class.getResource(prefix+"On.png")));
        result.setIcon(new ImageIcon(MapControlPanel.class.getResource(prefix+"Off.png")));
        result.setSelected(true);
        result.setMargin(new Insets(1, 1, 1, 1));
        result.setToolTipText(tooltip);
        result.setBorder(BorderFactory.createEtchedBorder());
        return result;
    }

    protected void setupDefaultMouseGestures()
    {
        DefaultMouseGestures listener=new DefaultMouseGestures<COORD>(mapPanel);
        setMouseGestures(listener);
    }

    protected void setMouseGestures(MouseGestures listener)
    {
        if(previousMouseGestures!=null)
        {
            mapPanel.removeMouseListener(previousMouseGestures);
            mapPanel.removeMouseWheelListener(previousMouseGestures);
            mapPanel.removeMouseMotionListener(previousMouseGestures);
        }
        mapPanel.addMouseListener(listener);
        mapPanel.addMouseWheelListener(listener);
        mapPanel.addMouseMotionListener(listener);
        previousMouseGestures=listener;
    }

}
