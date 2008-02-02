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

import org.free.garminimg.ObjectKind;
import org.free.garminimg.utils.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class SearchPanel extends JDialog implements ListSelectionListener
{
    private String previousSearch=null;

    private MapPanel<Point2D.Double> mapPanel;

    private List<FoundObject> results;

    private JList list;

    private JLabel detail;

    private int minLon;

    private int maxLon;

    private int minLat;

    private int maxLat;

    private int resolution;

    private static final String kindNames[]={"All", "Points", "Lines", "Surfaces"};

    private static final int kindValues[]={ObjectKind.ALL, ObjectKind.POINT|ObjectKind.INDEXED_POINT, ObjectKind.POLYLINE, ObjectKind.POLYGON};

    private int kindFilter=ObjectKind.ALL;

    private JTextField searchText;

    private BitSet typeFilter=null;

    private Vector<String> polylineTypeNames;

    private ArrayList<BitSet> polylineTypeValues;

    private Vector<String> polygonTypeNames;

    private ArrayList<BitSet> polygonTypeValues;

    private Vector<String> pointTypeNames;

    private ArrayList<BitSet> pointTypeValues;

    public SearchPanel(Frame owner, MapPanel<Point2D.Double> mapPanel)
    {
        super(owner, "Search by name");
        this.mapPanel=mapPanel;

        MapTransformer transformer=mapPanel.getTransformer();
        Rectangle bbox=transformer.getGarminBoundingBox();
        minLon=bbox.x;
        maxLon=bbox.x+bbox.width;
        minLat=bbox.y;
        maxLat=bbox.y+bbox.height;
        resolution=mapPanel.getResolution(minLon, maxLon);

        setLayout(new GridBagLayout());

        DiscoverTypesMapListener typesListener=new DiscoverTypesMapListener();
        try
        {
            mapPanel.getMap().readMap(minLon, maxLon, minLat, maxLat, resolution, ObjectKind.ALL, null, typesListener);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        createPointEnums(typesListener.getPointTypes());
        createPolylineEnums(typesListener.getPolylineTypes());
        createPolygonEnums(typesListener.getPolygonTypes());

        createContent();

        setSize(350, 400);
        validate();
    }

    private void createPointEnums(BitSet availablePointTypes)
    {
        pointTypeNames=new Vector<String>();
        pointTypeValues=new ArrayList<BitSet>();

        pointTypeNames.add("Any");
        pointTypeValues.add(null);

        Map<Integer, ImgConstants.PointDescription> map=ImgConstants.getPointTypes();

        ArrayList<ImgConstants.PointDescription> types=new ArrayList<ImgConstants.PointDescription>(map.values());
        Collections.sort(types, new Comparator<ImgConstants.PointDescription>()
        {
            public int compare(ImgConstants.PointDescription o1, ImgConstants.PointDescription o2)
            {
                int result=o1.getType()-o2.getType();
                if(result==0)
                {
                    result=o1.getSubType()-o2.getSubType();
                }
                if(result==0)
                {
                    result=o1.getDescription().compareTo(o2.getDescription());
                }
                return result;
            }
        });

        ArrayList<ImgConstants.PointDescription> filtered=new ArrayList<ImgConstants.PointDescription>();
        for(ImgConstants.PointDescription fullType : types)
        {
            int type=fullType.getType();
            if(availablePointTypes.get(type))
            {
                availablePointTypes.clear(type);
                filtered.add(fullType);
            }
        }

        Collections.sort(filtered, new Comparator<ImgConstants.PointDescription>()
        {
            public int compare(ImgConstants.PointDescription o1, ImgConstants.PointDescription o2)
            {
                int result=o1.getDescription().compareTo(o2.getDescription());
                if(result==0)
                {
                    result=o1.getType()-o2.getType();
                }
                return result;
            }
        });

        for(ImgConstants.PointDescription fullType : filtered)
        {
            int type=fullType.getType();
            BitSet bitSet=new BitSet(255);
            bitSet.set(type);
            pointTypeValues.add(bitSet);
            pointTypeNames.add(fullType.getDescription()+" 0x"+Integer.toHexString(type));
        }
    }

    private void createPolylineEnums(BitSet availablePolylineTypes)
    {
        polylineTypeNames=new Vector<String>();
        polylineTypeValues=new ArrayList<BitSet>();

        polylineTypeNames.add("Any");
        polylineTypeValues.add(null);

        ArrayList<ImgConstants.PolylineDescription> values=new ArrayList<ImgConstants.PolylineDescription>(ImgConstants.getPolylineTypes().values());
        Collections.sort(values, new Comparator<ImgConstants.PolylineDescription>()
        {
            public int compare(ImgConstants.PolylineDescription o1, ImgConstants.PolylineDescription o2)
            {
                int result=o1.getDescription().compareTo(o2.getDescription());
                if(result==0)
                    result=o1.getType()-o2.getType();
                return result;
            }
        });
        for(ImgConstants.PolylineDescription value : values)
        {
            int type=value.getType();
            if(availablePolylineTypes.get(type))
            {
                BitSet bitSet=new BitSet(511);
                bitSet.set(type);
                polylineTypeValues.add(bitSet);
                polylineTypeNames.add(value.getDescription()+" 0x"+Integer.toHexString(type));
            }
        }
    }

    private void createPolygonEnums(BitSet availablePolygonTypes)
    {
        polygonTypeNames=new Vector<String>();
        polygonTypeValues=new ArrayList<BitSet>();

        polygonTypeNames.add("Any");
        polygonTypeValues.add(null);

        ArrayList<ImgConstants.PolygonDescription> values=new ArrayList<ImgConstants.PolygonDescription>(ImgConstants.getPolygonTypes().values());
        Collections.sort(values, new Comparator<ImgConstants.PolygonDescription>()
        {
            public int compare(ImgConstants.PolygonDescription o1, ImgConstants.PolygonDescription o2)
            {
                int result=o1.getDescription().compareTo(o2.getDescription());
                if(result==0)
                    result=o1.getType()-o2.getType();
                return result;
            }
        });
        for(ImgConstants.PolygonDescription value : values)
        {
            int type=value.getType();
            if(availablePolygonTypes.get(type))
            {
                BitSet bitSet=new BitSet(511);
                bitSet.set(type);
                polygonTypeValues.add(bitSet);
                polygonTypeNames.add(value.getDescription()+" 0x"+Integer.toHexString(type));
            }
        }
    }

    private void createContent()
    {
        GridBagConstraints c=new GridBagConstraints();
        c.gridx=0;
        c.gridy=0;
        c.weightx=0;
        c.weighty=0;
        c.gridwidth=1;
        add(new JLabel("Search"), c);

        searchText=new JTextField();
        c.gridx=1;
        c.weightx=1;
        c.gridwidth=2;
        c.fill=GridBagConstraints.HORIZONTAL;
        add(searchText, c);
        searchText.addKeyListener(new KeyAdapter()
        {
            public void keyReleased(KeyEvent e)
            {
                searchTextModified();
            }
        });

        c.gridx=0;
        c.gridy++;
        c.weightx=0;
        c.weighty=0;
        c.gridwidth=1;
        add(new JLabel("Filter"), c);

        final JComboBox kind=new JComboBox(kindNames);
        final JComboBox pointTypes=createTypesComboBox(pointTypeNames, pointTypeValues);
        final JComboBox polygonTypes=createTypesComboBox(polygonTypeNames, polygonTypeValues);
        final JComboBox polyLineTypes=createTypesComboBox(polylineTypeNames, polylineTypeValues);
        kind.setSelectedIndex(0);
        c.gridx=1;
        c.weightx=1;
        c.gridwidth=1;
        c.fill=GridBagConstraints.HORIZONTAL;
        add(kind, c);
        kind.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int index=kind.getSelectedIndex();
                if(index>=0)
                {
                    kindFilter=kindValues[index];
                    updateTypesComboBox(pointTypes, polygonTypes, polyLineTypes);
                    searchAgain();
                }
            }
        });

        c.gridx=2;
        c.weightx=1;
        c.gridwidth=1;
        c.fill=GridBagConstraints.HORIZONTAL;
        pointTypes.setVisible(false);
        polygonTypes.setVisible(false);
        polyLineTypes.setVisible(false);
        add(pointTypes, c);
        add(polygonTypes, c);
        add(polyLineTypes, c);

        c.gridx=0;
        c.gridy++;
        c.weightx=1;
        c.weighty=1;
        c.gridwidth=3;
        c.fill=GridBagConstraints.BOTH;
        list=new JList();
        list.setCellRenderer(new MyListCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(this);
        add(new JScrollPane(list), c);

        c.gridx=0;
        c.gridy++;
        c.weightx=1;
        c.weighty=0;
        c.gridwidth=3;
        c.fill=GridBagConstraints.HORIZONTAL;
        detail=new JLabel();
        add(detail, c);

        c.gridy++;
        c.weightx=1;
        c.weighty=0;
        c.fill=GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        createButtons(buttonPanel);
        add(buttonPanel, c);
    }

    private void updateTypesComboBox(JComboBox pointTypes, JComboBox polygonTypes, JComboBox polyLineTypes)
    {
        pointTypes.setVisible(false);
        polygonTypes.setVisible(false);
        polyLineTypes.setVisible(false);

        if((kindFilter&(ObjectKind.POINT|ObjectKind.INDEXED_POINT))!=0)
        {
            pointTypes.setVisible(true);
        }
        else if((kindFilter&ObjectKind.POLYLINE)!=0)
        {
            polyLineTypes.setVisible(true);
        }
        else if((kindFilter&ObjectKind.POLYGON)!=0)
        {
            polygonTypes.setVisible(true);
        }
    }

    private JComboBox createTypesComboBox(Vector<String> names, final ArrayList<BitSet> values)
    {
        final JComboBox types=new JComboBox(names);
        types.setSelectedIndex(0);
        types.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int index=types.getSelectedIndex();
                if(index>=0)
                {
                    typeFilter=values.get(index);
                    searchAgain();
                }
            }
        });
        return types;
    }

    private void createButtons(JPanel buttonPanel)
    {
        JButton zoomInButton=new JButton("+");
        buttonPanel.add(zoomInButton);
        zoomInButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                zoomOnSelection(0.8);
            }
        });

        JButton zoomOutButton=new JButton("-");
        buttonPanel.add(zoomOutButton);
        zoomOutButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                zoomOnSelection(1/0.8);
            }
        });

        JButton cancelButton=new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        });
    }

    private void zoomOnSelection(double factor)
    {
        int selectedIndex=list.getMinSelectionIndex();
        if(selectedIndex>=0)
        {
            FoundObject selected=results.get(selectedIndex);
            mapPanel.zoomToGarminGeo(factor, selected.getLongitude(), selected.getLatitude());
        }
    }

    private void searchTextModified()
    {
        String text=searchText.getText();
        if(text.equals(previousSearch))
            return;
        previousSearch=text;
        searchAgain();
    }

    private void searchAgain()
    {
        String text=searchText.getText();
        try
        {
            long milliStart=System.currentTimeMillis();

            FindObjectByNameListener listener=new FindObjectByNameListener(Pattern.compile(text, Pattern.CASE_INSENSITIVE));
            mapPanel.getMap().readMap(minLon, maxLon, minLat, maxLat, resolution, kindFilter|ObjectKind.ALL_MAPS, typeFilter, listener);

            long milliEnd=System.currentTimeMillis();
            System.out.println("Time to search [ms]: "+(milliEnd-milliStart));

            results=listener.getFounds();
            list.setListData(results.toArray());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void valueChanged(ListSelectionEvent e)
    {
        StringBuilder text=new StringBuilder();
        text.append("<html>");
        try
        {
            int selected=list.getMinSelectionIndex();
            if(selected<0)
            {
                detail.setText("");
                return;
            }
            results.get(selected).toDebugHtml(text);
            zoomOnSelection(1.0);
        }
        catch(IOException e1)
        {
            e1.printStackTrace();
        }
        text.append("</html>");
        detail.setText(text.toString());
    }

    private static class MyListCellRenderer extends DefaultListCellRenderer
    {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            FoundObject found=(FoundObject)value;
            String name=null;
            try
            {
                name=found.getLabel().getName();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
        }
    }
}
