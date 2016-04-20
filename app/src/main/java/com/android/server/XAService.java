package com.android.server;

import android.content.Context;
import android.os.Environment;
import android.os.IXAService;
import android.os.RemoteException;
import android.util.Log;
import com.gfamily.resource.Business.Managers.IScriptManager;
import com.gfamily.resource.Business.Managers.ScriptManager;
import com.gfamily.resource.Model.GyleeScript;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;
import java.util.Map;

public class XAService extends IXAService.Stub
{
  private Gson _jsonParser;
  private String _packageName;
  private IScriptManager _scriptManager;

  public XAService( Context context )
  {
    Context mContext = context;
    _jsonParser = new Gson();
    _packageName = "com.gfamily.resource";
  }

  private void SystemReady()
  {
    Log.d( _packageName, "starting service here" );

    try
    {
      File externalStorageDirectory = Environment.getExternalStorageDirectory();
      Log.d( _packageName, "Environment setting for external data storage is " + externalStorageDirectory );

      if( externalStorageDirectory.getPath().equals( "/dev/null" ) )
        return;

      File scriptDirectory = new File( externalStorageDirectory, _packageName + "/Mods" );

      _scriptManager = new ScriptManager( externalStorageDirectory.getPath(), scriptDirectory.getPath() );
      _scriptManager.LoadScripts();
      _scriptManager.ParseScripts();
    }
    catch( Exception e )
    {
      _scriptManager = null;
      //e.printStackTrace();
    }
  }

  @Override
  public String GetScript( String packageName ) throws RemoteException
  {
    if( _scriptManager == null )
    {
      SystemReady();

      if( _scriptManager == null )
        return null;
    }

    Map<String, List<GyleeScript>> scriptMap = DoGetScript( packageName );
    String jsonResult = scriptMap.size() == 0 ? null : _jsonParser.toJson( scriptMap );

    return jsonResult;
  }

  private Map<String, List<GyleeScript>> DoGetScript( String packageName )
  {
    Map<String, List<GyleeScript>> scriptMap = _scriptManager.GetScript( packageName );

    return scriptMap;
  }
}
