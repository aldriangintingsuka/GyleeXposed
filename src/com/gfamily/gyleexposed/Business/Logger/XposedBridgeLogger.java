package com.gfamily.gyleexposed.Business.Logger;

import de.robv.android.xposed.XposedBridge;

public class XposedBridgeLogger implements ILogger
{
  private String _prefix;

  public XposedBridgeLogger( String prefix )
  {
    _prefix = prefix;
  }

  @Override
  public void LogInfo( String message )
  {
    XposedBridge.log( _prefix + message );
  }

  @Override
  public void LogError( Throwable throwable )
  {
    XposedBridge.log( _prefix + throwable );
  }
}
