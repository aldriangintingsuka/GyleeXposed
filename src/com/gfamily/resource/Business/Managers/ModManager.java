package com.gfamily.resource.Business.Managers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gfamily.common.logger.ILogger;
import com.gfamily.resource.Model.GyleeScript;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;

public class ModManager implements IModManager
{
  private ILogger _logger;
  private HashMap<String, String> _modulePathMap;

  public ModManager( ILogger logger )
  {
    _logger = logger;
    _modulePathMap = new HashMap<String, String>();
  }

  @Override
  public void ReplaceResource( XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scriptItems )
  {
    for( GyleeScript scriptItem : scriptItems )
    {
      String sourceName = scriptItem.GetResourceName();
      List<Map<String, String>> replacements = scriptItem.GetReplacements();
      Map<String, String> replacement = replacements.get( 0 );
      String replacementType = replacement.get( "Type" );
      String targetName = replacement.get( "Name" );

      if( resourceType.equals( "drawable" ) )
      {
        if( replacementType.equals( "file" ) )
        {
          int density = Integer.parseInt( replacement.get( "Density" ) );
          ReplaceDrawable( sourceResources, packageName, sourceName, targetName, density );
        }
        else if( replacementType.equals( "resource" ) )
        {
          String forwardPackageName = replacement.get( "ForwardPackageName" );
          ForwardDrawable( sourceResources, packageName, sourceName, forwardPackageName, targetName );
        }
        else if( replacementType.equals( "levelFile" ) )
        {
          ReplaceLevelListDrawable( sourceResources, packageName, sourceName, replacements );
        }
      }
      else
        ReplaceSimple( sourceResources, packageName, resourceType, sourceName, targetName );
    }
  }

  private void ReplaceLevelListDrawable( XResources sourceResources, String packageName, String sourceName, List<Map<String, String>> replacements )
  {
    String resourceType = "drawable";
    LevelListDrawable levelListDrawable = new LevelListDrawable();

    for( Map<String, String> replacement : replacements )
    {
      String targetName = replacement.get( "Name" );
      File targetFile = new File( targetName );
      int density = Integer.parseInt( replacement.get( "Density" ) );
      int level = Integer.parseInt( replacement.get( "Level" ) );

      if( targetFile.exists() )
      {
        WriteLog( "Setting level " + targetName );
        final Bitmap bitmap = BitmapFactory.decodeFile( targetName );
        bitmap.setDensity( density );
        BitmapDrawable targetDrawable = new BitmapDrawable( sourceResources, bitmap );
        levelListDrawable.addLevel( level, level, targetDrawable );
      }
      else
      {
        WriteLog( "Level file is not found : " + targetName );
      }
    }

    XResources.DrawableLoader drawableLoader = CreateDrawableLoader( levelListDrawable );
    sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
  }

  private void ForwardDrawable( XResources sourceResources, String packageName, String sourceName, final String forwardPackageName, String targetName )
  {
    String resourceType = "drawable";
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      String modulePath = GetModulePath( forwardPackageName );

      if( modulePath.equals( "" ) ) return;

      XModuleResources moduleResources = XModuleResources.createInstance( modulePath, sourceResources );
      int targetID = moduleResources.getIdentifier( targetName, "drawable", forwardPackageName );

      if( targetID <= 0 )
      {
        WriteLog( "Target resource not found : " + targetName );
        return;
      }

      WriteLog( "Forwarding " + sourceName + " with ID " + targetID + " of " + modulePath );
      sourceResources.setReplacement( packageName, resourceType, sourceName, moduleResources.fwd( targetID ) );
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
    }
  }

  private void ReplaceDrawable( final XResources sourceResources, String packageName, String sourceName, final String targetName, final int density )
  {
    String resourceType = "drawable";
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      Drawable sourceDrawable = sourceResources.getDrawable( sourceID );
      WriteLog( "Source type = " + sourceDrawable );

      File targetFile = new File( targetName );
      Drawable targetDrawable = null;

      if( targetFile.exists() )
      {
        WriteLog( "Replacing " + sourceName + " with " + targetName );

        if( sourceDrawable instanceof BitmapDrawable )
        {
          final Bitmap bitmap = BitmapFactory.decodeFile( targetName );
          bitmap.setDensity( density );
          targetDrawable = new BitmapDrawable( sourceResources, bitmap );
        }
        else
        {
          targetDrawable = Drawable.createFromPath( targetName );
        }

        XResources.DrawableLoader drawableLoader = CreateDrawableLoader( targetDrawable );
        sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
      }
      else
      {
        WriteLog( "Replacement not found : " + targetName );
      }
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
    }
  }

  private void ReplaceSimple( XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      WriteLog( "Replacing " + sourceName + " with " + targetName );

      Object replacementValue = targetName;

      if( resourceType.equals( "integer" ) ) replacementValue = Integer.parseInt( targetName );

      if( resourceType.equals( "boolean" ) ) replacementValue = Boolean.parseBoolean( targetName );

      sourceResources.setReplacement( packageName, resourceType, sourceName, replacementValue );
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
    }
  }

  private XResources.DrawableLoader CreateDrawableLoader( final Drawable drawable )
  {
    XResources.DrawableLoader drawableLoader = new XResources.DrawableLoader()
      {
        @Override
        public Drawable newDrawable( XResources res, int id ) throws Throwable
        {
          return drawable;
        }
      };

    return drawableLoader;
  }

  private String GetModulePath( String packageName )
  {
    String modulePath = "";

    if( _modulePathMap.containsKey( packageName ) ) return _modulePathMap.get( packageName );

    File modulePathFile = new File( "/data/app/" + packageName + "-1.apk" );

    if( !modulePathFile.exists() ) modulePathFile = new File( "/data/app/" + packageName + "-2.apk" );

    if( !modulePathFile.exists() )
    {
      WriteLog( "Module path is not found : " + packageName );
      return modulePath;
    }

    modulePath = modulePathFile.getAbsolutePath();
    _modulePathMap.put( packageName, modulePath );

    return modulePath;
  }

  private void WriteLog( String message )
  {
    _logger.LogInfo( message );
  }
}
