package org.free.garminimg.export;

import org.pvalsecc.opts.Option;
import org.pvalsecc.opts.GetOptions;
import org.pvalsecc.opts.InvalidOption;
import org.pvalsecc.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class BaseExporter {
    @Option(desc="DB to connect to. For example: jdbc:postgresql_postGIS://localhost:3210/geo", mandatory = true)
    private String db=null;

    @Option(desc="DB username", mandatory = true)
    private String user=null;

    @Option(desc="DB password", mandatory = true)
    private String password=null;

    protected void getOptions(String[] argv) throws IllegalAccessException {
        try {
            GetOptions.parse(argv, this);
        } catch (InvalidOption invalidOption) {
            help(invalidOption.getMessage());
        }
    }

    private void help(String message) throws IllegalAccessException {
        if(message!=null)
        {
            System.out.println(message);
            System.out.println();
        }

        System.out.println("Usage:");
        System.out.println("  java ... "+getClass().getName()+" "+ GetOptions.getShortList(this));
        System.out.println();
        System.out.println("Properties:");
        System.out.println(GetOptions.getLongList(this));

        System.exit(-1);
    }

    protected Connection getConnection()
    {
        Connection conn;
        try
        {
            conn= ConnectionFactory.getConnection(db, user, password);
            conn.setAutoCommit(false);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
        return conn;
    }
}
