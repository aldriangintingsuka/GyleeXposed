package com.gfamily.gyleexposed.Business.Managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import android.util.DisplayMetrics;

import com.gfamily.gyleexposed.Business.Logger.ILogger;
import com.gfamily.gyleexposed.Model.GyleeScript;

public class ScriptManager implements IScriptManager
{
  private Hashtable<String, Map<String, List<GyleeScript>>> _scripts;
  private File _scriptFile;
  private ILogger _logger;

  public ScriptManager( ILogger logger )
  {
    _logger = logger;
    _scripts = new Hashtable<String, Map<String, List<GyleeScript>>>();
  }

  @Override
  public void LoadScript( String scriptDirectoryName, String scriptFileName ) throws IOException
  {
    File scriptDirectory = new File( scriptDirectoryName );

    if( !scriptDirectory.exists() )
    {
      if( scriptDirectory.mkdirs() && scriptDirectory.isDirectory() )
        WriteLog( "Script directory is created at " + scriptDirectory );
      else
      {
        String message = "Cannot create script directory at " + scriptDirectory;
        WriteLog( message );
        throw new IOException( message );
      }
    }

    File scriptFile = new File( scriptDirectory, scriptFileName );
    if( scriptFile.exists() ) _scriptFile = scriptFile;
  }

  @Override
  public void ParseScript() throws IOException
  {
    _scripts = new Hashtable<String, Map<String, List<GyleeScript>>>();
    int scriptItemCount = 0;

    if( _scriptFile == null ) return;

    BufferedReader br = null;

    try
    {
      br = new BufferedReader( new FileReader( _scriptFile ) );
      String line;

      String description = "";
      while( ( line = br.readLine() ) != null )
      {
        WriteLog( "Script line : " + line );

        if( line.matches( "^\\s*$" ) ) continue;

        if( line.startsWith( "#" ) )
        {
          description += "\n" + line.replaceFirst( "^#", "" );
          continue;
        }

        String[] lineComponents = line.split( ",", 5 );
        String packageName = lineComponents[ 0 ];
        String resourceType = lineComponents[ 1 ];
        String resourceName = lineComponents[ 2 ];
        String replacementValue = lineComponents[ 3 ];
        int density = DisplayMetrics.DENSITY_DEFAULT;

        if( lineComponents.length > 4 ) density = Integer.parseInt( lineComponents[ 4 ] );

        if( description == "" ) description = "No description";

        WriteLog( "Adding script item." );
        GyleeScript scriptItem = new GyleeScript( resourceName, replacementValue, description, density );

        if( !_scripts.containsKey( packageName ) ) _scripts.put( packageName, new Hashtable<String, List<GyleeScript>>() );

        Hashtable<String, List<GyleeScript>> packageScripts = (Hashtable<String, List<GyleeScript>>) _scripts.get( packageName );

        if( !packageScripts.containsKey( resourceType ) ) packageScripts.put( resourceType, new ArrayList<GyleeScript>() );

        List<GyleeScript> scriptItems = packageScripts.get( resourceType );
        scriptItems.add( scriptItem );
        scriptItemCount++;
        description = "";
      }
    }
    catch( IOException e )
    {
      WriteLog( e.getMessage() );
      throw e;
    }
    finally
    {
      if( br != null ) br.close();
    }

    WriteLog( "Found " + scriptItemCount + " script items." );
  }

  @Override
  public Map<String, List<GyleeScript>> GetScript( String packageName )
  {
    if( _scripts.containsKey( packageName ) )
      return _scripts.get( packageName );
    else
      return new Hashtable<String, List<GyleeScript>>();
  }

  private void WriteLog( String message )
  {
    _logger.LogInfo( message );
  }
}
