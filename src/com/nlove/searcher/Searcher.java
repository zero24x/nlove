package com.nlove.searcher;

import java.awt.List;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class Searcher {

	public void searchFor(String term)  {
		
        File root = Paths.get( System.getProperty("user.dir"),"share").toFile();
        File[] list = root.listFiles();
        
        LinkedList<String> matches = new LinkedList<String>();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath() );       
            }
            else {
                System.out.println( "found match:" + f.getAbsoluteFile() );
            }
        }
        
	}
	
    private void walk( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isFile())
                System.out.println( "File:" + f.getAbsoluteFile() );
            }
        }
    }

