package com.gfamily.gyleexposed.Business.Managers;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import com.gfamily.gyleexposed.R;
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
  public void ReplaceResource( XModuleResources moduleResources, XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scriptItems )
  {
    for( GyleeScript scriptItem : scriptItems )
    {
      String sourceName = scriptItem.GetResourceName();
      String targetName = scriptItem.GetReplacementValue();
      int density = scriptItem.GetDensity();

      if( resourceType.equals( "drawable" ) )
        ReplaceDrawable( moduleResources, sourceResources, packageName, resourceType, sourceName, targetName, density );
      else
        ReplaceSimple( moduleResources, sourceResources, packageName, resourceType, sourceName, targetName );
    }
  }

  @Override
  public void ForwardDrawable( XModuleResources moduleResources, XResources sourceResources, String packageName, String resourceType, String sourceName, String targetName )
  {
    int sourceID = sourceResources.getIdentifier( sourceName, resourceType, packageName );

    if( sourceID != 0 )
    {
      int targetID = GetResourceByName( targetName );
      WriteLog( "Forwarding " + sourceName + " with ID " + targetID );
      sourceResources.setReplacement( packageName, resourceType, sourceName, moduleResources.fwd( targetID ) );
    }
    else
    {
      WriteLog( "Resource not found : " + sourceName );
    }
  }

  private void ReplaceDrawable( XModuleResources moduleResources, final XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName, final int density )
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

  private void ReplaceSimple( XModuleResources moduleResources, XResources sourceResources, String packageName, String resourceType, String sourceName, final String targetName )
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

  private int GetResourceByName( String name )
  {
    try
    {
      Class<R.drawable> resourceClass = R.drawable.class;
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
