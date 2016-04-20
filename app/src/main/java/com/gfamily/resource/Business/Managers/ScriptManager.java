package com.gfamily.resource.Business.Managers;

import android.util.DisplayMetrics;
import android.util.Log;
import com.gfamily.resource.Model.GyleeScript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptManager implements IScriptManager
{
  private HashMap<String, Map<String, List<GyleeScript>>> _scripts;
  private List<String> _scriptFileNames;
  private Map<String, List<Map<String, String>>> _levelListMap;
  private String _scriptDirectoryName;
  private String _externalStorageDirectory;

  public ScriptManager( String externalStorageDirectory, String scriptDirectoryName )
  {
    _externalStorageDirectory = externalStorageDirectory;
    _scriptDirectoryName = scriptDirectoryName;
    _scripts = new HashMap<>();
    _scriptFileNames = new ArrayList<>();
    _levelListMap = new HashMap<>();
  }

  @Override
  public void LoadScripts() throws IOException
  {
    _scriptFileNames.clear();
    Process process = Runtime.getRuntime().exec( new String[] { "su", "-c", "mkdir -p " + _scriptDirectoryName } );
    process.destroy();
    process = Runtime.getRuntime().exec( new String[] { "su", "-c", "ls " + _scriptDirectoryName } );

    BufferedReader stdout = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
    String line;

    while( ( line = stdout.readLine() ) != null )
    {
      WriteLog( "Found script file : " + line );
      _scriptFileNames.add( line );
    }

    stdout.close();
    process.destroy();

    if( _scriptFileNames.size() == 0 )
      throw new IOException( "No script files found in " + _scriptDirectoryName );
  }

  @Override
  public void ParseScripts() throws IOException
  {
    _scripts.clear();
    _levelListMap.clear();

    for( String scriptFileName : _scriptFileNames )
    {
      WriteLog( "Parsing script file " + scriptFileName );
      int scriptItemCount = 0;

      BufferedReader br = null;
      Process process = null;

      try
      {
        File fullScriptFile = new File( _scriptDirectoryName, scriptFileName );
        process = Runtime.getRuntime().exec( new String[] { "su", "-c", "cat " + fullScriptFile.getPath() } );

        br = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        String line;

        String description = "";
        String packageName = "";
        String resourceType = "";
        String baseDirectory = _externalStorageDirectory;
        String baseForwardPackageName = "";
        String resourcePackageName = "";
        int density = DisplayMetrics.DENSITY_DEFAULT;
        String resourceName = "";

        while( ( line = br.readLine() ) != null )
        {
          try
          {
            //WriteLog( "Script line : " + line );

            if( !line.matches( ".*\\S.*" ) )
              continue;

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
            }

            if( line.startsWith( "setBaseDir" ) )
            {
              String[] lineComponents = line.split( "\\s+", 2 );
              baseDirectory = lineComponents[ 1 ];
            }

            if( line.startsWith( "setBaseForwardPackage" ) )
            {
              String[] lineComponents = line.split( "\\s+", 2 );
              baseForwardPackageName = lineComponents[ 1 ];
            }

            if( line.startsWith( "setBaseDensity" ) )
            {
              String[] lineComponents = line.split( "\\s+", 2 );
              density = Integer.parseInt( lineComponents[ 1 ] );
            }

            if( line.startsWith( "withValue" ) && !packageName.equals( "" ) )
            {
              String[] lineComponents = line.split( "\\s+", 3 );
              resourceName = lineComponents[ 1 ];
              String replacementValue = lineComponents[ 2 ];

              List<Map<String, String>> replacements = new ArrayList<>();

              HashMap<String, String> replacement = new HashMap<>();
              replacement.put( "Type", "value" );
              replacement.put( "Name", replacementValue );

              replacements.add( replacement );

              AddScript( packageName, resourceType, resourceName, replacements, description );
              scriptItemCount++;
              description = "";
            }

            if( ( line.startsWith( "withFile" ) || line.startsWith( "withMaskFile" ) ) && !packageName.equals( "" ) )
            {
              String[] lineComponents = line.split( "\\s+", 5 );
              String resourceNamePattern = lineComponents[ 1 ];
              String replacementValuePattern = lineComponents[ 2 ];

              if( lineComponents.length > 3 && !lineComponents[ 3 ].equals( "-" ) )
                density = Integer.parseInt( lineComponents[ 3 ] );

              if( lineComponents.length > 4 )
                resourcePackageName = lineComponents[ 4 ];

              List<ReplacementItem> replacementItems = FillReplacement( resourceNamePattern, replacementValuePattern );

              for( ReplacementItem replacementItem : replacementItems )
              {
                resourceName = replacementItem.GetResourceName();
                String replacementValue = replacementItem.GetReplacementValue();

                List<Map<String, String>> replacements = new ArrayList<>();

                HashMap<String, String> replacement = new HashMap<>();
                replacement.put( "Type", line.startsWith( "withFile" ) ? "file" : "maskFile" );
                replacement.put( "Name", new File( baseDirectory, replacementValue ).toString() );
                replacement.put( "Density", density + "" );
                replacement.put( "ResourcePackageName", resourcePackageName );

                replacements.add( replacement );

                AddScript( packageName, resourceType, resourceName, replacements, description );
                scriptItemCount++;
              }

              scriptItemCount++;
              description = "";
            }

            if( line.startsWith( "withLevelFile" ) && !packageName.equals( "" ) )
            {
              String[] lineComponents = line.split( "\\s+", 6 );
              String resourceNamePattern = lineComponents[ 1 ];
              String levelRange = lineComponents[ 2 ];
              String endIndicator = lineComponents[ 3 ];
              String replacementValuePattern = lineComponents[ 4 ];

              if( lineComponents.length == 6 )
                density = Integer.parseInt( lineComponents[ 5 ] );

              if( !_levelListMap.containsKey( resourceNamePattern ) )
                _levelListMap.put( resourceNamePattern, new ArrayList<Map<String, String>>() );

              List<Map<String, String>> replacements = _levelListMap.get( resourceNamePattern );

              List<LevelReplacementItem> levelReplacementItems = FillLevelReplacement( resourceNamePattern, replacementValuePattern, levelRange );

              for( LevelReplacementItem levelReplacementItem : levelReplacementItems )
              {
                resourceName = levelReplacementItem.GetResourceName();
                String replacementValue = levelReplacementItem.GetReplacementValue();
                int level = levelReplacementItem.GetLevel();

                HashMap<String, String> replacement = new HashMap<>();
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
            }

            if( ( line.startsWith( "withResource" ) || line.startsWith( "withMaskResource" ) ) && !packageName.equals( "" ) )
            {
              String[] lineComponents = line.split( "\\s+", 5 );
              String resourceNamePattern = lineComponents[ 1 ];
              String replacementValuePattern = lineComponents[ 2 ];
              String forwardPackageName = baseForwardPackageName;
              resourcePackageName = packageName;

              if( lineComponents.length > 3 && !lineComponents[ 3 ].equals( "-" ) )
                forwardPackageName = lineComponents[ 3 ];

              if( lineComponents.length > 4 )
                resourcePackageName = lineComponents[ 4 ];

              List<ReplacementItem> replacementItems = FillReplacement( resourceNamePattern, replacementValuePattern );

              for( ReplacementItem replacementItem : replacementItems )
              {
                resourceName = replacementItem.GetResourceName();
                String replacementValue = replacementItem.GetReplacementValue();

                List<Map<String, String>> replacements = new ArrayList<>();

                HashMap<String, String> replacement = new HashMap<>();
                replacement.put( "Type", line.startsWith( "withResource" ) ? "resource" : "maskResource" );
                replacement.put( "Name", replacementValue );
                replacement.put( "ForwardPackageName", forwardPackageName );
                replacement.put( "ResourcePackageName", resourcePackageName );

                replacements.add( replacement );

                AddScript( packageName, resourceType, resourceName, replacements, description );
                scriptItemCount++;
              }

              scriptItemCount++;
              description = "";
            }
          }
          catch( Exception e )
          {
            WriteLog( e.getMessage() );
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

        if( process != null ) process.destroy();
      }

      WriteLog( "Found " + scriptItemCount + " script items in " + scriptFileName );
    }
  }

  @Override
  public Map<String, List<GyleeScript>> GetScript( String packageName )
  {
    if( _scripts.containsKey( packageName ) )
      return _scripts.get( packageName );
    else
      return new HashMap<>();
  }

  private void WriteLog( String message )
  {
    Log.d( "com.gfamily.resource", message );
  }

  private List<ReplacementItem> FillReplacement( String resourceNamePattern, String replacementValuePattern )
  {
    List<ReplacementItem> replacements = new ArrayList<>();

    String resourceName;
    String replacementValue;
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
        //WriteLog( "Adding multiple replacement item : " + resourceName + " | " + replacementValue );

        ReplacementItem replacement = new ReplacementItem( resourceName, replacementValue );
        replacements.add( replacement );
      }
    }
    else
    {
      resourceName = resourceNamePattern.replaceAll( "\\(.+\\)", "" );
      replacementValue = replacementValuePattern.replace( "$0", resourceName );
      //WriteLog( "Adding single replacement item : " + resourceName + " | " + replacementValue );

      ReplacementItem replacement = new ReplacementItem( resourceName, replacementValue );
      replacements.add( replacement );
    }

    return replacements;
  }

  private List<LevelReplacementItem> FillLevelReplacement( String resourceName, String replacementValuePattern, String levelRange )
  {
    List<LevelReplacementItem> replacements = new ArrayList<>();

    String replacementValue;

    String[] rangeComponents = levelRange.split( "-" );
    int startIndex = Integer.parseInt( rangeComponents[ 0 ] );
    int endIndex = Integer.parseInt( rangeComponents[ 1 ] );

    for( int i = startIndex; i <= endIndex; i++ )
    {
      replacementValue = replacementValuePattern.replace( "$1", i + "" );
      replacementValue = replacementValue.replace( "$0", resourceName );
      //WriteLog( "Adding multiple level replacement item : " + resourceName + " | " + replacementValue );

      LevelReplacementItem replacement = new LevelReplacementItem( resourceName, replacementValue, i );
      replacements.add( replacement );
    }

    return replacements;
  }

  private void AddScript( String packageName, String resourceType, String resourceName, List<Map<String, String>> replacements, String description )
  {
    //WriteLog( "Adding script item." );
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
