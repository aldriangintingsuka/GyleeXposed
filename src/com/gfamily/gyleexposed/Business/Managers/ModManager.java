package com.gfamily.gyleexposed.Business.Managers;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.gfamily.gyleexposed.R.drawable;
import com.gfamily.gyleexposed.Business.Logger.ILogger;
import com.gfamily.gyleexposed.Model.GyleeScript;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ModManager implements IModManager
{
  private ILogger _logger;

  public ModManager( ILogger logger )
  {
    _logger = logger;
  }

  @Override
  public void ReplaceResource( XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scriptItems )
  {
    for( GyleeScript scriptItem : scriptItems )
    {
      String sourceName = scriptItem.GetResourceName();
      Map<String, String> replacement = scriptItem.GetReplacement();
      String replacementType = replacement.get( "Type" );
      String targetName = replacement.get( "Name" );

      if( resourceType.equals( "drawable" ) )
      {
        if( replacementType.equals( "file" ) )
        {
          int density = Integer.parseInt( replacement.get( "Density" ) );
          ReplaceDrawable( sourceResources, packageName, resourceType, sourceName, targetName, density );
        }
        else if( replacementType.equals( "resource" ) )
        {
          String modulePath = replacement.get( "ModulePath" );
          ForwardDrawable( sourceResources, packageName, resourceType, sourceName, modulePath, targetName );
        }
      }
      else
        ReplaceSimple( sourceResources, packageName, resourceType, sourceName, targetName );
    }
  }

  private void ForwardDrawable( XResources sourceResources, String packageName, String resourceType, String sourceName, String modulePath, String targetName )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      XModuleResources moduleResources = XModuleResources.createInstance( modulePath, sourceResources );
      File forwardPackageFile = new File( modulePath );
      String forwardPackageName = forwardPackageFile.getName().replaceAll( "-.*\\.apk$", "" );
      //int targetID = GetResourceByName( forwardPackageName, targetName );
      int targetID = moduleResources.getIdentifier( targetName, "drawable", forwardPackageName );
      WriteLog( "Forwarding " + sourceName + " with ID " + targetID + " of " + forwardPackageName );
      sourceResources.setReplacement( packageName, resourceType, sourceName, moduleResources.fwd( targetID ) );
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
    }
  }

  private void ReplaceDrawable( final XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName, final int density )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      File targetFile = new File( targetName );

      if( targetFile.exists() )
      {
        WriteLog( "Replacing " + sourceName + " with " + targetName );
        final Bitmap bitmap = BitmapFactory.decodeFile( targetName );
        bitmap.setDensity( density );

        XResources.DrawableLoader drawableLoader = new XResources.DrawableLoader()
          {
            @Override
            public Drawable newDrawable( XResources res, int id ) throws Throwable
            {
              return new BitmapDrawable( sourceResources, bitmap );
            }
          };

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

  private int GetResourceByName( String packageName, String name )
  {
    try
    {
      Class<drawable> resourceClass = (Class<drawable>) Class.forName( packageName + ".R.drawable");
      Field field = resourceClass.getField( name );
      int drawableId = field.getInt( null );

      return drawableId;
    }
    catch( Exception e )
    {
      return 0;
    }
  }

  private void WriteLog( String message )
  {
    _logger.LogInfo( message );
  }
}
