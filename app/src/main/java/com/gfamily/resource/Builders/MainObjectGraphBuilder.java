package com.gfamily.resource.Builders;

import java.util.*;

import com.gfamily.common.logger.XposedBridgeLogger;
import com.gfamily.resource.Business.Managers.ModManager;
import com.gfamily.resource.Business.Managers.ScriptManager;

public class MainObjectGraphBuilder
{
  private String _className;
  private String _scriptDirectoryName;
  private static HashMap<String, Object> _objectMap;

  public MainObjectGraphBuilder( String mainClassName, String scriptDirectoryName )
  {
    _className = mainClassName;
    _scriptDirectoryName = scriptDirectoryName;
    _objectMap = new HashMap<String, Object>();
  }

  public void BuildObjects()
  {
    XposedBridgeLogger logger = new XposedBridgeLogger( _className + ": " );
    ScriptManager scriptManager = new ScriptManager( _scriptDirectoryName, logger );

    ModManager modManager = new ModManager( logger );

    _objectMap.put( "Logger", logger );
    _objectMap.put( "ScriptManager", scriptManager );
    _objectMap.put( "ModManager", modManager );
  }

  public static Object GetObject( String objectName )
  {
    return _objectMap.get( objectName );
  }
}
