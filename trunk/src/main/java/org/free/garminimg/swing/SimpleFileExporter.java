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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class SimpleFileExporter extends FileExporter
{
    private static JPanel configurationPanel=null;

    private static FileExporterConfigListener itemListener=null;

    private static JTextField width=null;

    private static JTextField height=null;

    public JPanel getConfigurationPanel(FileExporterConfigListener itemListener)
    {
        SimpleFileExporter.itemListener=itemListener;
        if(configurationPanel==null)
            configurationPanel=createConfigurationPanel();
        sizeChanged();
        return configurationPanel;
    }

    protected JPanel createConfigurationPanel()
    {
        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();

        c.anchor=GridBagConstraints.FIRST_LINE_START;
        c.fill=0;
        c.weightx=0;
        c.gridy=0;
        c.gridx=0;
        panel.add(new JLabel("width"), c);

        width=new JTextField("2000", 5);
        c.fill=GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.weightx=1;
        panel.add(width, c);

        c.fill=0;
        c.gridy++;
        c.gridx=0;
        c.weightx=0;
        panel.add(new JLabel("height"), c);

        height=new JTextField("2000", 5);
        c.fill=GridBagConstraints.HORIZONTAL;
        c.gridx=1;
        c.weightx=1;
        panel.add(height, c);

        c.anchor=GridBagConstraints.CENTER;
        c.fill=0;
        c.gridy++;
        c.gridx=0;
        c.gridwidth=GridBagConstraints.REMAINDER;
        c.weightx=0;
        JButton apply=new JButton("Apply");
        panel.add(apply, c);
        apply.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sizeChanged();
            }
        });

        return panel;
    }

    protected void sizeChanged()
    {
        int widthVal=Integer.parseInt(width.getText());
        int heightVal=Integer.parseInt(height.getText());
        if(widthVal<=0)
        {
            width.setText("100");
            widthVal=100;
        }
        if(heightVal<=0)
        {
            height.setText("100");
            heightVal=100;
        }
        itemListener.exportSizeChanged(widthVal, heightVal);
    }
}
