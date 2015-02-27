package com.gfamily.resource.Business.Managers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.gfamily.resource.R;

public class ModManager implements IModManager
{
  private ILogger _logger;
  private HashMap<String, String> _modulePathMap;
  private double _sourceBitmapScaleFactor;

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

      if( resourceType.equals( "drawable" ) || resourceType.equals( "mipmap" ) )
      {
        switch( replacementType )
        {
          case "file":
          case "maskFile":
            int density = Integer.parseInt( replacement.get( "Density" ) );
            ReplaceDrawable( sourceResources, packageName, resourceType, sourceName, targetName, density, replacementType.equals( "maskFile" ) );
            break;
          case "resource":
          case "maskResource":
            String forwardPackageName = replacement.get( "ForwardPackageName" );
            ForwardDrawable( sourceResources, packageName, resourceType, sourceName, forwardPackageName, targetName, replacementType.equals( "maskResource" ) );
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
          WriteLog( "Target resource not found : " + targetName );
          return;
        }

        Drawable sourceDrawable = sourceResources.getDrawable( sourceID );

        if( !isMask && sourceDrawable instanceof BitmapDrawable )
        {
          WriteLog( "Forwarding " + sourceName + " with ID " + targetID + " of " + modulePath );
          sourceResources.setReplacement( packageName, resourceType, sourceName, moduleResources.fwd( targetID ) );
        }
        else
        {
          WriteLog( "Masking " + sourceName + " with " + targetName );

          Drawable maskDrawable = moduleResources.getDrawable( targetID );
          Drawable targetDrawable;

          if( sourceDrawable instanceof BitmapDrawable )
          {
            Bitmap maskBitmap = ( (BitmapDrawable) maskDrawable ).getBitmap();

            Bitmap sourceBitmap = ( (BitmapDrawable) sourceDrawable ).getBitmap();
            int sizeX = (int) Math.round( maskBitmap.getWidth() * _sourceBitmapScaleFactor );
            int sizeY = (int) Math.round( maskBitmap.getHeight() * _sourceBitmapScaleFactor );
            Bitmap sourceBitmapResized = Bitmap.createScaledBitmap( sourceBitmap, sizeX, sizeY, false );

            Bitmap bitmapOverlay = Bitmap.createBitmap( maskBitmap.getWidth(), maskBitmap.getHeight(), maskBitmap.getConfig() );
            Canvas canvas = new Canvas( bitmapOverlay );
            double offsetFactor = ( 1.0 - _sourceBitmapScaleFactor ) * 0.5;
            canvas.drawBitmap( maskBitmap, 0, 0, null );
            canvas.drawBitmap( sourceBitmapResized, (int) ( maskBitmap.getWidth() * offsetFactor ), (int) ( maskBitmap.getHeight() * offsetFactor ), null );
            targetDrawable = new BitmapDrawable( sourceResources, bitmapOverlay );
          }
          else
          {
            targetDrawable = maskDrawable;
          }

          XResources.DrawableLoader drawableLoader = CreateDrawableLoader( targetDrawable );
          sourceResources.setReplacement( packageName, resourceType, sourceName, drawableLoader );
        }
      }
      catch( Exception e )
      {
        WriteLog( "Error in forwarding : " + packageName + " | " + sourceName + " | " + targetName + " : " + e.getMessage() );
      }
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
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
        bitmap.setDensity( density );

        if( sourceDrawable instanceof BitmapDrawable )
        {
          if( !isMask )
          {
            WriteLog( "Replacing " + sourceName + " with " + targetName );
            targetDrawable = new BitmapDrawable( sourceResources, bitmap );
          }
          else
          {
            WriteLog( "Masking " + sourceName + " with " + targetName );

            Bitmap sourceBitmap = ( (BitmapDrawable) sourceDrawable ).getBitmap();
            int sizeX = (int) Math.round( bitmap.getWidth() * _sourceBitmapScaleFactor );
            int sizeY = (int) Math.round( bitmap.getHeight() * _sourceBitmapScaleFactor );
            Bitmap sourceBitmapResized = Bitmap.createScaledBitmap( sourceBitmap, sizeX, sizeY, false );

            Bitmap bitmapOverlay = Bitmap.createBitmap( bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig() );
            Canvas canvas = new Canvas( bitmapOverlay );
            double offsetFactor = ( 1.0 - _sourceBitmapScaleFactor ) * 0.5;
            canvas.drawBitmap( bitmap, 0, 0, null );
            canvas.drawBitmap( sourceBitmapResized, (int) ( bitmap.getWidth() * offsetFactor ), (int) ( bitmap.getHeight() * offsetFactor ), null );
            targetDrawable = new BitmapDrawable( sourceResources, bitmapOverlay );
          }
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
    WriteLog( "Replacing " + sourceName + " with " + targetName );
    Object replacementValue = targetName;

    if( resourceType.equals( "integer" ) ) replacementValue = Integer.parseInt( targetName );

    if( resourceType.equals( "boolean" ) ) replacementValue = Boolean.parseBoolean( targetName );

    if( sourceResources == null )
      XResources.setSystemWideReplacement( packageName, resourceType, sourceName, targetName );
    else
    {
      int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

      if( sourceID != 0 )
        sourceResources.setReplacement( packageName, resourceType, sourceName, replacementValue );
      else
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
