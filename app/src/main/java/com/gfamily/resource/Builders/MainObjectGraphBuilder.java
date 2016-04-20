package com.gfamily.resource.Builders;

import java.util.*;

import com.gfamily.common.logger.XposedBridgeLogger;
import com.gfamily.resource.Business.Managers.ModManager;
import com.google.gson.Gson;

public class MainObjectGraphBuilder
{
  private String _className;
  private static HashMap<String, Object> _objectMap;

  public MainObjectGraphBuilder( String mainClassName )
  {
    _className = mainClassName;
    _objectMap = new HashMap<>();
  }

  public void BuildObjects()
  {
    XposedBridgeLogger logger = new XposedBridgeLogger( _className + ": " );
    ModManager modManager = new ModManager( logger );
    Gson jsonParser = new Gson();

    _objectMap.put( "Logger", logger );
    _objectMap.put( "ModManager", modManager );
    _objectMap.put( "JsonParser", jsonParser );
  }

  public Object GetObject( String objectName )
  {
    return _objectMap.get( objectName );
  }
}
