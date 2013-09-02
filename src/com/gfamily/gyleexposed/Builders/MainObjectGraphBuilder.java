package com.gfamily.gyleexposed.Builders;

import java.util.*;

import com.gfamily.gyleexposed.Business.Logger.XposedBridgeLogger;
import com.gfamily.gyleexposed.Business.Managers.ModManager;
import com.gfamily.gyleexposed.Business.Managers.ScriptManager;

public class MainObjectGraphBuilder
{
  private String _className;

  public MainObjectGraphBuilder( String mainClassName )
  {
    _className = mainClassName;
  }

  public Map<String, Object> BuildObjects()
  {
    Hashtable<String, Object> objects = new Hashtable<String, Object>();

    XposedBridgeLogger logger = new XposedBridgeLogger( _className );
    ScriptManager scriptManager = new ScriptManager( logger );
    
    ModManager modManager = new ModManager( logger );

    objects.put( "Logger", logger );
    objects.put( "ScriptManager", scriptManager );
    objects.put( "ModManager", modManager );

    return objects;
  }
}
