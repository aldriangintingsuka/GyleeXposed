package com.gfamily.resource.Business.Managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.DisplayMetrics;

import com.gfamily.common.logger.ILogger;
import com.gfamily.resource.Model.GyleeScript;

public class ScriptManager implements IScriptManager
{
  private HashMap<String, Map<String, List<GyleeScript>>> _scripts;
  private List<File> _scriptFiles;
  private ILogger _logger;
  private Map<String, List<Map<String, String>>> _levelListMap;
  private String _scriptDirectoryName;

  public ScriptManager( String scriptDirectoryName, ILogger logger )
  {
    _scriptDirectoryName = scriptDirectoryName;
    _logger = logger;
    _scripts = new HashMap<String, Map<String, List<GyleeScript>>>();
    _scriptFiles = new ArrayList<File>();
    _levelListMap = new HashMap<String, List<Map<String, String>>>();
  }

  @Override
  public void LoadScripts() throws IOException
  {
    _scriptFiles.clear();
    File scriptDirectory = new File( _scriptDirectoryName );

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

    File[] scriptFiles = scriptDirectory.listFiles();

    if( scriptFiles == null ) return;

    for( File scriptFile : scriptFiles )
    {
      if( !scriptFile.isFile() ) continue;

      _scriptFiles.add( scriptFile );
    }

    Collections.sort( _scriptFiles, new Comparator<File>()
      {
        public int compare( File file1, File file2 )
        {
          return file1.getName().compareToIgnoreCase( file2.getName() );
        }
      } );
  }

  @Override
  public void ParseScripts() throws IOException
  {
    _scripts.clear();
    _levelListMap.clear();

    for( File scriptFile : _scriptFiles )
    {
      WriteLog( "Parsing script file " + scriptFile.getName() );
      int scriptItemCount = 0;

      BufferedReader br = null;

      try
      {
        br = new BufferedReader( new FileReader( scriptFile ) );
        String line;

        String description = "";
        String packageName = "";
        String resourceType = "";
        String baseDirectory = "/sdcard";
        String baseForwardPackageName = "";
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

          if( line.startsWith( "setBaseForwardPackage" ) )
          {
            String[] lineComponents = line.split( "\\s+", 2 );
            baseForwardPackageName = lineComponents[ 1 ];
            continue;
          }

          if( line.startsWith( "setBaseDensity" ) )
          {
            String[] lineComponents = line.split( "\\s+", 2 );
            density = Integer.parseInt( lineComponents[ 1 ] );
            continue;
          }

          if( line.startsWith( "withValue" ) && !packageName.equals( "" ) )
          {
            String[] lineComponents = line.split( "\\s+", 3 );
            resourceName = lineComponents[ 1 ];
            String replacementValue = lineComponents[ 2 ];

            List<Map<String, String>> replacements = new ArrayList<Map<String, String>>();

            HashMap<String, String> replacement = new HashMap<String, String>();
            replacement.put( "Type", "value" );
            replacement.put( "Name", replacementValue );

            replacements.add( replacement );

            AddScript( packageName, resourceType, resourceName, replacements, description );
            scriptItemCount++;
            description = "";
            continue;
          }

          if( ( line.startsWith( "withFile" ) ||  line.startsWith( "withMaskFile" ) ) && !packageName.equals( "" ) )
          {
            String[] lineComponents = line.split( "\\s+", 4 );
            String resourceNamePattern = lineComponents[ 1 ];
            String replacementValuePattern = lineComponents[ 2 ];

            if( lineComponents.length == 4 ) density = Integer.parseInt( lineComponents[ 3 ] );

            List<ReplacementItem> replacementItems = FillReplacement( resourceNamePattern, replacementValuePattern );

            for( ReplacementItem replacementItem : replacementItems )
            {
              resourceName = replacementItem.GetResourceName();
              String replacementValue = replacementItem.GetReplacementValue();

              List<Map<String, String>> replacements = new ArrayList<Map<String, String>>();

              HashMap<String, String> replacement = new HashMap<String, String>();
              replacement.put( "Type", line.startsWith( "withFile" ) ? "file" : "maskFile" );
              replacement.put( "Name", new File( baseDirectory, replacementValue ).toString() );
              replacement.put( "Density", density + "" );

              replacements.add( replacement );

              AddScript( packageName, resourceType, resourceName, replacements, description );
              scriptItemCount++;
            }

            description = "";
            continue;
          }

          if( line.startsWith( "withLevelFile" ) && !packageName.equals( "" ) )
          {
            String[] lineComponents = line.split( "\\s+", 6 );
            String resourceNamePattern = lineComponents[ 1 ];
            String levelRange = lineComponents[ 2 ];
            String endIndicator = lineComponents[ 3 ];
            String replacementValuePattern = lineComponents[ 4 ];

            if( lineComponents.length == 6 ) density = Integer.parseInt( lineComponents[ 5 ] );

            if( !_levelListMap.containsKey( resourceNamePattern ) ) _levelListMap.put( resourceNamePattern, new ArrayList<Map<String, String>>() );

            List<Map<String, String>> replacements = _levelListMap.get( resourceNamePattern );

            List<LevelReplacementItem> levelReplacementItems = FillLevelReplacement( resourceNamePattern, replacementValuePattern, levelRange );

            for( LevelReplacementItem levelReplacementItem : levelReplacementItems )
            {
              resourceName = levelReplacementItem.GetResourceName();
              String replacementValue = levelReplacementItem.GetReplacementValue();
              int level = levelReplacementItem.GetLevel();

              HashMap<String, String> replacement = new HashMap<String, String>();
              replacement.put( "Type", "levelFile" );
              replacement.put( "Level", level + "" );

              replacement.put( "Name", new File( baseDirectory, replacementValue ).toString() );
              replacement.put( "Density", density + "" );

              replacements.add( replacement );
            }

            if( endIndicator.equals( "last" ) )
              AddScript( packageName, resourceType, resourceName, replacements, description );
            
            scriptItemCount++;
            description = "";
            continue;
          }

          if( ( line.startsWith( "withResource" ) ||  line.startsWith( "withMaskResource" ) ) && !packageName.equals( "" ) )
          {
            String[] lineComponents = line.split( "\\s+", 4 );
            String resourceNamePattern = lineComponents[ 1 ];
            String replacementValuePattern = lineComponents[ 2 ];

            if( lineComponents.length == 4 ) baseForwardPackageName = lineComponents[ 3 ];

            List<ReplacementItem> replacementItems = FillReplacement( resourceNamePattern, replacementValuePattern );

            for( ReplacementItem replacementItem : replacementItems )
            {
              resourceName = replacementItem.GetResourceName();
              String replacementValue = replacementItem.GetReplacementValue();

              List<Map<String, String>> replacements = new ArrayList<Map<String, String>>();

              HashMap<String, String> replacement = new HashMap<String, String>();
              replacement.put( "Type", line.startsWith( "withResource" ) ? "resource" : "maskResource" );
              replacement.put( "Name", replacementValue );
              replacement.put( "ForwardPackageName", baseForwardPackageName );

              replacements.add( replacement );

              AddScript( packageName, resourceType, resourceName, replacements, description );
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

      WriteLog( "Found " + scriptItemCount + " script items in " + scriptFile.getName() );
    }
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

        ReplacementItem replacement = new ReplacementItem( resourceName, replacementValue );
        replacements.add( replacement );
      }
    }
    else
    {
      resourceName = resourceNamePattern.replaceAll( "\\(.+\\)", "" );
      replacementValue = replacementValuePattern.replace( "$0", resourceName );
      WriteLog( "Adding single replacement item : " + resourceName + " | " + replacementValue );

      ReplacementItem replacement = new ReplacementItem( resourceName, replacementValue );
      replacements.add( replacement );
    }

    return replacements;
  }

  private List<LevelReplacementItem> FillLevelReplacement( String resourceName, String replacementValuePattern, String levelRange )
  {
    List<LevelReplacementItem> replacements = new ArrayList<LevelReplacementItem>();

    String replacementValue = replacementValuePattern;

    String[] rangeComponents = levelRange.split( "-" );
    int startIndex = Integer.parseInt( rangeComponents[ 0 ] );
    int endIndex = Integer.parseInt( rangeComponents[ 1 ] );

    for( int i = startIndex; i <= endIndex; i++ )
    {
      replacementValue = replacementValuePattern.replace( "$1", i + "" );
      replacementValue = replacementValue.replace( "$0", resourceName );
      WriteLog( "Adding multiple level replacement item : " + resourceName + " | " + replacementValue );

      LevelReplacementItem replacement = new LevelReplacementItem( resourceName, replacementValue, i );
      replacements.add( replacement );
    }

    return replacements;
  }

  private void AddScript( String packageName, String resourceType, String resourceName, List<Map<String, String>> replacements, String description )
  {
    WriteLog( "Adding script item." );
    GyleeScript scriptItem = new GyleeScript( resourceName, replacements, description );

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

  private class LevelReplacementItem
  {
    private int _level;
    private ReplacementItem _replacementItem;

    public LevelReplacementItem( String resourceName, String replacementValue, int level )
    {
      _replacementItem = new ReplacementItem( resourceName, replacementValue );
      _level = level;
    }

    public String GetResourceName()
    {
      return _replacementItem.GetResourceName();
    }

    public String GetReplacementValue()
    {
      return _replacementItem.GetReplacementValue();
    }

    public int GetLevel()
    {
      return _level;
    }
  }
}
