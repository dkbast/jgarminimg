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

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SvgFileExporter extends SimpleFileExporter
{
    private SVGGraphics2D svgGenerator;

    private File selectedFile;

    public String getExtension()
    {
        return "svg";
    }

    public void setup(File selectedFile, int width, int height)
    {
        this.selectedFile=selectedFile;
        DOMImplementation domImpl=GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS="http://www.w3.org/2000/svg";
        Document document=domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        svgGenerator=new SVGGraphics2D(document);
    }

    public Graphics2D getG2()
    {
        return svgGenerator;
    }

    public void finishSave() throws IOException
    {
        svgGenerator.stream(selectedFile.getAbsolutePath(), true);
    }

    public String toString()
    {
        return "SVG";
    }
}
