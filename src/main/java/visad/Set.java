//
// Set.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2007 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/
package visad;

public class Set
{
    public static float[][] copyFloats(float[][] samples)
    {
        if(samples==null) return null;
        int dim=samples.length;
        float[][] s_copy=new float[dim][];
        for(int j=0; j<dim; j++)
        {
            int len=samples[j].length;
            s_copy[j]=new float[len];
            System.arraycopy(samples[j], 0, s_copy[j], 0, len);
        }
        return s_copy;
    }
}
