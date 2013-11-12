package com.gfamily.gyleexposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gfamily.gyleexposed.Builders.MainObjectGraphBuilder;
import com.gfamily.gyleexposed.Business.Logger.ILogger;
import com.gfamily.gyleexposed.Business.Managers.IModManager;
import com.gfamily.gyleexposed.Business.Managers.IScriptManager;
import com.gfamily.gyleexposed.Model.GyleeScript;

import android.content.pm.ApplicationInfo;
import android.content.res.XResources;
import android.os.Environment;
import android.telephony.SignalStrength;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GyleeXposed implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage
{
  private String _modulePath;
  private String _packageName;
  private ILogger _logger;
  private IScriptManager _scriptManager;
  private IModManager _modManager;
  private Map<String, ApplicationInfo> _applicationInfoMap;

  public GyleeXposed()
  {
    _applicationInfoMap = new HashMap<String, ApplicationInfo>();
//    BuildObjects();
//
//    try
//    {
//      InitializeFromSettings();
//    }
//    catch( Exception e )
//    {
//      WriteLog( e.getMessage() );
//    }
  }

  private void InitializeFromSettings() throws Exception
  {
    File externalStorageDirectory = GetExternalStorageDirectory();
    File scriptDirectory = new File( externalStorageDirectory, _packageName + "/Mods" );
    String scriptFileName = "ModScript.txt";

    _scriptManager.LoadScript( scriptDirectory.getPath(), scriptFileName );
    _scriptManager.ParseScript();
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
     BuildObjects();

    _modulePath = startupParam.modulePath;
    WriteLog( "Handle init zygote : " + _modulePath );

    try
    {
       InitializeFromSettings();
    }
    catch( Exception e )
    {
      WriteLog( e.getMessage() );
    }
  }

  @Override
  public void handleInitPackageResources( InitPackageResourcesParam resparam ) throws Throwable
  {
    LoadFromScripts( resparam );
  }

  @Override
  public void handleLoadPackage( LoadPackageParam lpparam ) throws Throwable
  {
    _applicationInfoMap.put( lpparam.packageName, lpparam.appInfo );
    HookSignalLevel( lpparam );
  }

  private void BuildObjects()
  {
    _packageName = getClass().getPackage().getName();
    MainObjectGraphBuilder builder = new MainObjectGraphBuilder( _packageName + ": " );
    Map<String, Object> objects = builder.BuildObjects();

    _logger = (ILogger) objects.get( "Logger" );
    _scriptManager = (IScriptManager) objects.get( "ScriptManager" );
    _modManager = (IModManager) objects.get( "ModManager" );
  }

  private void HookSignalLevel( LoadPackageParam lpparam )
  {
    if( !lpparam.packageName.equals( "com.android.systemui" ) ) return;

    WriteLog( "Loading SystemUI for hook" );

    // correction for signal strength level
    XC_MethodHook getLevelHook = new XC_MethodHook()
      {
        @Override
        protected void afterHookedMethod( MethodHookParam param ) throws Throwable
        {
          int correctedLevel = GetCorrectedLevel( (Integer) param.getResult() );
          // WriteLog( "New level = " + correctedLevel );
          param.setResult( correctedLevel );
        }

        private int GetCorrectedLevel( int level )
        {
          // WriteLog( "Old level = " + level );

          // value was overridden by our more specific method already
          if( level >= 10000 ) return level - 10000;

          // int newLevel = (int) Math.round( level * 6 / 4.0 );
          int newLevel = level;

          return newLevel;
        }
      };

    findAndHookMethod( SignalStrength.class, "getLevel", getLevelHook );

    XC_MethodHook getGsmLevelHook = new XC_MethodHook()
      {
        @Override
        protected void beforeHookedMethod( MethodHookParam param ) throws Throwable
        {
          int asu = ( (SignalStrength) param.thisObject ).getGsmSignalStrength();
          int correctedGsmSignalLevel = GetSignalLevel( asu );
          // WriteLog( "New GSM level = " + correctedGsmSignalLevel );
          param.setResult( correctedGsmSignalLevel );
        }

        private int GetSignalLevel( int asu )
        {
          // WriteLog( "Old GSM level = " + asu );

          if( asu == 99 ) return 10000;

          asu = asu >= 12 ? 12 : asu;
          int newGsmLevel = 10000 + asu / 2;

          return newGsmLevel;
        };
      };

    findAndHookMethod( SignalStrength.class, "getGsmLevel", getGsmLevelHook );
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
