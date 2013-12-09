package com.gfamily.resource;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gfamily.common.logger.ILogger;
import com.gfamily.resource.Builders.MainObjectGraphBuilder;
import com.gfamily.resource.Business.Managers.IModManager;
import com.gfamily.resource.Business.Managers.IScriptManager;
import com.gfamily.resource.Model.GyleeScript;

import android.content.res.XResources;
import android.os.Environment;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class XposedMain implements IXposedHookZygoteInit, IXposedHookInitPackageResources
{
  private String _modulePath;
  private String _packageName;
  private ILogger _logger;
  private IScriptManager _scriptManager;
  private IModManager _modManager;

  public XposedMain()
  {
    BuildObjects();

    try
    {
      InitializeFromSettings();
    }
    catch( Exception e )
    {
      WriteLog( e.getMessage() );
    }
  }

  private void BuildObjects()
  {
    _packageName = getClass().getPackage().getName();
    MainObjectGraphBuilder builder = new MainObjectGraphBuilder( _packageName );
    Map<String, Object> objects = builder.BuildObjects();

    _logger = (ILogger) objects.get( "Logger" );
    _scriptManager = (IScriptManager) objects.get( "ScriptManager" );
    _modManager = (IModManager) objects.get( "ModManager" );
  }

  private void InitializeFromSettings() throws Exception
  {
    File externalStorageDirectory = GetExternalStorageDirectory();
    File scriptDirectory = new File( externalStorageDirectory, _packageName + "/Mods" );

    _scriptManager.LoadScripts( scriptDirectory.getPath() );
    _scriptManager.ParseScripts();
  }

  private File GetExternalStorageDirectory() throws Exception
  {
    // try
    // {
    // String externalStorageState = Environment.getExternalStorageState();
    //
    // if( externalStorageState != Environment.MEDIA_MOUNTED )
    // {
    // WriteLog( "External storage is not mounted." );
    //
    // return false;
    // }
    // }
    // catch( Exception e )
    // {
    // WriteLog( e.getMessage() );
    // }

    File externalStorageDirectory = Environment.getExternalStorageDirectory();
    WriteLog( "External storage is " + externalStorageDirectory );

    if( !externalStorageDirectory.exists() )
    {
      externalStorageDirectory = new File( "/mnt/shell/emulated/0" );

      if( !externalStorageDirectory.exists() )
      {
        WriteLog( "External storage directory does not exist at " + externalStorageDirectory );

        return null;
      }

      WriteLog( "Fallback to " + externalStorageDirectory );
    }

    return externalStorageDirectory;
  }

  @Override
  public void initZygote( StartupParam startupParam ) throws Throwable
  {
    _modulePath = startupParam.modulePath;
    WriteLog( "Handle init zygote : " + _modulePath );
  }

  @Override
  public void handleInitPackageResources( InitPackageResourcesParam resparam ) throws Throwable
  {
    LoadFromScripts( resparam );
  }

  private void LoadFromScripts( InitPackageResourcesParam resparam )
  {
    String packageName = resparam.packageName;
    XResources res = resparam.res;

    Map<String, List<GyleeScript>> scripts = _scriptManager.GetScript( packageName );
    Set<String> resourceTypes = scripts.keySet();

    for( String resourceType : resourceTypes )
    {
      WriteLog( "Loading " + packageName + " for replacement of type " + resourceType );

      List<GyleeScript> scriptItems = scripts.get( resourceType );
      _modManager.ReplaceResource( res, packageName, resourceType, scriptItems );
    }
  }

  private void WriteLog( String message )
  {
    _logger.LogInfo( message );
  }
}
