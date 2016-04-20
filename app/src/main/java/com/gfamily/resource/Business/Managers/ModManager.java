package com.gfamily.resource.Business.Managers;

import java.io.File;
import java.util.*;

import android.graphics.Canvas;
import com.gfamily.common.logger.ILogger;
import com.gfamily.resource.Model.GyleeScript;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;

import static android.content.res.XResources.*;

public class ModManager implements IModManager
{
  private ILogger _logger;
  private HashMap<String, String> _modulePathMap;
  private double _sourceBitmapScaleFactor;
  private HashMap<String, Boolean> _resourceTrackerMap;

  public ModManager( ILogger logger )
  {
    _logger = logger;
    _modulePathMap = new HashMap<>();
    _sourceBitmapScaleFactor = 0.7;
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
      String resourcePackageName = replacement.get( "ResourcePackageName" );

      if( resourceType.equals( "drawable" ) || resourceType.equals( "mipmap" ) )
      {
        switch( replacementType )
        {
          case "file":
          case "maskFile":
            int density = Integer.parseInt( replacement.get( "Density" ) );
            ReplaceDrawable( sourceResources, resourcePackageName, resourceType, sourceName, targetName, density, replacementType.equals( "maskFile" ) );
            break;
          case "resource":
          case "maskResource":
            String forwardPackageName = replacement.get( "ForwardPackageName" );
            ForwardDrawable( sourceResources, resourcePackageName, resourceType, sourceName, forwardPackageName, targetName, replacementType.equals( "maskResource" ) );
            break;
          case "levelFile":
            ReplaceLevelListDrawable( sourceResources, packageName, sourceName, replacements );
            break;
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
        //WriteLog( "Setting level " + targetName );
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

    DrawableLoader drawableLoader = CreateDrawableLoader( levelListDrawable );
    sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
  }

  private void ForwardDrawable( XResources sourceResources, String packageName, String resourceType, String sourceName, final String forwardPackageName, String targetName, final boolean isMask )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      String modulePath = GetModulePath( forwardPackageName );

      if( modulePath.equals( "" ) ) return;

      try
      {
        XModuleResources moduleResources = XModuleResources.createInstance( modulePath, sourceResources );
        int targetID = moduleResources.getIdentifier( targetName, "drawable", forwardPackageName );

        if( targetID <= 0 )
        {
          WriteLog( "Target drawable resource not found : " + targetName );

          targetID = moduleResources.getIdentifier( targetName, "mipmap", forwardPackageName );

          if( targetID <= 0 )
          {
            WriteLog( "Target mipmap resource not found : " + targetName );
            return;
          }
        }

        Drawable sourceDrawable = sourceResources.getDrawable( sourceID );

        if( !isMask )
        {
          //WriteLog( "Forwarding " + packageName + "#" + sourceName + " with ID " + targetID + " of " + modulePath );
          sourceResources.setReplacement( packageName, resourceType, sourceName, moduleResources.fwd( targetID ) );
        }
        else
        {
          //WriteLog( "Masking " + packageName + "#" + sourceName + " with " + targetName + " | " + sourceDrawable.getClass() );

          Drawable maskDrawable = moduleResources.getDrawable( targetID );
          Drawable targetDrawable;

          Bitmap maskBitmap = ( (BitmapDrawable) maskDrawable ).getBitmap();
          final int maskBitmapWidth = maskBitmap.getWidth();
          final int maskBitmapHeight = maskBitmap.getHeight();
          Bitmap bitmapOverlay = Bitmap.createBitmap( maskBitmapWidth, maskBitmapHeight, maskBitmap.getConfig() );
          Canvas canvas = new Canvas( bitmapOverlay );
          double offsetFactor = ( 1.0 - _sourceBitmapScaleFactor ) * 0.5;
          canvas.drawBitmap( maskBitmap, 0, 0, null );

          final int left = (int) ( maskBitmapWidth * offsetFactor );
          final int top = (int) ( maskBitmapHeight * offsetFactor );
          final int right = (int) ( maskBitmapWidth * ( 1 - offsetFactor ) );
          final int bottom = (int) ( maskBitmapHeight * ( 1 - offsetFactor ) );
          sourceDrawable.setBounds( left, top, right, bottom );
          sourceDrawable.draw( canvas );

          targetDrawable = new BitmapDrawable( sourceResources, bitmapOverlay );
          DrawableLoader drawableLoader = CreateDrawableLoader( targetDrawable );
          sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
        }
      }
      catch( Exception e )
      {
        WriteLog( "Error in forwarding : " + packageName + "#" + sourceName + " | " + targetName + " : " + e.getMessage() );
      }
    }
    else
    {
      WriteLog( "Resource not found for forwarding : " + packageName + "#" + sourceName + "#" + resourceType );
    }
  }

  private void ReplaceDrawable( final XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName, final int density,final  boolean isMask )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      Drawable sourceDrawable = sourceResources.getDrawable( sourceID );
      File targetFile = new File( targetName );
      Drawable targetDrawable;

      if( targetFile.exists() )
      {
        final Bitmap bitmap = BitmapFactory.decodeFile( targetName );
        final int maskBitmapWidth = bitmap.getWidth();
        final int maskBitmapHeight = bitmap.getHeight();
        bitmap.setDensity( density );

        if( !isMask )
        {
          //WriteLog( "Replacing " + packageName + "#" + sourceName + " with " + targetName );
          targetDrawable = new BitmapDrawable( sourceResources, bitmap );
        }
        else
        {
          //WriteLog( "Masking " + packageName + "#" + sourceName + " with " + targetName );

          Bitmap bitmapOverlay = Bitmap.createBitmap( maskBitmapWidth, maskBitmapHeight, bitmap.getConfig() );
          Canvas canvas = new Canvas( bitmapOverlay );
          double offsetFactor = ( 1.0 - _sourceBitmapScaleFactor ) * 0.5;
          canvas.drawBitmap( bitmap, 0, 0, null );

          final int left = (int) ( maskBitmapWidth * offsetFactor );
          final int top = (int) ( maskBitmapHeight * offsetFactor );
          final int right = (int) ( maskBitmapWidth * ( 1 - offsetFactor ) );
          final int bottom = (int) ( maskBitmapHeight * ( 1 - offsetFactor ) );
          sourceDrawable.setBounds( left, top, right, bottom );
          sourceDrawable.draw( canvas );

          targetDrawable = new BitmapDrawable( sourceResources, bitmapOverlay );
        }

        DrawableLoader drawableLoader = CreateDrawableLoader( targetDrawable );
        sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
      }
      else
      {
        WriteLog( "Replacement not found : " + targetName );
      }
    }
    else
    {
      WriteLog( "Resource not found for replacement : " + packageName + "#" + sourceName + "#" + resourceType );
    }
  }

  private void ReplaceSimple( XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName )
  {
    //WriteLog( "Replacing " + packageName + "#" + sourceName + " with " + targetName );
    Object replacementValue = targetName;

    if( resourceType.equals( "integer" ) ) replacementValue = Integer.parseInt( targetName );

    if( resourceType.equals( "boolean" ) ) replacementValue = Boolean.parseBoolean( targetName );

    if( sourceResources == null )
      setSystemWideReplacement( packageName, resourceType, sourceName, targetName );
    else
    {
      int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

      if( sourceID != 0 )
        sourceResources.setReplacement( packageName, resourceType, sourceName, replacementValue );
      else
        WriteLog( "Resource not found : " + packageName + "#" + sourceName );
    }
  }

  private DrawableLoader CreateDrawableLoader( final Drawable drawable )
  {
    DrawableLoader drawableLoader;

    drawableLoader = new DrawableLoader()
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

    if( _modulePathMap.containsKey( packageName ) )
      return _modulePathMap.get( packageName );

    File modulePathFile = new File( "/data/app/" + packageName + "-1/base.apk" );

    if( !modulePathFile.exists() ) modulePathFile = new File( "/data/app/" + packageName + "-2/base.apk" );

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
