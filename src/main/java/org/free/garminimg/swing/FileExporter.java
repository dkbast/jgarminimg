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
import java.io.File;
import java.io.IOException;

public abstract class FileExporter
{
    public static final FileExporter[] exporters={new PngFileExporter(), new SvgFileExporter(), new PdfFileExporter()};

    public abstract String getExtension();

    public abstract void setup(File selectedFile, int width, int height) throws IOException;

    public abstract Graphics2D getG2();

    public abstract void finishSave() throws IOException;

    public abstract JPanel getConfigurationPanel(FileExporterConfigListener itemListener);

    public static FileExporter getExporterForFile(File f)
    {
        final String name=f.getName().toLowerCase();
        if(name.length()>4 && name.charAt(name.length()-4)!='.')
            return null;
        for(int i=0; i<exporters.length; i++)
        {
            FileExporter exporter=exporters[i];
            if(name.endsWith(exporter.getExtension()))
                return exporter;
        }
        return null;
    }

    public float getFontSize()
    {
        return 9.0f;
    }
}
