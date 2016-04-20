package com.gfamily.resource;

import android.content.Context;
import android.content.res.XResources;
import android.os.IBinder;
import android.os.XAServiceManager;
import android.util.Log;
import com.android.server.XAService;
import com.gfamily.common.logger.ILogger;
import com.gfamily.resource.Builders.MainObjectGraphBuilder;
import com.gfamily.resource.Business.Managers.IModManager;
import com.gfamily.resource.Model.GyleeScript;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XposedMain implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources
{
  private ILogger _logger;
  private IModManager _modManager;
  private Gson _jsonParser;
  private String _packageName;
  private XAServiceManager _xaService;

  public XposedMain()
  {
    Initialize();
  }

  private void Initialize()
  {
    try
    {
      WriteLog( "Building object graph" );
      BuildObjects();
    }
    catch( Exception e )
    {
      WriteLog( e.getMessage() );
    }
  }

  private void BuildObjects() throws Exception
  {
    _packageName = getClass().getPackage().getName();
    MainObjectGraphBuilder builder = new MainObjectGraphBuilder( _packageName );
    builder.BuildObjects();

    _logger = (ILogger) builder.GetObject( "Logger" );
    _modManager = (IModManager) builder.GetObject( "ModManager" );
    _jsonParser = (Gson) builder.GetObject( "JsonParser" );
  }

  @Override
  public void initZygote( StartupParam startupParam ) throws Throwable
  {
    String modulePath = startupParam.modulePath;
    WriteLog( "Handle init zygote : " + modulePath );
  }


  @Override
  public void handleInitPackageResources( InitPackageResourcesParam resparam ) throws Throwable
  {
    String packageName = resparam.packageName;
    XResources res = resparam.res;
    //WriteLog( "ReSource is loading from " + packageName );

    LoadFromScripts( packageName, res );
  }

  private void LoadFromScripts( final String packageName, final XResources sourceResources ) throws Throwable
  {
    if( !packageName.equals( "android" ) && sourceResources == null )
      WriteLog( "NO RESOURCE for " + packageName );

    if( _xaService == null )
    {
      _xaService = XAServiceManager.GetService();

      if( _xaService == null )
      {
        WriteLog( "xa.service is not available" );
        return;
      }
    }

    String jsonResult = _xaService.GetScript( packageName );
    //WriteLog( "received json result : " + jsonResult );

    if( jsonResult == null )
      return;

    Type type = new TypeToken<Map<String, List<GyleeScript>>>()
    {
    }.getType();
    Map<String, List<GyleeScript>> scriptMap = _jsonParser.fromJson( jsonResult, type );

    Set<String> resourceTypes = scriptMap.keySet();

    for( String resourceType : resourceTypes )
    {
      if( sourceResources == null && ( resourceType.equals( "drawable" ) || resourceType.equals( "mipmap" ) ) )
        continue;

      //WriteLog( "ReSource is loading " + packageName + " for replacement of type " + resourceType );

      List<GyleeScript> scriptItems = scriptMap.get( resourceType );
      _modManager.ReplaceResource( sourceResources, packageName, resourceType, scriptItems );
    }
  }

  private void WriteLog( String message )
  {
    final String convertedMessage = message == null ? "No error message" : message;

    if( _logger == null )
      Log.i( "Xposed", _packageName + " : " + convertedMessage );
    else
      _logger.LogInfo( convertedMessage );
  }

  @Override
  public void handleLoadPackage( XC_LoadPackage.LoadPackageParam loadPackageParam ) throws Throwable
  {
    if( loadPackageParam.packageName.equals( "android" ) )
    {
      WriteLog( "injecting hooked ActivityManagerService" );
      final Class ActivityManagerServiceClazz = XposedHelpers.findClass( "com.android.server.am.ActivityManagerService", loadPackageParam.classLoader );

      if( ActivityManagerServiceClazz == null )
        WriteLog( "Cannot find class com.android.server.am.ActivityManagerService" );

      XposedBridge.hookAllConstructors(
          ActivityManagerServiceClazz,
          new XC_MethodHook()
          {
            @Override
            protected final void beforeHookedMethod( final MethodHookParam param )
            {
              WriteLog( "hooked pre constructor" );
              Context context = (Context) param.args[ 0 ];

              try
              {
                XposedHelpers.callStaticMethod(
                    Class.forName( "android.os.ServiceManager" ),
                    "addService",
                    new Class[]{ String.class, IBinder.class },
                    "xa.service",
                    new XAService( context )
                );
              }
              catch( ClassNotFoundException e )
              {
                e.printStackTrace();
              }

              WriteLog( "added hooked start service xa.service" );
            }

            @Override
            protected final void afterHookedMethod( final MethodHookParam param )
            {
              WriteLog( "hooked post constructor" );
            }
          }
      );
    }
  }
}
