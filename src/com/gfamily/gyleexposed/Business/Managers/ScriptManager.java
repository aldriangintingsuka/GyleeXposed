package com.gfamily.gyleexposed.Business.Managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.DisplayMetrics;

import com.gfamily.gyleexposed.Business.Logger.ILogger;
import com.gfamily.gyleexposed.Model.GyleeScript;

public class ScriptManager implements IScriptManager
{
  private HashMap<String, Map<String, List<GyleeScript>>> _scripts;
  private File _scriptFile;
  private ILogger _logger;

  public ScriptManager( ILogger logger )
  {
    _logger = logger;
    _scripts = new HashMap<String, Map<String, List<GyleeScript>>>();
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
    _scripts = new HashMap<String, Map<String, List<GyleeScript>>>();
    int scriptItemCount = 0;

    if( _scriptFile == null ) return;

    BufferedReader br = null;

    try
    {
      br = new BufferedReader( new FileReader( _scriptFile ) );
      String line;

      String description = "";
      String packageName = "";
      String resourceType = "";
      String baseDirectory = "/sdcard";
      int density = DisplayMetrics.DENSITY_DEFAULT;
      String resourceName = "";

      while( ( line = br.readLine() ) != null )
      {
        WriteLog( "Script line : " + line );

        if( !line.matches( ".*\\S.*" ) ) continue;

        if( line.startsWith( "#" ) )
        {
          description += "\n" + line.replaceFirst( "^#", "" );
          continue;
        }

        if( line.startsWith( "beginReplace" ) )
        {
          String[] lineComponents = line.split( "\\s+", 3 );
          packageName = lineComponents[ 1 ];
          resourceType = lineComponents[ 2 ];
          continue;
        }

        if( line.startsWith( "setBaseDir" ) )
        {
          String[] lineComponents = line.split( "\\s+", 2 );
          baseDirectory = lineComponents[ 1 ];
          continue;
        }

        if( line.startsWith( "setBaseDensity" ) )
        {
          String[] lineComponents = line.split( "\\s+", 2 );
          density = Integer.parseInt( lineComponents[ 1 ] );
          continue;
        }

        if( line.startsWith( "withFile" ) && !packageName.equals( "" ) )
        {
          String[] lineComponents = line.split( "\\s+", 4 );
          String resourceNamePattern = lineComponents[ 1 ];
          String replacementValuePattern = lineComponents[ 2 ];

          if( lineComponents.length == 4 ) density = Integer.parseInt( lineComponents[ 3 ] );
          
          List<ReplacementItem> replacements = FillReplacement( resourceNamePattern, replacementValuePattern );
          
          for( ReplacementItem replacementItem : replacements){
            resourceName = replacementItem.GetResourceName();
            String replacementValue = replacementItem.GetReplacementValue();

            HashMap<String,String> replacement = new HashMap<String, String>();
            replacement.put( "Type", "file" );
            replacement.put( "Name", new File( baseDirectory, replacementValue ).toString() );
            replacement.put( "Density", density + "" );
            
            AddScript( packageName, resourceType, resourceName, replacement, description );
            scriptItemCount++;
          }
          
          description = "";
          continue;
        }

        if( line.startsWith( "withResource" ) && !packageName.equals( "" ) )
        {
          String[] lineComponents = line.split( "\\s+", 4 );
          String resourceNamePattern = lineComponents[ 1 ];
          String modulePath = lineComponents[ 2 ];
          String replacementValuePattern = lineComponents[ 3 ];

          List<ReplacementItem> replacements = FillReplacement( resourceNamePattern, replacementValuePattern );

          for( ReplacementItem replacementItem : replacements){
            resourceName = replacementItem.GetResourceName();
            String replacementValue = replacementItem.GetReplacementValue();

            HashMap<String,String> replacement = new HashMap<String, String>();
            replacement.put( "Type", "resource" );
            replacement.put( "Name", replacementValue );
            replacement.put( "ModulePath", modulePath );
            
            AddScript( packageName, resourceType, resourceName, replacement, description );
            scriptItemCount++;
          }

          description = "";
          continue;
        }
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
      return new HashMap<String, List<GyleeScript>>();
  }

  private void WriteLog( String message )
  {
    _logger.LogInfo( message );
  }

  private List<ReplacementItem> FillReplacement( String resourceNamePattern, String replacementValuePattern )
  {
    List<ReplacementItem> replacements = new ArrayList<ReplacementItem>();

    String resourceName = resourceNamePattern;
    String replacementValue = replacementValuePattern;
    Pattern pattern = Pattern.compile( "\\((.+)\\)" );
    Matcher matcher = pattern.matcher( resourceNamePattern );

    if( matcher.find() )
    {
      String[] rangeComponents = matcher.group( 1 ).split( "-" );
      int startIndex = Integer.parseInt( rangeComponents[ 0 ] );
      int endIndex = Integer.parseInt( rangeComponents[ 1 ] );

      for( int i = startIndex; i <= endIndex; i++ )
      {
        resourceName = resourceNamePattern.replaceAll( "\\(.+\\)", i + "" );
        replacementValue = replacementValuePattern.replace( "$1", i + "" );
        replacementValue = replacementValue.replace( "$0", resourceName );
        WriteLog( "Adding multiple replacement item : " + resourceName + " | " + replacementValue );
        
        ReplacementItem replacement = new ReplacementItem(resourceName, replacementValue );
        replacements.add( replacement );
      }
    }
    else
    {
      resourceName = resourceNamePattern.replaceAll( "\\(.+\\)", "" );
      replacementValue = replacementValuePattern.replace( "$0", resourceName );
      WriteLog( "Adding single replacement item : " + resourceName + " | " + replacementValue );

      ReplacementItem replacement = new ReplacementItem(resourceName, replacementValue );
      replacements.add( replacement );
    }

    return replacements;
  }
  
  private void AddScript(String packageName, String resourceType, String resourceName, Map<String,String>replacement,String description){
    WriteLog( "Adding script item." );
    GyleeScript scriptItem = new GyleeScript( resourceName, replacement, description );

    if( !_scripts.containsKey( packageName ) ) _scripts.put( packageName, new HashMap<String, List<GyleeScript>>() );

    HashMap<String, List<GyleeScript>> packageScripts = (HashMap<String, List<GyleeScript>>) _scripts.get( packageName );

    if( !packageScripts.containsKey( resourceType ) ) packageScripts.put( resourceType, new ArrayList<GyleeScript>() );

    List<GyleeScript> scriptItems = packageScripts.get( resourceType );
    scriptItems.add( scriptItem );
  }
  
  private class ReplacementItem
  {
    private String _resourceName;
    private String _replacementValue;

    public ReplacementItem( String resourceName, String replacementValue )
    {
      _resourceName = resourceName;
      _replacementValue = replacementValue;
    }

    public String GetResourceName()
    {
      return _resourceName;
    }

    public String GetReplacementValue()
    {
      return _replacementValue;
    }
  }
}
