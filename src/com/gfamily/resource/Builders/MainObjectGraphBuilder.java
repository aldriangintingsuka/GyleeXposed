package com.gfamily.resource.Builders;

import java.util.*;

import com.gfamily.common.logger.XposedBridgeLogger;
import com.gfamily.resource.Business.Managers.ModManager;
import com.gfamily.resource.Business.Managers.ScriptManager;

public class MainObjectGraphBuilder
{
  private String _className;

  public MainObjectGraphBuilder( String mainClassName )
  {
    _className = mainClassName;
  }

  public Map<String, Object> BuildObjects()
  {
    HashMap<String, Object> objects = new HashMap<String, Object>();

    XposedBridgeLogger logger = new XposedBridgeLogger( _className + ": " );
    ScriptManager scriptManager = new ScriptManager( logger );

    ModManager modManager = new ModManager( logger );

    objects.put( "Logger", logger );
    objects.put( "ScriptManager", scriptManager );
    objects.put( "ModManager", modManager );

    return objects;
  }
}
