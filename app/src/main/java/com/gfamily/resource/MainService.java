package com.gfamily.resource;

import java.io.IOException;

import com.gfamily.resource.Builders.MainObjectGraphBuilder;
import com.gfamily.resource.Business.Managers.IScriptManager;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service
{
  private IScriptManager _scriptManager;

  @Override
  public IBinder onBind( Intent arg0 )
  {
    return null;
  }

  @Override
  public int onStartCommand( Intent intent, int flags, int startId )
  {
    Log.d( "com.gfamily.resource", "received intent" );

    Bundle extras = intent.getExtras();

    if( extras == null ) return START_NOT_STICKY;

    String script = extras.getString( "content" );
    Log.d( "com.gfamily.resource", "intent is received " + script );

    try
    {
      _scriptManager.LoadScripts();
      _scriptManager.ParseScripts();
    }
    catch( IOException e )
    {
      e.printStackTrace();
    }

    return START_NOT_STICKY;
  }

  @Override
  public void onCreate()
  {
    Log.d( "com.gfamily.resource", "service started" );
    _scriptManager = (IScriptManager) MainObjectGraphBuilder.GetObject( "ScriptManager" );
  }
}
